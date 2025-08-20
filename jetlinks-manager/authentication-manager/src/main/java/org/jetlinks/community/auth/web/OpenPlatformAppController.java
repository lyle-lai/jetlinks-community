package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.auth.service.OpenPlatformAppService;
import org.jetlinks.community.auth.entity.OpenPlatformAppEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/open-platform/application")
@Resource(id = "open-platform", name = "开放平台应用")
@Authorize
@Tag(name = "开放平台应用管理")
@AllArgsConstructor
public class OpenPlatformAppController implements ReactiveServiceCrudController<OpenPlatformAppEntity, String> {

    private final OpenPlatformAppService service;

    @Override
    public ReactiveCrudService<OpenPlatformAppEntity, String> getService() {
        return service;
    }
}
