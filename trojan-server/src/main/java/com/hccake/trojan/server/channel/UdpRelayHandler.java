package com.hccake.trojan.server.channel;

import io.netty5.buffer.Buffer;
import io.netty5.buffer.BufferAllocator;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandler;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.socket.DatagramPacket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @author hccake
 */
@Slf4j
@RequiredArgsConstructor
public class UdpRelayHandler implements ChannelHandler {

	private final Channel userUdpChannel;

	private final InetSocketAddress userUdpAddress;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		log.trace("UdpData from Remote");
		// 把消息发到用户
		Buffer messageBuffer = (Buffer) msg;
		BufferAllocator bufferAllocator = ctx.bufferAllocator();
		Buffer data = bufferAllocator.allocate(10 + messageBuffer.readableBytes());
		// 伪装头信息
		data.writeBytes(new byte[]{0, 0, 0, 1, 0, 0, 0, 0, 0, 0});
		data.writeBytes(messageBuffer);
		userUdpChannel.writeAndFlush(new DatagramPacket(data, userUdpAddress));
	}

}
