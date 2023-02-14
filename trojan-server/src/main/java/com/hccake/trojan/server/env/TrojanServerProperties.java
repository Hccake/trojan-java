package com.hccake.trojan.server.env;

import lombok.Data;

import java.util.List;

/**
 * trojan 服务端的配置文件
 *
 * @author hccake
 */
@Data
public class TrojanServerProperties {

    /**
     * 全局的日志级别
     */
    private String loggingLevel = "INFO";

    /**
     * trojan 用户密码，格式为 hex(SHA224(raw_password)), 默认提供一个 a123456 的密码
     */
    private List<String> passwords = List.of("28d0bdd80b63fe9c847b405fd86a51cd9d4e7c66af99d61b6dd579b7");

    /**
     * 重定向地址，用于处理非 trojan 协议的流量
     */
    private String redirectHost = "127.0.0.1";

    /**
     * 重定向端口，用于处理非 trojan 协议的流量
     */
    private int redirectPort = 80;

    /**
     * ssl 证书配置
     */
    private SslConfig ssl = new SslConfig();

    /**
     * 使用 netty 启动一个 http 服务器
     */
    private HttpStaticFileServerConfig staticFileServer = new HttpStaticFileServerConfig();


    @Data
    public static class SslConfig {
        private String key;
        private String cert;
        private String keyPassword;
    }

    @Data
    public static class HttpStaticFileServerConfig {
        private boolean enable = true;
        private String host = "0.0.0.0";
        private int port = 80;
    }

}
