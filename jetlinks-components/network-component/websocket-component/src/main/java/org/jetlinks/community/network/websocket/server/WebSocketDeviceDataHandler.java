package org.jetlinks.community.network.websocket.server;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.auth.common.AppCredentials;
import org.jetlinks.community.auth.common.AppCredentialsValidator;
import org.hswebframework.web.authorization.token.ParsedToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class WebSocketDeviceDataHandler implements WebSocketHandler {

    @Autowired
    private AppCredentialsValidator credentialsValidator;

    private final Map<String, String> subscriptions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> allSessions = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // 1. Perform authentication
        Mono<ParsedToken> authResult = Mono.defer(() -> {
            String query = session.getHandshakeInfo().getUri().getQuery();
            Map<String, String> queryParams = query == null
                ? Collections.emptyMap()
                :
                Stream.of(query.split("&"))
                .map(s -> s.split("=", 2))
                .collect(Collectors.toMap(arr -> arr[0], arr -> arr.length > 1 ? arr[1] : ""));

            Map<String, String> businessParams = new java.util.HashMap<>(queryParams);
            businessParams.remove("X-App-Id");
            businessParams.remove("X-App-Key");
            businessParams.remove("X-Timestamp");
            businessParams.remove("X-Nonce");
            businessParams.remove("X-Signature");

            AppCredentials credentials = AppCredentials.builder()
                .appId(queryParams.get("X-App-Id"))
                .appKey(queryParams.get("X-App-Key"))
                .timestamp(queryParams.get("X-Timestamp"))
                .nonce(queryParams.get("X-Nonce"))
                .signature(queryParams.get("X-Signature"))
                .requestPath(session.getHandshakeInfo().getUri().getPath())
                .requestMethod(HttpMethod.GET) // WebSocket handshake is always GET
                .queryParams(businessParams)
                .build();

            return credentialsValidator.validate(credentials);
        }).doOnError(err -> log.warn("WebSocket handshake authentication failed", err));

        return authResult
            .flatMap(token -> {
                // 2. Authentication successful, handle connection
                allSessions.put(session.getId(), session);
                log.debug("WebSocket connection established: {}. Total sessions: {}", session.getId(), allSessions.size());
                session.getAttributes().put("user", token);

                // 3. Handle incoming messages
                return session.receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .flatMap(payload -> handleTextMessage(session, payload))
                    .then();
            })
            .doOnTerminate(() -> {
                // 4. Handle connection closed
                subscriptions.remove(session.getId());
                allSessions.remove(session.getId());
                log.debug("WebSocket connection closed: {}. Total sessions: {}", session.getId(), allSessions.size());
            })
            .onErrorResume(err -> {
                log.warn("WebSocket authentication failed, closing connection.", err);
                return session.close();
            });
    }

    private Mono<Void> handleTextMessage(WebSocketSession session, String payload) {
        try {
            log.debug("Received WebSocket message from {}: {}", session.getId(), payload);
            Map<String, Object> msg = JSON.parseObject(payload, Map.class);
            String action = (String) msg.get("action");
            if ("subscribe".equals(action)) {
                String deviceId = (String) msg.get("deviceId");
                subscriptions.put(session.getId(), deviceId);
                return session.send(Mono.just(session.textMessage("{\"success\":true, \"message\":\"Subscribed to " + deviceId + "\"}")));
            } else if ("unsubscribe".equals(action)) {
                subscriptions.remove(session.getId());
                return session.send(Mono.just(session.textMessage("{\"success\":true, \"message\":\"Unsubscribed\"}")));
            }
        } catch (Exception e) {
            log.error("Handle WebSocket message failed", e);
            return session.send(Mono.just(session.textMessage("{\"success\":false, \"message\":\"Invalid message format\"}")));
        }
        return Mono.empty();
    }

    public void sendDataToSubscribers(String deviceId, String data) {
        subscriptions.forEach((sessionId, subscribedDeviceId) -> {
            if (subscribedDeviceId != null && subscribedDeviceId.equals(deviceId)) {
                WebSocketSession session = allSessions.get(sessionId);
                if (session != null) {
                    try {
                        // In reactive, send is asynchronous
                        session.send(Mono.just(session.textMessage(data)))
                               .subscribe(null, err -> log.error("Send data to subscriber {} failed", sessionId, err));
                    } catch (Exception e) {
                        log.error("Send data to subscriber {} failed", sessionId, e);
                    }
                }
            }
        });
    }
}