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
import org.jetlinks.community.auth.entity.OpenPlatformAppEntity;
import org.jetlinks.community.auth.enums.DefaultUserEntityType;
import org.jetlinks.community.auth.enums.ResourceType;
import org.jetlinks.community.auth.service.DimensionDeviceService;
import org.jetlinks.community.auth.service.OpenPlatformAppDeviceAuthService;
import org.jetlinks.community.auth.service.OpenPlatformAppService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private Dimension createDimension(String typeId, String typeName, String id, String name, String appId) {
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
                    .collectList() // 1. Collect all rules
                    .flatMap(rules -> {
                        // 2. Separate rules by type
                        Map<ResourceType, List<OpenPlatformAppDeviceAuthEntity>> groupedRules = rules
                            .stream()
                            .collect(Collectors.groupingBy(OpenPlatformAppDeviceAuthEntity::getResourceType));

                        // 3. Handle direct dimensions (DEVICE, PRODUCT)
                        Flux<Dimension> directDimensions = Flux
                            .fromIterable(groupedRules.getOrDefault(ResourceType.DEVICE, Collections.emptyList()))
                            .map(rule -> createDimension("device", "设备", rule.getResourceId(), rule.getResourceName(), app.getId()))
                            .concatWith(
                                Flux.fromIterable(groupedRules.getOrDefault(ResourceType.PRODUCT, Collections.emptyList()))
                                    .map(rule -> createDimension("product", "产品", rule.getResourceId(), rule.getResourceName(), app.getId()))
                            );

                        List<OpenPlatformAppDeviceAuthEntity> orgRules = groupedRules.getOrDefault(ResourceType.ORGANIZATION, Collections.emptyList());

                        // 4. Handle organization dimensions
                        Flux<Dimension> orgDeviceDimensions;
                        if (orgRules.isEmpty()) {
                            orgDeviceDimensions = Flux.empty();
                        } else {
                            List<String> orgIds = orgRules.stream()
                                .map(OpenPlatformAppDeviceAuthEntity::getResourceId)
                                .collect(Collectors.toList());
                            // Single query for all orgs
                            orgDeviceDimensions = dimensionDeviceService
                                .createQuery()
                                .where(DimensionDeviceEntity::getDimensionTypeId, "org")
                                .in(DimensionDeviceEntity::getDimensionId, orgIds)
                                .fetch()
                                .map(device -> createDimension("device", "设备", device.getDeviceId(), device.getDeviceName(), app.getId()));
                        }

                        // 5. Combine all dimensions and build Authentication
                        return Flux.concat(directDimensions, orgDeviceDimensions)
                            .collectList()
                            .map(dimensions -> {
                                SimpleAuthentication authentication = new SimpleAuthentication();
                                authentication.setUser(user);
                                authentication.setDimensions(dimensions);
                                authentication.setPermissions(Collections.emptyList());
                                return (Authentication) authentication;
                            });
                    });
            });
    }
}
