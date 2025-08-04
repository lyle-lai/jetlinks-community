package org.jetlinks.community.device.function;

import org.jetlinks.core.device.DeviceThingType;
import org.jetlinks.core.things.ThingEvent;
import org.jetlinks.core.things.ThingsDataManager;
import org.jetlinks.reactor.ql.supports.map.FunctionMapFeature;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 在reactorQL中获取设备最新事件
 * <pre>{@code
 * select device.event.recent(deviceId,'eventId',timestamp) recent from ...
 *
 * select * from ... where device.event.recent(deviceId,'eventId',timestamp)  = 'xxx'
 * }</pre>
 *
 * @author zhouhao
 * @since 2.2
 */
@Component
public class DeviceEventFunction extends FunctionMapFeature {
    public DeviceEventFunction(ThingsDataManager dataManager) {
        super("device.event.recent", 3, 2, flux -> flux
            .collectList()
            .flatMap(args -> {
                if (args.size() < 2) {
                    return Mono.empty();
                }
                String deviceId = String.valueOf(args.get(0));
                String property = String.valueOf(args.get(1));
                long timestamp = args.size() > 2
                    ? CastUtils.castNumber(args.get(2))
                               .longValue()
                    : System.currentTimeMillis();

                return dataManager
                    .getLastEvent(DeviceThingType.device.getId(), deviceId, property, timestamp)
                    .map(ThingEvent::getData);
            }));
    }
}
