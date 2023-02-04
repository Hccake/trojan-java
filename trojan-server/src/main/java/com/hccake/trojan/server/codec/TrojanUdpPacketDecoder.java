package com.hccake.trojan.server.codec;

import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.socket.DatagramPacket;
import io.netty5.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * Trojan UdpPacket 解码为 DatagramPacket
 * @author hccake
 */
@Slf4j
public class TrojanUdpPacketDecoder extends ByteToMessageDecoder {

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
     * @param in  the {@link Buffer} from which to read data
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, Buffer in) throws Exception {
        DatagramPacket datagramPacket = getDatagramPacket(ctx, in);
        ctx.fireChannelRead(datagramPacket);
    }

    public static DatagramPacket getDatagramPacket(ChannelHandlerContext ctx, Buffer in) throws Exception {
        /*
         * 获取 UDP 数据包部分，udp 的实际请求地址根据这里的 addr 和 port 走
         * +------+----------+----------+--------+---------+----------+
         * | ATYP | DST.ADDR | DST.PORT | Length |  CRLF   | Payload  |
         * +------+----------+----------+--------+---------+----------+
         * |  1   | Variable |    2     |   2    | X'0D0A' | Variable |
         * +------+----------+----------+--------+---------+----------+
         */
        TrojanAddressType dstAddrType = TrojanAddressType.valueOf(in.readByte());
        final String dstAddr = TrojanAddressDecoder.DEFAULT.decodeAddress(dstAddrType, in);
        final int dstPort = in.readUnsignedShort();
        log.debug("udp 请求目标地址为：[{}:{}]", dstAddr, dstPort);

        // 数据长度
        int udpDataLength = in.readShort();
        log.info("udp data 长度为：{}", udpDataLength);

        // skip CRLF
        in.skipReadableBytes(2);

        ByteBuffer dataByteBuffer = ByteBuffer.allocateDirect(udpDataLength);
        in.readBytes(dataByteBuffer);
        Buffer dataBuffer = ctx.bufferAllocator().allocate(udpDataLength);
        dataBuffer.writeBytes(dataByteBuffer.flip());

        SocketAddress socketAddress = ctx.channel().remoteAddress();
        return new DatagramPacket(dataBuffer, new InetSocketAddress(dstAddr, dstPort), socketAddress);
    }

}
