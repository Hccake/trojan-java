package com.hccake.trojan.server;

import com.hccake.trojan.server.account.Account;
import com.hccake.trojan.server.account.AccountService;
import com.hccake.trojan.server.account.InMemoryAccountService;
import io.netty5.bootstrap.ServerBootstrap;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelOption;
import io.netty5.channel.EventLoopGroup;
import io.netty5.channel.socket.nio.NioServerSocketChannel;
import io.netty5.handler.logging.LogLevel;
import io.netty5.handler.logging.LoggingHandler;
import io.netty5.handler.ssl.SslContext;
import io.netty5.handler.ssl.SslContextBuilder;
import io.netty5.util.concurrent.Future;

import javax.net.ssl.SSLException;
import java.io.InputStream;

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

    public TrojanServer(EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
    }

    public Future<Void> start() throws Exception {
        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.SO_BACKLOG, 1024);

        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .childHandler(new TrojanProxyServerInitializer(sslContext(), accountService()))
                .childOption(ChannelOption.AUTO_READ, false);

        Channel ch = b.bind(TROJAN_PORT).asStage().get();
        return ch.closeFuture();
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

    private AccountService accountService() {
        // a123456 TODO 后续抽取到配置文件中
        Account account = new Account("28d0bdd80b63fe9c847b405fd86a51cd9d4e7c66af99d61b6dd579b7");
        return new InMemoryAccountService(account);
    }

}
