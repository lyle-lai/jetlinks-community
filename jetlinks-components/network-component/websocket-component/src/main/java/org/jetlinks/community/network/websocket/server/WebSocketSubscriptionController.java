package org.jetlinks.community.network.websocket.server;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.event.EventBus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/websocket")
@AllArgsConstructor
public class WebSocketSubscriptionController {

    private final EventBus eventBus;

    @PostMapping("/subscribe")
    public Mono<Void> subscribe(@RequestBody SubscriptionRequest request) {
        return eventBus.publish("/websocket/subscription-change", SubscriptionChangeEvent.of(request)).then();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionRequest {
        private String appId;
        private List<String> deviceId;
        private boolean overwrite;
    }
}
