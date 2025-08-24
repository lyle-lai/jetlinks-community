package org.jetlinks.community.auth.service.thirdparty;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.User;
import org.hswebframework.web.authorization.exception.AuthenticationException;
import org.hswebframework.web.authorization.simple.SimpleDimension;
import org.hswebframework.web.authorization.simple.SimpleUser;
import org.jetlinks.community.auth.dimension.OpenPlatformDimensionType;
import org.jetlinks.community.auth.entity.OpenPlatformApiConfigEntity;
import org.jetlinks.community.auth.entity.OpenPlatformAppDeviceAuthEntity;
import org.jetlinks.community.auth.entity.OpenPlatformAppEntity;
import org.jetlinks.community.auth.enums.DefaultUserEntityType;
import org.jetlinks.community.auth.service.OpenPlatformApiConfigService;
import org.jetlinks.community.auth.service.OpenPlatformAppDeviceAuthService;
import org.jetlinks.community.auth.service.OpenPlatformAppService;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class DefaultOpenPlatformAuthenticationService implements OpenPlatformAuthenticationService {

    private final OpenPlatformAppService appService;
    private final OpenPlatformApiConfigService apiConfigService;
    private final OpenPlatformAppDeviceAuthService deviceAuthService;
    private final ReactiveRedisOperations<String, String> redis;

    private static final long TIMESTAMP_EXPIRE_SECONDS = TimeUnit.MINUTES.toSeconds(5); // 5 minutes
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();


    @Override
    public Mono<Authentication> authenticate(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String appId = request.getHeaders().getFirst("X-App-Id");
        String appKey = request.getHeaders().getFirst("X-App-Key");
        String timestamp = request.getHeaders().getFirst("X-Timestamp");
        String nonce = request.getHeaders().getFirst("X-Nonce");
        String signature = request.getHeaders().getFirst("X-Signature");

        if (StringUtils.isAnyBlank(appId, appKey, timestamp, nonce, signature)) {
            return Mono.empty(); // Not an open platform request, fall through
        }

        // 1. Validate timestamp
        try {
            long requestTimestamp = Long.parseLong(timestamp);
            if (Math.abs(System.currentTimeMillis() / 1000 - requestTimestamp) > TIMESTAMP_EXPIRE_SECONDS) {
                return Mono.error(new AuthenticationException("request_timestamp_expired", "error.request_timestamp_expired"));
            }
        } catch (NumberFormatException e) {
            return Mono.error(new AuthenticationException("illegal_timestamp_format", "error.illegal_timestamp_format"));
        }

        // 2. Validate nonce
        String nonceKey = "nonce:" + nonce;
        Mono<Boolean> nonceCheck = redis.opsForValue()
            .setIfAbsent(nonceKey, "1", Duration.ofSeconds(TIMESTAMP_EXPIRE_SECONDS));

        return nonceCheck
            .filter(Boolean::booleanValue)
            .switchIfEmpty(Mono.error(new AuthenticationException("nonce_replayed", "error.nonce_replayed")))
            .then(appService.createQuery().where(OpenPlatformAppEntity::getAppId, appId).fetchOne())
            .switchIfEmpty(Mono.error(new AuthenticationException("app_id_not_found", "error.app_id_not_found")))
            .flatMap(app -> {
                // 3. Validate App status and key
                if (app.getStatus() != 1) {
                    return Mono.error(new AuthenticationException("app_disabled", "error.app_disabled"));
                }
                if (!app.getAppKey().equals(appKey)) {
                    return Mono.error(new AuthenticationException("invalid_app_key", "error.invalid_app_key"));
                }

                // 4. Validate signature
                try {
                    String stringToSign = createSignString(appKey, timestamp, nonce, request);
                    String calculatedSignature = calculateSignature(stringToSign, app.getAppSecret());
                    if (!signature.equals(calculatedSignature)) {
                        return Mono.error(new AuthenticationException("invalid_signature", "error.invalid_signature"));
                    }
                } catch (Exception e) {
                    log.error("Signature calculation failed", e);
                    return Mono.error(new AuthenticationException("signature_calculation_failed", "error.signature_calculation_failed"));
                }

                // 5. Check API permission
                return apiConfigService.getApiConfigsByAppId(app.getId())
                    .filter(apiConfig -> {
                        boolean methodMatch = apiConfig.getRequestMethod().equalsIgnoreCase("ALL") ||
                            apiConfig.getRequestMethod().equalsIgnoreCase(request.getMethod().name());
                        boolean pathMatch = pathMatcher.match(apiConfig.getApiPath(), request.getPath().value());
                        return methodMatch && pathMatch;
                    })
                    .next()
                    .switchIfEmpty(Mono.error(new AuthenticationException("api_access_denied", "error.api_access_denied")))
                    .flatMap(apiConfig -> buildAuthentication(app));
            });
    }

    private String createSignString(String appKey, String timestamp, String nonce, ServerHttpRequest request) {
        // appKey + timestamp + nonce + requestPath + sortedQueryString
        String path = request.getPath().value();
        String queryString = request.getQueryParams().toSingleValueMap().entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining("&"));

        StringBuilder sb = new StringBuilder();
        sb.append(appKey).append(timestamp).append(nonce).append(path);
        if (StringUtils.isNotEmpty(queryString)) {
            sb.append("?").append(queryString);
        }
        return sb.toString();
    }

    private Mono<Authentication> buildAuthentication(OpenPlatformAppEntity app) {
        User user = new SimpleUser(app.getId(), app.getAppId(), app.getName(), DefaultUserEntityType.APPLICATION.getId(), Collections.emptyMap());

        return deviceAuthService.createQuery()
            .where(OpenPlatformAppDeviceAuthEntity::getPlatformAppId, app.getId())
            .fetch()
            .collectList()
            .map(authRules -> {
                List<Dimension> dimensions = authRules.stream()
                    .map(rule -> {
                        SimpleDimension dimension = new SimpleDimension();
                        dimension.setId(rule.getResourceId());
                        dimension.setType(OpenPlatformDimensionType.openPlatform);
                        dimension.setName(rule.getResourceName());
                        Map<String, Object> dimensionOptions = new HashMap<>();
                        dimensionOptions.put("mode", rule.getMode().name());
                        dimensionOptions.put("platformAppId", app.getId());
                        dimension.setOptions(dimensionOptions);
                        return dimension;
                    })
                    .collect(Collectors.toList());

                Map<String, Serializable> attributes = new HashMap<>();
                attributes.put("app", app);

                ThirdPartyRequestAuthentication authentication = new ThirdPartyRequestAuthentication(attributes);
                authentication.setUser(user);
                authentication.setDimensions(dimensions);
                authentication.setPermissions(Collections.emptyList());

                return authentication;
            });
    }

    private String calculateSignature(String data, String secret) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
