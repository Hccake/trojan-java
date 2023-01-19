package com.hccake.trojan.server.test;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.Proxy;


/**
 * @author hccake
 */
class HutoolHttpProxyTest {

    @Test
    void socks5ProxyTest() {

        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 51837));
        String[] websites = new String[]{
                // "https://www.google.com"
                "http://hutool.cn/"
                // "https://www.youtube.com/",
        };

        for (String website : websites) {
            HttpRequest httpRequest = HttpRequest.get(website)
					.setProxy(proxy)
					.setConnectionTimeout(30000)
					.setReadTimeout(30000);
            try (HttpResponse httpResponse = httpRequest.execute()) {
                String result = httpResponse.body();
                System.out.println(result);
            }
            System.out.println();
            System.out.println();
        }
    }

}
