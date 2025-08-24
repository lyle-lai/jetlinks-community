package org.jetlinks.community.auth.service;

import lombok.AllArgsConstructor;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.auth.entity.OpenPlatformApiConfigEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class OpenPlatformApiConfigService extends GenericReactiveCrudService<OpenPlatformApiConfigEntity, String> {

    /**
     * 批量保存或更新API配置。
     * 如果存在相同platformAppId, apiPath, requestMethod的配置，则更新；否则新增。
     *
     * @param platformAppId 开放平台应用ID
     * @param configs       API配置列表
     * @return 保存结果
     */
    @Transactional
    public Mono<Void> saveApiConfigs(String platformAppId, Flux<OpenPlatformApiConfigEntity> configs) {
        // Collect the configs first to check if the Flux is empty
        return configs.collectList()
            .flatMap(configList -> {
                if (configList.isEmpty()) {
                    // If the list is empty, just delete all existing configs
                    return deleteByPlatformAppId(platformAppId).then();
                } else {
                    // If not empty, delete all existing and then save the new ones
                    return deleteByPlatformAppId(platformAppId)
                        .thenMany(Flux.fromIterable(configList)) // Use the collected list
                        .doOnNext(config -> {
                            config.setPlatformAppId(platformAppId);
                        })
                        .as(this::save)
                        .then();
                }
            });
    }

    /**
     * 根据应用ID查询所有API配置。
     *
     * @param platformAppId 开放平台应用ID
     * @return API配置列表
     */
    public Flux<OpenPlatformApiConfigEntity> getApiConfigsByAppId(String platformAppId) {
        return createQuery()
            .where(OpenPlatformApiConfigEntity::getPlatformAppId, platformAppId)
            .fetch();
    }

    /**
     * 删除指定应用下的所有API配置。
     *
     * @param platformAppId 开放平台应用ID
     * @return 删除数量
     */
    @Transactional
    public Mono<Integer> deleteByPlatformAppId(String platformAppId) {
        return createDelete()
            .where(OpenPlatformApiConfigEntity::getPlatformAppId, platformAppId)
            .execute();
    }
}
