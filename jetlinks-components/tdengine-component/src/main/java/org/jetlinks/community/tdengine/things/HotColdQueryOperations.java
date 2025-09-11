package org.jetlinks.community.tdengine.things;

import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Iterator;
import java.util.function.Function;

class HotColdQueryOperations extends TDengineColumnModeQueryOperations {

    static final String DATA_SET_COLUMN = "dataSet";

    public HotColdQueryOperations(String thingType,
                                  String thingTemplateId,
                                  String thingId,
                                  MetricBuilder metricBuilder,
                                  DataSettings settings,
                                  ThingsRegistry registry,
                                  TDengineThingDataHelper helper) {
        super(thingType, thingTemplateId, thingId, metricBuilder, settings, registry, helper);
    }

    private String getMetricAndCleanParam(QueryParamEntity param) {
        String metric = metricBuilder.createPropertyMetric(thingType, thingTemplateId, thingId); // 默认查低频
        if (param == null || CollectionUtils.isEmpty(param.getTerms())) {
            return metric;
        }
        Iterator<Term> iterator = param.getTerms().iterator();
        while (iterator.hasNext()) {
            Term term = iterator.next();
            if (DATA_SET_COLUMN.equals(term.getColumn())) {
                iterator.remove(); // 从查询条件中移除,因为它不是一个真实的列
                if ("hot".equals(term.getValue())) {
                    metric = metricBuilder.createPropertyMetric(thingType, thingTemplateId, thingId) + "_hot";
                }
                break;
            }
        }
        return metric;
    }

    @Override
    protected Flux<TimeSeriesData> doQuery(String metric, Query<?, QueryParamEntity> query) {
        // 忽略传入的metric, 自己根据条件判断
        String realMetric = getMetricAndCleanParam(query.getParam());
        return helper.doQuery(realMetric, query);
    }

    @Override
    protected <T> Mono<PagerResult<T>> doQueryPage(String metric, Query<?, QueryParamEntity> query, Function<TimeSeriesData, T> mapper) {
        // 忽略传入的metric, 自己根据条件判断
        String realMetric = getMetricAndCleanParam(query.getParam());
        return helper.doQueryPage(realMetric, query, mapper);
    }

}