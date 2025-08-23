package org.jetlinks.community.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

@Getter
@AllArgsConstructor
public enum AuthorizationMode  implements EnumDict<String> {
    WHITELIST("白名单"),
    BLACKLIST("黑名单");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
