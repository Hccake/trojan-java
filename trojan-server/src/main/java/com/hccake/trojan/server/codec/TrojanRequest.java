package com.hccake.trojan.server.codec;

import lombok.Data;

/**
 * Trojan Request is a SOCKS5-like request:
 * <p>
 * +-----+------+----------+----------+
 * | CMD | ATYP | DST.ADDR | DST.PORT |
 * +-----+------+----------+----------+
 * |  1  |  1   | Variable |    2     |
 * +-----+------+----------+----------+
 *
 * @author hccake
 */
@Data
public class TrojanRequest {

    /**
     * 操作类型
     */
    private TrojanCommandType commandType;

    /**
     * 地址类型
     */
    private TrojanAddressType addressType;

    /**
     * 目标地址
     */
    private String dstAddr;

    /**
     * 目标端口
     */
    private int dstPort;
}
