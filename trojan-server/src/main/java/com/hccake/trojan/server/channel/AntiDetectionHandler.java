package com.hccake.trojan.server.channel;

import com.hccake.trojan.server.codec.TrojanMessageDecoder;
import com.hccake.trojan.server.exception.TrojanProtocolException;
import com.hccake.trojan.server.util.TrojanServerUtils;
import io.netty5.bootstrap.Bootstrap;
import io.netty5.buffer.Buffer;
import io.netty5.channel.*;
import io.netty5.channel.socket.nio.NioSocketChannel;
import io.netty5.handler.codec.DecoderException;
import io.netty5.handler.flow.FlowControlHandler;
import io.netty5.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

/**
 * 防探测处理，对于非 trojan 协议的流量转发到 http 服务中
 *
 * @author hccake
 */
@Slf4j
public class AntiDetectionHandler implements ChannelHandler {

    private final String redirectHost;

    private final int redirectPort;

    public AntiDetectionHandler(String redirectHost, int redirectPort) {
        this.redirectHost = redirectHost;
        this.redirectPort = redirectPort;
    }

    @Override
    public void channelExceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Throwable ex = cause;
        if (cause instanceof DecoderException) {
            ex = cause.getCause();
        }
        if (ex instanceof TrojanProtocolException tpEx) {
            log.warn("not a trojan protocol message, now redirect to html, reason: {}", tpEx.getMessage());
            redirectToHtml(ctx, tpEx.getContent());
        } else {
            log.error("trojan proxy error", cause);
            TrojanServerUtils.closeOnFlush(ctx.channel());
        }
    }

    private void redirectToHtml(ChannelHandlerContext ctx, Buffer msg) {
        ctx.pipeline().remove(TrojanMessageDecoder.class);
        ctx.pipeline().remove(TrojanMessageHandler.class);
        ctx.pipeline().remove(AntiDetectionHandler.class);

        final Channel inboundChannel = ctx.channel();
        Promise<Channel> promise = ctx.executor().newPromise();
        Bootstrap b = new Bootstrap();
        b.group(inboundChannel.executor()).channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new DirectClientHandler(promise));

        b.connect(redirectHost, redirectPort).addListener(future -> {
            if (future.isSuccess()) {
                // Connection established use handler provided results
                Channel outboundChannel = future.getNow();

                outboundChannel.pipeline().addLast(new RelayHandler(inboundChannel));

                ChannelPipeline inboundChannelPipeline = inboundChannel.pipeline();
                inboundChannelPipeline.addLast(new RelayHandler(outboundChannel));
                inboundChannelPipeline.remove(FlowControlHandler.class);
                inboundChannel.setOption(ChannelOption.AUTO_READ, true);

                outboundChannel.writeAndFlush(msg);
            } else {
                // Close the connection if the connection attempt has failed.
                TrojanServerUtils.closeOnFlush(inboundChannel);
            }
        });
    }


}
