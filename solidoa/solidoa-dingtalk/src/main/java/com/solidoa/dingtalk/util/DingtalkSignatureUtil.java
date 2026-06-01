package com.solidoa.dingtalk.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 钉钉签名验证工具
 */
public class DingtalkSignatureUtil {

    /**
     * 验证钉钉回调签名
     * @param token 钉钉配置的 Token
     * @param timestamp 时间戳
     * @param nonce 随机字符串
     * @param echostr 加密字符串（验证时）
     * @param signature 请求中的签名
     * @return true=验证通过
     */
    public static boolean verifySignature(String token, String timestamp, String nonce, String echostr, String signature) {
        try {
            // 排序拼接
            String[] arr = {token, timestamp, nonce, echostr};
            java.util.Arrays.sort(arr);
            StringBuilder sb = new StringBuilder();
            for (String s : arr) {
                sb.append(s);
            }

            // SHA-1 加密
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));

            // 转为十六进制字符串
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String hexStr = Integer.toHexString(b & 0xff);
                if (hexStr.length() == 1) {
                    hex.append("0");
                }
                hex.append(hexStr);
            }

            return hex.toString().equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生成签名（用于测试）
     */
    public static String generateSignature(String token, String timestamp, String nonce, String echostr) {
        try {
            String[] arr = {token, timestamp, nonce, echostr};
            java.util.Arrays.sort(arr);
            StringBuilder sb = new StringBuilder();
            for (String s : arr) {
                sb.append(s);
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String hexStr = Integer.toHexString(b & 0xff);
                if (hexStr.length() == 1) {
                    hex.append("0");
                }
                hex.append(hexStr);
            }

            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("签名生成失败", e);
        }
    }
}