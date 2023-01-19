package com.hccake.trojan.server.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;

import java.util.List;

/**
 * @author hccake
 */
public class TrojanToSocks5UdpPacketDecoder extends ReplayingDecoder<ByteBuf> {

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
	 * @param in the {@link ByteBuf} from which to read data
	 * @param out the {@link List} to which decoded messages should be added
	 */
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		// ATYP
		byte addrTypeByte = in.readByte();
		final Socks5AddressType addrType = Socks5AddressType.valueOf(addrTypeByte);

		ByteBuf socks5UdpPacket = ByteBufAllocator.DEFAULT.buffer();
		// RSV
		socks5UdpPacket.writeShort(0);
		// FRAG
		socks5UdpPacket.writeByte(0);
		// ATYP
		socks5UdpPacket.writeByte(addrTypeByte);

		// DST.ADDR
		if (addrType == Socks5AddressType.IPv4) {
			socks5UdpPacket.writeBytes(in, 4);
		}
		else if (addrType == Socks5AddressType.DOMAIN) {
			short domainLength = in.readUnsignedByte();
			socks5UdpPacket.writeByte(domainLength);
			socks5UdpPacket.writeBytes(in, domainLength);
		}
		else if (addrType == Socks5AddressType.IPv6) {
			socks5UdpPacket.writeBytes(in, 16);
		}
		else {
			throw new DecoderException("error addrType: " + addrType);
		}

		// DST.PORT
		socks5UdpPacket.writeBytes(in, 2);

		// payload length
		int length = in.readUnsignedShort();

		// CRLF
		if (in.readByte() != '\r' || in.readByte() != '\n') {
			throw new DecoderException("error trojan udp message, not CRLF");
		}

		socks5UdpPacket.writeBytes(in, length);

		out.add(socks5UdpPacket);
	}

}
