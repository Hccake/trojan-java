package com.hccake.trojan.server.codec;

import io.netty5.buffer.Buffer;
import io.netty5.handler.codec.EncoderException;
import io.netty5.util.NetUtil;

import java.nio.charset.StandardCharsets;


/**
 * Encodes a Trojan address into binary representation.
 */
public interface TrojanAddressEncoder {

    TrojanAddressEncoder DEFAULT = (addrType, addrValue, out) -> {
        final byte typeVal = addrType.byteValue();
        if (typeVal == TrojanAddressType.IPv4.byteValue()) {
            if (addrValue != null) {
                out.writeBytes(NetUtil.createByteArrayFromIpAddressString(addrValue));
            } else {
                out.writeInt(0);
            }
        } else if (typeVal == TrojanAddressType.DOMAIN.byteValue()) {
            if (addrValue != null) {
                out.writeByte((byte) addrValue.length());
                out.writeCharSequence(addrValue, StandardCharsets.US_ASCII);
            } else {
                out.writeByte((byte) 0);
            }
        } else if (typeVal == TrojanAddressType.IPv6.byteValue()) {
            if (addrValue != null) {
                out.writeBytes(NetUtil.createByteArrayFromIpAddressString(addrValue));
            } else {
                out.writeLong(0);
                out.writeLong(0);
            }
        } else {
            throw new EncoderException("unsupported addrType: " + (addrType.byteValue() & 0xFF));
        }
    };

    /**
     * Encodes a SOCKS5 address.
     *
     * @param addrType the type of the address
     * @param addrValue the string representation of the address
     * @param out the output buffer where the encoded SOCKS5 address field will be written to
     */
    void encodeAddress(TrojanAddressType addrType, String addrValue, Buffer out) throws Exception;
}