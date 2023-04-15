package me.bteuk.proxy.events;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.bteuk.proxy.Linked;
import me.bteuk.proxy.Proxy;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class BotChatListener extends ListenerAdapter {

    public BotChatListener() {
        Proxy.getInstance().getLogger().info("Enabling Bot Chat Listener");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        //Check channel type.
        if (event.getChannelType() != ChannelType.PRIVATE) {
            return;
        }

        //Block messages from bot.
        if (event.getAuthor().isBot()) {
            return;
        }

        if (StringUtils.isBlank(event.getMessage().getContentRaw())) {
            return;
        }

        //Check if author is in the linked list.
        Linked l = null;
        for (Linked linked : Proxy.getInstance().getLinking()) {

            //Check message
            if (event.getMessage().getContentRaw().equalsIgnoreCase(linked.token)) {

                try {
                    //Link accounts.
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(stream);

                    Component component = PlainTextComponentSerializer.plainText().deserialize("link " + linked.uuid + " " + event.getAuthor().getId());
                    String json = GsonComponentSerializer.gson().serialize(component);

                    out.writeUTF(json);

                    for (RegisteredServer server : Proxy.getInstance().getServer().getAllServers()) {
                        if (!server.getPlayersConnected().isEmpty()) {
                            server.sendPluginMessage(MinecraftChannelIdentifier.create("uknet", "discord"), stream.toByteArray());
                        }
                    }

                    l = linked;
                } catch (IOException e) {
                    e.printStackTrace();
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
