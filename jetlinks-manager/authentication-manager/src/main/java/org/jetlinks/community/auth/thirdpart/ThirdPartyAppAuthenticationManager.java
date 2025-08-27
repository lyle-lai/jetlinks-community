package org.jetlinks.community.auth.thirdpart;

import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.simple.SimpleAuthentication;
import org.hswebframework.web.authorization.simple.SimpleUser;
import org.hswebframework.web.authorization.token.ThirdPartReactiveAuthenticationManager;
import org.jetlinks.community.auth.entity.OpenPlatformAppEntity;
import org.jetlinks.community.auth.enums.DefaultUserEntityType;
import org.jetlinks.community.auth.service.OpenPlatformAppService;
import reactor.core.publisher.Mono;

import java.util.Collections;

@AllArgsConstructor
public class ThirdPartyAppAuthenticationManager implements ThirdPartReactiveAuthenticationManager {

    private final OpenPlatformAppService appService;

    @Override
    public String getTokenType() {
        return "third-party-app";
    }

    @Override
    public Mono<Authentication> getByUserId(String userId) { // userId is appId
        return appService.createQuery()
            .where(OpenPlatformAppEntity::getAppId, userId)
            .fetchOne()
            .map(app -> {
                SimpleAuthentication authentication = new SimpleAuthentication();
                SimpleUser user = SimpleUser.builder()
                                    .id(app.getAppId()) // Use appId as user id
                                    .username(app.getName())
                                    .userType(DefaultUserEntityType.APPLICATION.getId())
                                    .build();
                authentication.setUser(user);
                // TODO: Load app permissions
                authentication.setPermissions(Collections.emptyList());
                return (Authentication) authentication;
            });
    }
}
