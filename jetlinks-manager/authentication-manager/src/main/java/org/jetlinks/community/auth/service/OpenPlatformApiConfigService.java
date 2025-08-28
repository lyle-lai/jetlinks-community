package org.jetlinks.community.auth.service;

import org.hswebframework.web.cache.ReactiveCache;
import org.hswebframework.web.cache.ReactiveCacheManager;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.auth.entity.OpenPlatformApiConfigEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class OpenPlatformApiConfigService extends GenericReactiveCrudService<OpenPlatformApiConfigEntity, String> {

    public static final String CACHE_NAME = "open-platform-api-configs";

    private final ReactiveCacheManager cacheManager;

    private ReactiveCache<List<OpenPlatformApiConfigEntity>> cache;

    public OpenPlatformApiConfigService(ReactiveCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @PostConstruct
    public void init() {
        this.cache = cacheManager.getCache(CACHE_NAME);
    }

    @Transactional
    public Mono<Void> saveApiConfigs(String platformAppId, Flux<OpenPlatformApiConfigEntity> configs) {
        return cache
            .evict(platformAppId)
            .then(
                configs.collectList()
                    .flatMap(configList -> {
                        Mono<Integer> deleteMono = createDelete()
                            .where(OpenPlatformApiConfigEntity::getPlatformAppId, platformAppId)
                            .execute();

                        if (configList.isEmpty()) {
                            return deleteMono.then();
                        } else {
                            return deleteMono
                                .thenMany(Flux.fromIterable(configList))
                                .doOnNext(config -> config.setPlatformAppId(platformAppId))
                                .as(this::save)
                                .then();
                        }
                    })
            );
    }

    public Flux<OpenPlatformApiConfigEntity> getApiConfigsByAppId(String platformAppId) {
        return cache
            .mono(platformAppId)
            .onCacheMissResume(
                () -> createQuery()
                    .where(OpenPlatformApiConfigEntity::getPlatformAppId, platformAppId)
                    .fetch()
                    .collectList()
            )
            .flatMapIterable(list -> list);
    }

    @Transactional
    public Mono<Integer> deleteByPlatformAppId(String platformAppId) {
        return createDelete()
            .where(OpenPlatformApiConfigEntity::getPlatformAppId, platformAppId)
            .execute()
            .doOnSuccess(i -> {
                if (i > 0) {
                    cache.evict(platformAppId).subscribe();
                }
            });
    }
}