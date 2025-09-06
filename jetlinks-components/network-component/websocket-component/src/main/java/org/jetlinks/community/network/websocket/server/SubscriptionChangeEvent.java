package org.jetlinks.community.network.websocket.server;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SubscriptionChangeEvent {
    private String appId;
    private List<String> deviceIds;
    private boolean overwrite;
    private boolean unsubscribeAll;

    public static SubscriptionChangeEvent of(WebSocketSubscriptionController.SubscriptionRequest request) {
        SubscriptionChangeEvent event = new SubscriptionChangeEvent();
        event.setAppId(request.getAppId());
        event.setDeviceIds(request.getDeviceId());
        event.setOverwrite(request.isOverwrite());
        if (request.getDeviceId() == null || request.getDeviceId().isEmpty()) {
            event.setUnsubscribeAll(true);
        }
        return event;
    }
}
