package com.solidoa.gateway.filter;

import com.solidoa.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import java.util.List;

@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtProperties jwtProperties;

    @Value("${gateway.internal-secret}")
    private String gatewaySecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 跳过无需认证的路径（exclusion 初始化为空列表，无需判空）
        if (jwtProperties.getExclusion().stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        // 从请求头获取Token
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            // 使用JJWT库验证JWT签名
            String secret = jwtProperties.getSecret();
            log.debug("JWT Secret length: {}, token length: {}", secret != null ? secret.length() : 0, token.length());
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 提取用户信息（手动类型检查，避免 ClassCastException）
            Long userId = parseUserId(claims.get("userId"));
            String username = claims.get("username", String.class);
            String deptId = claims.get("deptId", String.class);
            List<String> roles = parseRoles(claims.get("roles"));

            if (userId == null) {
                throw new RuntimeException("Invalid token: missing userId");
            }

            // 将用户信息传递给下游服务（使用时间戳nonce防止重放攻击）
            // 设置 X-Request-Source: EXTERNAL 使 FeignRequestInterceptor 能正确识别外部请求
            long timestamp = System.currentTimeMillis();
            String signature = generateSignature(String.valueOf(userId), timestamp);
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-User-Name", username != null ? username : "")
                    .header("X-User-DeptId", deptId != null ? deptId : "")
                    .header("X-User-Roles", roles != null ? String.join(",", roles) : "")
                    .header("X-Request-Source", "EXTERNAL")
                    .header("X-Gateway-Timestamp", String.valueOf(timestamp))
                    .header("X-Gateway-Signature", signature)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token已过期: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        } catch (Exception e) {
            log.error("JWT验证失败: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * 安全解析 userId，支持 Integer/Long/String 类型
     */
    private Long parseUserId(Object userIdObj) {
        if (userIdObj == null) {
            return null;
        }
        if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        }
        if (userIdObj instanceof String) {
            try {
                return Long.parseLong((String) userIdObj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 安全解析 roles，支持 List/LinkedHashMap 类型
     */
    @SuppressWarnings("unchecked")
    private List<String> parseRoles(Object rolesObj) {
        if (rolesObj == null) {
            return null;
        }
        if (rolesObj instanceof List) {
            return (List<String>) rolesObj;
        }
        // 兼容 JJWT 将 List 解析为 LinkedHashMap 的情况
        if (rolesObj instanceof java.util.Map) {
            try {
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) rolesObj;
                return new java.util.ArrayList<>(map.values()).stream()
                        .filter(v -> v instanceof String)
                        .map(v -> (String) v)
                        .toList();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 生成带时间戳nonce的签名，防止重放攻击
     * 签名格式: HmacSHA256(gatewaySecret, userId + timestamp)
     * @param userId 用户ID
     * @param timestamp 时间戳（毫秒）
     * @return 签名前32位十六进制字符串
     */
    private String generateSignature(String userId, long timestamp) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(
                    gatewaySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            // 拼接userId和时间戳，防止重放
            String data = userId + timestamp;
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().substring(0, 32);
        } catch (Exception e) {
            throw new RuntimeException("生成网关签名失败", e);
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}