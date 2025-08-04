package org.jetlinks.community.rule.engine.alarm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.i18n.SingleI18nSupportEntity;

import java.util.Map;

/**
 * @author bestfeng
 */
@Getter
@Setter
public class AlarmLevelInfo implements SingleI18nSupportEntity {

    @Schema(description = "级别")
    private Integer level;

    @Schema(description = "名称")
    private String title;

    @JsonCodec
    @Schema(description = "国际化信息")
    private Map<String, String> i18nMessages;

    public String getTitle() {
        return getI18nMessage("title", title);
    }
}
