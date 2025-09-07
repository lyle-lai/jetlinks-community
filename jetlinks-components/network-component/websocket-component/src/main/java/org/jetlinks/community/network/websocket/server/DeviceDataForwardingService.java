package org.jetlinks.community.network.websocket.server;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.network.websocket.enums.DataType;
import org.jetlinks.community.network.websocket.protocol.DeviceMessageProto;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class DeviceDataForwardingService {

    @Autowired
    private WebSocketDeviceDataHandler webSocketHandler;

    private byte[] convertDeviceMessageToProto(DeviceMessage deviceMessage) {
        try {
            // 针对 ReportPropertyMessage 进行优化,直接访问对象,避免toJson开销
            if (deviceMessage instanceof ReportPropertyMessage) {
                return convertReportPropertyMessageToProto((ReportPropertyMessage) deviceMessage);
            }
            // 预留其他消息类型的处理,保留原有的通用转换逻辑作为回退
            log.debug("Fallback to generic message conversion for type: {}", deviceMessage.getMessageType());
            return null;
        } catch (Exception e) {
            log.error("Failed to convert device message to protobuf", e);
            return null;
        }
    }

    /**
     * 针对ReportPropertyMessage的优化转换方法
     */
    private byte[] convertReportPropertyMessageToProto(ReportPropertyMessage reportMessage) {
        DeviceMessageProto.DeviceData.Builder dataBuilder = DeviceMessageProto.DeviceData.newBuilder();

        dataBuilder.setDeviceId(reportMessage.getDeviceId());
        // 优化: 直接使用long类型时间戳, 减少字符串转换和解析的开销, 同时减小数据体积.
        dataBuilder.setTime(reportMessage.getTimestamp());

        // 使用枚举来处理数据类型
        DataType dataType = reportMessage.getHeader("dataType")
                                         .map(String::valueOf)
                                         .map(s -> {
                                             try {
                                                 return Integer.parseInt(s);
                                             } catch (NumberFormatException e) {
                                                 return 0;
                                             }
                                         })
                                         .map(DataType::of)
                                         .orElse(DataType.PROPERTY);
        dataBuilder.setDataType(dataType.getValue());

        Map<String, Object> properties = reportMessage.getProperties();
        if (properties == null) {
            return dataBuilder.build().toByteArray();
        }

        // TakeTime从消息的PropertySourceTimes来
        Map<String, Long> sourceTimes = Optional
            .ofNullable(reportMessage.getPropertySourceTimes())
            .orElse(Collections.emptyMap());

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            DeviceMessageProto.ContentItem.Builder itemBuilder = DeviceMessageProto.ContentItem.newBuilder();

            String propertyKey = entry.getKey();
            itemBuilder.setCategory(propertyKey);
            itemBuilder.setUnit(""); // Unit is not available in ReportPropertyMessage
            itemBuilder.setAlarmValue(""); // AlarmValue is not available

            // 从 propertySourceTimes 获取采集时间
            long sourceTimestamp = sourceTimes.getOrDefault(propertyKey, 0L);
            // 优化: 直接使用long类型时间戳
            if (sourceTimestamp > 0) {
                itemBuilder.setTakeTime(sourceTimestamp);
            }

            DeviceMessageProto.ResultValue.Builder resultBuilder = DeviceMessageProto.ResultValue.newBuilder();
            Object result = entry.getValue();

            // 如果是波形数据，那对应值就是数组
            if (dataType == DataType.WAVEFORM) {
                DeviceMessageProto.Waveform.Builder waveformBuilder = DeviceMessageProto.Waveform.newBuilder();
                if (result instanceof Collection) {
                    for (Object val : (Collection<?>) result) {
                        if (val instanceof Number) {
                            waveformBuilder.addValues(((Number) val).intValue());
                        }
                    }
                } else if (result instanceof Object[]) {
                    for (Object val : (Object[]) result) {
                        if (val instanceof Number) {
                            waveformBuilder.addValues(((Number) val).intValue());
                        }
                    }
                }
                resultBuilder.setWaveformValue(waveformBuilder);

                // 约定:采样率放在PropertyState
                reportMessage.getPropertyState(propertyKey)
                             .ifPresent(rate -> {
                                 try {
                                     itemBuilder.setSamplingRate(Integer.parseInt(rate));
                                 } catch (NumberFormatException e) {
                                     log.warn("Invalid SamplingRate format for device {}: {}", reportMessage.getDeviceId(), rate);
                                 }
                             });
            } else { // It's a PROPERTY
                // 优化: 根据值的实际类型进行设置, 避免全部转换为字符串.
                // 这样可以减小最终protobuf报文的大小, 并提高客户端的处理效率.
                if (result instanceof Number) {
                    resultBuilder.setDoubleValue(((Number) result).doubleValue());
                } else if (result instanceof Boolean) {
                    resultBuilder.setBoolValue((Boolean) result);
                } else {
                    resultBuilder.setStringValue(String.valueOf(result));
                }
            }
            itemBuilder.setResult(resultBuilder);
            dataBuilder.addContent(itemBuilder);
        }

        return dataBuilder.build().toByteArray();
    }

    @Subscribe(topics = "/device/**", id = "websocket-data-forwarder")
    public Mono<Void> handleDeviceMessage(DeviceMessage message) {
        String deviceId = message.getDeviceId();
        if (deviceId == null) {
            return Mono.empty();
        }
        byte[] protoData = convertDeviceMessageToProto(message);
        if (protoData != null) {
            webSocketHandler.sendDataToSubscribers(deviceId, protoData);
        }
        return Mono.empty();
    }
}
