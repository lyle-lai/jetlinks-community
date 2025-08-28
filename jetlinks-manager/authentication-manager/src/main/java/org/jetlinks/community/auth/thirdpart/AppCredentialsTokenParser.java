package org.jetlinks.community.auth.thirdpart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.hswebframework.web.authorization.basic.web.AuthorizedToken;
import org.hswebframework.web.authorization.basic.web.ReactiveUserTokenParser;
import org.hswebframework.web.authorization.exception.AuthenticationException;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.authorization.token.ParsedToken;
import org.hswebframework.web.authorization.token.TokenState;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.auth.entity.OpenPlatformApiConfigEntity;
import org.jetlinks.community.auth.entity.OpenPlatformAppEntity;
import org.jetlinks.community.auth.service.OpenPlatformApiConfigService;
import org.jetlinks.community.auth.service.OpenPlatformAppService;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
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
@Slf4j
public class AppCredentialsTokenParser implements ReactiveUserTokenParser {

    private final OpenPlatformAppService appService;
    private final OpenPlatformApiConfigService apiConfigService;
    private final ReactiveRedisOperations<String, String> redis;
    private static final long TIMESTAMP_EXPIRE_SECONDS = TimeUnit.MINUTES.toSeconds(5);
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

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

        try {
            long requestTimestamp = Long.parseLong(timestamp);
            if (Math.abs(System.currentTimeMillis() / 1000 - requestTimestamp) > TIMESTAMP_EXPIRE_SECONDS) {
                return Mono.error(new UnAuthorizedException("request_timestamp_expired", TokenState.deny));
            }
        } catch (NumberFormatException e) {
            return Mono.error(new UnAuthorizedException("illegal_timestamp_format", TokenState.deny));
        }

        return isReplayAttack(nonce)
            .filter(isReplay -> !isReplay)
            .switchIfEmpty(Mono.error(new UnAuthorizedException("error.replay_attack", TokenState.deny)))
            .then(appService.createQuery().where(OpenPlatformAppEntity::getAppId, appId).fetchOne())
            .switchIfEmpty(Mono.error(new UnAuthorizedException("error.app_id_not_found", TokenState.deny)))
            .flatMap(app -> {
                if (!app.getAppKey().equals(appKey)) {
                    return Mono.error(new UnAuthorizedException("error.invalid_app_key", TokenState.deny));
                }
                if (app.getStatus() == null || app.getStatus() != 1) {
                    return Mono.error(new UnAuthorizedException("error.app_disabled", TokenState.deny));
                }
                if (app.getAuthorizationTypes() == null || !app.getAuthorizationTypes().contains("api")) {
                    return Mono.error(new UnAuthorizedException("error.api_access_denied", TokenState.deny));
                }

                return apiConfigService.getApiConfigsByAppId(app.getId())
                    .filter(apiConfig -> {
                        boolean methodMatch = apiConfig.getRequestMethod().equalsIgnoreCase("ALL") ||
                            apiConfig.getRequestMethod().equalsIgnoreCase(request.getMethod().name());
                        boolean pathMatch = pathMatcher.match(apiConfig.getApiPath(), request.getPath().value());
                        return methodMatch && pathMatch;
                    })
                    .next()
                    .switchIfEmpty(Mono.error(new UnAuthorizedException("error.api_access_denied", TokenState.deny)))
                    .flatMap(apiConfig ->
                        checkRateLimit(app.getId(), apiConfig)
                            .then(Mono.defer(() -> {
                                if (Boolean.FALSE.equals(apiConfig.getSignatureVerificationEnabled())) {
                                    return buildToken(app, signature);
                                }

                                String serverSignature;
                                try {
                                    serverSignature = calculateSignature(appKey, timestamp, nonce, request, app.getAppSecret());
                                } catch (Exception e) {
                                    log.error("Signature calculation failed", e);
                                    return Mono.error(new UnAuthorizedException("error.signature_calculation_failed", TokenState.deny));
                                }

                                if (!serverSignature.equals(signature)) {
                                    return Mono.error(new UnAuthorizedException("error.invalid_signature", TokenState.deny));
                                }

                                return buildToken(app, signature);
                            }))
                    );
            });
    }

    private Mono<ParsedToken> buildToken(OpenPlatformAppEntity app, String signature) {
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
    }

    private Mono<Boolean> isReplayAttack(String nonce) {
        String nonceKey = "nonce:" + nonce;
        return redis.opsForValue()
            .setIfAbsent(nonceKey, "1", Duration.ofSeconds(TIMESTAMP_EXPIRE_SECONDS))
            .map(success -> !success);
    }

    private Mono<Void> checkRateLimit(String appId, OpenPlatformApiConfigEntity apiConfig) {
        String rateLimit = apiConfig.getRateLimit();
        if (!StringUtils.hasText(rateLimit)) {
            return Mono.empty();
        }

        String[] parts = rateLimit.split("/");
        if (parts.length != 2) {
            log.warn("Invalid rate limit format for app {}: {}", appId, rateLimit);
            return Mono.empty();
        }

        try {
            long limit = Long.parseLong(parts[0]);
            if (limit <= 0) {
                return Mono.empty();
            }
            char unit = parts[1].toLowerCase().charAt(0);
            long durationSeconds;

            switch (unit) {
                case 's':
                    durationSeconds = 1;
                    break;
                case 'm':
                    durationSeconds = 60;
                    break;
                case 'h':
                    durationSeconds = 3600;
                    break;
                case 'd':
                    durationSeconds = 86400;
                    break;
                default:
                    log.warn("Invalid rate limit time unit for app {}: {}", appId, unit);
                    return Mono.empty();
            }

            String redisKey = "rate-limit:" + appId + ":" + apiConfig.getApiPath() + ":" + apiConfig.getRequestMethod();

            return redis.opsForValue()
                .increment(redisKey)
                .flatMap(count -> {
                    Mono<Long> countMono = Mono.just(count);
                    if (count == 1) {
                        countMono = redis.expire(redisKey, Duration.ofSeconds(durationSeconds)).thenReturn(count);
                    }
                    return countMono;
                })
                .flatMap(count -> {
                    if (count > limit) {
//                        return Mono.error(new AuthenticationException("rate_limit_exceeded", "error.rate_limit_exceeded"));
                        BusinessException businessException = new BusinessException("error.rate_limit_exceeded", "rate_limit_exceeded", 429);
                        return Mono.error(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "rate_limit_exceeded", businessException));
                    }
                    return Mono.empty();
                });

        } catch (NumberFormatException e) {
            log.warn("Invalid rate limit number format for app {}: {}", appId, rateLimit);
            return Mono.empty();
        }
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
