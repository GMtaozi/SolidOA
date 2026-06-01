package com.solidoa.gateway.filter;

import com.solidoa.common.constant.TraceConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 全链路追踪过滤器
 * - 生成 TraceId 并传递给下游服务
 * - 将 TraceId 写入 HTTP 响应头
 */
@Component
@Slf4j
public class TraceIdFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 检查请求头是否已有 TraceId
        String traceId = exchange.getRequest().getHeaders().getFirst(TraceConstants.TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
        }

        // 存入 Exchange Attributes（供后续 filter 使用）
        exchange.getAttributes().put(TraceConstants.TRACE_ID_KEY, traceId);

        // 将 TraceId 传递给下游服务
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(TraceConstants.TRACE_ID_HEADER, traceId)
                .build();

        // 将 TraceId 写入响应头
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        log.debug("TraceId: {}", traceId);

        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return -200; // 最先执行
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
