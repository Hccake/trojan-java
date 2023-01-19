package com.hccake.trojan.server.channel;

import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandler;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.util.concurrent.Promise;

public final class DirectClientHandler implements ChannelHandler {

	private final Promise<Channel> promise;

	public DirectClientHandler(Promise<Channel> promise) {
		this.promise = promise;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		ctx.pipeline().remove(this);
		promise.setSuccess(ctx.channel());
	}

	@Override
	public void channelExceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
		promise.setFailure(throwable);
	}

}
