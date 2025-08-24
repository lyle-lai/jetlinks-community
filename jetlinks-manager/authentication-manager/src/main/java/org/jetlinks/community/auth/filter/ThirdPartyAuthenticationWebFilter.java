package org.jetlinks.community.auth.filter;

import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.ReactiveAuthenticationHolder;
import org.jetlinks.community.auth.service.thirdparty.OpenPlatformAuthenticationService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(-100) // Run before hsweb's default authentication filter
@AllArgsConstructor
public class ThirdPartyAuthenticationWebFilter implements WebFilter {

    private final OpenPlatformAuthenticationService authenticationService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return authenticationService.authenticate(exchange)
                                    .flatMap(auth -> chain.filter(exchange)
                                                          .contextWrite(ctx -> ctx.put(org.hswebframework.web.authorization.Authentication.class, auth)))
                                    .switchIfEmpty(chain.filter(exchange)); // If empty, continue chain for normal auth
    }
}
