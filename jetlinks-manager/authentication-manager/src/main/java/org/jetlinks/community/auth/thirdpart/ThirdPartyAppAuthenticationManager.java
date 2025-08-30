package org.jetlinks.community.auth.thirdpart;

import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.DimensionType;
import org.hswebframework.web.authorization.simple.SimpleAuthentication;
import org.hswebframework.web.authorization.simple.SimpleDimension;
import org.hswebframework.web.authorization.simple.SimpleUser;
import org.hswebframework.web.authorization.token.ThirdPartReactiveAuthenticationManager;
import org.jetlinks.community.auth.entity.DimensionDeviceEntity;
import org.jetlinks.community.auth.entity.OpenPlatformAppDeviceAuthEntity;
import org.jetlinks.community.auth.enums.AuthorizationMode;
import org.jetlinks.community.auth.enums.DefaultUserEntityType;
import org.jetlinks.community.auth.enums.ResourceType;
import org.jetlinks.community.auth.service.DimensionDeviceService;
import org.jetlinks.community.auth.service.OpenPlatformAppDeviceAuthService;
import org.jetlinks.community.auth.service.OpenPlatformAppService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
public class ThirdPartyAppAuthenticationManager implements ThirdPartReactiveAuthenticationManager {

    private final OpenPlatformAppService appService;
    private final OpenPlatformAppDeviceAuthService deviceAuthService;
    private final DimensionDeviceService dimensionDeviceService;

    @Override
    public String getTokenType() {
        return "third-party-app";
    }

    private Dimension createDimension(String typeId, String typeName, String id, String name, String appId, AuthorizationMode mode) {
        SimpleDimension dimension = new SimpleDimension();
        dimension.setId(id);
        dimension.setName(name);
        dimension.setType(new DimensionType() {
            @Override
            public String getId() {
                return typeId;
            }

            @Override
            public String getName() {
                return typeName;
            }
        });
        Map<String, Object> options = new HashMap<>();
        options.put("platformAppId", appId);
        if (mode != null) {
            options.put("mode", mode.name());
        }
        dimension.setOptions(options);
        return dimension;
    }

    @Override
    public Mono<Authentication> getByUserId(String userId) { // userId is appId
        return appService.findByAppId(userId)
            .flatMap(app -> {
                SimpleUser user = SimpleUser.builder()
                    .id(app.getAppId())
                    .username(app.getName())
                    .userType(DefaultUserEntityType.APPLICATION.getId())
                    .build();

                return deviceAuthService
                    .getAuthRulesByAppId(app.getId())
                    .collectList()
                    .flatMap(rules -> {
                        Map<AuthorizationMode, List<OpenPlatformAppDeviceAuthEntity>> byMode = rules.stream()
                            .collect(Collectors.groupingBy(OpenPlatformAppDeviceAuthEntity::getMode));

                        Mono<List<Dimension>> whitelistDims$ = resolveRulesToDimensions(app.getId(), byMode.getOrDefault(AuthorizationMode.WHITELIST, Collections.emptyList()), AuthorizationMode.WHITELIST);
                        Mono<List<Dimension>> blacklistDims$ = resolveRulesToDimensions(app.getId(), byMode.getOrDefault(AuthorizationMode.BLACKLIST, Collections.emptyList()), AuthorizationMode.BLACKLIST);

                        return Mono.zip(whitelistDims$, blacklistDims$)
                            .map(tuple -> {
                                List<Dimension> finalDims = new ArrayList<>();
                                finalDims.addAll(tuple.getT1());
                                finalDims.addAll(tuple.getT2());

                                SimpleAuthentication authentication = new SimpleAuthentication();
                                authentication.setUser(user);
                                authentication.setDimensions(finalDims);
                                authentication.setPermissions(Collections.emptyList());
                                return (Authentication) authentication;
                            });
                    });
            });
    }

    private Mono<List<Dimension>> resolveRulesToDimensions(String appId, List<OpenPlatformAppDeviceAuthEntity> rules, AuthorizationMode mode) {
        if (rules.isEmpty()) {
            return Mono.just(Collections.emptyList());
        }

        Map<ResourceType, List<OpenPlatformAppDeviceAuthEntity>> grouped = rules.stream()
            .collect(Collectors.groupingBy(OpenPlatformAppDeviceAuthEntity::getResourceType));

        // Direct device rules
        Flux<Dimension> deviceDims = Flux.fromIterable(grouped.getOrDefault(ResourceType.DEVICE, Collections.emptyList()))
            .map(rule -> createDimension("device", "设备", rule.getResourceId(), rule.getResourceName(), appId, mode));

        // Direct product rules
        Flux<Dimension> productDims = Flux.fromIterable(grouped.getOrDefault(ResourceType.PRODUCT, Collections.emptyList()))
            .map(rule -> createDimension("product", "产品", rule.getResourceId(), rule.getResourceName(), appId, mode));

        // Organization rules -> resolve to device dimensions
        List<OpenPlatformAppDeviceAuthEntity> orgRules = grouped.getOrDefault(ResourceType.ORGANIZATION, Collections.emptyList());
        Flux<Dimension> orgDeviceDims;
        if (orgRules.isEmpty()) {
            orgDeviceDims = Flux.empty();
        } else {
            List<String> orgIds = orgRules.stream().map(OpenPlatformAppDeviceAuthEntity::getResourceId).collect(Collectors.toList());
            orgDeviceDims = dimensionDeviceService.createQuery()
                .where(DimensionDeviceEntity::getDimensionTypeId, "org")
                .in(DimensionDeviceEntity::getDimensionId, orgIds)
                .fetch()
                .map(device -> createDimension("device", "设备", device.getDeviceId(), device.getDeviceName(), appId, mode));
        }

        return Flux.concat(deviceDims, productDims, orgDeviceDims).collectList();
    }
}
