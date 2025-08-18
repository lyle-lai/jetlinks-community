package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;
import org.hswebframework.web.authorization.DimensionProvider;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.community.auth.service.AssetService;
import org.jetlinks.community.auth.web.request.AssetBindRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping(value = {"/assets", "/asset"})
@Resource(id = "asset-management", name = "资产管理")
@Tag(name = "资产管理")
@AllArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final List<DimensionProvider> providers;


    @GetMapping("/types")
    @Operation(summary = "获取所有资产类型")
    public Flux<AssetTypeView> getAssetTypes() {
        return Flux.fromIterable(providers)
                   .flatMap(DimensionProvider::getAllType)
                   .map(type -> AssetTypeView.of(type.getId(), type.getName()));
    }

    @PostMapping("/bind/device")
    @Operation(summary = "绑定设备到目标")
    @Authorize(merge = false)
    public Mono<Integer> bindDevice(@RequestBody List<AssetBindRequest> requests) {
        return assetService.bindDevice(requests);
    }

    @PostMapping("/unbind/device")
    @Operation(summary = "从目标解绑设备")
    @Authorize(merge = false)
    public Mono<Integer> unbindDevice(@RequestBody List<AssetBindRequest> requests) {
        return assetService.unbindDevice(requests);
    }

    @Getter
    @Setter
    @AllArgsConstructor(staticName = "of")
    @NoArgsConstructor
    public static class AssetTypeView {
        private String id;
        private String name;
    }
}
