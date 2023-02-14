package com.hccake.trojan.server;

import com.hccake.trojan.server.account.Account;
import com.hccake.trojan.server.account.AccountService;
import com.hccake.trojan.server.account.InMemoryAccountService;
import com.hccake.trojan.server.channel.AntiDetectionHandler;
import com.hccake.trojan.server.channel.TrojanMessageHandler;
import com.hccake.trojan.server.codec.TrojanMessageDecoder;
import com.hccake.trojan.server.env.TrojanServerProperties;
import com.hccake.trojan.server.util.Assert;
import com.hccake.trojan.server.util.StringUtils;
import io.netty5.channel.ChannelInitializer;
import io.netty5.channel.ChannelPipeline;
import io.netty5.channel.socket.SocketChannel;
import io.netty5.handler.flow.FlowControlHandler;
import io.netty5.handler.logging.LogLevel;
import io.netty5.handler.logging.LoggingHandler;
import io.netty5.handler.ssl.SslContext;
import io.netty5.handler.ssl.SslContextBuilder;
import io.netty5.handler.ssl.SslHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * @author hccake
 */
@Slf4j
@RequiredArgsConstructor
public final class TrojanProxyServerInitializer extends ChannelInitializer<SocketChannel> {

    private final TrojanServerProperties trojanServerProperties;

    @Override
    public void initChannel(SocketChannel ch) throws SSLException {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("LoggingHandler", new LoggingHandler(LogLevel.DEBUG));

        TrojanServerProperties.SslConfig ssl = trojanServerProperties.getSsl();
        SslContext sslContext = sslContext(ssl.getKey(), ssl.getCert(), ssl.getKeyPassword());
        SslHandler sslHandler = sslContext.newHandler(ch.bufferAllocator());
        pipeline.addLast(sslHandler);

        // 流量管控，某些情况下 autoread false 不起作用
        pipeline.addLast(new FlowControlHandler());
        pipeline.addLast(new TrojanMessageDecoder());

        String redirectHost = trojanServerProperties.getRedirectHost();
        int redirectPort = trojanServerProperties.getRedirectPort();
        AntiDetectionHandler antiDetectionHandler = new AntiDetectionHandler(redirectHost, redirectPort);
        pipeline.addLast(antiDetectionHandler);

        AccountService accountService = accountService(trojanServerProperties.getPasswords());
        TrojanMessageHandler trojanMessageHandler = new TrojanMessageHandler(accountService);
        pipeline.addLast(trojanMessageHandler);
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
