package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.community.auth.service.AssetService;
import org.jetlinks.community.auth.web.request.AssetBindRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/assets")
@Resource(id = "asset-management", name = "资产管理")
@Tag(name = "资产管理")
@AllArgsConstructor
public class AssetController {

    private final AssetService assetService;

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
}
