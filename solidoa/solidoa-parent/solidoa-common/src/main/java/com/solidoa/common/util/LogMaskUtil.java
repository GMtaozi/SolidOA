package com.solidoa.common.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 日志脱敏工具类
 * 支持常见敏感字段自动脱敏
 */
public class LogMaskUtil {

    // 手机号正则
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\d{3})\\d{4}(\\d{4})");
    private static final String PHONE_REPLACEMENT = "$1****$2";

    // 身份证正则
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{4})\\d{10}(\\d{4})");
    private static final String ID_CARD_REPLACEMENT = "$1**********$2";

    // 邮箱正则（用户名最长40位，域名最长60位）
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(\\w{2})\\w{1,40}(@\\w{1,30}\\.\\w{1,20})");
    private static final String EMAIL_REPLACEMENT = "$1***$2";

    // 银行卡正则（16-19位：4位前缀 + 8-11位中间 + 4位后缀）
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("(\\d{4})\\d{8,11}(\\d{4})");
    private static final String BANK_CARD_REPLACEMENT = "$1********$2";

    // JWT Token 正则（前缀必选，避免误匹配普通字符串，token最长2000位）
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(Bearer\\s+|token=)([a-zA-Z0-9\\-_\\.]{1,2000})");
    private static final String TOKEN_REPLACEMENT = "$1****";

    // 密码正则
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("([\"']?password[\"']?\\s*[:=]\\s*[\"'])([^\"']+)([\"'])",
            Pattern.CASE_INSENSITIVE);
    private static final String PASSWORD_REPLACEMENT = "$1******$3";

    /**
     * 敏感字段映射表（用于 JSON 对象脱敏）
     */
    private static final Map<String, String> FIELD_MASK_MAP = new ConcurrentHashMap<>();

    static {
        // 姓名相关
        FIELD_MASK_MAP.put("name", "name");
        FIELD_MASK_MAP.put("realName", "name");
        FIELD_MASK_MAP.put("username", "name");

        // 联系方式
        FIELD_MASK_MAP.put("mobile", "phone");
        FIELD_MASK_MAP.put("phone", "phone");
        FIELD_MASK_MAP.put("tel", "phone");
        FIELD_MASK_MAP.put("email", "email");

        // 证件号
        FIELD_MASK_MAP.put("idCard", "idCard");
        FIELD_MASK_MAP.put("idNumber", "idCard");
        FIELD_MASK_MAP.put("identity", "idCard");

        // 银行相关
        FIELD_MASK_MAP.put("bankCard", "bankCard");
        FIELD_MASK_MAP.put("cardNo", "bankCard");
        FIELD_MASK_MAP.put("accountNo", "bankCard");

        // 密码密钥
        FIELD_MASK_MAP.put("password", "fixed:******");
        FIELD_MASK_MAP.put("pwd", "fixed:******");
        FIELD_MASK_MAP.put("secret", "fixed:******");
        FIELD_MASK_MAP.put("secretKey", "fixed:******");
        FIELD_MASK_MAP.put("appSecret", "fixed:******");
        FIELD_MASK_MAP.put("token", "fixed:******");
        FIELD_MASK_MAP.put("accessToken", "fixed:******");
    }

    /**
     * 脱敏手机号
     */
    public static String maskPhone(String phone) {
        if (phone == null) return null;
        return PHONE_PATTERN.matcher(phone).replaceAll(PHONE_REPLACEMENT);
    }

    /**
     * 脱敏身份证号
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null) return null;
        return ID_CARD_PATTERN.matcher(idCard).replaceAll(ID_CARD_REPLACEMENT);
    }

    /**
     * 脱敏邮箱
     */
    public static String maskEmail(String email) {
        if (email == null) return null;
        return EMAIL_PATTERN.matcher(email).replaceAll(EMAIL_REPLACEMENT);
    }

    /**
     * 脱敏银行卡号
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null) return null;
        return BANK_CARD_PATTERN.matcher(bankCard).replaceAll(BANK_CARD_REPLACEMENT);
    }

    /**
     * 脱敏 Token
     */
    public static String maskToken(String token) {
        if (token == null) return null;
        return TOKEN_PATTERN.matcher(token).replaceAll(TOKEN_REPLACEMENT);
    }

    /**
     * 脱敏密码
     */
    public static String maskPassword(String content) {
        if (content == null) return null;
        return PASSWORD_PATTERN.matcher(content).replaceAll(PASSWORD_REPLACEMENT);
    }

    /**
     * 对日志内容进行综合脱敏
     */
    public static String mask(String content) {
        if (content == null) return null;

        String masked = content;
        masked = maskPhone(masked);
        masked = maskIdCard(masked);
        masked = maskEmail(masked);
        masked = maskBankCard(masked);
        masked = maskToken(masked);
        masked = maskPassword(masked);

        return masked;
    }

    /**
     * 获取字段脱敏类型
     */
    public static String getMaskType(String fieldName) {
        return FIELD_MASK_MAP.get(fieldName.toLowerCase());
    }

    /**
     * 批量脱敏 Map 中的敏感字段
     */
    public static Map<String, Object> maskMapFields(Map<String, Object> data) {
        if (data == null) return null;

        Map<String, Object> masked = new ConcurrentHashMap<>(data);
        for (Map.Entry<String, Object> entry : masked.entrySet()) {
            String maskType = getMaskType(entry.getKey());
            if (maskType != null && entry.getValue() instanceof String value) {
                if ("phone".equals(maskType)) {
                    masked.put(entry.getKey(), maskPhone(value));
                } else if ("idCard".equals(maskType)) {
                    masked.put(entry.getKey(), maskIdCard(value));
                } else if ("email".equals(maskType)) {
                    masked.put(entry.getKey(), maskEmail(value));
                } else if ("bankCard".equals(maskType)) {
                    masked.put(entry.getKey(), maskBankCard(value));
                } else if ("name".equals(maskType)) {
                    masked.put(entry.getKey(), maskName(value));
                } else if (maskType.startsWith("fixed:")) {
                    masked.put(entry.getKey(), maskType.substring(6));
                }
            }
        }
        return masked;
    }

    /**
     * 脱敏姓名（保留首尾）
     */
    private static String maskName(String name) {
        if (name == null || name.length() < 2) return "**";
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }
}