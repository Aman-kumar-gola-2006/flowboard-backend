package com.flowboard.notification.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            sessions.put(userId, session);
            System.out.println("WebSocket connected for user: " + userId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status)
            throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            sessions.remove(userId);
            System.out.println("WebSocket disconnected for user: " + userId);
        }
    }

    public void sendNotification(Long userId, Object notification) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(notification);
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                System.err.println("Failed to send notification: " + e.getMessage());
            }
        }
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        if (session.getUri() == null)
            return null;
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            try {
                String userIdStr = query.split("userId=")[1].split("&")[0];
                return Long.parseLong(userIdStr);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}