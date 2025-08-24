package org.jetlinks.community.device.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/open-api/v1")
@Tag(name = "开放平台接口")
public class OpenApiController {

    @GetMapping("/test")
    @Operation(summary = "测试接口")
    public Mono<Map<String, Object>> test() {
        return Mono.just(Collections.singletonMap("success", true));
    }
}
