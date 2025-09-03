package org.jetlinks.community.device.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.ezorm.rdb.executor.reactive.ReactiveSqlExecutor;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.jetlinks.community.device.entity.DeviceDetail;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.device.web.request.DepartmentRequest;
import org.jetlinks.community.device.web.response.DepartmentInfo;
import org.jetlinks.community.device.web.response.SimpleDeviceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/open-api/v1")
@Tag(name = "开放平台接口")
public class OpenApiController {

    @Autowired
    private LocalDeviceInstanceService deviceService;

    @Autowired
    private ReactiveSqlExecutor sqlExecutor;

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

    @PostMapping("/devices-by-departments")
    @Operation(summary = "根据科室ID查询权限下的设备,如果科室ID为空,则返回该用户权限下的所有设备")
    @Authorize
    public Flux<SimpleDeviceDto> getDevicesByDepartments(@RequestBody DepartmentRequest request) {
        List<String> departmentIds = request.getDepartmentIds();
        QueryParamEntity query = QueryParamEntity.of().noPaging();

        if (!CollectionUtils.isEmpty(departmentIds)) {
            query.and("id", "in-department", departmentIds);
        }

        return deviceService.queryDeviceDetailList(query)
            .collectList()
            .flatMapMany(deviceDetails -> {
                if (CollectionUtils.isEmpty(deviceDetails)) {
                    return Flux.empty();
                }
                List<String> deviceIds = deviceDetails.stream()
                    .map(DeviceDetail::getId)
                    .collect(Collectors.toList());

                if (deviceIds.isEmpty()) {
                    return Flux.fromIterable(deviceDetails)
                        .map(deviceDetail -> {
                            SimpleDeviceDto dto = new SimpleDeviceDto();
                            dto.setId(deviceDetail.getId());
                            dto.setName(deviceDetail.getName());
                            dto.setProductId(deviceDetail.getProductId());
                            dto.setProductName(deviceDetail.getProductName());
                            dto.setState(deviceDetail.getState());
                            dto.setCreateTime(deviceDetail.getCreateTime());
                            dto.setDepartments(Collections.emptyList());
                            return dto;
                        });
                }

                // 动态构建SQL以支持IN子句
                List<Object> params = new ArrayList<>();
                params.add("org");
                params.addAll(deviceIds);

                String placeholders = String.join(",", Collections.nCopies(deviceIds.size(), "?"));
                String sql = "SELECT t1.device_id, t2.code as dimension_id, t1.dimension_name " +
                             "FROM s_dimension_device t1 " +
                             "JOIN s_organization t2 ON t1.dimension_id = t2.id " +
                             "WHERE t1.dimension_type_id = ? AND t1.device_id IN (" + placeholders + ")";

                Mono<Map<String, List<DepartmentInfo>>> departmentsMapMono = sqlExecutor
                    .select(sql, params.toArray())
                    .collect(Collectors.groupingBy(
                        row -> (String) row.get("device_id"),
                        Collectors.mapping(
                            row -> new DepartmentInfo((String) row.get("dimension_id"), (String) row.get("dimension_name")),
                            Collectors.toList()
                        )
                    ));

                return departmentsMapMono
                    .defaultIfEmpty(Collections.emptyMap())
                    .flatMapMany(departmentsMap -> Flux.fromIterable(deviceDetails)
                        .map(deviceDetail -> {
                            SimpleDeviceDto dto = new SimpleDeviceDto();
                            dto.setId(deviceDetail.getId());
                            dto.setName(deviceDetail.getName());
                            dto.setProductId(deviceDetail.getProductId());
                            dto.setProductName(deviceDetail.getProductName());
                            dto.setState(deviceDetail.getState());
                            dto.setCreateTime(deviceDetail.getCreateTime());
                            dto.setDepartments(departmentsMap.getOrDefault(deviceDetail.getId(), Collections.emptyList()));
                            return dto;
                        })
                    );
            });
    }
}