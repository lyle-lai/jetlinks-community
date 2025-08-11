package com.lyle;

import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.defaults.CompositeProtocolSupport;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.MetadataFeature;
import org.jetlinks.core.metadata.types.DoubleType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.core.spi.ProtocolSupportProvider;
import org.jetlinks.core.spi.ServiceContext;
import org.jetlinks.supports.official.JetLinksDeviceMetadata;
import org.jetlinks.supports.official.JetLinksPropertyMetadata;
import reactor.core.publisher.Mono;

public class HL7ProtocolSupportProvider implements ProtocolSupportProvider {

    @Override
    public Mono<? extends ProtocolSupport> create(ServiceContext context) {
        CompositeProtocolSupport support = new CompositeProtocolSupport();
        support.setId("hl7-2.6-protocol");
        support.setName("hl7-2.6-protocol");

        // 配置编解码
        support.addMessageCodecSupport(new TcpDeviceMessageCodec(context));

        //开启diffMetadataSameProduct特性
        support.addFeature(MetadataFeature.diffMetadataSameProduct);

        // 配置设备元数据
        JetLinksDeviceMetadata metadata = new JetLinksDeviceMetadata("protocol", "协议包物模A型");
        metadata.addProperty(new JetLinksPropertyMetadata("188436_MDC_TEMP_BLD", "温度", IntType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("150033_MDC_PRESS_BLD_ART_SYS", "有创收缩压", DoubleType.GLOBAL));
        support.addDefaultMetadata(DefaultTransport.TCP, metadata);

        //设置配置定义信息
        support.addConfigMetadata(DefaultTransport.TCP,new DefaultConfigMetadata(
            "TCP"
            , ""));

        return Mono.just(support);
    }
}
