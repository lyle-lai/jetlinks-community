package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.jetlinks.community.auth.entity.OpenPlatformAppDeviceAuthEntity;
import org.jetlinks.community.auth.service.OpenPlatformAppDeviceAuthService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/open-platform/device-auth")
@Resource(id = "open-platform-device-auth", name = "开放平台设备授权")
@Authorize
@Tag(name = "开放平台-设备授权管理")
@AllArgsConstructor
public class OpenPlatformAppDeviceAuthController {

    private final OpenPlatformAppDeviceAuthService service;

    @GetMapping("/{platformAppId}")
    @Operation(summary = "获取应用的授权规则列表")
    public Flux<OpenPlatformAppDeviceAuthEntity> getAuthRules(
        @Parameter(description = "开放平台应用ID") @PathVariable String platformAppId) {
        return service.createQuery()
                      .where(OpenPlatformAppDeviceAuthEntity::getPlatformAppId, platformAppId)
                      .fetch();
    }

    @PostMapping("/{platformAppId}")
    @Operation(summary = "保存应用的授权规则列表（全量覆盖）")
    @SaveAction
    public Mono<Void> saveAuthRules(
        @Parameter(description = "开放平台应用ID") @PathVariable String platformAppId,
        @RequestBody Mono<List<OpenPlatformAppDeviceAuthEntity>> rulesMono) {
        return rulesMono
            .flatMapMany(Flux::fromIterable)
            .as(flux -> service.saveAuthRules(platformAppId, flux));
    }
}
