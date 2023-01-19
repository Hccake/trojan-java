package com.hccake.trojan.server.channel;

import com.hccake.trojan.server.exception.TrojanProtocolException;
import com.hccake.trojan.server.util.TrojanServerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socks.SocksCmdType;
import io.netty.handler.codec.socksx.v5.Socks5AddressDecoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * @author hccake
 */
@Slf4j
@ChannelHandler.Sharable
public final class TrojanMessageHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final Bootstrap b = new Bootstrap();

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final ByteBuf message) throws Exception {
        ctx.pipeline().remove(this);
        // 关闭自动读取，等建连后，直接将后续的数据直接转发到
        ctx.channel().config().setAutoRead(false);
        try {
            handleTrojanMessage(ctx, message);
        } catch (TrojanProtocolException trojanProtocolException) {
            log.warn("not a trojan protocol message: " + trojanProtocolException.getMessage());
            message.resetReaderIndex();
            message.retain();
            redirectToHtml(ctx, message);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("trojan 处理异常", cause);
        TrojanServerUtils.closeOnFlush(ctx.channel());
    }

    private void redirectToHtml(ChannelHandlerContext ctx, ByteBuf message) {
        Promise<Channel> promise = ctx.executor().newPromise();

        final Channel inboundChannel = ctx.channel();
        b.group(inboundChannel.eventLoop()).channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000).option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new DirectClientHandler(promise));

        b.connect("127.0.0.1", 80).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                // Connection established use handler provided results
                Channel outboundChannel = future.channel();
                ChannelFuture responseFuture = outboundChannel.writeAndFlush(message);
                responseFuture.addListener((ChannelFutureListener) channelFuture -> {
                    outboundChannel.pipeline().addLast(new TcpRelayHandler(ctx.channel()));
                    ctx.pipeline().addLast(new TcpRelayHandler(outboundChannel));
                    ctx.channel().config().setAutoRead(true);
                });
            } else {
                // Close the connection if the connection attempt has failed.
                TrojanServerUtils.closeOnFlush(ctx.channel());
            }
        });
    }

    private void handleTrojanMessage(ChannelHandlerContext ctx, ByteBuf message) throws Exception {
        // +-----------------------+---------+----------------+---------+----------+
        // | hex(SHA224(password)) | CRLF | Trojan Request | CRLF | Payload |
        // +-----------------------+---------+----------------+---------+----------+
        // | 56 | X'0D0A' | Variable | X'0D0A' | Variable |
        // +-----------------------+---------+----------------+---------+----------+

        // 读取 trojan 的密码
        byte[] passwordBytes = new byte[56];
        message.getBytes(0, passwordBytes);
        String requestHash = new String(passwordBytes, StandardCharsets.UTF_8);
        // TODO requestHash 校验
        // hex(SHA224('a123456'))
        if(!"28D0BDD80B63FE9C847B405FD86A51CD9D4E7C66AF99D61B6DD579B7".equalsIgnoreCase(requestHash)) {
            throw new TrojanProtocolException("error request hash");
        }

        // 后续两个是 CRLF
        if (message.getByte(56) != '\r' || message.getByte(57) != '\n') {
            throw new TrojanProtocolException("error request message");
        }

        // 跳过密码位置 + CRLF
        message.skipBytes(58);

        // CMD
        // o CONNECT X'01'
        // o UDP ASSOCIATE X'03'
        byte cmdByte = message.readByte();
        SocksCmdType cmdType = SocksCmdType.valueOf(cmdByte);
        // 暂时只支持 CONNECT 和 UDP
        if (!(cmdType.equals(SocksCmdType.CONNECT) || cmdType.equals(SocksCmdType.UDP))) {
            throw new TrojanProtocolException("error cmd type");
        }
        log.trace("trojan cmdType :{}", cmdType);

        // TODO 添加流量校验和统计

        // ATYP address type of following address
        // o IP V4 address: X'01'
        // o DOMAINNAME: X'03'
        // o IP V6 address: X'04'
        final Socks5AddressType dstAddrType = Socks5AddressType.valueOf(message.readByte());
        final String dstAddr = Socks5AddressDecoder.DEFAULT.decodeAddress(dstAddrType, message);
        final int dstPort = message.readUnsignedShort();
        log.debug("请求目标地址为：[{}:{}]", dstAddr, dstPort);

        // TCP 则读取下剩余的数据
        if (SocksCmdType.CONNECT.equals(cmdType)) {
            // Connection established use handler provided results
            // skip CRLF
            message.skipBytes(2);
            int payloadLength = message.readableBytes();
            log.info("payload 长度为：{}", payloadLength);
            ByteBuf payload = message.readBytes(payloadLength);

            Promise<Channel> promise = ctx.executor().newPromise();
            promise.addListener(
                    (FutureListener<Channel>) future -> {
                        final Channel outboundChannel = future.getNow();
                        if (future.isSuccess()) {
                            ChannelFuture responseFuture = outboundChannel.writeAndFlush(payload);
                            responseFuture.addListener((ChannelFutureListener) channelFuture -> {
                                outboundChannel.pipeline().addLast(new TcpRelayHandler(ctx.channel()));
                                ctx.pipeline().addLast(new TcpRelayHandler(outboundChannel));
                                ctx.channel().config().setAutoRead(true);
                            });
                        } else {
                            TrojanServerUtils.closeOnFlush(ctx.channel());
                        }
                    });

            final Channel inboundChannel = ctx.channel();
            b.group(inboundChannel.eventLoop())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new DirectClientHandler(promise));

            b.connect(dstAddr, dstPort).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    // Close the connection if the connection attempt has failed.
                    TrojanServerUtils.closeOnFlush(ctx.channel());
                }
            });
        }
    }

}
