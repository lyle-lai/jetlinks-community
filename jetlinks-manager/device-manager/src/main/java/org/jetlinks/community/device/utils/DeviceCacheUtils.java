package org.jetlinks.community.device.utils;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class DeviceCacheUtils {

    private final static Map<String, Mono<DeviceProductEntity>> productCache = Caffeine
        .newBuilder()
        .expireAfterAccess(Duration.ofSeconds(30))
        .<String, Mono<DeviceProductEntity>>build()
        .asMap();

    private final static Map<String, Mono<DeviceInstanceEntity>> deviceCache = Caffeine
        .newBuilder()
        .expireAfterAccess(Duration.ofSeconds(2))
        .<String, Mono<DeviceInstanceEntity>>build()
        .asMap();


    public static Mono<DeviceProductEntity> getProductOrLoad(String productId,
                                                             Function<String, Mono<DeviceProductEntity>> loader) {
        return Mono.defer(() -> productCache.computeIfAbsent(
            productId,
            k -> loader
                .apply(k)
                .cache(val -> Duration.ofSeconds(10),
                       error -> Duration.ZERO,
                       () -> Duration.ofSeconds(5))));
    }

    public static void addDeviceCache(DeviceInstanceEntity entity) {
        deviceCache.putIfAbsent(entity.getId(), Mono.just(entity));
    }

    public static Mono<DeviceInstanceEntity> getDeviceOrLoad(String deviceId,
                                                             Function<String, Mono<DeviceInstanceEntity>> loader) {
        return Mono.deferContextual(ctx -> {
            DeviceInstanceEntity inContext = ctx
                .<DeviceInstanceEntity>getOrEmpty(DeviceInstanceEntity.class).orElse(null);

            if (inContext != null && Objects.equals(inContext.getId(), deviceId)) {
                return Mono.just(inContext);
            }
            return deviceCache.computeIfAbsent(
                deviceId,
                k -> loader
                    .apply(k)
                    .cache(val -> Duration.ofSeconds(1),
                           error -> Duration.ZERO,
                           () -> Duration.ofSeconds(1)));
        });
    }

}
