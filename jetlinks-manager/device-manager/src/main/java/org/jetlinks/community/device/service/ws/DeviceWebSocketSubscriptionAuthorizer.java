package org.jetlinks.community.device.service.ws;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.executor.reactive.ReactiveSqlExecutor;
import org.jetlinks.community.auth.common.WebSocketSubscriptionAuthorizer;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
@Slf4j
public class DeviceWebSocketSubscriptionAuthorizer implements WebSocketSubscriptionAuthorizer {

    private final ReactiveSqlExecutor sqlExecutor;
    private final LocalDeviceInstanceService deviceService;

    @Override
    public Mono<Set<String>> authorize(String appId, List<String> deviceIds) {
        return fetchAuthRules(appId)
            .collectList()
            .flatMap(rules -> {
                if (rules.isEmpty()) {
                    // No rules configured for this app, deny all subscriptions.
                    return Mono.just(Collections.emptySet());
                }

                // Per business logic, all rules are either WHITELIST or BLACKLIST, so we check the first rule's mode.
                AuthRule.Mode mode = rules.get(0).getMode();

                return resolveDeviceIdsFromRules(rules)
                    .map(resolvedDeviceIds -> {
                        if (mode == AuthRule.Mode.WHITELIST) {
                            // WHITELIST mode: Only allow devices that are explicitly in the resolved list.
                            return deviceIds
                                .stream()
                                .filter(resolvedDeviceIds::contains)
                                .collect(Collectors.toSet());
                        } else { // BLACKLIST mode
                            // BLACKLIST mode: Allow any device that is NOT in the resolved list.
                            return deviceIds
                                .stream()
                                .filter(id -> !resolvedDeviceIds.contains(id))
                                .collect(Collectors.toSet());
                        }
                    });
            });
    }

    private Flux<AuthRule> fetchAuthRules(String appId) {
        String sql = "SELECT resource_type, resource_id, mode FROM s_open_platform_device_auth WHERE platform_app_id = (SELECT id FROM s_open_platform_app WHERE app_id = ?)";
        return sqlExecutor.select(sql, appId)
            .map(row -> new AuthRule(
                (String) row.get("resource_type"),
                (String) row.get("resource_id"),
                AuthRule.Mode.valueOf((String) row.get("mode"))
            ))
            .doOnError(err -> log.error("Failed to fetch auth rules for app: {}", appId, err));
    }

    private Mono<Set<String>> resolveDeviceIdsFromRules(List<AuthRule> rules) {
        if (rules.isEmpty()) {
            return Mono.just(Collections.emptySet());
        }

        Map<String, List<String>> grouped = rules.stream()
            .collect(Collectors.groupingBy(
                AuthRule::getResourceType,
                Collectors.mapping(AuthRule::getResourceId, Collectors.toList())
            ));

        // Direct device rules
        Flux<String> directDevices = Flux.fromIterable(grouped.getOrDefault("DEVICE", Collections.emptyList()));

        // Product rules -> resolve to device IDs
        Flux<String> productDevices = Flux.fromIterable(grouped.getOrDefault("PRODUCT", Collections.emptyList()))
            .flatMap(productId -> deviceService.createQuery()
                .select(DeviceInstanceEntity::getId)
                .where(DeviceInstanceEntity::getProductId, productId)
                .fetch()
                .map(DeviceInstanceEntity::getId));

        // Organization rules -> resolve to device IDs via SQL
        List<String> orgIds = grouped.getOrDefault("ORGANIZATION", Collections.emptyList());
        Flux<String> orgDevices;
        if (CollectionUtils.isEmpty(orgIds)) {
            orgDevices = Flux.empty();
        } else {
            String placeholders = String.join(",", Collections.nCopies(orgIds.size(), "?"));
            String sql = "SELECT device_id FROM s_dimension_device WHERE dimension_type_id = ? AND dimension_id IN (" + placeholders + ")";

            List<Object> params = new ArrayList<>();
            params.add("org");
            params.addAll(orgIds);

            orgDevices = sqlExecutor
                .select(sql, params.toArray())
                .map(row -> (String) row.get("device_id"));
        }

        return Flux.concat(directDevices, productDevices, orgDevices)
            .collect(Collectors.toSet());
    }

    @Getter
    @AllArgsConstructor
    private static class AuthRule {
        private final String resourceType;
        private final String resourceId;
        private final Mode mode;

        enum Mode {
            WHITELIST, BLACKLIST
        }
    }
}
