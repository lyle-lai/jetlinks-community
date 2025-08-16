package org.jetlinks.community.auth.service;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.jetlinks.community.auth.entity.DimensionDeviceEntity;
import org.jetlinks.community.auth.web.request.AssetBindRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@AllArgsConstructor
public class AssetService {

    private final DimensionDeviceService dimensionDeviceService;
    private final OrganizationService organizationService;

    @Transactional
    public Mono<Integer> bindDevice(List<AssetBindRequest> requests) {
        return Flux.fromIterable(requests)
            .flatMap(request -> {
                // For now, we only handle binding to organization
                if ("org".equals(request.getTargetType()) && "device".equals(request.getAssetType())) {
                    return this.doBind(request.getTargetId(), request.getAssetIdList());
                }
                return Mono.just(0);
            })
            .reduce(0, Integer::sum);
    }

    private Mono<Integer> doBind(String orgId, List<String> deviceIdList) {
        if (CollectionUtils.isEmpty(deviceIdList)) {
            return Mono.just(0);
        }
        // Find organization to get its name
        return organizationService.findById(orgId)
            .flatMap(org -> Flux.fromIterable(deviceIdList)
                .map(deviceId -> {
                    DimensionDeviceEntity deviceEntity = new DimensionDeviceEntity();
                    deviceEntity.setDeviceId(deviceId);
                    deviceEntity.setDeviceName(deviceId); // Using ID as name for now
                    deviceEntity.setDimensionId(orgId);
                    deviceEntity.setDimensionTypeId("org"); // The dimension type is organization
                    deviceEntity.setDimensionName(org.getName());
                    return deviceEntity;
                })
                .as(dimensionDeviceService::save)
                .map(SaveResult::getTotal)
            )
            .defaultIfEmpty(0);
    }

    @Transactional
    public Mono<Integer> unbindDevice(List<AssetBindRequest> requests) {
        return Flux.fromIterable(requests)
            .flatMap(request -> {
                if ("org".equals(request.getTargetType()) && "device".equals(request.getAssetType())) {
                    return this.doUnbind(request.getTargetId(), request.getAssetIdList());
                }
                return Mono.just(0);
            })
            .reduce(0, Integer::sum);
    }

    private Mono<Integer> doUnbind(String orgId, List<String> deviceIdList) {
        if (CollectionUtils.isEmpty(deviceIdList)) {
            return Mono.just(0);
        }
        return dimensionDeviceService
            .createDelete()
            .where(DimensionDeviceEntity::getDimensionTypeId, "org")
            .in(DimensionDeviceEntity::getDeviceId, deviceIdList)
            .and(DimensionDeviceEntity::getDimensionId, orgId)
            .execute();
    }
}