package org.jetlinks.community.auth.dimension;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import org.hswebframework.web.authorization.DimensionType;

/**
 * 开放平台维度类型
 *
 * @author gyl
 * @since 2.2
 */
@AllArgsConstructor
@Getter
@Generated
public enum OpenPlatformDimensionType implements DimensionType {
    openPlatform("open-platform", "开放平台");

    private final String id;
    private final String name;

}
