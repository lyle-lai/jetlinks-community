package org.jetlinks.community.network.websocket.server;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WebSocketDeviceDataHandler extends TextWebSocketHandler {

    // a thread-safe map to store sessions and their subscriptions
    // SessionId -> SubscriptionInfo (e.g., deviceId)
    private final Map<String, String> subscriptions = new ConcurrentHashMap<>();

    // A dedicated session manager would be better, but this is fine for now.
    private final Map<String, WebSocketSession> allSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        allSessions.put(session.getId(), session);
        log.debug("WebSocket connection established: {}. Total sessions: {}", session.getId(), allSessions.size());
        // The authentication has been done in the interceptor.
        // The user info is stored in session.getAttributes().get("user")
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            log.debug("Received WebSocket message from {}: {}", session.getId(), payload);
            // Handle subscription messages, e.g., {"action":"subscribe","deviceId":"device123"}
            Map<String, Object> msg = JSON.parseObject(payload, Map.class);
            String action = (String) msg.get("action");
            if ("subscribe".equals(action)) {
                String deviceId = (String) msg.get("deviceId");
                subscriptions.put(session.getId(), deviceId);
                session.sendMessage(new TextMessage("{\"success\":true, \"message\":\"Subscribed to " + deviceId + "\"}"));
            } else if ("unsubscribe".equals(action)) {
                subscriptions.remove(session.getId());
                session.sendMessage(new TextMessage("{\"success\":true, \"message\":\"Unsubscribed\"}"));
            }

        } catch (Exception e) {
            log.error("Handle WebSocket message failed", e);
            session.sendMessage(new TextMessage("{\"success\":false, \"message\":\"Invalid message format\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        subscriptions.remove(session.getId());
        allSessions.remove(session.getId());
        log.debug("WebSocket connection closed: {} status: {}. Total sessions: {}", session.getId(), status, allSessions.size());
    }

    public void sendDataToSubscribers(String deviceId, String data) {
        subscriptions.forEach((sessionId, subscribedDeviceId) -> {
            if (subscribedDeviceId != null && subscribedDeviceId.equals(deviceId)) {
                WebSocketSession session = allSessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(data));
                    } catch (IOException e) {
                        log.error("Send data to subscriber {} failed", sessionId, e);
                    }
                }
            }
        });
    }
}
