package com.solidoa.system.websocket;

import com.solidoa.common.security.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 配置 (V2.0 第 9 章实时推送)
 * 端点: /ws/notification?userId=123 (简化鉴权: 用查询参数传 userId)
 * 生产建议: 握手时校验 JWT, 这里 MVP 简化为读 header
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketSessionManager sessionManager;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(sessionManager, "/ws/notification")
            .addInterceptors(new HandshakeInterceptor() {
                @Override
                public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                               WebSocketHandler wsHandler, Map<String, Object> attributes) {
                    if (request instanceof ServletServerHttpRequest servletReq) {
                        String userIdHeader = servletReq.getHeaders().getFirst("X-User-Id");
                        if (userIdHeader != null) {
                            attributes.put("userId", Long.valueOf(userIdHeader));
                        } else {
                            String userIdParam = servletReq.getServletRequest().getParameter("userId");
                            if (userIdParam != null) {
                                attributes.put("userId", Long.valueOf(userIdParam));
                            }
                        }
                    }
                    return true;
                }

                @Override
                public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                          WebSocketHandler wsHandler, Exception exception) {
                    // no-op
                }
            })
            .setAllowedOriginPatterns("*");
    }
}
