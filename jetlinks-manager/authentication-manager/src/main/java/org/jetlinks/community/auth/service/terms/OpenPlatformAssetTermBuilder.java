package org.jetlinks.community.auth.service.terms;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.hswebframework.ezorm.rdb.utils.SqlUtils;
import org.jetlinks.community.auth.enums.ResourceType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Locale;

/**
 * 按开放平台查询资产
 * <pre>{@code
 * "terms":[{
 *      "column":"id$in-open-platform-asset$device$blacklist",
 *      "value": ["app-id-1","app-id-2"]
 * }]
 * }</pre>
 *
 * @author gyl
 * @since 2.2
 */
@Component
public class OpenPlatformAssetTermBuilder extends AbstractTermFragmentBuilder {

    public OpenPlatformAssetTermBuilder() {
        super("in-open-platform-asset", "按开放平台查询资产");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {
        PrepareSqlFragments fragments = PrepareSqlFragments.of();

        List<Object> appIds = convertList(column, term);
        if (CollectionUtils.isEmpty(appIds)) {
            return fragments.addSql("1=2");
        }

        List<String> options = term.getOptions();

        String resourceType = options
            .stream()
            .map(opt -> opt.toUpperCase(Locale.ROOT))
            .filter(opt -> {
                try {
                    ResourceType.valueOf(opt);
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            })
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("A valid asset type (device, product, organization) option is required for term " + term.getTermType()));

        // Default to whitelist, but allow overriding via option
        String mode = "WHITELIST";
        if (options.contains("blacklist")) {
            mode = "BLACKLIST";
        }

        // The 'not' option controls IN vs NOT IN
        if (options.contains("not")) {
            fragments.addSql(columnFullName, "not in");
        } else {
            fragments.addSql(columnFullName, "in");
        }

        fragments.addSql("(select resource_id from s_open_platform_device_auth where resource_type = ? and mode = ? and platform_app_id in (")
                 .addParameter(resourceType.toUpperCase(Locale.ROOT))
                 .addParameter(mode);


        fragments.add(SqlUtils.createQuestionMarks(appIds.size()))
                 .addParameter(appIds);

        fragments.addSql("))");

        return fragments;
    }
}
