package com.lyle;

import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.defaults.CompositeProtocolSupport;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.core.spi.ProtocolSupportProvider;
import org.jetlinks.core.spi.ServiceContext;
import reactor.core.publisher.Mono;

public class HL7ProtocolSupportProvider implements ProtocolSupportProvider {

    @Override
    public Mono<? extends ProtocolSupport> create(ServiceContext context) {
        CompositeProtocolSupport support = new CompositeProtocolSupport();
        support.setId("hl7-2.6-protocol");
        support.setName("hl7-2.6-protocol");

        // 配置编解码
        support.addMessageCodecSupport(new TcpDeviceMessageCodec(context));

        //设置配置定义信息
        support.addConfigMetadata(DefaultTransport.TCP,new DefaultConfigMetadata(
            "TCP配置"
            , "").add("ipv4", "ipv4", "ip地址", new StringType()));

        return Mono.just(support);
    }
}
