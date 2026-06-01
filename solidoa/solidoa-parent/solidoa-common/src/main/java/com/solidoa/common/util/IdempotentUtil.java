package com.solidoa.common.util;

/**
 * 幂等性工具类
 */
public class IdempotentUtil {

    /**
     * 生成业务幂等键
     * @param businessType 业务类型（如 LEAVE, EXPENSE）
     * @param businessId 业务ID
     * @param action 操作类型（如 CREATE, APPROVE, CANCEL）
     */
    public static String generateKey(String businessType, Long businessId, String action) {
        return String.format("%s:%d:%s", businessType, businessId, action);
    }

    /**
     * 生成消息幂等键
     * @param messageType 消息类型
     * @param messageId 消息ID
     */
    public static String generateMessageKey(String messageType, String messageId) {
        return String.format("msg:%s:%s", messageType, messageId);
    }

    /**
     * 生成 API 幂等键
     * @param userId 用户ID
     * @param apiPath API 路径
     * @param requestId 请求ID（客户端生成）
     */
    public static String generateApiKey(Long userId, String apiPath, String requestId) {
        return String.format("api:%d:%s:%s", userId, apiPath, requestId);
    }
}