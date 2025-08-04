package org.jetlinks.community.rule.engine.alarm;

import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.rule.engine.scene.internal.triggers.DeviceTriggerProvider;
import org.jetlinks.community.things.holder.ThingsRegistryHolder;
import org.jetlinks.core.device.DeviceThingType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author bestfeng
 */
@Component
public class ProductAlarmTarget extends AbstractAlarmTarget {

    public static final String TYPE = "product";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getName() {
        return LocaleUtils
            .resolveMessage("message.rule_engine_alarm_product", "产品");
    }

    @Override
    public Integer getOrder() {
        return 100;
    }

    private final Set<String> configKeys = new HashSet<>();


    public ProductAlarmTarget() {
        configKeys.add(PropertyConstants.creatorId.getKey());
    }

    @Override
    public Flux<AlarmTargetInfo> doConvert(AlarmData data) {
        Map<String, Object> output = data.getOutput();
        String productId = AbstractAlarmTarget.getFromOutput("productId", output).map(String::valueOf).orElse(null);
        String productName = AbstractAlarmTarget.getFromOutput("productName", output).map(String::valueOf).orElse(productId);
        String deviceId = AbstractAlarmTarget.getFromOutput("deviceId", output).map(String::valueOf).orElse(null);
        if (deviceId == null || productId == null) {
            return Flux.empty();
        }
        return ThingsRegistryHolder
            .registry()
            .getThing(DeviceThingType.device, deviceId)
            .flatMap(thing -> thing
                .getTemplate()
                .flatMap(template-> template.getConfigs(configKeys)))
            .flatMapMany(values -> {
                String creatorId =  values.getValue(PropertyConstants.creatorId).orElse(null);
                return Flux.just(AlarmTargetInfo.of(productId, productName, getType(), creatorId));
            });
    }

    @Override
    public boolean isSupported(String trigger) {
        return DeviceTriggerProvider.PROVIDER.equals(trigger);
    };
}
