package org.jetlinks.community.network.websocket.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.basic.web.AuthorizedToken;
import org.hswebframework.web.authorization.token.ParsedToken;
import org.jetlinks.community.auth.common.AppCredentials;
import org.jetlinks.community.auth.common.AppCredentialsValidator;
import org.jetlinks.community.auth.common.WebSocketSubscriptionAuthorizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class WebSocketDeviceDataHandler implements WebSocketHandler {

    @Autowired
    private AppCredentialsValidator credentialsValidator;

    @Autowired(required = false)
    private WebSocketSubscriptionAuthorizer authorizer;

    @Autowired
    private ObjectMapper mapper;

    // <sessionId, Set<deviceId>>
    private final Map<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();
    // <deviceId, Set<sessionId>>
    private final Map<String, Set<String>> deviceToSessionMap = new ConcurrentHashMap<>();
    // <appId, Set<sessionId>>
    private final Map<String, Set<String>> sessionsByAppId = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> allSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> lastPongTime = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Heartbeat
        Schedulers.parallel().schedulePeriodically(() -> {
            log.debug("Checking heartbeat for {} sessions", allSessions.size());
            long now = System.currentTimeMillis();
            allSessions.forEach((sessionId, session) -> {
                if (!session.isOpen()) {
                    return;
                }

                Long lastPong = lastPongTime.get(sessionId);
                if (lastPong != null && (now - lastPong > 60 * 1000)) {
                    log.warn("WebSocket session {} is not responsive, closing.", sessionId);
                    session.close().subscribe(null, err -> log.warn("Failed to close unresponsive session {}", sessionId, err));
                    return;
                }

                try {
                    session.send(Mono.just(session.pingMessage(dataBufferFactory -> dataBufferFactory.wrap("ping".getBytes(StandardCharsets.UTF_8)))))
                        .subscribe(null, err -> log.warn("Failed to send ping to session {}", sessionId, err));
                } catch (Exception e) {
                    log.warn("Failed to send ping to session {}", sessionId, e);
                }
            });
        }, 30, 30, TimeUnit.SECONDS);
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
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

            return credentialsValidator.validate(credentials, true);
        }).doOnError(err -> log.warn("WebSocket handshake authentication failed", err));

        return authResult
            .flatMap(token -> {
                String appId = ((AuthorizedToken) token).getUserId();
                allSessions.put(session.getId(), session);
                sessionsByAppId.computeIfAbsent(appId, k -> ConcurrentHashMap.newKeySet()).add(session.getId());
                session.getAttributes().put("appId", appId);
                lastPongTime.put(session.getId(), System.currentTimeMillis());
                log.info("WebSocket connection established from app {}: {}. Total sessions: {}", appId, session.getId(), allSessions.size());

                return session
                    .receive()
                    .flatMap(message -> handleMessage(session, message))
                    .then();
            })
            .doOnTerminate(() -> {
                String sessionId = session.getId();
                // Clean up subscriptions
                Set<String> subs = sessionSubscriptions.remove(sessionId);
                if (subs != null) {
                    subs.forEach(devId -> {
                        Set<String> sessions = deviceToSessionMap.get(devId);
                        if (sessions != null) {
                            sessions.remove(sessionId);
                            if (sessions.isEmpty()) {
                                deviceToSessionMap.remove(devId);
                            }
                        }
                    });
                }

                // Clean up session tracking
                lastPongTime.remove(sessionId);
                String appId = (String) session.getAttributes().get("appId");
                if (appId != null) {
                    Set<String> appSessions = sessionsByAppId.get(appId);
                    if (appSessions != null) {
                        appSessions.remove(sessionId);
                        if (appSessions.isEmpty()) {
                            sessionsByAppId.remove(appId);
                        }
                    }
                }
                allSessions.remove(sessionId);
                log.info("WebSocket connection closed: {}. Total sessions: {}", sessionId, allSessions.size());
            })
            .onErrorResume(err -> {
                log.warn("WebSocket authentication failed, closing connection.", err);
                return session.close();
            });
    }

    private Mono<Void> handleMessage(WebSocketSession session, WebSocketMessage message) {
        if (message.getType() == WebSocketMessage.Type.PONG) {
            log.debug("Received pong from session {}", session.getId());
            lastPongTime.put(session.getId(), System.currentTimeMillis());
            return Mono.empty();
        }
        if (message.getType() == WebSocketMessage.Type.TEXT) {
            String payload = message.getPayloadAsText();
            String appId = (String) session.getAttributes().get("appId");

            return Mono.fromCallable(() -> mapper.readValue(payload, SubscriptionRequest.class))
                .doOnError(err -> log.error("Failed to parse websocket message from app {}: {}", appId, payload, err))
                .flatMap(request -> {
                    if (request.getType() == null || request.getDeviceIds() == null) {
                        return Mono.empty();
                    }
                    switch (request.getType().toLowerCase()) {
                        case "subscribe":
                            return handleSubscription(session, appId, request.getDeviceIds(), request.isOverwrite());
                        case "unsubscribe":
                            return handleUnsubscription(session, request.getDeviceIds());
                        default:
                            return Mono.empty();
                    }
                })
                .onErrorResume(err -> {
                    log.warn("Error handling websocket message for app {}", appId, err);
                    return Mono.empty(); // Ignore errors in message handling to keep session alive
                });
        }
        return Mono.empty();
    }

    private Mono<Void> handleSubscription(WebSocketSession session, String appId, List<String> deviceIds, boolean overwrite) {
        if (deviceIds.isEmpty()) {
            return Mono.empty();
        }

        // Authorize device IDs. If no authorizer is configured, allow all requested IDs.
        Mono<Set<String>> authorizedDeviceIds$ = authorizer != null
            ? authorizer.authorize(appId, deviceIds)
            : Mono.just(Collections.emptySet()); // Return an empty set if no authorizer

        return authorizedDeviceIds$
            .doOnNext(authorized -> {
                if (authorized.isEmpty()) {
                    return;
                }
                log.debug("App {} session {} subscribing to devices: {}", appId, session.getId(), authorized);
                Set<String> currentSubs = sessionSubscriptions.computeIfAbsent(session.getId(), k -> ConcurrentHashMap.newKeySet());

                if (overwrite) {
                    // Find devices to remove from the global map
                    Set<String> toRemove = new HashSet<>(currentSubs);
                    toRemove.removeAll(authorized);
                    toRemove.forEach(devId -> {
                        Set<String> sessions = deviceToSessionMap.get(devId);
                        if (sessions != null) {
                            sessions.remove(session.getId());
                        }
                    });
                    currentSubs.clear();
                }

                currentSubs.addAll(authorized);
                authorized.forEach(devId -> deviceToSessionMap
                    .computeIfAbsent(devId, k -> ConcurrentHashMap.newKeySet())
                    .add(session.getId()));
            })
            .then();
    }

    private Mono<Void> handleUnsubscription(WebSocketSession session, List<String> deviceIds) {
        return Mono.fromRunnable(() -> {
            if (deviceIds.isEmpty()) {
                return;
            }
            log.debug("App {} session {} unsubscribing from devices: {}", session.getAttributes().get("appId"), session.getId(), deviceIds);
            Set<String> currentSubs = sessionSubscriptions.get(session.getId());
            if (currentSubs == null) {
                return;
            }
            currentSubs.removeAll(new HashSet<>(deviceIds));
            deviceIds.forEach(devId -> {
                Set<String> sessions = deviceToSessionMap.get(devId);
                if (sessions != null) {
                    sessions.remove(session.getId());
                    if (sessions.isEmpty()) {
                        deviceToSessionMap.remove(devId);
                    }
                }
            });
        });
    }


    public void sendDataToSubscribers(String deviceId, byte[] data) {
        Set<String> sessionIds = deviceToSessionMap.get(deviceId);
        if (sessionIds != null) {
            sessionIds.forEach(sessionId -> {
                WebSocketSession session = allSessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    session.send(Mono.just(session.binaryMessage(dataBufferFactory -> dataBufferFactory.wrap(data))))
                        .subscribe(null, err -> log.error("Send data to subscriber {} failed", sessionId, err));
                }
            });
        }
    }

    @Getter
    @Setter
    private static class SubscriptionRequest {
        private String type;
        private List<String> deviceIds;
        private boolean overwrite = false;
    }
}
