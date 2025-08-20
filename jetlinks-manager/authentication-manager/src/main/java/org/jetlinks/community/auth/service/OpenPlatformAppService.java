package org.jetlinks.community.auth.service;

import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.auth.entity.OpenPlatformAppEntity;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
public class OpenPlatformAppService extends GenericReactiveCrudService<OpenPlatformAppEntity, String> {


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

