package org.jetlinks.community.tdengine.metadata;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBTableMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.ddl.CreateTableSqlBuilder;
import org.hswebframework.utils.StringUtils;
import org.jetlinks.community.tdengine.TDengineConstants;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all")
@Getter
@Setter
public class TDengineCreateTableSqlBuilder implements CreateTableSqlBuilder {

    @Override
    public SqlRequest build(RDBTableMetadata table) {
        PrepareSqlFragments sql = PrepareSqlFragments.of();

        sql.addSql("CREATE STABLE IF NOT EXISTS", table.getName(), "(")
            .addSql("_ts timestamp");


        List<RDBColumnMetadata> tags = new ArrayList<>();
        for (RDBColumnMetadata column : table.getColumns()) {
            if (column.getProperty(TDengineConstants.COLUMN_IS_TS).isTrue()) {
                continue;
            }
            if (column.getProperty(TDengineConstants.COLUMN_IS_TAG).isTrue()) {
                tags.add(column);
                continue;
            }
            sql
                .addSql(",`" + column.getName() + "`")
                .addSql(column.getType().getName());
            if ((column.getType().getSqlType() == java.sql.JDBCType.NCHAR || column.getType().getSqlType() == java.sql.JDBCType.VARCHAR) && column.getLength() > 0) {
                sql.addSql("(" + column.getLength() + ")");
            }

        }
        sql.addSql(")");
        if (!tags.isEmpty()) {
            sql.addSql("TAGS (");
            int index = 0;
            for (RDBColumnMetadata tag : tags) {
                if (index++ > 0) {
                    sql.addSql(",");
                }
                sql
                    .addSql("`" + tag.getName() + "`")
                    .addSql(tag.getType().getName());
                if ((tag.getType().getSqlType() == java.sql.JDBCType.NCHAR || tag.getType().getSqlType() == java.sql.JDBCType.VARCHAR) && tag.getLength() > 0) {
                    sql.addSql("(" + tag.getLength() + ")");
                }
            }
            sql.addSql(")");
        }
        // 添加TTL支持
        String ttl = table.getAlias();
        if (!StringUtils.isNullOrEmpty(ttl)) {
            sql.addSql("KEEP " + ttl);
        }
        return sql.toRequest();
    }

}
