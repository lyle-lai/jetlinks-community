package org.jetlinks.community.device.message.writer;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.device.service.DeviceMetadataMappingService;
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


    @Subscribe(topics = "/device/**", id = "device-message-ts-writer")
    @Generated
    public Mono<Void> writeDeviceMessageToTs(DeviceMessage message) {
        return deviceMetadataMappingService
            .transformDeviceData(message)
            .flatMap(data ->
                         dataService
                             .saveDeviceMessage(message)
                             .onErrorResume(err -> {
                                 log.warn("write device message error {}", message, err);
                                 return Mono.empty();
                             }));
    }


}
