package org.jetlinks.community.auth.thirdpart;

import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.ReactiveAuthenticationHolder;
import org.hswebframework.web.authorization.ReactiveAuthenticationSupplier;
import org.hswebframework.web.authorization.basic.web.AuthorizedToken;
import org.hswebframework.web.authorization.token.ParsedToken;
import org.hswebframework.web.authorization.token.ThirdPartReactiveAuthenticationManager;
import org.hswebframework.web.authorization.token.UserToken;
import org.hswebframework.web.authorization.token.UserTokenManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ThirdPartyAppAuthenticationSupplier implements ReactiveAuthenticationSupplier {

    @Autowired
    private UserTokenManager userTokenManager;

    @Autowired
    private List<ThirdPartReactiveAuthenticationManager> thirdPartAuthenticationManagers;

    private Map<String, ThirdPartReactiveAuthenticationManager> managerMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        for (ThirdPartReactiveAuthenticationManager manager : thirdPartAuthenticationManagers) {
            managerMap.put(manager.getTokenType(), manager);
        }
        ReactiveAuthenticationHolder.addSupplier(this);
    }

    @Override
    public Mono<Authentication> get() {
        return Mono
            .deferContextual(context -> context
                .<ParsedToken>getOrEmpty(ParsedToken.class)
                .filter(token -> "third-party-app".equals(token.getType()))
                .map(this::getAuthenticationFromToken)
                .orElse(Mono.empty())
            );
    }

    private Mono<Authentication> getAuthenticationFromToken(ParsedToken parsedToken) {
        if (!(parsedToken instanceof AuthorizedToken)) {
            return Mono.empty();
        }
        AuthorizedToken authorized = (AuthorizedToken) parsedToken;

        Mono<UserToken> userTokenMono = userTokenManager
            .getByToken(authorized.getToken())
            .switchIfEmpty(Mono.defer(() -> userTokenManager.signIn(
                authorized.getToken(),
                authorized.getType(),
                authorized.getUserId(),
                authorized.getMaxInactiveInterval()
            )));

        return userTokenMono
            .flatMap(token -> {
                if (!token.isNormal()) {
                    return Mono.empty();
                }
                return userTokenManager.touch(token.getToken())
                    .then(loadAuthentication(token));
            });
    }

    private Mono<Authentication> loadAuthentication(UserToken token) {
        ThirdPartReactiveAuthenticationManager manager = managerMap.get(token.getType());
        if (manager != null) {
            return manager.getByUserId(token.getUserId());
        }
        return Mono.empty();
    }

    @Override
    public Mono<Authentication> get(String userId) {
        return Mono.empty();
    }
}
