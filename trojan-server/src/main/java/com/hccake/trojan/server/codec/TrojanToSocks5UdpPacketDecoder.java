package com.hccake.trojan.server.codec;

import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.handler.codec.ByteToMessageDecoder;
import io.netty5.handler.codec.DecoderException;

import java.nio.ByteBuffer;

/**
 * @author hccake
 */
public class TrojanToSocks5UdpPacketDecoder extends ByteToMessageDecoder {

	/**
	 * <pre>
	 * +------+----------+----------+--------+---------+----------+
	 * | ATYP | DST.ADDR | DST.PORT | Length |  CRLF   | Payload  |
	 * +------+----------+----------+--------+---------+----------+
	 * |  1   | Variable |    2     |   2    | X'0D0A' | Variable |
	 * +------+----------+----------+--------+---------+----------+
	 * </pre> <pre>
	 *  +-----+------+------+----------+----------+----------+
	 * | RSV | FRAG | ATYP | DST.ADDR | DST.PORT |   DATA   |
	 * +-----+------+------+----------+----------+----------+
	 * |  2  |  1   |  1   | Variable |    2     | Variable |
	 * +-----+------+------+----------+----------+----------+
	 *  </pre>
	 * @param ctx the {@link ChannelHandlerContext} which this
	 * {@link ByteToMessageDecoder} belongs to
	 * @param in the {@link Buffer} from which to read data
	 */
	@Override
	protected void decode(ChannelHandlerContext ctx, Buffer in) {
		// 先分配 10 个字节
		Buffer socks5UdpPacket = ctx.bufferAllocator().allocate(10);

		// RSV
		socks5UdpPacket.writeShort((short) 0);
		// FRAG
		socks5UdpPacket.writeByte((byte) 0);
		// ATYP
		byte addrTypeByte = in.readByte();
		final TrojanAddressType addrType = TrojanAddressType.valueOf(addrTypeByte);
		socks5UdpPacket.writeByte(addrTypeByte);

		// DST.ADDR
		if (addrType == TrojanAddressType.IPv4) {
			int ipv4Length = 4;
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(ipv4Length);
			in.readBytes(byteBuffer);
			socks5UdpPacket.writeBytes(byteBuffer.flip());
		}
		else if (addrType == TrojanAddressType.DOMAIN) {
			short domainLength = (short) in.readUnsignedByte();
			socks5UdpPacket.writeByte((byte) domainLength);
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(domainLength);
			in.readBytes(byteBuffer);
			socks5UdpPacket.writeBytes(byteBuffer.flip());
		}
		else if (addrType == TrojanAddressType.IPv6) {
			int ipv6Length = 16;
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(ipv6Length);
			in.readBytes(byteBuffer);
			socks5UdpPacket.writeBytes(byteBuffer.flip());
		}
		else {
			throw new DecoderException("error addrType: " + addrType);
		}

		// DST.PORT
		ByteBuffer portByteBuffer = ByteBuffer.allocateDirect(2);
		in.readBytes(portByteBuffer);
		socks5UdpPacket.writeBytes(portByteBuffer.flip());

		// payload length
		int length = in.readUnsignedShort();

		// CRLF
		if (in.readByte() != '\r' || in.readByte() != '\n') {
			throw new DecoderException("error trojan udp message, not CRLF");
		}

		ByteBuffer payloadByteBuffer = ByteBuffer.allocateDirect(length);
		in.readBytes(payloadByteBuffer);
		socks5UdpPacket.writeBytes(payloadByteBuffer.flip());

		ctx.fireChannelRead(socks5UdpPacket);
	}

}
