package org.jetlinks.community.auth.service.terms;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DimensionAssetTermBuilder extends AbstractTermFragmentBuilder {

    public DimensionAssetTermBuilder() {
        super("dim-assets", "按维度查询资产");
    }

    @Override
    @SuppressWarnings("unchecked")
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {
        PrepareSqlFragments fragments = PrepareSqlFragments.of();

        Object value = term.getValue();
        if (value == null) {
            return fragments;
        }

        Map<String, Object> valueAsMap;
        if (value instanceof Map) {
            valueAsMap = (Map<String, Object>) value;
        } else {
            return fragments;
        }

        String assetType = (String) valueAsMap.get("assetType");
        // Currently we only support device assets with this builder
        if (!"device".equals(assetType)) {
            return fragments;
        }

        List<Map<String, String>> targets = (List<Map<String, String>>) valueAsMap.get("targets");
        if (CollectionUtils.isEmpty(targets)) {
            return fragments;
        }

        // Filter by targets
        List<Map<String, String>> validTargets = targets.stream()
            .filter(t -> StringUtils.hasText(t.get("type")) && StringUtils.hasText(t.get("id")))
            .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(validTargets)) {
            return fragments;
        }

        if (term.getOptions().contains("not")) {
            fragments.addSql("not");
        }

        // Assumes the main query column (columnFullName) is the device ID.
        // Links to the s_dimension_device table on the device_id column.
        fragments.addSql("exists(select 1 from s_dimension_device dim where dim.device_id = ", columnFullName);

        fragments.addSql("and (");
        for (int i = 0; i < validTargets.size(); i++) {
            Map<String, String> target = validTargets.get(i);
            if (i > 0) {
                fragments.addSql("or");
            }
            fragments.addSql("(dim.dimension_type_id = ? and dim.dimension_id = ?)")
                     .addParameter(target.get("type"))
                     .addParameter(target.get("id"));
        }
        fragments.addSql("))"); // close the OR group and the EXISTS subquery

        return fragments;
    }
}