package org.jetlinks.community.auth.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.GenericEntity;

import javax.persistence.Column;
import javax.persistence.Table;

@Getter
@Setter
@Table(name = "s_dimension_device")
public class DimensionDeviceEntity extends GenericEntity<String> {

    @Column(length = 64, nullable = false, updatable = false)
    private String deviceId;

    @Column(length = 256)
    private String deviceName;

    @Column(length = 64, nullable = false, updatable = false)
    private String dimensionTypeId;

    @Column(length = 64, nullable = false, updatable = false)
    private String dimensionId;

    @Column(length = 256)
    private String dimensionName;
}
