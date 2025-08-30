package org.jetlinks.community.auth.service.terms;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.hswebframework.ezorm.rdb.utils.SqlUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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

        Map<String, Object> valueAsMap = (Map<String, Object>) term.getValue();
        if (valueAsMap == null) {
            return fragments.addSql("1=2");
        }

        String assetType = (String) valueAsMap.get("assetType");
        if (!"device".equals(assetType)) {
            return fragments.addSql("1=2");
        }

        List<Map<String, Object>> targets = (List<Map<String, Object>>) valueAsMap.get("targets");
        if (CollectionUtils.isEmpty(targets)) {
            return fragments.addSql("1=2");
        }

        fragments.addSql("exists(select 1 from s_dimension_device dim where dim.device_id = ", columnFullName);
        fragments.addSql(" and (");

        Map<String, List<Object>> groupedByType = targets.stream()
            .collect(Collectors.groupingBy(t -> (String) t.get("type"),
                Collectors.mapping(t -> t.get("id"), Collectors.toList())));

        int groupIndex = 0;
        for (Map.Entry<String, List<Object>> entry : groupedByType.entrySet()) {
            String type = entry.getKey();
            List<Object> ids = entry.getValue();
            if (groupIndex++ > 0) {
                fragments.addSql(" or ");
            }
            fragments.addSql("(dim.dimension_type_id = ? and dim.dimension_id in (")
                .addParameter(type)
                .addFragments(SqlUtils.createQuestionMarks(ids.size()))
                .addSql("))");
            fragments.addParameter(ids);
        }
        fragments.addSql("))");
        return fragments;
    }
}