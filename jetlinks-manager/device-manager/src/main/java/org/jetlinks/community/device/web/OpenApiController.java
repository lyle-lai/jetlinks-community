package org.jetlinks.community.device.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/open-api/v1")
@Tag(name = "开放平台接口")
public class OpenApiController {

    @GetMapping("/test")
    @Operation(summary = "测试接口")
    @Authorize
    public Mono<Map<String, Object>> test() {
        return Authentication.currentReactive()
            .map(auth -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);

                // Get dimensions and map them to a more descriptive structure
                List<Map<String, String>> dimensions = auth.getDimensions()
                    .stream()
                    .map(dim -> {
                        Map<String, String> dimInfo = new HashMap<>();
                        dimInfo.put("type", dim.getType().getId());
                        dimInfo.put("id", dim.getId());
                        dimInfo.put("name", dim.getName());
                        return dimInfo;
                    })
                    .collect(Collectors.toList());

                response.put("dataPermissions", dimensions);
                return response;
            })
            .defaultIfEmpty(Collections.singletonMap("error", "No authentication found"));
    }
}
