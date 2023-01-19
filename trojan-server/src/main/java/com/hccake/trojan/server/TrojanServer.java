package com.hccake.trojan.server;

import io.netty5.bootstrap.ServerBootstrap;
import io.netty5.channel.ChannelOption;
import io.netty5.channel.EventLoopGroup;
import io.netty5.channel.MultithreadEventLoopGroup;
import io.netty5.channel.nio.NioHandler;
import io.netty5.channel.socket.nio.NioServerSocketChannel;
import io.netty5.handler.logging.LogLevel;
import io.netty5.handler.logging.LoggingHandler;
import io.netty5.handler.ssl.SslContext;
import io.netty5.handler.ssl.SslContextBuilder;

import javax.net.ssl.SSLException;
import java.io.InputStream;

/**
 * @author hccake
 */
public final class TrojanServer {

    /**
     * trojan 协议端口号必须是 443
     */
    private static final int PORT = 443;

    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new MultithreadEventLoopGroup(1, NioHandler.newFactory());
        EventLoopGroup workerGroup = new MultithreadEventLoopGroup(NioHandler.newFactory());
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                    .childHandler(new TrojanProxyServerInitializer(sslContext()));
            b.bind(PORT).asStage().get().closeFuture().asStage().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static SslContext sslContext() throws SSLException {
        // 配置 ssl
        final SslContext sslCtx;
        // 开发测试，用本地的 key
        InputStream keyInputStream = TrojanProxyServerInitializer.class
                .getResourceAsStream("/certificate/pkcs8_localhost.key");
        InputStream keyCertChainInputStream = TrojanProxyServerInitializer.class
                .getResourceAsStream("/certificate/localhost.crt");
        sslCtx = SslContextBuilder.forServer(keyCertChainInputStream, keyInputStream).build();
        return sslCtx;
    }

}
