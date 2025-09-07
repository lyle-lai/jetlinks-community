package org.jetlinks.community.device.message.writer;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.device.service.DeviceMetadataMappingService;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.community.gateway.annotation.Subscribe;
import reactor.core.publisher.Mono;

/**
 * 用于将设备消息写入到时序数据库
 *
 * @author zhouhao
 * @since 1.0
 */
@Slf4j
@AllArgsConstructor
public class TimeSeriesMessageWriterConnector {

    private final DeviceDataService dataService;
    private final DeviceMetadataMappingService deviceMetadataMappingService;
    private final EventBus eventBus;

    @Subscribe(topics = "/device/**", id = "device-message-ts-writer")
    @Generated
    public Mono<Void> writeDeviceMessageToTs(DeviceMessage message) {
        return deviceMetadataMappingService
            .transformDeviceData(message)
            .flatMap(data ->
                // 并行执行: 1. 保存到时序库, 2. 发布到新的'transformed'主题
                Mono.zip(
                    dataService
                        .saveDeviceMessage(data)
                        .onErrorResume(err -> {
                            log.warn("write device ts message error {}", data, err);
                            return Mono.empty();
                        }),
                    // 将转换后的消息发布到新的topic,供其他服务(如websocket)订阅
                    eventBus.publish("/device-transformed/" + data.getDeviceId(), data)
                ).then());
    }


}
