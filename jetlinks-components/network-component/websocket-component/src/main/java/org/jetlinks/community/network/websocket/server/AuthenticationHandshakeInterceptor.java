package org.jetlinks.community.network.websocket.server;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.auth.common.AppCredentials;
import org.jetlinks.community.auth.common.AppCredentialsValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.hswebframework.web.authorization.token.ParsedToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class AuthenticationHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private AppCredentialsValidator credentialsValidator;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        String query = request.getURI().getQuery();
        Map<String, String> queryParams = query == null
                ? Collections.emptyMap()
                : Stream.of(query.split("&"))
                .map(s -> s.split("=", 2))
                .collect(Collectors.toMap(arr -> arr[0], arr -> arr.length > 1 ? arr[1] : ""));

        // For signature calculation, we only consider non-authentication parameters.
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
            .requestPath(request.getURI().getPath())
            .requestMethod(HttpMethod.GET) // WebSocket handshake is always GET
            .queryParams(businessParams) // Use only non-auth params for signature
            .build();

        try {
            ParsedToken result = credentialsValidator
                .validate(credentials)
                .block(); // Block to get result in sync handshake process

            if (result != null ) {
                attributes.put("user", result);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("WebSocket handshake authentication failed", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // do nothing
    }
}
