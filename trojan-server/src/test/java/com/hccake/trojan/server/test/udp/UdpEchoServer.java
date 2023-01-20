package com.hccake.trojan.server.test.udp;

import io.netty5.bootstrap.Bootstrap;
import io.netty5.buffer.Buffer;
import io.netty5.channel.*;
import io.netty5.channel.nio.NioHandler;
import io.netty5.channel.socket.DatagramPacket;
import io.netty5.channel.socket.nio.NioDatagramChannel;
import io.netty5.handler.logging.LogLevel;
import io.netty5.handler.logging.LoggingHandler;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class UdpEchoServer {

	public static int PORT = 9090;

	public static void main(String[] args) throws InterruptedException {
		final Bootstrap bootstrap = new Bootstrap()
				.group(new MultithreadEventLoopGroup(NioHandler.newFactory()))
				.channel(NioDatagramChannel.class)
				.handler(new ChannelInitializer<NioDatagramChannel>() {
					@Override
					protected void initChannel(NioDatagramChannel ch) {
						ch.pipeline().addLast(new LoggingHandler(LogLevel.TRACE))
								.addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
									@Override
									protected void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg) {
										String contentStr = msg.content().toString(StandardCharsets.UTF_8);
										System.out.println(LocalDateTime.now().toString() + msg.sender() + ": " +contentStr);
										System.out.println();
										String response = contentStr + " x 2";
										Buffer responseBuffer = ctx.bufferAllocator().copyOf(response.getBytes(StandardCharsets.UTF_8));
										ctx.writeAndFlush(new DatagramPacket(responseBuffer, msg.sender()));
									}
								});
					}
				});
		bootstrap.bind(PORT).asStage().sync();
	}

}