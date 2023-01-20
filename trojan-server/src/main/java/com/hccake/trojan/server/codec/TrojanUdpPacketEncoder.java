package com.hccake.trojan.server.codec;

import inet.ipaddr.HostName;
import inet.ipaddr.HostNameException;
import inet.ipaddr.IPAddress;
import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.socket.DatagramPacket;
import io.netty5.handler.codec.ByteToMessageDecoder;
import io.netty5.handler.codec.DecoderException;
import io.netty5.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * DatagramPacket 编码为 Trojan UdpPacket
 * @author hccake
 */
@Slf4j
public class TrojanUdpPacketEncoder extends MessageToByteEncoder<DatagramPacket> {

    @Override
    protected Buffer allocateBuffer(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        return ctx.bufferAllocator().allocate(12);
    }

    /**
     * <pre>
     * +------+----------+----------+--------+---------+----------+
     * | ATYP | DST.ADDR | DST.PORT | Length |  CRLF   | Payload  |
     * +------+----------+----------+--------+---------+----------+
     * |  1   | Variable |    2     |   2    | X'0D0A' | Variable |
     * +------+----------+----------+--------+---------+----------+
     * </pre>
     *
     * @param ctx the {@link ChannelHandlerContext} which this
     *            {@link ByteToMessageDecoder} belongs to
     * @param msg the {@link DatagramPacket} from which to write data
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, DatagramPacket msg, Buffer out) throws Exception {

        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String host = remoteAddress.getHostString();
        int port = remoteAddress.getPort();
        TrojanAddressType dstAddrType = getDstAddrType(host);
        if (dstAddrType == null) {
            throw new DecoderException("error host: " + host);
        }
        // ATYP
        out.writeByte(dstAddrType.byteValue());
        TrojanAddressEncoder.DEFAULT.encodeAddress(dstAddrType, host, out);
        out.writeShort((short) port);

        Buffer content = msg.content();
        out.writeShort((short) content.readableBytes());
        out.writeByte((byte) '\r');
        out.writeByte((byte) '\n');
        out.writeBytes(content);
    }


    private static TrojanAddressType getDstAddrType(String host) {
        HostName hostName = new HostName(host);
        try {
            hostName.validate();
            if (hostName.isAddress()) {
                IPAddress addr = hostName.asAddress();
                return addr.getIPVersion().isIPv4() ? TrojanAddressType.IPv4 : TrojanAddressType.IPv6;
            } else {
                return TrojanAddressType.DOMAIN;
            }
        } catch (HostNameException e) {
            log.error("解析 Host 信息失败：{}", host);
        }
        return null;
    }

}
