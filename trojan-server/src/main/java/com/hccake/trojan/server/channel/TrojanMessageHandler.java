package com.hccake.trojan.server.channel;

import com.hccake.trojan.server.codec.TrojanAddressDecoder;
import com.hccake.trojan.server.codec.TrojanAddressType;
import com.hccake.trojan.server.codec.TrojanCommandType;
import com.hccake.trojan.server.exception.TrojanProtocolException;
import com.hccake.trojan.server.util.TrojanServerUtils;
import io.netty5.bootstrap.Bootstrap;
import io.netty5.buffer.Buffer;
import io.netty5.buffer.BufferAllocator;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.ChannelOption;
import io.netty5.channel.SimpleChannelInboundHandler;
import io.netty5.channel.socket.nio.NioSocketChannel;
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
    public void messageReceived(final ChannelHandlerContext ctx, final Buffer message) throws Exception {
        ctx.pipeline().remove(this);
        // 关闭自动读取，等建连后，直接将后续的数据直接转发到
        ctx.channel().setOption(ChannelOption.AUTO_READ, false);
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
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000).option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new DirectClientHandler(promise));

        b.connect("127.0.0.1", 80).addListener(future -> {
            if (future.isSuccess()) {
                // Connection established use handler provided results
                Channel outboundChannel = future.getNow();
                bindChannel(inboundChannel, outboundChannel, message);
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
        log.trace("trojan cmdType :{}", cmdType);

        // TODO 添加流量校验和统计

        // ATYP address type of following address
        // o IP V4 address: X'01'
        // o DOMAINNAME: X'03'
        // o IP V6 address: X'04'
        final TrojanAddressType dstAddrType = TrojanAddressType.valueOf(message.readByte());
        final String dstAddr = TrojanAddressDecoder.DEFAULT.decodeAddress(dstAddrType, message);
        final int dstPort = message.readUnsignedShort();
        log.debug("请求目标地址为：[{}:{}]", dstAddr, dstPort);

        // TCP 则读取下剩余的数据
        if (TrojanCommandType.CONNECT.equals(cmdType)) {
            // Connection established use handler provided results
            // skip CRLF
            message.skipReadableBytes(2);
            int payloadLength = message.readableBytes();
            log.info("payload 长度为：{}", payloadLength);
            ByteBuffer payload = ByteBuffer.allocateDirect(payloadLength);
            message.readBytes(payload);
            Buffer payloadBuffer = ctx.bufferAllocator().allocate(payloadLength);
            payloadBuffer.writeBytes(payload.flip());

            final Channel inboundChannel = ctx.channel();

            Promise<Channel> promise = ctx.executor().newPromise();
            promise.asFuture().addListener(future -> {
                final Channel outboundChannel = future.getNow();
                if (future.isSuccess()) {
                    bindChannel(inboundChannel, outboundChannel, payloadBuffer);
                } else {
                    TrojanServerUtils.closeOnFlush(ctx.channel());
                }
            });

            b.group(inboundChannel.executor())
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
        }
    }

    private static void bindChannel(Channel inboundChannel, Channel outboundChannel, Buffer message) {
        Future<Void> responseFuture = outboundChannel.writeAndFlush(message);
        responseFuture.addListener(channelFuture -> {
            outboundChannel.pipeline().addLast(new TcpRelayHandler(inboundChannel));
            inboundChannel.pipeline().addLast(new TcpRelayHandler(outboundChannel));
            inboundChannel.setOption(ChannelOption.AUTO_READ, true);
        });
    }

}
