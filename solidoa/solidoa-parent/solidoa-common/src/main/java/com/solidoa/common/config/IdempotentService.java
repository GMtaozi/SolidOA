package com.solidoa.common.config;

import com.solidoa.common.util.IdempotentUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 幂等性服务
 * 基于 Redis 实现接口/消息幂等
 */
@Component
@Slf4j
public class IdempotentService {

    private final StringRedisTemplate redisTemplate;

    private static final String IDEMPOTENT_PREFIX = "idempotent:";
    private static final String LOCK_PREFIX = "idempotent:lock:";
    private static final String RETRY_PREFIX = "idempotent:retry:";
    private static final long DEFAULT_EXPIRE_SECONDS = 300; // 5分钟
    private static final int DEFAULT_MAX_RETRY_COUNT = 3; // 默认最大重试次数

    /** Lua脚本：原子检查并删除锁（仅当值匹配时才删除，防止误删其他线程的锁） */
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    /** 本地存储锁的唯一值及过期时间，用于释放时验证所有权 */
    private final ConcurrentHashMap<String, LockEntry> lockValueStore = new ConcurrentHashMap<>();

    /** 锁条目，包含锁值和过期时间戳 */
    private static class LockEntry {
        final String lockValue;
        final long expireTimeMs;

        LockEntry(String lockValue, long expireTimeMs) {
            this.lockValue = lockValue;
            this.expireTimeMs = expireTimeMs;
        }
    }

    public IdempotentService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        // 启动定时清理任务，每60秒清理一次过期条目
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "idempotent-lock-cleanup");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(this::cleanupExpiredEntries, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * 清理已过期的锁条目，防止内存泄漏
     */
    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        lockValueStore.entrySet().removeIf(entry -> {
            if (entry.getValue().expireTimeMs < now) {
                log.debug("清理过期锁条目: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * 检查操作是否已执行（幂等检查）
     * 注意：单独调用存在 TOCTOU 竞态，推荐改用 {@link #tryExecute(String)} 或 {@link #tryExecute(String, long)}
     * @param key 幂等键
     * @return true=已执行（应跳过）, false=未执行（可继续）
     */
    public boolean isExecuted(String key) {
        String redisKey = IDEMPOTENT_PREFIX + key;
        return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
    }

    /**
     * 标记操作已执行
     * @param key 幂等键
     * @param expireSeconds 过期时间（秒）
     */
    public void markExecuted(String key, long expireSeconds) {
        String redisKey = IDEMPOTENT_PREFIX + key;
        redisTemplate.opsForValue().set(redisKey, "1", expireSeconds, TimeUnit.SECONDS);
    }

    /**
     * 标记操作已执行（默认5分钟过期）
     */
    public void markExecuted(String key) {
        markExecuted(key, DEFAULT_EXPIRE_SECONDS);
    }

    /**
     * 原子性地尝试执行操作（检查+标记合一，无竞态）
     * @param key 幂等键
     * @param expireSeconds 过期时间
     * @return true=首次执行（可继续业务逻辑）, false=已执行过（应跳过）
     */
    public boolean tryExecute(String key, long expireSeconds) {
        String redisKey = IDEMPOTENT_PREFIX + key;
        Boolean result = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 原子性地尝试执行操作（默认5分钟过期）
     */
    public boolean tryExecute(String key) {
        return tryExecute(key, DEFAULT_EXPIRE_SECONDS);
    }

    /**
     * 尝试获取执行锁（原子操作）
     * @param key 幂等键
     * @param expireSeconds 过期时间
     * @return true=获取成功（可执行）, false=已存在（不可重复执行）
     */
    public boolean tryLock(String key, long expireSeconds) {
        String redisKey = LOCK_PREFIX + key;
        String lockValue = UUID.randomUUID().toString();
        Boolean result = redisTemplate.opsForValue().setIfAbsent(redisKey, lockValue, expireSeconds, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(result)) {
            long expireTimeMs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expireSeconds);
            lockValueStore.put(redisKey, new LockEntry(lockValue, expireTimeMs));
        }
        return Boolean.TRUE.equals(result);
    }

    /**
     * 尝试获取执行锁（默认5分钟过期）
     */
    public boolean tryLock(String key) {
        return tryLock(key, DEFAULT_EXPIRE_SECONDS);
    }

    /**
     * 释放执行锁（Lua脚本原子检查并删除，防止误删其他线程的锁）
     */
    public void releaseLock(String key) {
        String redisKey = LOCK_PREFIX + key;
        LockEntry entry = lockValueStore.get(redisKey);
        if (entry == null) {
            return;
        }
        redisTemplate.execute(RELEASE_LOCK_SCRIPT, Collections.singletonList(redisKey), entry.lockValue);
        lockValueStore.remove(redisKey);
    }

    /**
     * 生成业务幂等键
     * @param businessType 业务类型（如 LEAVE, EXPENSE）
     * @param businessId 业务ID
     * @param action 操作类型（如 CREATE, APPROVE, CANCEL）
     */
    public String generateKey(String businessType, Long businessId, String action) {
        return IdempotentUtil.generateKey(businessType, businessId, action);
    }

    /**
     * 获取消息重试次数
     * @param messageId 消息ID
     * @return 当前重试次数
     */
    public int getRetryCount(String messageId) {
        String redisKey = RETRY_PREFIX + messageId;
        String count = redisTemplate.opsForValue().get(redisKey);
        return count != null ? Integer.parseInt(count) : 0;
    }

    /**
     * 增加消息重试次数
     * @param messageId 消息ID
     * @param expireSeconds 过期时间（秒）
     * @return 增加后的重试次数
     */
    public int incrementRetryCount(String messageId, long expireSeconds) {
        String redisKey = RETRY_PREFIX + messageId;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        // 设置过期时间
        redisTemplate.expire(redisKey, expireSeconds, TimeUnit.SECONDS);
        return count != null ? count.intValue() : 1;
    }

    /**
     * 检查是否超过最大重试次数
     * @param messageId 消息ID
     * @param maxRetryCount 最大重试次数
     * @return true=已超过最大重试次数，应进入死信队列
     */
    public boolean isMaxRetryExceeded(String messageId, int maxRetryCount) {
        int count = getRetryCount(messageId);
        return count >= maxRetryCount;
    }

    /**
     * 检查是否超过最大重试次数（使用默认次数）
     * @param messageId 消息ID
     * @return true=已超过最大重试次数，应进入死信队列
     */
    public boolean isMaxRetryExceeded(String messageId) {
        return isMaxRetryExceeded(messageId, DEFAULT_MAX_RETRY_COUNT);
    }

    /**
     * 清除消息重试计数（成功处理后调用）
     * @param messageId 消息ID
     */
    public void clearRetryCount(String messageId) {
        String redisKey = RETRY_PREFIX + messageId;
        redisTemplate.delete(redisKey);
    }
}