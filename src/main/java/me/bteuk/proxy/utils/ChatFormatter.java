package me.bteuk.proxy.utils;

public class ChatFormatter {

    public static String escapeDiscordFormatting(String input) {
        return input.replaceAll("[*_#\\[\\]()\\-`>]", "\\\\$0");
    }
}
