package com.hccake.trojan.server.codec;

import io.netty5.buffer.Buffer;
import lombok.Data;

/**
 * Trojan 协议消息
 * @see TrojanMessageDecoder
 * @author hccake
 */
@Data
public class TrojanMessage {

    /**
     * hex(SHA224(password))
     */
    private String key;

    private TrojanRequest trojanRequest;

    private Buffer payload;
}
