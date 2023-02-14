package com.hccake.trojan.server;

import com.hccake.trojan.server.env.TrojanServerProperties;
import io.netty5.bootstrap.ServerBootstrap;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelInitializer;
import io.netty5.channel.ChannelOption;
import io.netty5.channel.EventLoopGroup;
import io.netty5.channel.socket.SocketChannel;
import io.netty5.channel.socket.nio.NioServerSocketChannel;
import io.netty5.handler.codec.http.HttpObjectAggregator;
import io.netty5.handler.codec.http.HttpServerCodec;
import io.netty5.handler.logging.LogLevel;
import io.netty5.handler.logging.LoggingHandler;
import io.netty5.handler.stream.ChunkedWriteHandler;
import io.netty5.util.concurrent.Future;

/**
 * @author hccake
 */
public class HttpStaticFileServer {

    private static final int MAX_CONTENT_LENGTH = 65536;

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final TrojanServerProperties.HttpStaticFileServerConfig staticFileServerConfig;

    public HttpStaticFileServer(EventLoopGroup bossGroup, EventLoopGroup workerGroup, TrojanServerProperties.HttpStaticFileServerConfig staticFileServerConfig) {
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.staticFileServerConfig = staticFileServerConfig;
    }

    public Future<Void> start() throws Exception {
        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.SO_BACKLOG, 1024);

        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new HttpServerCodec(),
                                new HttpObjectAggregator<>(MAX_CONTENT_LENGTH),
                                new ChunkedWriteHandler(),
                                new HttpStaticFileServerHandler());
                    }
                });

        String host = staticFileServerConfig.getHost();
        int port = staticFileServerConfig.getPort();
        Channel ch = b.bind(host, port).asStage().get();
        return ch.closeFuture();
    }
}
