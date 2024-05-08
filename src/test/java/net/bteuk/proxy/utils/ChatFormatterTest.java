package net.bteuk.proxy.utils;

import org.junit.jupiter.api.Test;

import static me.bteuk.proxy.utils.ChatFormatter.escapeDiscordFormatting;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatFormatterTest {

    @Test
    void testEscapeDiscordFormatting() {

        assertEquals("\\_Formatted Message\\_", escapeDiscordFormatting("_Formatted Message_"));
        assertEquals("\\*\\_\\#\\[\\]\\(\\)\\-\\`\\>", escapeDiscordFormatting("*_#[]()-`>"));

    }
}