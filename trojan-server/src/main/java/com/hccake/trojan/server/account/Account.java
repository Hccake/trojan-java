package com.hccake.trojan.server.account;

import lombok.Data;

/**
 * 账户体系
 * @author hccake
 */
@Data
public class Account {

    private String key;

    public Account() {
    }

    public Account(String key) {
        this.key = key;
    }
}
