package org.jetlinks.community.device.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "精简设备信息")
public class SimpleDeviceDto {

    @Schema(description = "设备ID")
    private String id;

    @Schema(description = "设备名称")
    private String name;

    @Schema(description = "产品ID")
    private String productId;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "设备状态")
    private Object state;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "科室信息")
    private List<DepartmentInfo> departments;
}
