package org.jetlinks.community.auth.service.thirdparty;

import lombok.Getter;
import org.hswebframework.web.authorization.simple.SimpleAuthentication;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

@Getter
public class ThirdPartyRequestAuthentication extends SimpleAuthentication implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, Serializable> attributes;

    public ThirdPartyRequestAuthentication(Map<String, Serializable> attributes) {
        this.attributes = attributes;
    }

    @Override
    public Optional<Object> getAttribute(String name) {
        return Optional.ofNullable(attributes.get(name));
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        return attributes;
    }
}
