package com.hccake.trojan.server;

import com.hccake.trojan.server.env.TrojanServerProperties;
import io.netty5.bootstrap.ServerBootstrap;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelOption;
import io.netty5.channel.EventLoopGroup;
import io.netty5.channel.socket.nio.NioServerSocketChannel;
import io.netty5.handler.logging.LogLevel;
import io.netty5.handler.logging.LoggingHandler;
import io.netty5.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hccake
 */
public class TrojanServer {

    /**
     * trojan 协议端口号必须是 443
     */
    private static final int TROJAN_PORT = 443;

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final TrojanServerProperties trojanServerProperties;

    public TrojanServer(EventLoopGroup bossGroup, EventLoopGroup workerGroup, TrojanServerProperties trojanServerProperties) {
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.trojanServerProperties = trojanServerProperties;
    }

    public Future<Void> start() throws Exception {
        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.SO_BACKLOG, 1024);

        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .childHandler(new TrojanProxyServerInitializer(trojanServerProperties))
                .childOption(ChannelOption.AUTO_READ, false);

        Channel ch = b.bind(TROJAN_PORT).asStage().get();
        return ch.closeFuture();
    }

}
