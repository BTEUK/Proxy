package me.bteuk.proxy.events;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.bteuk.proxy.Linked;
import me.bteuk.proxy.Proxy;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class BotChatListener extends ListenerAdapter {

    private ArrayList<Linked> linking;

    public BotChatListener() {
        linking = Proxy.getInstance().getLinking();
        Proxy.getInstance().getLogger().info("Enabling Bot Chat Listener");
    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {

        //Block messages from bot.
        if (event.getAuthor().isBot()) {
            return;
        }

        if (StringUtils.isBlank(event.getMessage().getContentRaw())) {
            return;
        }

        //Check if author is in the linked list.
        Linked l = null;
        for (Linked linked : linking) {
            //Check message
            if (event.getMessage().getContentRaw().equalsIgnoreCase(linked.token)) {

                try {
                    //Link accounts.
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(stream);
                    out.writeUTF("link " + linked.uuid + " " + event.getAuthor().getId());
                    Proxy.getInstance().getDiscord().sendMessage("uknet:discord", stream.toByteArray());

                    l = linked;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (l != null) {
            //Close link and remove it from list.
            l.close();
            linking.remove(l);
        }
    }
}
