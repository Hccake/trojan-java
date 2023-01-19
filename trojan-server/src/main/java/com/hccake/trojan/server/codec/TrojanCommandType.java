package com.hccake.trojan.server.codec;

import static java.util.Objects.requireNonNull;

/**
 * The type of Trojan Request
 */
public class TrojanCommandType implements Comparable<TrojanCommandType> {

    public static final TrojanCommandType CONNECT = new TrojanCommandType(0x01, "CONNECT");
    public static final TrojanCommandType BIND = new TrojanCommandType(0x02, "BIND");
    public static final TrojanCommandType UDP_ASSOCIATE = new TrojanCommandType(0x03, "UDP_ASSOCIATE");

    public static TrojanCommandType valueOf(byte b) {
        switch (b) {
        case 0x01:
            return CONNECT;
        case 0x02:
            return BIND;
        case 0x03:
            return UDP_ASSOCIATE;
        }

        return new TrojanCommandType(b);
    }

    private final byte byteValue;
    private final String name;
    private String text;

    public TrojanCommandType(int byteValue) {
        this(byteValue, "UNKNOWN");
    }

    public TrojanCommandType(int byteValue, String name) {
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
        if (!(obj instanceof TrojanCommandType)) {
            return false;
        }

        return byteValue == ((TrojanCommandType) obj).byteValue;
    }

    @Override
    public int compareTo(TrojanCommandType o) {
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
