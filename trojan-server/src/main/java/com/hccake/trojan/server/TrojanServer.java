package com.hccake.trojan.server;

import com.hccake.trojan.server.http.HttpStaticFileServerInitializer;
import io.netty5.bootstrap.ServerBootstrap;
import io.netty5.channel.Channel;
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
    private static final int TROJAN_PORT = 443;

    private static final int STATIC_FILE_PORT = 80;

    public static void main(String[] args) throws Exception {

        Channel ch1 = null;
        Channel ch2 = null;

        EventLoopGroup bossGroup = new MultithreadEventLoopGroup(1, NioHandler.newFactory());
        EventLoopGroup workerGroup = new MultithreadEventLoopGroup(NioHandler.newFactory());
        try {
            ServerBootstrap b1 = new ServerBootstrap();
            b1.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                    .childHandler(new TrojanProxyServerInitializer(sslContext()))
                    .childOption(ChannelOption.AUTO_READ, false);
            ch1 = b1.bind(TROJAN_PORT).asStage().get();

            ServerBootstrap b2 = new ServerBootstrap();
            b2.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new HttpStaticFileServerInitializer());
            ch2 = b2.bind(STATIC_FILE_PORT).asStage().get();
        } finally {
            if (ch1 != null) {
                ch1.closeFuture().asStage().sync();
            }
            if (ch2 != null) {
                ch2.closeFuture().asStage().sync();
            }

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
