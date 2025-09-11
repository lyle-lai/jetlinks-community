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
        // 格式: properties_thingType_templateId
        String[] parts = metric.split("_");
        if (parts.length < 3) {
            // 直接调用父类doSave,保持和以前一样的动作
            return super.doSave(metric, data);
        }
        String thingType = parts[1];
        String templateId = parts[2];

        return registry
            .getTemplate(thingType,templateId)
            .flatMap(org.jetlinks.core.things.ThingTemplate::getMetadata)
            .flatMap(metadata -> {
                Map<String, Object> allValues = data.getData();
                long timestamp = data.getTimestamp();

                Map<String, Object> hotValues = new HashMap<>();
                Map<String, Object> coldValues = new HashMap<>();

                for (Map.Entry<String, Object> entry : allValues.entrySet()) {
                    String propertyId = entry.getKey();
                    metadata.getProperty(propertyId).ifPresent(propMeta -> {
                        Object frequency = propMeta.getExpands().get("frequency");
                        if ("high".equals(frequency)) {
                            hotValues.put(propertyId, entry.getValue());
                        } else {
                            coldValues.put(propertyId, entry.getValue());
                        }
                    });
                }

                Mono<Void> hot = Mono.empty();
                if (!hotValues.isEmpty()) {
                    String hotMetric = metric + "_hot";
                    TimeSeriesData hotData = TimeSeriesData.of(timestamp, hotValues);
                    hot = super.doSave(hotMetric, hotData);
                }

                Mono<Void> cold = Mono.empty();
                if (!coldValues.isEmpty()) {
                    TimeSeriesData coldData = TimeSeriesData.of(timestamp, coldValues);
                    cold = super.doSave(metric, coldData);
                }
                return Mono.zip(hot, cold).then();
            })
            .switchIfEmpty(super.doSave(metric, data)); // 如果找不到物模型,则调用默认的保存逻辑
    }
}