package org.jetlinks.community.device.web.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DepartmentRequest {
    private List<String> departmentIds;
}
