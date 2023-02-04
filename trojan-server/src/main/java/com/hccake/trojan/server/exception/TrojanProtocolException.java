package com.hccake.trojan.server.exception;

import io.netty5.buffer.Buffer;

/**
 * @author hccake
 */
public class TrojanProtocolException extends RuntimeException {

    private final Buffer content;

    public TrojanProtocolException(Buffer content) {
        this.content = content;
    }

    public TrojanProtocolException(String message, Buffer content) {
        super(message);
        this.content = content;
    }

    public Buffer getContent() {
        return content;
    }
}
