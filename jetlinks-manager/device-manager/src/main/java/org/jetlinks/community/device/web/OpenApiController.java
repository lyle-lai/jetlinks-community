package org.jetlinks.community.device.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.jetlinks.community.device.entity.DeviceDetail;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
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

    @Autowired
    private LocalDeviceInstanceService deviceService;

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

    @Getter
    @Setter
    public static class DepartmentRequest {
        private List<String> departmentIds;
    }

    @PostMapping("/devices-by-departments")
    @Operation(summary = "根据科室ID查询权限下的设备,如果科室ID为空,则返回该用户权限下的所有设备")
    @Authorize
    public Flux<DeviceDetail> getDevicesByDepartments(@RequestBody DepartmentRequest request) {
        List<String> departmentIds = request.getDepartmentIds();
        QueryParamEntity query = QueryParamEntity.of().noPaging();

        if (!CollectionUtils.isEmpty(departmentIds)) {
            query.and("id", "in-department", departmentIds);
        }

        return deviceService.queryDeviceDetailList(query);
    }
}
