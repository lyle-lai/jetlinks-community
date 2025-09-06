package org.jetlinks.community.auth.common;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * Interface for authorizing WebSocket subscription requests.
 * An implementation of this interface should be provided in a higher-level module
 * that has access to the necessary authentication and device management services.
 */
public interface WebSocketSubscriptionAuthorizer {

    /**
     * Authorizes a list of device IDs for a given application.
     *
     * @param appId     The ID of the application requesting the subscription.
     * @param deviceIds The list of device IDs to authorize.
     * @return A Mono emitting a Set of device IDs that the application is permitted to subscribe to.
     */
    Mono<Set<String>> authorize(String appId, List<String> deviceIds);
}
