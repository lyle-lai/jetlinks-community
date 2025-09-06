package org.jetlinks.community.network.websocket.server;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.auth.common.AppCredentials;
import org.jetlinks.community.auth.common.AppCredentialsValidator;
import org.hswebframework.web.authorization.token.ParsedToken;
import org.hswebframework.web.authorization.basic.web.AuthorizedToken;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import reactor.core.scheduler.Schedulers;

@Component
@Slf4j
public class WebSocketDeviceDataHandler implements WebSocketHandler {

    @Autowired
    private AppCredentialsValidator credentialsValidator;

    @Autowired
    private EventBus eventBus;

    // Map<appId, Set<deviceId>>
    private final Map<String, Set<String>> appSubscriptions = new ConcurrentHashMap<>();

    // Map<appId, Set<sessionId>>
    private final Map<String, Set<String>> sessionsByAppId = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> allSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> lastPongTime = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Subscribe to subscription change events from event bus
        eventBus.subscribe(
            Subscription.builder()
                .subscriberId("websocket-subscription-manager")
                .topics("/websocket/subscription-change")
                .build(),
            SubscriptionChangeEvent.class)
            .flatMap(event -> {
                log.info("Received subscription change event: {}", event);
                if (event.isUnsubscribeAll()) {
                    appSubscriptions.remove(event.getAppId());
                } else {
                    Set<String> subscriptions = appSubscriptions.computeIfAbsent(event.getAppId(), k -> ConcurrentHashMap.newKeySet());
                    if (event.isOverwrite()) {
                        subscriptions.clear();
                    }
                    if(event.getDeviceIds() != null){
                        subscriptions.addAll(event.getDeviceIds());
                    }
                }
                return Mono.empty();
            }).subscribe();

        // Heartbeat
        Schedulers.parallel().schedulePeriodically(() -> {
            log.debug("Checking heartbeat for {} sessions", allSessions.size());
            long now = System.currentTimeMillis();
            allSessions.forEach((sessionId, session) -> {
                if (!session.isOpen()) {
                    return;
                }

                Long lastPong = lastPongTime.get(sessionId);
                // If we haven't received a pong in 2 heartbeat intervals (60s), close the connection.
                if (lastPong != null && (now - lastPong > 60 * 1000)) {
                    log.warn("WebSocket session {} is not responsive, closing.", sessionId);
                    session.close().subscribe(null, err -> log.warn("Failed to close unresponsive session {}", sessionId, err));
                    return; // The doOnTerminate will handle cleanup.
                }

                // Send ping
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

                // Handle incoming messages (pongs for heartbeat)
                return session.receive()
                    .doOnNext(message -> {
                        if (message.getType() == WebSocketMessage.Type.PONG) {
                            log.debug("Received pong from session {}", session.getId());
                            lastPongTime.put(session.getId(), System.currentTimeMillis());
                        }
                    })
                    .then();
            })
            .doOnTerminate(() -> {
                String sessionId = session.getId();
                lastPongTime.remove(sessionId);
                String appId = (String) session.getAttributes().get("appId");
                if (appId != null) {
                    Set<String> appSessions = sessionsByAppId.get(appId);
                    if (appSessions != null) {
                        appSessions.remove(sessionId);
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

    public void sendDataToSubscribers(String deviceId, byte[] data) {
        appSubscriptions.forEach((appId, deviceIdSet) -> {
            if (deviceIdSet.contains(deviceId)) {
                Set<String> sessionIds = sessionsByAppId.get(appId);
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
        });
    }
}