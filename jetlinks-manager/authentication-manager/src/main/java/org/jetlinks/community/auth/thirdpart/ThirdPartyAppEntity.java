package org.jetlinks.community.auth.thirdpart;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import javax.persistence.Table;
import javax.persistence.Column;

@Getter
@Setter
@Table(name = "s_third_party_app")
public class ThirdPartyAppEntity extends GenericEntity<String> {

    @Column(length = 64, nullable = false, updatable = false)
    private String appId;

    @Column(length = 128, nullable = false)
    private String appKey;

    @Column(length = 256, nullable = false)
    private String appSecret;

    @Column(length = 256, nullable = false)
    private String appName;

    @Column(length = 64, nullable = false)
    private String userId;
}
