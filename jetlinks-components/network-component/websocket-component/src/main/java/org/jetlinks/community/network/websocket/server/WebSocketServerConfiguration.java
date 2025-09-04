package org.jetlinks.community.network.websocket.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketServerConfiguration implements WebSocketConfigurer {

    @Autowired
    private AuthenticationHandshakeInterceptor authenticationInterceptor;

    @Autowired
    private WebSocketDeviceDataHandler webSocketDeviceDataHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketDeviceDataHandler, "/medlink/device-data-ws")
            .addInterceptors(authenticationInterceptor)
            .setAllowedOrigins("*"); // For development, allow all origins.
    }
}
