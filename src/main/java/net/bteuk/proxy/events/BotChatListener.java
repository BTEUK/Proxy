package net.bteuk.proxy.events;

import net.bteuk.network.lib.dto.DiscordLinking;
import net.bteuk.proxy.chat.ChatHandler;
import net.bteuk.proxy.utils.Linked;
import net.bteuk.proxy.Proxy;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class BotChatListener extends ListenerAdapter {

    public BotChatListener() {
        Proxy.getInstance().getLogger().info("Enabling Bot Chat Listener");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        // Check channel type.
        if (event.getChannelType() != ChannelType.PRIVATE) {
            return;
        }

        // Block messages from bot.
        if (event.getAuthor().isBot()) {
            return;
        }

        if (StringUtils.isBlank(event.getMessage().getContentRaw())) {
            return;
        }

        // Check if author is in the linked list.
        Linked l = null;
        for (Linked linked : Proxy.getInstance().getLinking()) {

            // Check message
            if (event.getMessage().getContentRaw().equalsIgnoreCase(linked.token)) {

                try {
                    // Link accounts.
                    DiscordLinking discordLinking = new DiscordLinking();
                    discordLinking.setUuid(linked.uuid);
                    discordLinking.setDiscordId(event.getAuthor().getIdLong());

                    ChatHandler.handle(discordLinking);

                    l = linked;
                } catch (IOException e) {
                    // Ignored
                }
            }
        }

        if (l != null) {
            //Close link and remove it from list.
            l.close();
            Proxy.getInstance().getLinking().remove(l);
        }
    }
}
