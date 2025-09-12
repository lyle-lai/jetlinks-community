package org.jetlinks.community.tdengine.things;

import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrappers;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBTableMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.ddl.CreateTableSqlBuilder;
import org.jetlinks.community.tdengine.TDengineConstants;
import org.jetlinks.community.things.data.ThingsDataConstants;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.community.tdengine.metadata.TDengineSchema;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import reactor.core.publisher.Mono;

import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

class HotColdDDLOperations extends TDengineColumnModeDDLOperations {

    private final long hotDataTtl = 15; // 默认高频数据保留15天

    public HotColdDDLOperations(String thingType,
                                String templateId,
                                String thingId,
                                DataSettings settings,
                                MetricBuilder metricBuilder,
                                TDengineThingDataHelper helper) {
        super(thingType, templateId, thingId, settings, metricBuilder, helper);
    }

    @Override
    protected Mono<Void> register(MetricType metricType, String metric, List<PropertyMetadata> properties) {
        if (metricType == MetricType.properties) {
            List<PropertyMetadata> hotProperties = new ArrayList<>();
            List<PropertyMetadata> coldProperties = new ArrayList<>();
            for (PropertyMetadata property : properties) {
                if (property instanceof SimplePropertyMetadata){
                    hotProperties.add(property);
                    coldProperties.add(property);
                }

                if (property.getExpands() != null && "high".equals(property.getExpands().getOrDefault("frequency","low"))) {
                    hotProperties.add(property);
                } else {
                    coldProperties.add(property);
                }
            }

            String coldMetric = metric;
            String hotMetric = metric + "_hot";

            // 1. 为高低频数据分别注册元数据,这是应用层正确操作数据的前提
            super.helper.metadataManager.register(coldMetric, coldProperties);
            super.helper.metadataManager.register(hotMetric, hotProperties);

            // 2. 只为高频数据预创建带TTL的表
            Mono<Void> hotTable = createTableFor(hotMetric, hotProperties, hotDataTtl);

            // 3. 低频数据将由第一次save时自动创建,此处无需操作数据库
            return hotTable;
        }
        return super.register(metricType, metric, properties);
    }

    private Mono<Void> createTableFor(String metric, List<PropertyMetadata> properties, Long ttl) {
        TDengineSchema schema = new TDengineSchema("tdengine");
        RDBTableMetadata table = schema.newTable(metric);

        for (PropertyMetadata property : properties) {
            RDBColumnMetadata column = new RDBColumnMetadata();
            column.setName(property.getId());
            column.setComment(property.getName());

            // 手动进行类型映射
            org.hswebframework.ezorm.rdb.metadata.DataType rdbType = convertToRDBType(property.getValueType());
            if (rdbType != null) {
                column.setType(rdbType);
                // array, object , string , geo 都转为nchar
                if (rdbType.getSqlType() == JDBCType.ARRAY || rdbType.getSqlType() == JDBCType.NCHAR) {
                    // 从jetlinks的物模型定义中获取字符长度

                    int len = rdbType.getSqlType() == JDBCType.ARRAY ? 2048 : 255;
                    Object maxLength = property.getExpands() == null ? len :property.getExpands().getOrDefault("maxLength",len);
                    if (maxLength instanceof Number) {
                        len = ((Number) maxLength).intValue();
                    } else if (maxLength != null) {
                        try {
                            len = Integer.parseInt(String.valueOf(maxLength));
                        } catch (NumberFormatException ignore) {
                        }
                    }
                    column.setLength(len);
                    column.setType(org.hswebframework.ezorm.rdb.metadata.DataType.jdbc(JDBCType.NCHAR, String.class));
                }
            }

            if (Objects.equals(metricBuilder.getThingIdProperty(), property.getId())) {
                column.setProperty(TDengineConstants.COLUMN_IS_TAG, true);
            }
            table.addColumn(column);
        }

        if (ttl != null) {
            // 无扩展字段使用,此处临时使用Alias
            table.setAlias(ttl.toString());
        }

        CreateTableSqlBuilder builder = table.getSchema().findFeatureNow(CreateTableSqlBuilder.ID);
        String sql = builder.build(table).toNativeSql();

        return super.helper.operations.forQuery().query(sql, ResultWrappers.map()).then();
    }

    // 手动映射JetLinks数据类型到ORM框架数据类型
    private static org.hswebframework.ezorm.rdb.metadata.DataType convertToRDBType(org.jetlinks.core.metadata.DataType jetlinksType) {
        if (jetlinksType == null) {
            return null;
        }
        String typeId = jetlinksType.getId();
        switch (typeId) {
            case IntType.ID:
                return org.hswebframework.ezorm.rdb.metadata.DataType.jdbc(JDBCType.INTEGER, Integer.class);
            case LongType.ID:
                return org.hswebframework.ezorm.rdb.metadata.DataType.jdbc(JDBCType.BIGINT, Long.class);
            case DoubleType.ID:
                return org.hswebframework.ezorm.rdb.metadata.DataType.jdbc(JDBCType.DOUBLE, Double.class);
            case FloatType.ID:
                return org.hswebframework.ezorm.rdb.metadata.DataType.jdbc(JDBCType.FLOAT, Float.class);
            case BooleanType.ID:
                return org.hswebframework.ezorm.rdb.metadata.DataType.jdbc(JDBCType.BOOLEAN, Boolean.class);
            case DateTimeType.ID:
                return org.hswebframework.ezorm.rdb.metadata.DataType.jdbc(JDBCType.TIMESTAMP, Date.class);
            case ArrayType.ID:
                return org.hswebframework.ezorm.rdb.metadata.DataType.jdbc(JDBCType.ARRAY, String.class);
            case ObjectType.ID:
            case StringType.ID:
            default:
                // TDengine 推荐使用 NCHAR 存储字符串
                return org.hswebframework.ezorm.rdb.metadata.DataType.jdbc(JDBCType.NCHAR, String.class);
        }
    }
}