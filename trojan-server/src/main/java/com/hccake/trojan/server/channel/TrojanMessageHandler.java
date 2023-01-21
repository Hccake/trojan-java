package com.hccake.trojan.server.channel;

import com.hccake.trojan.server.codec.*;
import com.hccake.trojan.server.exception.TrojanProtocolException;
import com.hccake.trojan.server.util.TrojanServerUtils;
import io.netty5.bootstrap.Bootstrap;
import io.netty5.buffer.Buffer;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.ChannelOption;
import io.netty5.channel.SimpleChannelInboundHandler;
import io.netty5.channel.socket.nio.NioDatagramChannel;
import io.netty5.channel.socket.nio.NioSocketChannel;
import io.netty5.handler.flow.FlowControlHandler;
import io.netty5.util.concurrent.Future;
import io.netty5.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author hccake
 */
@Slf4j
public final class TrojanMessageHandler extends SimpleChannelInboundHandler<Buffer> {

    private final Bootstrap b = new Bootstrap();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 设置了 autoRead 为 false，所以这里需要手动 read 下
        ctx.read();
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final Buffer message) throws Exception {
        // 直接删除当前 handler
        ctx.pipeline().remove(TrojanMessageHandler.class);
        // 先记录消息 index, 在异常时 reset
        int readerIndex = message.readerOffset();
        try {
            handleTrojanMessage(ctx, message);
        } catch (TrojanProtocolException trojanProtocolException) {
            log.warn("not a trojan protocol message: " + trojanProtocolException.getMessage());
            message.readerOffset(readerIndex);
            redirectToHtml(ctx, message);
        }
    }

    @Override
    public void channelExceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("trojan 处理异常", cause);
        TrojanServerUtils.closeOnFlush(ctx.channel());
    }

    private void redirectToHtml(ChannelHandlerContext ctx, Buffer message) {
        Promise<Channel> promise = ctx.executor().newPromise();

        final Channel inboundChannel = ctx.channel();
        b.group(inboundChannel.executor()).channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new DirectClientHandler(promise));

        b.connect("127.0.0.1", 80).addListener(future -> {
            if (future.isSuccess()) {
                // Connection established use handler provided results
                Channel outboundChannel = future.getNow();
                bindChannelAndWrite(inboundChannel, outboundChannel, message);
            } else {
                // Close the connection if the connection attempt has failed.
                TrojanServerUtils.closeOnFlush(inboundChannel);
            }
        });
    }

    private void handleTrojanMessage(ChannelHandlerContext ctx, Buffer message) throws Exception {
        // +-----------------------+---------+----------------+---------+----------+
        // | hex(SHA224(password)) | CRLF | Trojan Request | CRLF | Payload |
        // +-----------------------+---------+----------------+---------+----------+
        // | 56 | X'0D0A' | Variable | X'0D0A' | Variable |
        // +-----------------------+---------+----------------+---------+----------+

        // 读取 trojan 的密码
        int hashLength = 56;
        byte[] passwordBytes = new byte[hashLength];
        message.readBytes(passwordBytes, 0, hashLength);
        String requestHash = new String(passwordBytes, StandardCharsets.UTF_8);
        // TODO requestHash 校验
        // hex(SHA224('a123456'))
        if (!"28D0BDD80B63FE9C847B405FD86A51CD9D4E7C66AF99D61B6DD579B7".equalsIgnoreCase(requestHash)) {
            throw new TrojanProtocolException("error request hash");
        }

        // 后续两个是 CRLF
        if (message.readByte() != '\r' || message.readByte() != '\n') {
            message.readerOffset(0);
            throw new TrojanProtocolException("error request message");
        }

        // CMD
        // o CONNECT X'01'
        // o UDP ASSOCIATE X'03'
        byte cmdByte = message.readByte();
        TrojanCommandType cmdType = TrojanCommandType.valueOf(cmdByte);
        // 暂时只支持 CONNECT 和 UDP
        if (!(cmdType.equals(TrojanCommandType.CONNECT) || cmdType.equals(TrojanCommandType.UDP_ASSOCIATE))) {
            throw new TrojanProtocolException("error cmd type");
        }

        // ATYP address type of following address
        // o IP V4 address: X'01'
        // o DOMAINNAME: X'03'
        // o IP V6 address: X'04'
        final TrojanAddressType dstAddrType = TrojanAddressType.valueOf(message.readByte());
        final String dstAddr = TrojanAddressDecoder.DEFAULT.decodeAddress(dstAddrType, message);
        final int dstPort = message.readUnsignedShort();
        log.debug("cmdType: {}, 请求目标地址为：[{}:{}]", cmdType, dstAddr, dstPort);

        // skip CRLF
        message.skipReadableBytes(2);

        // TODO 添加流量校验和统计
        final Channel userChannel = ctx.channel();

        // TCP 则读取下剩余的数据
        if (TrojanCommandType.CONNECT.equals(cmdType)) {
            int payloadLength = message.readableBytes();
            log.info("payload 长度为：{}", payloadLength);
            ByteBuffer payload = ByteBuffer.allocateDirect(payloadLength);
            message.readBytes(payload);
            Buffer payloadBuffer = ctx.bufferAllocator().allocate(payloadLength);
            payloadBuffer.writeBytes(payload.flip());

            Promise<Channel> promise = ctx.executor().newPromise();
            promise.asFuture().addListener(future -> {
                final Channel outboundChannel = future.getNow();
                if (future.isSuccess()) {
                    bindChannelAndWrite(userChannel, outboundChannel, payloadBuffer);
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

                    userChannel.pipeline().addLast(new TrojanUdpPacketEncoder());
                    userChannel.pipeline().addLast(new TrojanUdpPacketDecoder());
                    userChannel.pipeline().addLast(new RelayHandler(outboundChannel));
                    userChannel.pipeline().remove(FlowControlHandler.class);
                    userChannel.setOption(ChannelOption.AUTO_READ, true);
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
        Future<Void> responseFuture = outboundChannel.writeAndFlush(writeData);
        responseFuture.addListener(channelFuture -> {
            outboundChannel.pipeline().addLast(new RelayHandler(inboundChannel));

            inboundChannel.pipeline().addLast(new RelayHandler(outboundChannel));
            inboundChannel.pipeline().remove(FlowControlHandler.class);
            inboundChannel.setOption(ChannelOption.AUTO_READ, true);
        });
    }

}
