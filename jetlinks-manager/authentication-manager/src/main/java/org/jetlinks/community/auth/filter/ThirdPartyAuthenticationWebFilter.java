package org.jetlinks.community.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.exception.AuthenticationException;
import org.jetlinks.community.auth.service.thirdparty.OpenPlatformAuthenticationService;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Order(-100) // Run before hsweb's default authentication filter
@AllArgsConstructor
public class ThirdPartyAuthenticationWebFilter implements WebFilter {

    private final OpenPlatformAuthenticationService authenticationService;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return authenticationService
            .authenticate(exchange)
            .flatMap(auth -> chain
                .filter(exchange)
                .contextWrite(ctx -> ctx.put(Authentication.class, auth)))
            .switchIfEmpty(chain.filter(exchange))
            .onErrorResume(AuthenticationException.class, (e) -> {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

                String errorBody = "{\"code\":\"" + e.getCode() + "\",\"message\":\"" + e.getI18nCode() + "\",\"status\":401}";
                byte[] bytes = errorBody.getBytes(StandardCharsets.UTF_8);

                response.getHeaders().setContentLength(bytes.length);

                DataBuffer buffer = response.bufferFactory().wrap(bytes);

                return response.writeWith(Mono.just(buffer));
            });
    }
}
