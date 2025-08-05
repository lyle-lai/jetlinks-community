package com.lyle;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v26.message.ACK;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import ca.uhn.hl7v2.parser.PipeParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.defaults.BlockingDeviceOperator;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DeviceOnlineMessage;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.MessagePayloadType;
import org.jetlinks.core.message.codec.SimpleEncodedMessage;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.core.monitor.logger.Logger;
import org.jetlinks.core.spi.ServiceContext;
import org.jetlinks.supports.protocol.blocking.BlockingDeviceMessageCodec;
import org.jetlinks.supports.protocol.blocking.BlockingMessageDecodeContext;
import org.jetlinks.supports.protocol.blocking.BlockingMessageEncodeContext;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
            logger.debug("收到设备TCP报文: {}", ByteBufUtil.hexDump(payload));
        }

        String hl7Str = null;

        try {
            // 读取 HL7 报文内容（不影响 readerIndex）
            hl7Str = payload.toString(payload.readerIndex(), payload.readableBytes(), StandardCharsets.UTF_8).trim();
            Message message = parser.parse(hl7Str);

            // 获取设备
            BlockingDeviceOperator device = context.getDevice();
            if (device == null || device.getDeviceId() == null) {
                boolean success = handleLogin(context);
                if (!success) {
                    sendAck(context, message, false); // 发送 AE
                    return;
                }
            }

            // 设备上线且报文有效，解析属性
            ReportPropertyMessage report = parsePayload(message, context, logger);
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
    private ReportPropertyMessage parsePayload(Message message, BlockingMessageDecodeContext context, Logger logger) throws HL7Exception {
        if (message instanceof ORU_R01) {
            ORU_R01 oruMessage = (ORU_R01) message;
            List<ORU_R01_PATIENT_RESULT> patientResults = oruMessage.getPATIENT_RESULTAll();
            Map<String, Object> properties = new HashMap<>();

            for (ORU_R01_PATIENT_RESULT patientResult : patientResults) {
                List<ORU_R01_ORDER_OBSERVATION> orderObservations = patientResult.getORDER_OBSERVATIONAll();

                for (ORU_R01_ORDER_OBSERVATION orderObservation : orderObservations) {
                    List<ORU_R01_OBSERVATION> observations = orderObservation.getOBSERVATIONAll();
                    for (ORU_R01_OBSERVATION observation : observations) {
                        OBX obx = observation.getOBX();

                        // OBX-3: 观测项标识（参数名） -> obx.getObservationIdentifier().getIdentifier().getValue()
                        // OBX-5: 观测值（参数值） -> obx.getObservationValue(0).getData().toString()
                        String paramName = Optional.ofNullable(obx.getObservationIdentifier())
                                                   .map(id -> id.getIdentifier().getValue())
                                                   .orElse(null);

                        String paramValue = obx.getObservationValue().length > 0
                            ? obx.getObservationValue(0).getData().toString()
                            : null;

                        if (paramName != null && paramValue != null) {
                            properties.put(paramName, paramValue);
                        }
                    }
                }
            }

            ReportPropertyMessage report = new ReportPropertyMessage();
            report.setDeviceId(context.getDevice().getDeviceId());
            report.setProperties(properties);

            logger.info("HL7 解析成功, deviceId={}, properties={}", report.getDeviceId(), properties);

            return report;
        } else {
            logger.warn("不支持的 HL7 消息类型: {}", message.getClass().getSimpleName());
        }
        return null;
    }

    //处理登录逻辑
    private boolean handleLogin(BlockingMessageDecodeContext context) {
        BlockingDeviceOperator device = context.getDevice();
        //可能是首次连接,没有识别到当前设备,需要进行认证等处理.
        if (device == null) {
            String hostName = context.getSession().getClientAddress().get().getHostName();
            logger(hostName).debug("设备登录");
            device = context.getDevice(hostName);
            if (device == null) {
                logger(hostName).warn("设备不存在或未激活");
                context.disconnectLater();
                return false;
            }
        }

        DeviceOnlineMessage onlineMessage = new DeviceOnlineMessage();
        onlineMessage.setDeviceId(device.getDeviceId());
        context.sendToPlatformLater(onlineMessage);
        return true;
    }

    @Override
    protected void downstream(BlockingMessageEncodeContext blockingMessageEncodeContext) {

    }
}
