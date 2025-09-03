package org.jetlinks.community.device.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "科室信息")
public class DepartmentInfo {
    @Schema(description = "科室ID")
    private String id;
    @Schema(description = "科室名称")
    private String name;
}
