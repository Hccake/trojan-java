package com.hccake.trojan.server.channel;

import com.hccake.trojan.server.account.Account;
import com.hccake.trojan.server.account.AccountService;
import com.hccake.trojan.server.codec.*;
import com.hccake.trojan.server.util.TrojanServerUtils;
import io.netty5.bootstrap.Bootstrap;
import io.netty5.buffer.Buffer;
import io.netty5.channel.*;
import io.netty5.channel.socket.DatagramPacket;
import io.netty5.channel.socket.nio.NioDatagramChannel;
import io.netty5.channel.socket.nio.NioSocketChannel;
import io.netty5.handler.flow.FlowControlHandler;
import io.netty5.util.concurrent.Future;
import io.netty5.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hccake
 */
@Slf4j
public final class TrojanMessageHandler extends SimpleChannelInboundHandler<TrojanMessage> {

    private final Bootstrap b = new Bootstrap();

    private final AccountService accountService;

    public TrojanMessageHandler(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 设置了 autoRead 为 false，所以这里需要手动 read 下
        ctx.read();
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final TrojanMessage trojanMessage) throws Exception {
        // 直接删除当前 handler 和 解码器
        ctx.pipeline().remove(TrojanMessageDecoder.class);
        ctx.pipeline().remove(TrojanMessageHandler.class);

        // 先记录消息 index, 在异常时 reset
        handleTrojanMessage(ctx, trojanMessage);
    }

    @Override
    public void channelExceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("trojan proxy error", cause);
        TrojanServerUtils.closeOnFlush(ctx.channel());
    }

    private void handleTrojanMessage(ChannelHandlerContext ctx, TrojanMessage trojanMessage) throws Exception {
        String trojanKey = trojanMessage.getKey();
        Account account = accountService.findByKey(trojanKey);
        if (account == null) {
            throw new RuntimeException("error request hash");
        }

        TrojanRequest trojanRequest = trojanMessage.getTrojanRequest();
        TrojanCommandType cmdType = trojanRequest.getCommandType();
        // 只支持 tcp 和 udp
        if (!(cmdType.equals(TrojanCommandType.CONNECT) || cmdType.equals(TrojanCommandType.UDP_ASSOCIATE))) {
            throw new RuntimeException("unsupported cmd type: " + cmdType);
        }
        final String dstAddr = trojanRequest.getDstAddr();
        final int dstPort = trojanRequest.getDstPort();
        log.debug("cmdType: {}, 请求目标地址为：[{}:{}]", cmdType, dstAddr, dstPort);

        // TODO 添加流量校验和统计
        final Channel userChannel = ctx.channel();

        Buffer payload = trojanMessage.getPayload();

        if (TrojanCommandType.CONNECT.equals(cmdType)) {
            Promise<Channel> promise = ctx.executor().newPromise();
            promise.asFuture().addListener(future -> {
                final Channel outboundChannel = future.getNow();
                if (future.isSuccess()) {
                    bindChannelAndWrite(userChannel, outboundChannel, payload);
                } else {
                    TrojanServerUtils.closeOnFlush(userChannel);
                }
            });

            b.group(userChannel.executor())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new DirectClientHandler(promise));

            b.connect(dstAddr, dstPort).addListener(future -> {
                if (!future.isSuccess()) {
                    // Close the connection if the connection attempt has failed.
                    TrojanServerUtils.closeOnFlush(ctx.channel());
                }
            });
        } else {
            Promise<Channel> promise = ctx.executor().newPromise();
            promise.asFuture().addListener(futureListener -> {
                final Channel outboundChannel = futureListener.getNow();
                if (futureListener.isSuccess()) {
                    outboundChannel.pipeline().addLast(new RelayHandler(userChannel));

                    ChannelPipeline userChannelPipeline = userChannel.pipeline();
                    userChannelPipeline.addLast(new TrojanUdpPacketEncoder());
                    userChannelPipeline.addLast(new TrojanUdpPacketDecoder());
                    userChannelPipeline.addLast(new RelayHandler(outboundChannel));
                    userChannelPipeline.remove(FlowControlHandler.class);
                    userChannel.setOption(ChannelOption.AUTO_READ, true);

                    if (payload != null) {
                        DatagramPacket datagramPacket = TrojanUdpPacketDecoder.getDatagramPacket(ctx, payload);
                        outboundChannel.writeAndFlush(datagramPacket);
                    }
                } else {
                    TrojanServerUtils.closeOnFlush(userChannel);
                }
            });

            Bootstrap udpBootStrap = new Bootstrap();
            udpBootStrap.group(userChannel.executor())
                    .channel(NioDatagramChannel.class)
                    .handler(new DirectClientHandler(promise));

            udpBootStrap.bind(0).addListener(futureListener -> {
                if (!futureListener.isSuccess()) {
                    TrojanServerUtils.closeOnFlush(userChannel);
                }
            });
        }
    }

    private static void bindChannelAndWrite(Channel inboundChannel, Channel outboundChannel, Object writeData) {
        if (writeData != null) {
            Future<Void> responseFuture = outboundChannel.writeAndFlush(writeData);
            responseFuture.addListener(channelFuture -> bindChannel(inboundChannel, outboundChannel));
        } else {
            bindChannel(inboundChannel, outboundChannel);
        }
    }

    private static void bindChannel(Channel inboundChannel, Channel outboundChannel) {
        outboundChannel.pipeline().addLast(new RelayHandler(inboundChannel));

        ChannelPipeline inboundChannelPipeline = inboundChannel.pipeline();
        inboundChannelPipeline.addLast(new RelayHandler(outboundChannel));
        inboundChannelPipeline.removeIfExists(FlowControlHandler.class);

        inboundChannel.setOption(ChannelOption.AUTO_READ, true);
    }

}
