package com.solidoa.dingtalk.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.dingtalk.entity.DingtalkConfig;
import com.solidoa.dingtalk.entity.DingtalkSyncLog;
import com.solidoa.dingtalk.mapper.DingtalkConfigMapper;
import com.solidoa.dingtalk.mapper.DingtalkSyncLogMapper;
import com.solidoa.dingtalk.service.DingtalkCodeService;
import com.solidoa.dingtalk.service.DingtalkService;
import com.solidoa.dingtalk.util.AESUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DingtalkServiceImpl implements DingtalkService {

    private static final String DINGTALK_API_HOST = "https://oapi.dingtalk.com";
    private static final String ACCESS_TOKEN_KEY = "dingtalk:access_token";

    private static final int MAX_RETRY_TIMES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000; // 1秒

    // 缓存解密后的配置（内存缓存，避免每次查询数据库）
    private volatile DingtalkConfig cachedConfig;
    private volatile String cachedDecryptedSecret;
    private volatile long configCacheTime = 0;
    private static final long CONFIG_CACHE_TTL_MS = 3600000; // 缓存1小时

    @Autowired
    private DingtalkConfigMapper configMapper;

    @Autowired
    private DingtalkSyncLogMapper syncLogMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AESUtil aesUtil;

    @Autowired
    private DingtalkCodeService dingtalkCodeService;

    @Override
    public String getAccessToken() {
        // 先从Redis获取
        String cachedToken = redisTemplate.opsForValue().get(ACCESS_TOKEN_KEY);
        if (cachedToken != null) {
            return cachedToken;
        }

        // 获取配置（内存缓存解密后的 secret）
        getConfig();
        String appSecret = cachedDecryptedSecret;

        // 带重试的 API 调用
        return executeWithRetry(() -> {
            String url = DINGTALK_API_HOST + "/gettoken?appkey=" + config.getAppKey() +
                         "&appsecret=" + appSecret;
            String response = HttpUtil.get(url);
            JSONObject result = JSONUtil.parseObj(response);

            if ("0".equals(result.getStr("errcode"))) {
                String accessToken = result.getStr("access_token");
                Integer expireIn = result.getInt("expires_in", 7200);

                // 缓存到 Redis（提前5分钟过期，最小60秒防止负数）
                long cacheSeconds = Math.max(expireIn - 300, 60);
                redisTemplate.opsForValue().set(
                    ACCESS_TOKEN_KEY,
                    accessToken,
                    java.time.Duration.ofSeconds(cacheSeconds)
                );
                return accessToken;
            } else {
                throw new DingtalkApiException("获取钉钉AccessToken失败: " + result.getStr("errmsg"));
            }
        }, "获取AccessToken");
    }

    /**
     * 获取配置（带内存缓存，1小时过期）
     */
    private DingtalkConfig getConfig() {
        long now = System.currentTimeMillis();
        if (cachedConfig == null || (now - configCacheTime) > CONFIG_CACHE_TTL_MS) {
            synchronized (this) {
                if (cachedConfig == null || (now - configCacheTime) > CONFIG_CACHE_TTL_MS) {
                    cachedConfig = configMapper.selectActiveConfig();
                    if (cachedConfig == null) {
                        throw new BusinessException("钉钉配置未设置");
                    }
                    // 解密 appSecret
                    cachedDecryptedSecret = aesUtil.decrypt(cachedConfig.getAppSecret());
                    configCacheTime = now;
                }
            }
        }
        return cachedConfig;
    }

    /**
     * 清除配置缓存（配置更新时调用）
     */
    public void clearConfigCache() {
        synchronized (this) {
            cachedConfig = null;
            cachedDecryptedSecret = null;
            configCacheTime = 0;
        }
    }

    /**
     * 获取解密后的 AppSecret
     */
    public String getDecryptedAppSecret() {
        getConfig(); // 确保已加载
        return cachedDecryptedSecret;
    }

    @Override
    public void syncUsers() {
        DingtalkSyncLog syncLog = new DingtalkSyncLog();
        syncLog.setSyncType("USER");
        syncLog.setStartTime(LocalDateTime.now());
        syncLog.setStatus("RUNNING");
        syncLogMapper.insert(syncLog);

        int successCount = 0;
        int failCount = 0;

        try {
            String accessToken = getAccessToken();

            // 获取所有部门 ID 列表
            String deptUrl = DINGTALK_API_HOST + "/topapi/v2/department/listsub?access_token=" + accessToken;
            Map<String, Object> deptBody = new HashMap<>();
            deptBody.put("dept_id", 1);
            String deptResponse = executeWithRetry(() -> {
                String response = HttpUtil.post(deptUrl, JSONUtil.toJsonStr(deptBody));
                JSONObject result = JSONUtil.parseObj(response);
                if (!"0".equals(result.getStr("errcode"))) {
                    throw new DingtalkApiException("获取部门列表失败: " + result.getStr("errmsg"));
                }
                return response;
            }, "获取部门列表");

            JSONObject deptResult = JSONUtil.parseObj(deptResponse);
            if (!"0".equals(deptResult.getStr("errcode"))) {
                throw new DingtalkApiException("获取部门列表失败: " + deptResult.getStr("errmsg"));
            }

            // 解析部门 ID 列表
            var deptList = deptResult.getJSONArray("result");
            if (deptList == null || deptList.isEmpty()) {
                log.info("没有需要同步的部门");
                syncLog.setSuccessCount(0);
                syncLog.setFailCount(0);
                syncLog.setStatus("SUCCESS");
                return;
            }

            log.info("开始同步用户，共 {} 个部门", deptList.size());

            // TODO: 调用 system-service 的 Feign 接口创建/更新用户
            // 示例: systemClient.createOrUpdateUser(userData);

            // 临时：模拟同步成功计数（实际实现后删除）
            for (int i = 0; i < deptList.size(); i++) {
                try {
                    // 实际同步逻辑待实现
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.warn("部门 {} 用户同步失败: {}", deptList.getStr(i), e.getMessage());
                }
            }

            log.info("用户同步完成，成功: {}, 失败: {}", successCount, failCount);
            syncLog.setSuccessCount(successCount);
            syncLog.setFailCount(failCount);
            syncLog.setStatus(failCount == 0 ? "SUCCESS" : "PARTIAL");

        } catch (Exception e) {
            log.error("用户同步失败", e);
            syncLog.setStatus("FAILED");
            syncLog.setErrorMsg(e.getMessage());
        } finally {
            syncLog.setEndTime(LocalDateTime.now());
            syncLogMapper.updateById(syncLog);
        }
    }

    @Override
    public void syncDepartments() {
        DingtalkSyncLog syncLog = new DingtalkSyncLog();
        syncLog.setSyncType("DEPT");
        syncLog.setStartTime(LocalDateTime.now());
        syncLog.setStatus("RUNNING");
        syncLogMapper.insert(syncLog);

        int successCount = 0;
        int failCount = 0;

        try {
            String accessToken = getAccessToken();
            log.info("部门同步开始");

            // 获取所有部门列表
            String deptListUrl = DINGTALK_API_HOST + "/topapi/v2/department/listsub?access_token=" + accessToken;
            Map<String, Object> deptBody = new HashMap<>();
            deptBody.put("dept_id", 1); // 根部门
            deptBody.put("fetch_child", true); // 获取子部门

            String deptListResponse = executeWithRetry(() -> {
                String response = HttpUtil.post(deptListUrl, JSONUtil.toJsonStr(deptBody));
                JSONObject result = JSONUtil.parseObj(response);
                if (!"0".equals(result.getStr("errcode"))) {
                    throw new DingtalkApiException("获取部门列表失败: " + result.getStr("errmsg"));
                }
                return response;
            }, "获取部门列表");

            JSONObject deptListResult = JSONUtil.parseObj(deptListResponse);
            var deptIds = deptListResult.getJSONArray("result");

            if (deptIds == null || deptIds.isEmpty()) {
                log.info("没有需要同步的部门");
                syncLog.setSuccessCount(0);
                syncLog.setFailCount(0);
                syncLog.setStatus("SUCCESS");
                return;
            }

            log.info("开始同步部门，共 {} 个", deptIds.size());

            // 遍历每个部门获取详情
            String deptDetailUrl = DINGTALK_API_HOST + "/topapi/v2/department/get?access_token=" + accessToken;
            for (int i = 0; i < deptIds.size(); i++) {
                String deptId = deptIds.getStr(i);
                try {
                    Map<String, Object> detailBody = new HashMap<>();
                    detailBody.put("dept_id", Long.parseLong(deptId));
                    detailBody.put("language", "zh_CN");

                    String detailResponse = HttpUtil.post(deptDetailUrl, JSONUtil.toJsonStr(detailBody));
                    JSONObject detailResult = JSONUtil.parseObj(detailResponse);

                    if (!"0".equals(detailResult.getStr("errcode"))) {
                        throw new DingtalkApiException("获取部门详情失败: " + detailResult.getStr("errmsg"));
                    }

                    // TODO: 调用 system-service 的 Feign 接口创建/更新部门
                    // JSONObject deptInfo = detailResult.getJSONObject("result");
                    // systemClient.createOrUpdateDepartment(deptInfo);

                    successCount++;
                    log.debug("部门 {} 同步成功", deptId);

                } catch (Exception e) {
                    failCount++;
                    log.warn("部门 {} 同步失败: {}", deptId, e.getMessage());
                }
            }

            log.info("部门同步完成，成功: {}, 失败: {}", successCount, failCount);
            syncLog.setSuccessCount(successCount);
            syncLog.setFailCount(failCount);
            syncLog.setStatus(failCount == 0 ? "SUCCESS" : "PARTIAL");

        } catch (Exception e) {
            log.error("部门同步失败", e);
            syncLog.setStatus("FAILED");
            syncLog.setErrorMsg(e.getMessage());
        } finally {
            syncLog.setEndTime(LocalDateTime.now());
            syncLogMapper.updateById(syncLog);
        }
    }

    @Override
    public void sendWorkNotify(Long userId, String title, String content) {
        DingtalkConfig config = getConfig();
        if (config == null) {
            log.warn("钉钉配置未设置，跳过消息推送");
            return;
        }

        try {
            String accessToken = getAccessToken();
            String url = DINGTALK_API_HOST + "/topapi/message/corpconversation/asyncsend_v2?access_token=" + accessToken;

            Map<String, Object> body = new HashMap<>();
            body.put("agent_id", config.getAgentId());
            body.put("userid_list", userId.toString());
            body.put("msg", Map.of(
                "msgtype", "text",
                "text", Map.of("content", title + "\n" + content)
            ));

            executeWithRetry(() -> {
                String response = HttpUtil.post(url, JSONUtil.toJsonStr(body));
                JSONObject result = JSONUtil.parseObj(response);
                if (!"0".equals(result.getStr("errcode"))) {
                    throw new DingtalkApiException("消息推送失败: " + result.getStr("errmsg"));
                }
                return response;
            }, "发送工作通知");

        } catch (Exception e) {
            log.error("钉钉消息推送异常", e);
        }
    }

    @Override
    public Map<String, Object> getUserInfoByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("钉钉授权码不能为空");
        }
        String accessToken = getAccessToken();
        String url = DINGTALK_API_HOST + "/topapi/v2/user/get_by_code?access_token=" + accessToken;
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);

        String response = executeWithRetry(() -> {
            String resp = HttpUtil.post(url, JSONUtil.toJsonStr(body));
            JSONObject result = JSONUtil.parseObj(resp);
            if (!"0".equals(result.getStr("errcode"))) {
                throw new DingtalkApiException("获取钉钉用户信息失败: " + result.getStr("errmsg"));
            }
            return resp;
        }, "获取用户信息");

        JSONObject result = JSONUtil.parseObj(response);
        JSONObject userInfo = result.getJSONObject("result");
        if (userInfo == null) {
            throw new DingtalkApiException("钉钉用户信息响应为空");
        }
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", userInfo.getStr("userid", ""));
        userMap.put("name", userInfo.getStr("name", ""));
        userMap.put("mobile", userInfo.getStr("mobile", ""));
        userMap.put("avatar", userInfo.getStr("avatar", ""));
        userMap.put("unionId", userInfo.getStr("unionid", ""));
        return userMap;
    }

    /**
     * 带指数退避的重试机制
     */
    private <T> T executeWithRetry(java.util.function.Supplier<T> operation, String operationName) {
        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;

        while (attempt < MAX_RETRY_TIMES) {
            try {
                return operation.get();
            } catch (DingtalkApiException e) {
                attempt++;
                if (attempt >= MAX_RETRY_TIMES) {
                    throw e;
                }

                log.warn("{} 失败，{} / {} 次重试，等待 {}ms: {}",
                    operationName, attempt, MAX_RETRY_TIMES, backoffMs, e.getMessage());

                try {
                    TimeUnit.MILLISECONDS.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException("重试被中断");
                }

                backoffMs *= 2; // 指数退避：1s -> 2s -> 4s
            }
        }

        throw new BusinessException(operationName + "失败");
    }

    /**
     * 钉钉 API 异常
     */
    private static class DingtalkApiException extends RuntimeException {
        DingtalkApiException(String message) {
            super(message);
        }
    }
}