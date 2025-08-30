package org.jetlinks.community.device.service.term;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.hswebframework.ezorm.rdb.utils.SqlUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
public class DeviceInDepartmentTermBuilder extends AbstractTermFragmentBuilder {

    public DeviceInDepartmentTermBuilder() {
        super("in-department", "按科室查询设备");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {
        PrepareSqlFragments fragments = PrepareSqlFragments.of();
        List<Object> departmentIds = convertList(column, term);
        if (CollectionUtils.isEmpty(departmentIds)) {
            return fragments.addSql("1=2");
        }

        fragments.addSql("exists(select 1 from s_dimension_device dim where dim.device_id = ")
                 .addSql(columnFullName)
                 .addSql(" and dim.dimension_type_id = 'org' and dim.dimension_id in (")
                 .addFragments(SqlUtils.createQuestionMarks(departmentIds.size()))
                 .addSql("))")
                 .addParameter(departmentIds);

        return fragments;
    }
}

