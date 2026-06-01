package com.solidoa.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Redis-based API Rate Limiter
 * 使用固定窗口算法实现分布式限流
 */
@Component
@Slf4j
public class RateLimiterFilter implements GlobalFilter, Ordered {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${rate-limiter.enabled:true}")
    private boolean enabled;

    @Value("${rate-limiter.default-limit:100}")
    private int defaultLimit;

    @Value("${rate-limiter.default-window-seconds:60}")
    private int defaultWindowSeconds;

    @Value("${rate-limiter.exclusions:/api/v1/auth/login,/api/v1/auth/captcha}")
    private List<String> exclusions;

    // Lua脚本实现原子性限流检查，返回当前计数（0表示超限）
    private static final String RATE_LIMIT_SCRIPT = """
        local key = KEYS[1]
        local limit = tonumber(ARGV[1])
        local window = tonumber(ARGV[2])
        local count = redis.call('INCR', key)
        if count == 1 then
            redis.call('EXPIRE', key, window)
        end
        if count > limit then
            return 0
        end
        return count
        """;

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT_OBJ = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 跳过无需限流的路径
        if (exclusions.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        // 限流未启用时直接通过
        if (!enabled) {
            return chain.filter(exchange);
        }

        // 获取客户端标识（优先使用用户ID，否则使用IP）
        String clientId = getClientId(exchange);
        String rateLimitKey = "rate_limit:" + clientId + ":" + extractPathKey(path);

        return Mono.fromCallable(() -> {
            // 执行Lua脚本进行原子性限流检查（阻塞调用，调度到弹性线程池）
            return redisTemplate.execute(
                    RATE_LIMIT_SCRIPT_OBJ,
                    List.of(rateLimitKey),
                    String.valueOf(defaultLimit),
                    String.valueOf(defaultWindowSeconds)
            );
        }).subscribeOn(Schedulers.boundedElastic())
          .flatMap(result -> {
            if (result != null && result == 0) {
                // 触发限流
                log.warn("触发限流: clientId={}, path={}, limit={}/{}s",
                        clientId, path, defaultLimit, defaultWindowSeconds);

                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(defaultLimit));
                exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
                exchange.getResponse().getHeaders().add("X-RateLimit-Reset",
                        String.valueOf(System.currentTimeMillis() / 1000 + defaultWindowSeconds));
                return exchange.getResponse().setComplete();
            }

            // Lua脚本返回当前计数，直接计算剩余
            int currentCount = result != null ? result.intValue() : 0;
            int remainingCount = defaultLimit - currentCount;

            // 添加限流头信息
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-RateLimit-Limit", String.valueOf(defaultLimit))
                    .header("X-RateLimit-Remaining", String.valueOf(Math.max(0, remainingCount)))
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
          })
          .onErrorResume(e -> {
            // Redis异常时采用 fail-closed 策略，拒绝请求并返回服务不可用状态
            log.error("限流检查异常: clientId={}, path={}, error={}",
                    clientId, path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            exchange.getResponse().getHeaders().add("X-RateLimit-Fallback", "true");
            return exchange.getResponse().setComplete();
          });
    }

    /**
     * 获取客户端标识
     * 优先使用用户ID，否则使用IP地址
     */
    private String getClientId(ServerWebExchange exchange) {
        // 从请求头获取用户ID（由JWT过滤器设置）
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            return "user:" + userId;
        }

        // 降级为IP地址
        String ip = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
        return "ip:" + ip;
    }

    /**
     * 从路径中提取限流Key
     * 统一处理各种路径格式：
     * /api/v1/workflow/leave/123 -> /api/v1/workflow/leave
     * /api/v1/workflow/leave -> /api/v1/workflow/leave
     * /api/v1/leave -> /api/v1/leave
     */
    private String extractPathKey(String path) {
        // 规范化路径：移除连续斜杠
        String normalizedPath = path.replaceAll("/+", "/");
        // 移除末尾斜杠
        if (normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }

        String[] parts = normalizedPath.split("/");
        // 过滤空段（处理首尾斜杠和连续斜杠情况）
        java.util.List<String> segments = new java.util.ArrayList<>();
        for (String s : parts) {
            if (!s.isEmpty()) {
                segments.add(s);
            }
        }

        // 只保留前3个路径段（API版本级限流）
        if (segments.size() > 3) {
            return "/" + String.join("/", segments.subList(0, 3));
        }
        // 不足3段时使用完整路径
        return normalizedPath;
    }

    @Override
    public int getOrder() {
        return -150;
    }
}
