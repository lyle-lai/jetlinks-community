package org.jetlinks.community.auth.service;

import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.auth.entity.OpenPlatformAppEntity;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.hswebframework.web.cache.ReactiveCache;
import org.hswebframework.web.cache.ReactiveCacheManager;

import javax.annotation.PostConstruct;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
public class OpenPlatformAppService extends GenericReactiveCrudService<OpenPlatformAppEntity, String> {

    private final ReactiveCache<OpenPlatformAppEntity> appCache;

    public OpenPlatformAppService(ReactiveCacheManager cacheManager) {
        this.appCache = cacheManager.getCache("open-platform-app");
    }

    public Mono<OpenPlatformAppEntity> findByAppId(String appId) {
        return appCache.mono("app-id:" + appId)
            .onCacheMissResume(() -> createQuery()
                .where(OpenPlatformAppEntity::getAppId, appId)
                .fetchOne()
            );
    }

    @Override
    public Mono<SaveResult> save(Publisher<OpenPlatformAppEntity> entityPublisher) {
        return Flux.from(entityPublisher)
                   .collectList()
                   .flatMap(list -> Flux
                       .fromIterable(list)
                       .flatMap(app -> app.getAppId() == null ? Mono.empty() : appCache.evict("app-id:" + app.getAppId()))
                       .then(super.save(list)));
    }

    @Override
    public Mono<Integer> updateById(String id, Mono<OpenPlatformAppEntity> entityPublisher) {
        return this
            .findById(id)
            .flatMap(old -> appCache
                .evict("app-id:" + old.getAppId())
                .then(super.updateById(id, entityPublisher)));
    }

    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return findById(Flux.from(idPublisher))
            .flatMap(app -> appCache.evict("app-id:" + app.getAppId()).thenReturn(app.getId()))
            .collectList()
            .filter(list -> !list.isEmpty())
            .flatMap(list -> super.deleteById(Flux.fromIterable(list)));
    }

    @Override
    public Mono<Integer> insert(Publisher<OpenPlatformAppEntity> entityPublisher) {
        return Mono.from(entityPublisher)
                .map(this::generateEntity)
                .flatMap(super::insert);
    }

    private OpenPlatformAppEntity generateEntity(OpenPlatformAppEntity entity) {
        if (StringUtils.isEmpty(entity.getId())) {
            entity.setAppId(UUID.randomUUID().toString().replace("-", ""));
            entity.setAppKey(UUID.randomUUID().toString().replace("-", ""));
            SecureRandom random = new SecureRandom();
            byte[] secret = new byte[32];
            random.nextBytes(secret);
            entity.setAppSecret(Base64.getEncoder().encodeToString(secret));
            entity.setStatus((byte) 1);
        }
        return entity;
    }
}

