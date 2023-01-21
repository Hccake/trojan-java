package com.hccake.trojan.server.channel;

import com.hccake.trojan.server.util.TrojanServerUtils;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandler;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class RelayHandler implements ChannelHandler {

	private final Channel relayChannel;

	public RelayHandler(Channel relayChannel) {
		this.relayChannel = relayChannel;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		ctx.writeAndFlush(ctx.bufferAllocator().allocate(0));
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		if (relayChannel.isActive()) {
			relayChannel.writeAndFlush(msg);
		}
		else {
			ReferenceCountUtil.release(msg);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		if (relayChannel.isActive()) {
			TrojanServerUtils.closeOnFlush(relayChannel);
		}
	}

	@Override
	public void channelExceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		String message = cause.getMessage();
		Channel channel = ctx.channel();
		if (message.startsWith("远程主机强迫关闭了一个现有的连接") || message.startsWith("An existing connection was forcibly closed")
				|| message.startsWith("Connection reset by peer")) {
			log.warn("{}，channel: {}", message, channel);
		}
		else {
			log.error("代理数据转发异常, channel: {}", channel, cause);
		}
		ctx.close();
	}

}
