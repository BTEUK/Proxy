package net.bteuk.proxy.eventing.jda;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.bteuk.network.lib.dto.ChatMessage;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.proxy.Proxy;
import net.bteuk.proxy.chat.ChatManager;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DiscordChatListener extends ListenerAdapter {

    private final String chat_channel;
    private final String reviewer_channel;
    private final String staff_channel;

    private static final Component DISCORD_PREFIX = Component.text("[Discord] ", NamedTextColor.DARK_GRAY);

    private static final Component SEPARATOR = Component.text(" > ", NamedTextColor.GRAY).decorate(TextDecoration.BOLD);

    private static final Component STAFF_PREFIX = Component.text("[Staff]", NamedTextColor.RED);

    public DiscordChatListener(String chat_channel, String reviewer_channel, String staff_channel) {
        this.chat_channel = chat_channel;
        this.reviewer_channel = reviewer_channel;
        this.staff_channel = staff_channel;
        Proxy.getInstance().getLogger().info("Enabling Discord Chat Listener");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        // Block messages from bot and null author.
        if ((event.getMember() == null && !event.isWebhookMessage()) || Proxy.getInstance().getDiscord() == null || event.getAuthor().equals(Proxy.getInstance().getDiscord().getJda().getSelfUser())) {
            return;
        }

        // Block webhooks.
        if (event.isWebhookMessage()) {
            return;
        }

        if (StringUtils.isBlank(event.getMessage().getContentRaw())) {
            return;
        }

        // Block from all channels except linked.
        if (event.getChannel().getId().equals(chat_channel) || event.getChannel().getId().equals(staff_channel)) {
            try {
                TextColor nameColour = TextColor.color(event.getMember().getColorRaw());

                Component discordMessage = DISCORD_PREFIX
                        .append(Component.text(event.getMember().getEffectiveName(), nameColour))
                        .append(SEPARATOR)
                        .append(ChatUtils.line(event.getMessage().getContentRaw()));

                String channel = "global";

                if (event.getChannel().getId().equals(staff_channel)) {
                    // Add the prefix for staff chat.
                    discordMessage = STAFF_PREFIX.append(discordMessage);
                    channel = "staff";
                }

                ChatMessage chatMessage = new ChatMessage(channel, "discord", discordMessage);
                Proxy.getInstance().getChatManager().handle(chatMessage);

            } catch (IOException e) {
                Proxy.getInstance().getLogger().warn("An error occurred while sending a message received from Discord!");
            }
        }
    }
}
