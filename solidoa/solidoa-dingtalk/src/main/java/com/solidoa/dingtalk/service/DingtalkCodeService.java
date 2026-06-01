package com.solidoa.dingtalk.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 钉钉 Code 防重放服务
 */
@Service
public class DingtalkCodeService {

    private static final String CODE_USED_PREFIX = "dingtalk:code:used:";
    private static final long CODE_EXPIRE_SECONDS = 300; // 5分钟过期

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 检查 code 是否已被使用（防重放）
     * @param code 钉钉授权码
     * @return true=已被使用(不可再用), false=可用
     */
    public boolean isCodeUsed(String code) {
        if (code == null || code.isBlank()) return false;
        String key = CODE_USED_PREFIX + code;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 标记 code 已使用（消费后不可再用）
     * @param code 钉钉授权码
     * @return true=标记成功(code未被使用过), false=标记失败(code已被使用)
     */
    public boolean markCodeUsed(String code) {
        if (code == null || code.isBlank()) return false;
        String key = CODE_USED_PREFIX + code;
        // 使用 setIfAbsent 原子操作，确保幂等
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "1", CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 验证并消费 code（检查+标记原子操作）
     * @param code 钉钉授权码
     * @return true=code有效且未被使用过, false=code已被使用
     */
    public boolean validateAndConsumeCode(String code) {
        return markCodeUsed(code);
    }
}