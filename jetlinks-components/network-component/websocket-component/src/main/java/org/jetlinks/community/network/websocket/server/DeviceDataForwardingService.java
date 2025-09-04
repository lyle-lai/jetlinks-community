package org.jetlinks.community.network.websocket.server;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.DeviceMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class DeviceDataForwardingService {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private WebSocketDeviceDataHandler webSocketHandler;

    @PostConstruct
    public void init() {
        eventBus
            .subscribe(Subscription
                .builder()
                .subscriberId("websocket-data-forwarder")
                // Listen for device message report events
                .topics("/device/**/message/report")
                .shared()
                .build())
            .flatMap(payload -> {
                // The payload from event bus is DeviceMessage
                if (payload instanceof DeviceMessage) {
                    DeviceMessage deviceMessage = (DeviceMessage) payload;
                    String deviceId = deviceMessage.getDeviceId();
                    if (deviceId == null) {
                        return Mono.empty();
                    }
                    // Convert the message to JSON and forward it
                    String jsonData = JSON.toJSONString(deviceMessage.toJson());
                    webSocketHandler.sendDataToSubscribers(deviceId, jsonData);
                } else {
                    log.warn("Unsupported event payload type for websocket forwarding: {}", payload.getClass().getName());
                }
                return Mono.empty();
            })
            .subscribe(); // Activate the stream
    }
}
