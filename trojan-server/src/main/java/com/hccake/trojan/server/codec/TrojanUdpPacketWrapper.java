package com.hccake.trojan.server.codec;

import inet.ipaddr.HostName;
import inet.ipaddr.HostNameException;
import inet.ipaddr.IPAddress;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.socksx.v5.Socks5AddressEncoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author hccake
 */
@Slf4j
public class TrojanUdpPacketWrapper extends ByteToMessageDecoder {

	private final String host;

	public TrojanUdpPacketWrapper(String host) {
		this.host = host;
	}

	/**
	 * <pre>
	 * +------+----------+----------+--------+---------+----------+
	 * | ATYP | DST.ADDR | DST.PORT | Length |  CRLF   | Payload  |
	 * +------+----------+----------+--------+---------+----------+
	 * |  1   | Variable |    2     |   2    | X'0D0A' | Variable |
	 * +------+----------+----------+--------+---------+----------+
	 * </pre>
	 * @param ctx the {@link ChannelHandlerContext} which this
	 * {@link ByteToMessageDecoder} belongs to
	 * @param in the {@link ByteBuf} from which to read data
	 * @param out the {@link List} to which decoded messages should be added
	 */
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

		// ATYP
		ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer();

		Socks5AddressType dstAddrType = getDstAddrType(host);
		if (dstAddrType == null) {
			throw new DecoderException("error host: " + host);
		}
		byteBuf.writeByte(dstAddrType.byteValue());
		Socks5AddressEncoder.DEFAULT.encodeAddress(dstAddrType, host, byteBuf);
		byteBuf.writeShort(443);

		byteBuf.writeShort(in.readableBytes());
		byteBuf.writeByte('\r');
		byteBuf.writeByte('\n');
		byteBuf.writeBytes(in);

		out.add(byteBuf);
	}

	private static Socks5AddressType getDstAddrType(String host) {
		HostName hostName = new HostName(host);
		try {
			hostName.validate();
			if (hostName.isAddress()) {
				IPAddress addr = hostName.asAddress();
				return addr.getIPVersion().isIPv4() ? Socks5AddressType.IPv4 : Socks5AddressType.IPv6;
			}
			else {
				return Socks5AddressType.DOMAIN;
			}
		}
		catch (HostNameException e) {
			log.error("解析 Host 信息失败：{}", host);
		}
		return null;
	}

}
