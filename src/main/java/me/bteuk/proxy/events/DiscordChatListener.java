package me.bteuk.proxy.events;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.bteuk.proxy.Proxy;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DiscordChatListener extends ListenerAdapter {

    private String chat_channel;

    public DiscordChatListener(String chat_channel) {
        this.chat_channel = chat_channel;
        Proxy.getInstance().getLogger().info("Enabling Discord Chat Listener");
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {

        //Block messages from bot and null author.
        if ((event.getMember() == null && !event.isWebhookMessage()) || Proxy.getInstance().getDiscord() == null || event.getAuthor().equals(Proxy.getInstance().getDiscord().getJda().getSelfUser())) {
            return;
        }

        //Block webhooks.
        if (event.isWebhookMessage()) {
            return;
        }

        //Block from all channels except linked.
        if (!event.getChannel().getId().equals(chat_channel)) {
            return;
        }

        if (StringUtils.isBlank(event.getMessage().getContentRaw())) {
            return;
        }


        //TODO: Check for commands.

        //Send message
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);
        try {
            out.writeUTF((event.getMember().getEffectiveName() + " &7&l> &r&f" + event.getMessage().getContentRaw()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Proxy.getInstance().getLogger().info("Send Message");

        for(RegisteredServer server : Proxy.getInstance().getServer().getAllServers()) {
            if(!server.getPlayersConnected().isEmpty()) {
                server.sendPluginMessage(MinecraftChannelIdentifier.create("uknet", "globalchat"), stream.toByteArray());
            }
        }

    }
}
