package org.jetlinks.community.tdengine.things;


import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import org.jetlinks.community.timeseries.TimeSeriesData;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

class HotColdSaveOperations extends TDengineColumnModeSaveOperations {

    public HotColdSaveOperations(ThingsRegistry registry,
                                 MetricBuilder metricBuilder,
                                 DataSettings settings,
                                 TDengineThingDataHelper helper) {
        super(registry, metricBuilder, settings, helper);
    }

    @Override
    protected Mono<Void> doSave(String metric, TimeSeriesData data) {
        String thingId = data.getString(metricBuilder.getThingIdProperty(), null);
        if (thingId == null) {
            return super.doSave(metric, data); //无法获取物ID
        }
        //缓存物模型,避免重复获取
        Mono<org.jetlinks.core.things.Thing> thingMono = registry
            .getThing("device", thingId)
            .cache();

        return thingMono
            .hasElement()
            .flatMap(has -> {
                //物模型存在,则走高低频率分离
                if (has) {
                    return thingMono
                        .flatMap(org.jetlinks.core.things.Thing::getTemplate)
                        .flatMap(template -> template.getMetadata())
                        .flatMap(metadata -> {
                            //只要有一个高频属性,则全部存入高频表.
                            boolean isHot = data.getData()
                                .keySet()
                                .stream()
                                .anyMatch(propertyId -> metadata
                                    .getProperty(propertyId)
                                    .map(propMeta -> "high".equals(propMeta.getExpands().get("frequency")))
                                    .orElse(false));

                            if (isHot) {
                                return super.doSave(metric + "_hot", data);
                            }
                            return super.doSave(metric, data);
                        });
                }
                //物模型不存在,则调用默认的保存逻辑
                return super.doSave(metric, data);
            });
    }
}