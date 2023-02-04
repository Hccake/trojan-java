package com.hccake.trojan.server;

import io.netty5.channel.EventLoopGroup;
import io.netty5.channel.MultithreadEventLoopGroup;
import io.netty5.channel.nio.NioHandler;

/**
 * @author hccake
 */
public final class Launcher {

    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new MultithreadEventLoopGroup(1, NioHandler.newFactory());
        EventLoopGroup workerGroup = new MultithreadEventLoopGroup(NioHandler.newFactory());

        TrojanServer trojanServer = new TrojanServer(bossGroup, workerGroup);
        HttpStaticFileServer httpStaticFileServer = new HttpStaticFileServer(bossGroup, workerGroup);
        try {
            httpStaticFileServer.start();
            trojanServer.start().asStage().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
