package org.jetlinks.community.auth.thirdpart;

import lombok.AllArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.hswebframework.web.authorization.basic.web.AuthorizedToken;
import org.hswebframework.web.authorization.basic.web.ReactiveUserTokenParser;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.authorization.token.ParsedToken;
import org.hswebframework.web.authorization.token.TokenState;
import org.jetlinks.community.auth.service.OpenPlatformAppService;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@AllArgsConstructor
public class AppCredentialsTokenParser implements ReactiveUserTokenParser {

    private final OpenPlatformAppService appService;
    private final ReactiveRedisOperations<String, String> redis;
    private static final long TIMESTAMP_EXPIRE_SECONDS = TimeUnit.MINUTES.toSeconds(5);

    @Override
    public Mono<ParsedToken> parseToken(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String appId = request.getHeaders().getFirst("X-App-Id");
        String appKey = request.getHeaders().getFirst("X-App-Key");
        String timestamp = request.getHeaders().getFirst("X-Timestamp");
        String nonce = request.getHeaders().getFirst("X-Nonce");
        String signature = request.getHeaders().getFirst("X-Signature");

        if (appId == null || appKey == null || signature == null || timestamp == null || nonce == null) {
            return Mono.empty();
        }

        return isReplayAttack(timestamp, nonce)
            .filter(isReplay -> !isReplay)
            .switchIfEmpty(Mono.error(new UnAuthorizedException("error.replay_attack", TokenState.deny)))
            .then(appService.createQuery().where(org.jetlinks.community.auth.entity.OpenPlatformAppEntity::getAppId, appId).fetchOne())
            .switchIfEmpty(Mono.error(new UnAuthorizedException("error.app_id_not_found", TokenState.deny)))
            .flatMap(app -> {
                if (!app.getAppKey().equals(appKey)) {
                    return Mono.error(new UnAuthorizedException("error.invalid_app_key", TokenState.deny));
                }
                String serverSignature;
                try {
                    serverSignature = calculateSignature(appKey, timestamp, nonce, request, app.getAppSecret());
                } catch (Exception e) {
                    return Mono.error(new UnAuthorizedException("error.signature_calculation_failed", TokenState.deny));
                }

                if (!serverSignature.equals(signature)) {
                    return Mono.error(new UnAuthorizedException("error.invalid_signature", TokenState.deny));
                }

                return Mono.just(new AuthorizedToken() {
                    @Override
                    public String getUserId() {
                        return app.getAppId();
                    }

                    @Override
                    public String getToken() {
                        return signature;
                    }

                    @Override
                    public String getType() {
                        return "third-party-app";
                    }
                });
            });
    }

    private Mono<Boolean> isReplayAttack(String timestamp, String nonce) {
        try {
            long requestTimestamp = Long.parseLong(timestamp);
            if (Math.abs(System.currentTimeMillis() / 1000 - requestTimestamp) > TIMESTAMP_EXPIRE_SECONDS) {
                return Mono.just(true);
            }
        } catch (NumberFormatException e) {
            return Mono.just(true);
        }
        String nonceKey = "nonce:" + nonce;
        return redis.opsForValue()
            .setIfAbsent(nonceKey, "1", Duration.ofSeconds(TIMESTAMP_EXPIRE_SECONDS))
            .map(success -> !success);
    }

    private String calculateSignature(String appKey, String timestamp, String nonce, ServerHttpRequest request, String appSecret) throws Exception {
        String path = request.getPath().value();
        String queryString = request.getQueryParams().toSingleValueMap().entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining("&"));

        StringBuilder sb = new StringBuilder();
        sb.append(appKey).append(timestamp).append(nonce).append(path);
        if (StringUtils.hasText(queryString)) {
            sb.append("?").append(queryString);
        }
        String stringToSign = sb.toString();

        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return Hex.encodeHexString(sha256_HMAC.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
    }
}