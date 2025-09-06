package org.jetlinks.community.network.websocket.server;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.network.websocket.protocol.DeviceMessageProto;
import org.jetlinks.core.message.DeviceMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
public class DeviceDataForwardingService {

    @Autowired
    private WebSocketDeviceDataHandler webSocketHandler;

    private byte[] convertDeviceMessageToProto(DeviceMessage deviceMessage) {
        try {
            Map<String, Object> jsonMap = deviceMessage.toJson();
            DeviceMessageProto.DeviceData.Builder dataBuilder = DeviceMessageProto.DeviceData.newBuilder();

            Object deviceName = jsonMap.get("deviceName");
            if (deviceName != null) {
                dataBuilder.setDeviceName(String.valueOf(deviceName));
            }
            dataBuilder.setDeviceId(deviceMessage.getDeviceId());
            dataBuilder.setTime(String.valueOf(jsonMap.getOrDefault("Time","")));

            Object content = jsonMap.get("Content");
            if (content instanceof java.util.List) {
                for (Object item : (java.util.List<?>) content) {
                    if (item instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        DeviceMessageProto.ContentItem.Builder itemBuilder = DeviceMessageProto.ContentItem.newBuilder();
                        itemBuilder.setCategory(String.valueOf(itemMap.getOrDefault("Category", "")));
                        itemBuilder.setUnit(String.valueOf(itemMap.getOrDefault("Unit", "")));
                        itemBuilder.setDataType(Integer.parseInt(String.valueOf(itemMap.getOrDefault("DataType", "0"))));
                        itemBuilder.setAlarmValue(String.valueOf(itemMap.getOrDefault("AlarmValue", "")));
                        itemBuilder.setTakeTime(String.valueOf(itemMap.getOrDefault("TakeTime", "")));

                        DeviceMessageProto.ResultValue.Builder resultBuilder = DeviceMessageProto.ResultValue.newBuilder();
                        Object result = itemMap.get("Result");
                        if (itemBuilder.getDataType() == 1 && result instanceof String) {
                            // Waveform
                            DeviceMessageProto.Waveform.Builder waveformBuilder = DeviceMessageProto.Waveform.newBuilder();
                            for (String s : ((String) result).split("\\^")) {
                                try {
                                    waveformBuilder.addValues(Integer.parseInt(s));
                                } catch (NumberFormatException e) {
                                    // ignore
                                }
                            }
                            resultBuilder.setWaveformValue(waveformBuilder);
                        } else {
                            resultBuilder.setStringValue(String.valueOf(result));
                        }
                        itemBuilder.setResult(resultBuilder);
                        dataBuilder.addContent(itemBuilder);
                    }
                }
            }

            return dataBuilder.build().toByteArray();
        } catch (Exception e) {
            log.error("Failed to convert device message to protobuf", e);
            return null;
        }
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
