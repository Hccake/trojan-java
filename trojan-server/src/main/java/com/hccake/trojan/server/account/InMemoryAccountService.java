package com.hccake.trojan.server.account;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author hccake
 */
public class InMemoryAccountService implements AccountService {

    /**
     * 账户信息
     */
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    public InMemoryAccountService(Collection<Account> accounts) {
        for (Account account : accounts) {
            createAccount(account);
        }
    }

    public InMemoryAccountService(Account... accounts) {
        for (Account account : accounts) {
            createAccount(account);
        }
    }

    @Override
    public Account findByKey(String key) {
        return accounts.get(key.toLowerCase());
    }

    public void createAccount(Account account) {
        if (accountExists(account.getKey())) {
            throw new IllegalArgumentException("account key should not exist");
        }
        this.accounts.put(account.getKey().toLowerCase(), account);
    }

    public void deleteAccount(String key) {
        this.accounts.remove(key.toLowerCase());
    }

    public void updateAccount(Account account) {
        if (!accountExists(account.getKey())) {
            throw new IllegalArgumentException("account key should exist");
        }
        this.accounts.put(account.getKey().toLowerCase(), account);
    }

    public boolean accountExists(String key) {
        return this.accounts.containsKey(key.toLowerCase());
    }

}
