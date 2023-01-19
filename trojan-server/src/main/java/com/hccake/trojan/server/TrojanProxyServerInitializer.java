package com.hccake.trojan.server;

import com.hccake.trojan.server.channel.TrojanMessageHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hccake
 */
@Slf4j
@RequiredArgsConstructor
public final class TrojanProxyServerInitializer extends ChannelInitializer<SocketChannel> {

	private final SslContext sslContext;

	@Override
	public void initChannel(SocketChannel ch) {
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast("LoggingHandler", new LoggingHandler(LogLevel.DEBUG));
		pipeline.addLast(sslContext.newHandler(ch.alloc()));
		pipeline.addLast(new TrojanMessageHandler());
	}

}
