Trojan 协议的 java 服务端实现

主要为了个人学习 netty5 的语法变化，仅供学习交流使用，不可用于其他用途，若出现任何问题，与本人无关。


配置列表：

| key                       | 类型    | 描述                                                                   | 默认值                                                   |
| ------------------------- | ------- |----------------------------------------------------------------------| -------------------------------------------------------- |
| password                  | array   | trojan 用户密码，格式为 `hex(SHA224(raw_password))`, 默认提供一个明文为 `a123456` 的密码 | 28d0bdd80b63fe9c847b405fd86a51cd9d4e7c66af99d61b6dd579b7 |
| redirect-host             | string  | 防探测重定向地址，用于转发非 trojan 协议的流量，                                         | "127.0.0.1"                                              |
| redirect-port             | number  | 防探测重定向端口，用于转发非 trojan 协议的流量                                          | 80                                                       |
| ssl.key                   | string  | ssl证书密钥文件地址                                                          | -                                                        |
| ssl.cert                  | string  | ssl证书文件地址                                                            | -                                                        |
| ssl.key-password          | string  | ssl证书密钥密码                                                            | -                                                        |
| static-file-server.enable | boolean | 使用 netty 开启一个静态文件服务器，默认是一个吃豆人游戏                                      | true                                                     |
| static-file-server.host   | string  | 静态文件服务器监听 host                                                       | "127.0.0.1"                                              |
| static-file-server.port   | number  | 静态文件服务器监听 port                                                       | 80                                                       |


启动时可以通过 `--key=value` 的方式修改配置，一般仅需提供使用域名的 ssl 证书和密钥，以及用户密码。

```shell
java -jar xxx.jar --ssl.key=server.key --ssl.cert=server.cert --password=xxx,yyy
```

注意：
