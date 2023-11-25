package me.bteuk.proxy.utils;

public class ChatFormatter {

    public static String escapeDiscordFormatting(String input) {
        return input.replace("@", "@\u200B").replaceAll("[*_#\\[\\]()\\-`>]", "\\\\$0");
    }
}
