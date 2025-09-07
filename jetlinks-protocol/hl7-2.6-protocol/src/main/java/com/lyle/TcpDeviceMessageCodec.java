package com.lyle;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v26.message.ACK;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.jetlinks.core.defaults.BlockingDeviceOperator;
import org.jetlinks.core.message.DeviceOnlineMessage;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.MessagePayloadType;
import org.jetlinks.core.message.codec.SimpleEncodedMessage;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.monitor.logger.Logger;
import org.jetlinks.core.spi.ServiceContext;
import org.jetlinks.supports.protocol.blocking.BlockingDeviceMessageCodec;
import org.jetlinks.supports.protocol.blocking.BlockingMessageDecodeContext;
import org.jetlinks.supports.protocol.blocking.BlockingMessageEncodeContext;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class TcpDeviceMessageCodec extends BlockingDeviceMessageCodec {

    private final PipeParser parser = new PipeParser();

    public TcpDeviceMessageCodec(ServiceContext context) {
        super(context, DefaultTransport.TCP);
    }

    @Override
    protected void upstream(BlockingMessageDecodeContext context) {

        ByteBuf payload = context.getData().getPayload();
        Logger logger = context.logger();

        if (logger.isDebugEnabled()) {
            logger.debug("收到设备TCP报文(诊断): {}", ByteBufUtil.hexDump(payload));
        }

        String hl7Str = null;

        try {
            // 读取 HL7 报文内容（不影响 readerIndex）,同时保留分隔符
            hl7Str = payload.toString(payload.readerIndex(), payload.readableBytes(), StandardCharsets.UTF_8);

            // 去掉起始符 0x0B，如果存在
            if (!hl7Str.isEmpty() && hl7Str.charAt(0) == 0x0B) {
                hl7Str = hl7Str.substring(1);
            }

            // 禁用 Validation（重要）
            parser.setValidationContext(ValidationContextFactory.noValidation());
            Message message = parser.parse(hl7Str);

            // 获取设备mac地址
            Segment mshSegment = (Segment) message.get("MSH");
            MSH msh = (MSH) mshSegment;
            String deviceId = msh.getSendingApplication().getHd2_UniversalID().getValue();

            // 获取设备
            BlockingDeviceOperator device = context.getDevice();
            if (device == null || device.getDeviceId() == null) {
                boolean success = handleLogin(context,deviceId);
                if (!success) {
                    logger.error("设备登录失败,deviceId={}", deviceId);
                    sendAck(context, message, false); // 发送 AE
                    return;
                }
            }

            // 设备上线且报文有效，解析属性
            ReportPropertyMessage report = parsePayload(message, context, logger,deviceId);
            if (report != null) {
                context.sendToPlatformLater(report);
            }

            sendAck(context, message, true); // 发送 AA

        } catch (Exception e) {
            logger.error("处理 HL7 报文异常: \n{}", hl7Str, e);
            // 异常时也返回 AE
            try {
                Message failMessage = parser.parse(hl7Str);
                sendAck(context, failMessage, false);
            } catch (Exception ignored) {
            }
        } finally {
            // 主动释放 ByteBuf
            if (payload.refCnt() > 0) {
                payload.release();
            }
        }

    }

    private void sendAck(BlockingMessageDecodeContext context, Message incoming, boolean success) {
        try {
            ACK ack = (ACK) incoming.generateACK();

            if (!success) {
                ack.getMSA().getAcknowledgmentCode().setValue("AE");
                ack.getMSA().getTextMessage().setValue("设备认证失败或解析错误");
            }

            String ackStr = parser.encode(ack);
            ByteBuf ackBuf = Unpooled.buffer();
            ackBuf.writeByte(0x0B); // Start block
            ackBuf.writeBytes(ackStr.getBytes(StandardCharsets.UTF_8)); // HL7 message
            ackBuf.writeByte(0x1C); // End block
            ackBuf.writeByte(0x0D); // Carriage return

            // 封装为 EncodedMessage
            EncodedMessage ackMsg = new SimpleEncodedMessage(
                ackBuf,
                MessagePayloadType.HEX
            );

            context.sendToDeviceLater(ackMsg);

        } catch (Exception e) {
            context.logger().error("发送 ACK 失败", e);
        }
    }

    /// <summary>
    /// 解析payload,返回ReportPropertyMessage
    /// </summary>
    /// <param name="payload"></param>
    /// <param name="context"></param>
    /// <param name="logger"></param>
    /// <returns></returns>
    private ReportPropertyMessage parsePayload(Message message, BlockingMessageDecodeContext context, Logger logger, String deviceId) throws HL7Exception {
        if (!(message instanceof ORU_R01)) {
            logger.warn("Unsupported HL7 message type: {}", message.getClass().getSimpleName());
            return null;
        }

        ORU_R01 oruMessage = (ORU_R01) message;
        Map<String, Object> properties = new HashMap<>();
        Map<String, Long> sourceTimes = new HashMap<>();
        Map<String, String> propertyStates = new HashMap<>();
        final long currentTimeMillis = System.currentTimeMillis();
        // 使用原子引用, 以便在lambda中修改
        final AtomicReference<PayloadDataType> dataType = new AtomicReference<>(PayloadDataType.PROPERTY);

        oruMessage.getPATIENT_RESULTAll()
                  .stream()
                  .flatMap(pr -> {
                      try {
                          return pr.getORDER_OBSERVATIONAll().stream();
                      } catch (HL7Exception e) {
                          throw new RuntimeException(e);
                      }
                  })
                  .forEach(orderObs -> {
                      List<OBX> obxSegments = null;
                      try {
                          obxSegments = orderObs.getOBSERVATIONAll()
                                                          .stream()
                                                          .map(ORU_R01_OBSERVATION::getOBX)
                                                          .collect(Collectors.toList());
                      } catch (HL7Exception e) {
                          throw new RuntimeException(e);
                      }

                      // 优先查找是否存在波形数据 (值类型为NA)
                      Optional<OBX> waveformObxOpt = obxSegments.stream()
                                                                  .filter(obx -> "NA".equals(obx.getValueType().getValue()))
                                                                  .findFirst();

                      if (waveformObxOpt.isPresent()) {
                          dataType.set(PayloadDataType.WAVEFORM);
                          OBX waveformObx = waveformObxOpt.get();

                          // 1. 解析波形数据
                          String paramName = Optional.ofNullable(waveformObx.getObservationIdentifier())
                                                     .map(id -> id.getIdentifier().getValue() + "_" + id.getText().getValue())
                                                     .orElse("waveform");

                          // 修正: 移除 "NA[...]" 的包装
                          String rawWaveformString = waveformObx.getObservationValue(0).getData().toString();
                          String waveformContent = rawWaveformString;
                          if (rawWaveformString.startsWith("NA[") && rawWaveformString.endsWith("]")) {
                              waveformContent = rawWaveformString.substring(3, rawWaveformString.length() - 1);
                          }

                          String[] stringValues = waveformContent.split("\\^");
                          List<Integer> waveValues = Arrays.stream(stringValues)
                                                           .filter(s -> s != null && !s.isEmpty())
                                                           .map(Integer::parseInt)
                                                           .collect(Collectors.toList());

                          properties.put(paramName, waveValues);
                          sourceTimes.put(paramName, currentTimeMillis);

                          // 2. 从同组的其他OBX段中解析元数据, 如采样率
                          obxSegments.stream()
                                     .filter(obx -> obx != waveformObx)
                                     .forEach(metaObx -> {
                                         String metaName = Optional.ofNullable(metaObx.getObservationIdentifier())
                                                                 .map(id -> id.getText().getValue())
                                                                 .orElse("");
                                         if (metaObx.getObservationValue().length > 0) {
                                             String metaValue = metaObx.getObservationValue(0).getData().toString();
                                             if ("MDC_ATTR_SAMP_RATE".equals(metaName)) {
                                                 propertyStates.put(paramName, metaValue);
                                             }
                                         }
                                     });

                      } else {
                          // 3. 如果不是波形数据, 则按普通属性处理
                          obxSegments.forEach(obx -> {
                              String paramName = Optional.ofNullable(obx.getObservationIdentifier())
                                                         .map(id -> id.getIdentifier().getValue() + "_" + id.getText().getValue())
                                                         .orElse(null);
                              if (obx.getObservationValue() == null || obx.getObservationValue().length == 0) {
                                  return; // continue
                              }
                              String paramValue = obx.getObservationValue(0).getData().toString();

                              if (paramName != null && paramValue != null) {
                                  properties.put(paramName, paramValue);
                                  sourceTimes.put(paramName, currentTimeMillis);
                              }
                          });
                      }
                  });

        if (properties.isEmpty()) {
            return null;
        }

        ReportPropertyMessage report = new ReportPropertyMessage();
        report.setDeviceId(Optional.ofNullable(context.getDevice()).map(BlockingDeviceOperator::getDeviceId).orElse(deviceId));
        report.setProperties(properties);
        report.setTimestamp(currentTimeMillis);
        report.setPropertySourceTimes(sourceTimes);
        report.setPropertyStates(propertyStates);
        report.addHeader("dataType", dataType.get().getValue());

        logger.debug("HL7 parsed successfully(诊断), deviceId={}, dataType={}, properties.keys={}", report.getDeviceId(), dataType.get(), properties.keySet());

        return report;
    }

    //处理登录逻辑
    private boolean handleLogin(BlockingMessageDecodeContext context, String deviceId) throws HL7Exception {
        BlockingDeviceOperator device = context.getDevice();
        //可能是首次连接,没有识别到当前设备,需要进行认证等处理.
        if (device == null) {

            device = context.getDevice(deviceId);
            if (device == null) {
                // 根据IP获取设备
//                String hostName = context.getSession().getClientAddress().get().getHostName();
//                logger(hostName).debug("设备登录");
//                device = context.getDevice(hostName);
//                if (device == null) {
//                    logger(hostName).warn("设备不存在或未激活");
//                    context.disconnectLater();
//                    return false;
//                }
                context.disconnectLater();
                return false;
            }
            DeviceOnlineMessage onlineMessage = new DeviceOnlineMessage();
            onlineMessage.setDeviceId(device.getDeviceId());
            context.sendToPlatformNow(onlineMessage);

        }

        return true;
    }

    @Override
    protected void downstream(BlockingMessageEncodeContext blockingMessageEncodeContext) {

    }
}
