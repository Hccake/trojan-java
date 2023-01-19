package com.hccake.trojan.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.InputStream;
import java.security.cert.CertificateException;

/**
 * @author hccake
 */
public final class TrojanServer {

    /**
     * trojan 协议端口号必须是 443
     */
    private static final int PORT = 443;

    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                    .childHandler(new TrojanProxyServerInitializer(sslContext()));
            b.bind(PORT).sync().channel().closeFuture().sync();
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
