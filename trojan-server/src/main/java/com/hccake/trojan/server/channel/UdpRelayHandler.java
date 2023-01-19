package com.hccake.trojan.server.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @author hccake
 */
@Slf4j
@RequiredArgsConstructor
public class UdpRelayHandler extends ChannelInboundHandlerAdapter {

	private final Channel userUdpChannel;

	private final InetSocketAddress userUdpAddress;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		log.trace("UdpData from Remote");
		// 把消息发到用户
		ByteBuf fakeHeader = Unpooled.wrappedBuffer(new byte[] { 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 });
		final ByteBuf data = Unpooled.wrappedBuffer(fakeHeader, (ByteBuf) msg);
		userUdpChannel.writeAndFlush(new DatagramPacket(data, userUdpAddress));
	}

}
