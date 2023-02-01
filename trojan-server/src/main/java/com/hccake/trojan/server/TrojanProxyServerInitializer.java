package com.hccake.trojan.server;

import com.hccake.trojan.server.channel.TrojanMessageHandler;
import com.hccake.trojan.server.codec.TrojanMessageDecoder;
import io.netty5.channel.ChannelInitializer;
import io.netty5.channel.ChannelPipeline;
import io.netty5.channel.socket.SocketChannel;
import io.netty5.handler.flow.FlowControlHandler;
import io.netty5.handler.logging.LogLevel;
import io.netty5.handler.logging.LoggingHandler;
import io.netty5.handler.ssl.SslContext;
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
		pipeline.addLast(sslContext.newHandler(ch.bufferAllocator()));
		// 流量管控，某些情况下 autoread false 不起作用
		pipeline.addLast(new FlowControlHandler());
		pipeline.addLast(new TrojanMessageDecoder());
		pipeline.addLast(new TrojanMessageHandler());
	}

}
