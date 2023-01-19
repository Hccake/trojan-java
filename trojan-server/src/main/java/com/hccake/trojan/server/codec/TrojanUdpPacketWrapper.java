package com.hccake.trojan.server.codec;

import inet.ipaddr.HostName;
import inet.ipaddr.HostNameException;
import inet.ipaddr.IPAddress;
import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.handler.codec.ByteToMessageDecoder;
import io.netty5.handler.codec.DecoderException;
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
	 * @param in the {@link Buffer} from which to read data
	 */
	@Override
	protected void decode(ChannelHandlerContext ctx, Buffer in) throws Exception {

		// ATYP
		Buffer byteBuf = ctx.bufferAllocator().allocate(1);

		TrojanAddressType dstAddrType = getDstAddrType(host);
		if (dstAddrType == null) {
			throw new DecoderException("error host: " + host);
		}
		byteBuf.writeByte(dstAddrType.byteValue());
		TrojanAddressEncoder.DEFAULT.encodeAddress(dstAddrType, host, byteBuf);
		byteBuf.writeShort((short) 443);

		byteBuf.writeShort((short) in.readableBytes());
		byteBuf.writeByte((byte) '\r');
		byteBuf.writeByte((byte) '\n');
		byteBuf.writeBytes(in);

		ctx.fireChannelRead(byteBuf);
	}

	private static TrojanAddressType getDstAddrType(String host) {
		HostName hostName = new HostName(host);
		try {
			hostName.validate();
			if (hostName.isAddress()) {
				IPAddress addr = hostName.asAddress();
				return addr.getIPVersion().isIPv4() ? TrojanAddressType.IPv4 : TrojanAddressType.IPv6;
			}
			else {
				return TrojanAddressType.DOMAIN;
			}
		}
		catch (HostNameException e) {
			log.error("解析 Host 信息失败：{}", host);
		}
		return null;
	}

}
