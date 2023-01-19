package com.hccake.trojan.server.codec;

import static java.util.Objects.requireNonNull;

/**
 * The type of address in socks5 and trojan
 */
public class TrojanAddressType implements Comparable<TrojanAddressType> {

    public static final TrojanAddressType IPv4 = new TrojanAddressType(0x01, "IPv4");
    public static final TrojanAddressType DOMAIN = new TrojanAddressType(0x03, "DOMAIN");
    public static final TrojanAddressType IPv6 = new TrojanAddressType(0x04, "IPv6");

    public static TrojanAddressType valueOf(byte b) {
        switch (b) {
            case 0x01:
                return IPv4;
            case 0x03:
                return DOMAIN;
            case 0x04:
                return IPv6;
        }

        return new TrojanAddressType(b);
    }

    private final byte byteValue;
    private final String name;
    private String text;

    public TrojanAddressType(int byteValue) {
        this(byteValue, "UNKNOWN");
    }

    public TrojanAddressType(int byteValue, String name) {
        requireNonNull(name, "name");

        this.byteValue = (byte) byteValue;
        this.name = name;
    }

    public byte byteValue() {
        return byteValue;
    }

    @Override
    public int hashCode() {
        return byteValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TrojanAddressType)) {
            return false;
        }

        return byteValue == ((TrojanAddressType) obj).byteValue;
    }

    @Override
    public int compareTo(TrojanAddressType o) {
        return byteValue - o.byteValue;
    }

    @Override
    public String toString() {
        String text = this.text;
        if (text == null) {
            this.text = text = name + '(' + (byteValue & 0xFF) + ')';
        }
        return text;
    }
}
