package org.jetlinks.community.auth.service.thirdparty;

import org.hswebframework.web.authorization.Authentication;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public interface OpenPlatformAuthenticationService {
    Mono<Authentication> authenticate(ServerWebExchange exchange);
}
