package com.hccake.trojan.server;

import com.hccake.trojan.server.account.Account;
import com.hccake.trojan.server.account.AccountService;
import com.hccake.trojan.server.account.InMemoryAccountService;
import com.hccake.trojan.server.env.TrojanServerProperties;
import com.hccake.trojan.server.util.Assert;
import com.hccake.trojan.server.util.StringUtils;
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
import java.io.File;
import java.io.InputStream;
import java.util.List;

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

        AccountService accountService = accountService(trojanServerProperties.getPasswords());

        TrojanServerProperties.SslConfig ssl = trojanServerProperties.getSsl();
        SslContext sslContext = sslContext(ssl.getKey(), ssl.getCert(), ssl.getKeyPassword());

        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .childHandler(new TrojanProxyServerInitializer(sslContext, accountService))
                .childOption(ChannelOption.AUTO_READ, false);

        Channel ch = b.bind(TROJAN_PORT).asStage().get();
        return ch.closeFuture();
    }

    private static SslContext sslContext(String key, String cert, String keyPassword) throws SSLException {
        // 配置 ssl
        final SslContext sslCtx;
        if (StringUtils.hasText(key) && StringUtils.hasText(cert)) {
            sslCtx = SslContextBuilder.forServer(new File(key), new File(cert), keyPassword).build();
        } else {
            // 开发测试，用本地的 key
            InputStream keyInputStream = TrojanProxyServerInitializer.class
                    .getResourceAsStream("/certificate/pkcs8_localhost.key");
            InputStream keyCertChainInputStream = TrojanProxyServerInitializer.class
                    .getResourceAsStream("/certificate/localhost.crt");
            sslCtx = SslContextBuilder.forServer(keyCertChainInputStream, keyInputStream).build();
        }
        return sslCtx;
    }

    private AccountService accountService(List<String> passwords) {
        Assert.notEmpty(passwords, "密码不能为空");
        InMemoryAccountService accountService = new InMemoryAccountService();
        for (String password : passwords) {
            accountService.createAccount(new Account(password));
        }
        return accountService;
    }

}
