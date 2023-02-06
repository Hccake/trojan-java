package com.hccake.trojan.server.account;

/**
 * 账户 Service
 *
 * @author hccake
 */
public interface AccountService {

    /**
     * 根据 key 查询对应的账户
     *
     * @param key hex(SHA224(password))
     * @return 账户信息
     */
    Account findByKey(String key);
}
