package org.jetlinks.community.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.community.auth.enums.AuthorizationMode;
import org.jetlinks.community.auth.enums.ResourceType;

import javax.persistence.Column;
import javax.persistence.Table;

@Getter
@Setter
@Table(name = "s_open_platform_device_auth")
@Comment("开放应用设备授权规则表")
@EnableEntityEvent
public class OpenPlatformAppDeviceAuthEntity extends GenericEntity<String>  implements RecordCreationEntity {

    @Column(length = 64, nullable = false, updatable = false)
    @Schema(description = "开放平台应用ID")
    private String platformAppId;

    @Column(length = 32, nullable = false)
    @Schema(description = "授权模式")
    @EnumCodec
    @ColumnType(javaType = String.class)
    private AuthorizationMode mode;

    @Column(length = 32, nullable = false)
    @Schema(description = "资源类型")
    @EnumCodec
    @ColumnType(javaType = String.class)
    private ResourceType resourceType;

    @Column(length = 64, nullable = false)
    @Schema(description = "资源ID")
    private String resourceId;

    @Column(length = 256)
    @Schema(description = "资源名称")
    private String resourceName;

    @Column(updatable = false)
    @Schema(description = "创建者ID", accessMode = Schema.AccessMode.READ_ONLY)
    private String creatorId;

    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    private Long createTime;

}
