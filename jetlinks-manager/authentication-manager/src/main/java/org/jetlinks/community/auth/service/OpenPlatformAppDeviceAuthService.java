package org.jetlinks.community.auth.service;

import lombok.AllArgsConstructor;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.auth.entity.OpenPlatformAppDeviceAuthEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class OpenPlatformAppDeviceAuthService extends GenericReactiveCrudService<OpenPlatformAppDeviceAuthEntity, String> {

    /**
     * 保存指定应用的授权规则列表（全量覆盖）
     *
     * @param platformAppId 开放平台应用ID
     * @param entityFlux    规则实体流
     * @return void
     */
    @Transactional
    public Mono<Void> saveAuthRules(String platformAppId, Flux<OpenPlatformAppDeviceAuthEntity> entityFlux) {
        // 先删除该应用下的所有旧规则
        return createDelete()
            .where(OpenPlatformAppDeviceAuthEntity::getPlatformAppId, platformAppId)
            .execute()
            .then(
                // 然后保存新的规则
                entityFlux
                    // 确保每条规则都设置了正确的platformAppId
                    .doOnNext(entity -> entity.setPlatformAppId(platformAppId))
                    .as(this::save)
                    .then()
            );
    }
}
