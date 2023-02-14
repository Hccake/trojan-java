package com.hccake.trojan.server;

import com.hccake.trojan.server.env.CommandLineArgs;
import com.hccake.trojan.server.env.SimpleCommandLineArgsParser;
import com.hccake.trojan.server.env.TrojanServerProperties;
import io.netty5.channel.EventLoopGroup;
import io.netty5.channel.MultithreadEventLoopGroup;
import io.netty5.channel.nio.NioHandler;

import java.util.List;
import java.util.Set;

/**
 * @author hccake
 */
public final class Launcher {

    public static void main(String[] args) throws Exception {

        TrojanServerProperties trojanServerProperties = parseTrojanServerProperties(args);
        TrojanServerProperties.HttpStaticFileServerConfig staticFileServerConfig = trojanServerProperties.getStaticFileServer();

        EventLoopGroup bossGroup = new MultithreadEventLoopGroup(1, NioHandler.newFactory());
        EventLoopGroup workerGroup = new MultithreadEventLoopGroup(NioHandler.newFactory());

        try {
            if(staticFileServerConfig.isEnable()) {
                HttpStaticFileServer httpStaticFileServer = new HttpStaticFileServer(bossGroup, workerGroup, staticFileServerConfig);
                httpStaticFileServer.start();
            }
            TrojanServer trojanServer = new TrojanServer(bossGroup, workerGroup, trojanServerProperties);
            trojanServer.start().asStage().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static TrojanServerProperties parseTrojanServerProperties(String[] args) {
        TrojanServerProperties trojanServerProperties = new TrojanServerProperties();
        CommandLineArgs commandLineArgs = SimpleCommandLineArgsParser.parse(args);
        Set<String> optionNames = commandLineArgs.getOptionNames();

        if (optionNames.contains("passwords")) {
            trojanServerProperties.setPasswords(commandLineArgs.getOptionValues("passwords"));
        }
        if (optionNames.contains("redirect-host")) {
            trojanServerProperties.setRedirectHost(getStringValue(commandLineArgs, "redirect-host"));
        }
        if (optionNames.contains("redirect-port")) {
            trojanServerProperties.setRedirectPort(getIntValue(commandLineArgs, "redirect-port"));
        }

        TrojanServerProperties.SslConfig ssl = trojanServerProperties.getSsl();
        if (optionNames.contains("ssl.key")) {
            ssl.setKey(getStringValue(commandLineArgs, "ssl.key"));
        }
        if (optionNames.contains("ssl.cert")) {
            ssl.setCert(getStringValue(commandLineArgs, "ssl.cert"));
        }
        if (optionNames.contains("ssl.key-password")) {
            ssl.setKeyPassword(getStringValue(commandLineArgs, "ssl.key-password"));
        }

        TrojanServerProperties.HttpStaticFileServerConfig staticFileServerConfig = trojanServerProperties.getStaticFileServer();
        if (optionNames.contains("static-file-server.enable")) {
            staticFileServerConfig.setEnable(getBooleanValue(commandLineArgs, "static-file-server.enable"));
        }
        if (optionNames.contains("static-file-server.host")) {
            staticFileServerConfig.setHost(getStringValue(commandLineArgs, "static-file-server.host"));
        }
        if (optionNames.contains("static-file-server.port")) {
            staticFileServerConfig.setPort(getIntValue(commandLineArgs, "static-file-server.port"));
        }

        return trojanServerProperties;
    }


    public static String getStringValue(CommandLineArgs commandLineArgs, String optionName) {
        List<String> optionValues = commandLineArgs.getOptionValues(optionName);
        return optionValues.get(0);
    }

    public static Integer getIntValue(CommandLineArgs commandLineArgs, String optionName) {
        return Integer.valueOf(getStringValue(commandLineArgs, optionName));
    }

    public static Boolean getBooleanValue(CommandLineArgs commandLineArgs, String optionName) {
        return Boolean.valueOf(getStringValue(commandLineArgs, optionName));
    }

}
