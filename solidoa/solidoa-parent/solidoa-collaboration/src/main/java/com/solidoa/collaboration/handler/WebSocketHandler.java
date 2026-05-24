package com.solidoa.collaboration.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("收到WebSocket消息: {}", message.getPayload());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String uri = session.getUri().getPath();
        String userIdStr = uri.substring(uri.lastIndexOf('/') + 1);
        Long userId = Long.parseLong(userIdStr);
        sessions.put(userId, session);
        log.info("WebSocket连接建立: userId={}", userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String uri = session.getUri().getPath();
        String userIdStr = uri.substring(uri.lastIndexOf('/') + 1);
        Long userId = Long.parseLong(userIdStr);
        sessions.remove(userId);
        log.info("WebSocket连接关闭: userId={}", userId);
    }

    public void sendToUser(Long userId, String message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                log.info("发送消息到用户: userId={}", userId);
            } catch (Exception e) {
                log.error("发送消息失败", e);
            }
        }
    }

    public void broadcast(String message) {
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (Exception e) {
                    log.error("广播消息失败", e);
                }
            }
        });
    }
}