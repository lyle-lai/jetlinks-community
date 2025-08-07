package org.jetlinks.community.network.tcp.parser.strateies;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.text.StringEscapeUtils;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;
import org.jetlinks.community.ValueObject;

import java.util.function.Supplier;

/**
 * 以分隔符读取数据包
 * 在 Vert.x 中，使用 io.vertx.core.parsetools.RecordParser.newDelimited() 方法按分隔符拆包时，
 * 默认不会保留分隔符（即，分隔符不会出现在处理后的 Buffer 中）。
 * 如果你需要“保留分隔符”，需要手动处理，因为 RecordParser 的设计初衷是“按 delimiter 分割记录”。
 *
 * @author zhouhao
 * @since 1.0
 */
public class DelimitedPayloadParserBuilder extends VertxPayloadParserBuilder {
    @Override
    public PayloadParserType getType() {
        return PayloadParserType.DELIMITED;
    }

    @Override
    @SneakyThrows
    protected Supplier<RecordParser> createParser(ValueObject config) {

        String delimited = config
            .getString("delimited")
            .map(String::trim)
            .orElseThrow(() -> new IllegalArgumentException("delimited can not be null"));

        if (delimited.startsWith("0x")) {

            byte[] hex = Hex.decodeHex(delimited.substring(2));
            return () -> RecordParser
                .newDelimited(Buffer.buffer(hex));
        }

        return () -> RecordParser.newDelimited(StringEscapeUtils.unescapeJava(delimited));
    }


}
