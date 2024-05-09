package net.bteuk.proxy.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.bteuk.network.lib.dto.AbstractTransferObject;
import net.bteuk.network.lib.dto.ChatMessage;
import net.bteuk.network.lib.dto.DirectMessage;
import net.bteuk.network.lib.dto.DiscordDirectMessage;
import net.bteuk.network.lib.dto.DiscordEmbed;
import net.bteuk.network.lib.dto.DiscordLinking;
import net.bteuk.network.lib.dto.DiscordRole;
import net.bteuk.network.lib.dto.TabEvent;
import net.bteuk.network.lib.socket.SocketHandler;
import net.bteuk.proxy.Proxy;
import net.bteuk.proxy.ChatHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ProxySocketHandler implements SocketHandler {

    @Override
    public void handle(AbstractTransferObject abstractTransferObject) {
        // Handle the different objects.
        if (abstractTransferObject instanceof ChatMessage chatMessage) {
            try {
                ChatHandler.handle(chatMessage);
                // Send the chat message to discord.
                Proxy.getInstance().getDiscord().handle(chatMessage);
            } catch (IOException e) {
                // Ignored
            }
        } else if (abstractTransferObject instanceof DirectMessage directMessage) {
            try {
                ChatHandler.handle(directMessage);
            } catch (IOException e) {
                // Ignored
            }
        } else if (abstractTransferObject instanceof DiscordDirectMessage discordDirectMessage) {
            Proxy.getInstance().getDiscord().handle(discordDirectMessage);
        } else if (abstractTransferObject instanceof DiscordEmbed discordEmbed) {
            Proxy.getInstance().getDiscord().handle(discordEmbed);
        } else if (abstractTransferObject instanceof DiscordLinking discordLinking) {
            Proxy.getInstance().getDiscord().handle(discordLinking);
        } else if (abstractTransferObject instanceof DiscordRole discordRole) {
            Proxy.getInstance().getDiscord().handle(discordRole);
        } else if (abstractTransferObject instanceof TabEvent tabEvent) {
            try {
                ChatHandler.handle(tabEvent);
            } catch (IOException e) {
                // Ignored
            }
        } else {
            Proxy.getInstance().getLogger().warn(String.format("Socket object has an unrecognised type %s", abstractTransferObject.getClass().getTypeName()));
        }
    }
}
