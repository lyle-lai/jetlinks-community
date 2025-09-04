package org.jetlinks.community.auth.thirdpart;

import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.basic.web.ReactiveUserTokenParser;
import org.hswebframework.web.authorization.token.ParsedToken;
import org.jetlinks.community.auth.common.AppCredentials;
import org.jetlinks.community.auth.common.AppCredentialsValidator;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@AllArgsConstructor
public class AppCredentialsTokenParser implements ReactiveUserTokenParser {

    private final AppCredentialsValidator validator;

    @Override
    public Mono<ParsedToken> parseToken(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();

        String appId = request.getHeaders().getFirst("X-App-Id");
        // 如果没有AppId,则认为不是第三方应用调用,直接返回,尝试其他认证方式.
        if (appId == null) {
            return Mono.empty();
        }

        AppCredentials credentials = AppCredentials.builder()
            .appId(appId)
            .appKey(request.getHeaders().getFirst("X-App-Key"))
            .timestamp(request.getHeaders().getFirst("X-Timestamp"))
            .nonce(request.getHeaders().getFirst("X-Nonce"))
            .signature(request.getHeaders().getFirst("X-Signature"))
            .requestPath(request.getPath().value())
            .requestMethod(request.getMethod())
            .queryParams(request.getQueryParams().toSingleValueMap())
            .build();

        return validator.validate(credentials);
    }
}
