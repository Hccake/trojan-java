package com.hccake.trojan.server.test;

import com.hccake.trojan.server.env.CommandLineArgs;
import com.hccake.trojan.server.env.SimpleCommandLineArgsParser;

/**
 * @author hccake
 */
public class CommandLineArgsTest {

    public static void main(String[] args) {
        CommandLineArgs commandLineArgs = SimpleCommandLineArgsParser.parse(args);
        System.out.println("无选项的 arg: ");
        for (String nonOptionArg : commandLineArgs.getNonOptionArgs()) {
            System.out.println(nonOptionArg);
        }
        System.out.println("==========");


        System.out.println("有选项的 arg: ");
        for (String optionName: commandLineArgs.getOptionNames()) {
            System.out.print("name: " + optionName + ", value: ");
            System.out.println(commandLineArgs.getOptionValues(optionName));
        }
        System.out.println("==========");

    }
}
