package org.jetlinks.community.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

@Getter
@Setter
@Table(name = "s_open_platform_api_config")
@Comment("开放平台API配置表")
@EnableEntityEvent
public class OpenPlatformApiConfigEntity extends GenericEntity<String> implements RecordCreationEntity {

    @Column(length = 64, nullable = false, updatable = false)
    @Schema(description = "开放平台应用ID")
    @NotBlank
    private String platformAppId;

    @Column(length = 256, nullable = false)
    @Schema(description = "API路径")
    @NotBlank
    private String apiPath;

    @Column(length = 16, nullable = false)
    @Schema(description = "请求方法")
    @NotBlank
    private String requestMethod; // GET, POST, PUT, DELETE

    @Column(length = 64)
    @Schema(description = "调用次数限制")
    private String rateLimit; // e.g., "60/m"

    @Column
    @DefaultValue("true")
    @Schema(description = "是否启用签名校验")
    private Boolean signatureVerificationEnabled;

    @Column(length = 512)
    @Schema(description = "描述")
    private String description;

    @Column(updatable = false)
    @Schema(description = "创建者ID", accessMode = Schema.AccessMode.READ_ONLY)
    private String creatorId;

    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    private Long createTime;
}
