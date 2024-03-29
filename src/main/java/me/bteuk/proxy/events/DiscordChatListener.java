package me.bteuk.proxy.events;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.bteuk.proxy.Proxy;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
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

    public DiscordChatListener(String chat_channel, String reviewer_channel, String staff_channel) {
        this.chat_channel = chat_channel;
        this.reviewer_channel = reviewer_channel;
        this.staff_channel = staff_channel;
        Proxy.getInstance().getLogger().info("Enabling Discord Chat Listener");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        //Block messages from bot and null author.
        if ((event.getMember() == null && !event.isWebhookMessage()) || Proxy.getInstance().getDiscord() == null || event.getAuthor().equals(Proxy.getInstance().getDiscord().getJda().getSelfUser())) {
            return;
        }

        //Block webhooks.
        if (event.isWebhookMessage()) {
            return;
        }

        if (StringUtils.isBlank(event.getMessage().getContentRaw())) {
            return;
        }

        //Block from all channels except linked.
        if (event.getChannel().getId().equals(chat_channel) || event.getChannel().getId().equals(staff_channel)) {

            //Send message
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(stream);
            try {

                Color color = event.getMember().getColor();

                if (color == null) {

                    Component component = LegacyComponentSerializer.legacyAmpersand().deserialize("&8[Discord] &r" + event.getMember().getEffectiveName() + " &7&l> &r&f" + event.getMessage().getContentRaw());
                    String json = GsonComponentSerializer.gson().serialize(component);

                    out.writeUTF(json);

                } else {

                    String hex = String.format("&#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());

                    Component component = LegacyComponentSerializer.legacyAmpersand().deserialize("&8[Discord] &r" + hex + event.getMember().getEffectiveName() + " &7&l> &r&f" + event.getMessage().getContentRaw());
                    String json = GsonComponentSerializer.gson().serialize(component);

                    out.writeUTF(json);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            for(RegisteredServer server : Proxy.getInstance().getServer().getAllServers()) {
                if(!server.getPlayersConnected().isEmpty()) {
                    if (event.getChannel().getId().equals(staff_channel)) {
                        server.sendPluginMessage(MinecraftChannelIdentifier.create("uknet", "staff"), stream.toByteArray());
                    } else {
                        server.sendPluginMessage(MinecraftChannelIdentifier.create("uknet", "globalchat"), stream.toByteArray());
                    }
                }
            }

        } else if (event.getChannel().getId().equals(reviewer_channel)) {

        } else if (event.getChannel().getId().equals(staff_channel)) {

            //Send message
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(stream);
            try {

                Color color = event.getMember().getColor();

                if (color == null) {

                    Component component = LegacyComponentSerializer.legacyAmpersand().deserialize("&8[Discord] &r" + event.getMember().getEffectiveName() + " &7&l> &r&f" + event.getMessage().getContentRaw());
                    String json = GsonComponentSerializer.gson().serialize(component);

                    out.writeUTF(json);

                } else {

                    String hex = String.format("&#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());

                    Component component = LegacyComponentSerializer.legacyAmpersand().deserialize("&8[Discord] &r" + hex + event.getMember().getEffectiveName() + " &7&l> &r&f" + event.getMessage().getContentRaw());
                    String json = GsonComponentSerializer.gson().serialize(component);

                    out.writeUTF(json);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }


            for(RegisteredServer server : Proxy.getInstance().getServer().getAllServers()) {
                if(!server.getPlayersConnected().isEmpty()) {
                    server.sendPluginMessage(MinecraftChannelIdentifier.create("uknet", "staff"), stream.toByteArray());
                }
            }

        }
    }
}
