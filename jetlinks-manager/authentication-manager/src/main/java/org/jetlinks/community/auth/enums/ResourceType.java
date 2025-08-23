package org.jetlinks.community.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

@Getter
@AllArgsConstructor
public enum ResourceType implements EnumDict<String> {
    DEVICE("设备"),
    PRODUCT("产品"),
    ORGANIZATION("组织");


    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
