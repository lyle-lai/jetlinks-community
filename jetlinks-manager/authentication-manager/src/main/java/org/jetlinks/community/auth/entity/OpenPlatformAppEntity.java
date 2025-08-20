package org.jetlinks.community.auth.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.JDBCType;

@Getter
@Setter
@Table(name = "s_open_platform_app")
@Comment("开放平台应用信息表")
public class OpenPlatformAppEntity extends GenericEntity<String> {

    @Column(length = 64, nullable = false, updatable = false)
    @Comment("应用ID (AppId)")
    private String appId;

    @Column(length = 128, nullable = false)
    @Comment("应用Key (AppKey)")
    private String appKey;

    @Column(length = 256, nullable = false)
    @Comment("应用密钥 (AppSecret)，存储加密后的值")
    private String appSecret;

    @Column(length = 256, nullable = false)
    @Comment("应用名称")
    private String name;

    @Column
    @ColumnType(jdbcType = JDBCType.CLOB)
    @Comment("应用描述")
    private String description;

    @Column(nullable = false)
    @ColumnType(jdbcType = JDBCType.SMALLINT)
    @Comment("状态，0:禁用, 1:启用")
    private Byte status;

    @Column(updatable = false)
    @Comment("创建者ID")
    private String creatorId;

    @Column(updatable = false)
    @Comment("创建时间")
    private Long createTime;
}
