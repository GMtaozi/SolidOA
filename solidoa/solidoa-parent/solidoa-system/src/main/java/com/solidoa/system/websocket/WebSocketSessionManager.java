package com.solidoa.system.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket Session 管理 (V2.0 第 9 章实时推送)
 * - key: userId
 * - value: WebSocketSession
 * - 跨服务推送: 写到 Redis pub/sub, system 端订阅后调 pushMessage
 */
@Component
@Slf4j
public class WebSocketSessionManager extends TextWebSocketHandler {

    private static final Map<Long, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            SESSIONS.put(userId, session);
            log.info("[WebSocket] 用户 {} 已连接, 当前在线: {}", userId, SESSIONS.size());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            SESSIONS.remove(userId);
            log.info("[WebSocket] 用户 {} 已断开, 当前在线: {}", userId, SESSIONS.size());
        }
    }

    /**
     * 推送给指定用户
     */
    public void pushMessage(Long userId, String content) {
        WebSocketSession session = SESSIONS.get(userId);
        if (session == null || !session.isOpen()) {
            log.debug("[WebSocket] 用户 {} 不在线, 跳过推送", userId);
            return;
        }
        try {
            session.sendMessage(new TextMessage(content));
            log.debug("[WebSocket] 已推送: userId={}, len={}", userId, content.length());
        } catch (IOException e) {
            log.error("[WebSocket] 推送失败: userId={}, err={}", userId, e.getMessage());
        }
    }

    public int onlineCount() {
        return SESSIONS.size();
    }
}
