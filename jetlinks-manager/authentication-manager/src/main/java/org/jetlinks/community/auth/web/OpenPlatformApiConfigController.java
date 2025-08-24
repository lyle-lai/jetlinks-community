package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.auth.entity.OpenPlatformApiConfigEntity;
import org.jetlinks.community.auth.service.OpenPlatformApiConfigService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/open-platform/api-config")
@Resource(id = "open-platform-api-config", name = "开放平台API配置")
@Authorize
@Tag(name = "开放平台-API配置管理")
@AllArgsConstructor
public class OpenPlatformApiConfigController implements ReactiveServiceCrudController<OpenPlatformApiConfigEntity, String> {

    private final OpenPlatformApiConfigService service;

    @Override
    public ReactiveCrudService<OpenPlatformApiConfigEntity, String> getService() {
        return service;
    }

    @GetMapping("/{platformAppId}/all")
    @Operation(summary = "获取指定应用的所有API配置")
    public Flux<OpenPlatformApiConfigEntity> getApiConfigs(
        @Parameter(description = "开放平台应用ID") @PathVariable String platformAppId) {
        return service.getApiConfigsByAppId(platformAppId);
    }

        @PostMapping("/{platformAppId}")
    @Operation(summary = "批量保存或更新API配置")
    @SaveAction
    public Mono<Void> saveApiConfigs(
        @Parameter(description = "开放平台应用ID") @PathVariable String platformAppId,
        @RequestBody(required = false) Mono<List<OpenPlatformApiConfigEntity>> configsMono) {
        return configsMono
                .flatMapMany(Flux::fromIterable) // Convert list to Flux
                .switchIfEmpty(Flux.empty()) // If configsMono is empty (no body or null body), provide an empty Flux
                .collectList() // Collect back to list to ensure it's handled as a single unit
                .flatMap(list -> service.saveApiConfigs(platformAppId, Flux.fromIterable(list))); // Pass the Flux to service
    }


}
