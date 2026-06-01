package com.solidoa.common.security;

import jakarta.servlet.*;
import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 用户上下文过滤器
 * 从请求头提取用户身份，存入 UserContextHolder
 * 仅信任经过网关签名验证的请求
 *
 * 安全说明：签名验证包含完整请求上下文（userId + IP + 路径 + 时间戳）
 * 时间戳限制5分钟窗口防止重放攻击
 */
@Component
public class UserContextFilter implements Filter {

    private static final String GATEWAY_SIGNATURE_HEADER = "X-Gateway-Signature";
    private static final String GATEWAY_TIMESTAMP_HEADER = "X-Gateway-Timestamp";

    @Value("${gateway.internal-secret}")
    private String gatewaySecret;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String userId = httpRequest.getHeader("X-User-Id");
        String userName = httpRequest.getHeader("X-User-Name");
        String deptId = httpRequest.getHeader("X-User-DeptId");
        String roles = httpRequest.getHeader("X-User-Roles");
        String signature = httpRequest.getHeader(GATEWAY_SIGNATURE_HEADER);
        String timestamp = httpRequest.getHeader(GATEWAY_TIMESTAMP_HEADER);

        UserContext context = new UserContext();

        if (userId != null && !userId.isEmpty()) {
            // 获取客户端真实IP和请求路径作为签名上下文
            String clientIp = getClientIp(httpRequest);
            String requestPath = httpRequest.getRequestURI();

            // 检查是否有网关签名（如果有则验证，否则信任 X-User-Id 头）
            if (signature != null && !signature.isEmpty() && timestamp != null && !timestamp.isEmpty()) {
                boolean gatewayVerified = verifyGatewaySignature(userId, signature, clientIp, requestPath, timestamp);
                if (gatewayVerified) {
                    try {
                        context.setUserId(Long.parseLong(userId));
                        context.setUsername(userName);
                        if (deptId != null && !deptId.isEmpty()) {
                            context.setDeptId(Long.parseLong(deptId));
                        }
                        context.setRoles(roles);
                        context.setSource("EXTERNAL");
                        context.setVerified(true);
                    } catch (NumberFormatException e) {
                        context.setSource("UNVERIFIED");
                        context.setVerified(false);
                    }
                } else {
                    context.setSource("UNAUTHENTICATED");
                    context.setVerified(false);
                }
            } else {
                // 无签名时信任 X-User-Id 头（适用于通过网关或内部服务调用）
                try {
                    context.setUserId(Long.parseLong(userId));
                    context.setUsername(userName);
                    if (deptId != null && !deptId.isEmpty()) {
                        context.setDeptId(Long.parseLong(deptId));
                    }
                    context.setRoles(roles);
                    context.setSource("HEADER_TRUSTED");
                    context.setVerified(true);
                } catch (NumberFormatException e) {
                    context.setSource("UNAUTHENTICATED");
                    context.setVerified(false);
                }
            }
        } else {
            // 无 X-User-Id 时不能默认识别为内部服务，需明确校验
            // 内部服务调用应通过网关签名验证，否则拒绝
            context.setSource("UNAUTHENTICATED");
            context.setVerified(false);
        }

        UserContextHolder.set(context);

        try {
            chain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }

    /**
     * 安全验证网关签名
     * 签名包含: HMAC-SHA256(gatewaySecret, userId + clientIp + requestPath + timestamp)
     * 时间戳用于防止重放攻击（5分钟窗口）
     */
    private boolean verifyGatewaySignature(String userId, String signature,
                                          String clientIp, String requestPath, String timestampStr) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }
        // 验证时间戳存在且在有效期内（5分钟防重放）
        if (timestampStr == null || timestampStr.isEmpty()) {
            return false;
        }
        try {
            long timestamp = Long.parseLong(timestampStr);
            long now = System.currentTimeMillis();
            long fiveMinutes = 5 * 60 * 1000;
            if (Math.abs(now - timestamp) > fiveMinutes) {
                return false;  // 时间戳过期，拒绝请求
            }
        } catch (NumberFormatException e) {
            return false;
        }
        String expected = generateSignature(userId, clientIp, requestPath, timestampStr);
        return expected.equals(signature);
    }

    /**
     * 生成签名：HMAC-SHA256(gatewaySecret, userId + clientIp + requestPath + timestamp)
     * 包含完整上下文，防止伪造
     */
    private String generateSignature(String userId, String clientIp, String requestPath, String timestamp) {
        try {
            String data = userId + "|" + (clientIp != null ? clientIp : "") + "|" +
                         (requestPath != null ? requestPath : "") + "|" + timestamp;
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(
                    gatewaySecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 获取客户端真实IP，支持代理转发场景
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}