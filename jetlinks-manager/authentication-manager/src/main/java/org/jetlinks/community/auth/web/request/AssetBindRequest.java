package org.jetlinks.community.auth.web.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AssetBindRequest {
    private String targetType;
    private String targetId;
    private String assetType;
    private List<String> assetIdList;
}
