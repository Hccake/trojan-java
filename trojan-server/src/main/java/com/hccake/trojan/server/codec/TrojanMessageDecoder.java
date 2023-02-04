package com.hccake.trojan.server.codec;

import com.hccake.trojan.server.exception.TrojanProtocolException;
import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * trojan message 解码， 消息体结构如下：
 * <pre>
 * +-----------------------+---------+----------------+---------+----------+
 * | hex(SHA224(password)) |  CRLF   | Trojan Request |  CRLF   | Payload  |
 * +-----------------------+---------+----------------+---------+----------+
 * |          56           | X'0D0A' |    Variable    | X'0D0A' | Variable |
 * +-----------------------+---------+----------------+---------+----------+
 * </pre>
 * where Trojan Request is a SOCKS5-like request:
 * <pre>
 * +-----+------+----------+----------+
 * | CMD | ATYP | DST.ADDR | DST.PORT |
 * +-----+------+----------+----------+
 * |  1  |  1   | Variable |    2     |
 * +-----+------+----------+----------+
 * </pre>
 *
 * <pre>
 * where:
 *
 *  o  CMD
 *      o  CONNECT X'01'
 *      o  UDP ASSOCIATE X'03'
 *  o  ATYP address type of following address
 *      o  IP V4 address: X'01'
 *      o  DOMAINNAME: X'03'
 *      o  IP V6 address: X'04'
 *  o  DST.ADDR desired destination address
 *  o  DST.PORT desired destination port in network octet order
 * </pre>
 *
 * @author hccake
 * @see <a href="https://trojan-gfw.github.io/trojan/protocol">trojan protocol</a>
 */
@Slf4j
public class TrojanMessageDecoder extends ByteToMessageDecoder {

    private static final String ERROR_REQUEST_MESSAGE = "error request message";


    public TrojanMessageDecoder() {
        this.setSingleDecode(true);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, Buffer in) throws Exception {
        int readerOffset = in.readerOffset();

        // 读取 trojan 的密码
        int hashLength = 56;
        byte[] passwordBytes = new byte[hashLength];
        in.readBytes(passwordBytes, 0, hashLength);
        String trojanKey = new String(passwordBytes, StandardCharsets.UTF_8);

        // 后续两个是 CRLF
        if (in.readByte() != '\r' || in.readByte() != '\n') {
            in.readerOffset(readerOffset);
            throw new TrojanProtocolException(ERROR_REQUEST_MESSAGE, in.copy());
        }

        // like socks5
        TrojanRequest trojanRequest;
        try {
            trojanRequest = TrojanRequestDecoder.DEFAULT.decodeRequest(in);
        } catch (Exception ex) {
            log.error("trojan request decode error", ex);
            throw new TrojanProtocolException(ERROR_REQUEST_MESSAGE, in.copy());
        }

        // 后续两个是 CRLF
        if (in.readByte() != '\r' || in.readByte() != '\n') {
            in.readerOffset(readerOffset);
            throw new TrojanProtocolException(ERROR_REQUEST_MESSAGE, in.copy());
        }

        // 载荷
        Buffer payload = null;
        int payloadLength = in.readableBytes();
        if (payloadLength > 0) {
            payload = ctx.bufferAllocator().allocate(payloadLength);
            payload.writeBytes(in);
        }

        TrojanMessage trojanMessage = new TrojanMessage();
        trojanMessage.setKey(trojanKey);
        trojanMessage.setTrojanRequest(trojanRequest);
        trojanMessage.setPayload(payload);

        ctx.fireChannelRead(trojanMessage);
    }
}
