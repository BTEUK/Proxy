package net.bteuk.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.bteuk.network.lib.dto.AbstractTransferObject;
import net.bteuk.network.lib.dto.ChatMessage;
import net.bteuk.network.lib.dto.DirectMessage;
import net.bteuk.proxy.utils.Linked;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ChatHandler {

    /**
     * Handle a chat message.
     *
     * @param message the chat message to handle.
     */
    public static void handle(ChatMessage message) throws IOException {

        // Send the chat message to all servers.
        sendProxyMessage(message);

        switch (message.getChannel()) {

            case "uknet:discord_linking" -> {

                // Convert the component to plain text.
                String plain = PlainTextComponentSerializer.plainText().serialize(message.getComponent());

                //Split message.
                String[] args = plain.split(" ");

                if (args[0].equalsIgnoreCase("link")) {

                    //Add object for linking, with a time to remove.
                    //If there is already an instance, replace it.
                    Linked linked = null;
                    for (Linked l : Proxy.getInstance().getLinking()) {
                        if (l.uuid.equalsIgnoreCase(args[1])) {
                            linked = l;
                        }
                    }

                    //If there was already a task for this player, close it first.
                    if (linked != null) {
                        linked.close();
                        Proxy.getInstance().getLinking().remove(linked);
                    }

                    //Create new link.
                    Proxy.getInstance().getLinking().add(new Linked(args[1], args[2]));

                }

            }

            case "uknet:discord_dm" -> {

                // Convert the component to plain text.
                String plain = PlainTextComponentSerializer.plainText().serialize(message.getComponent());

                //Split the comma-separated message.
                String[] args = plain.split(",");

                //Check if the player has their discord linked, else ignore this message altogether.
                if (Proxy.getInstance().getGlobalSQL().hasRow("SELECT uuid FROM discord WHERE uuid='" + args[0] + "';")) {

                    //Get the discord id of the player.
                    String discord_id = Proxy.getInstance().getGlobalSQL().getString("SELECT discord_id FROM discord WHERE uuid='" + args[0] + "';");

                    //Send dm to player.
                    if (discord_id != null) {
                        Proxy.getInstance().getDiscord().sendReviewingUpdateDM(discord_id, args);
                    }


                }
            }
        }
    }

    /**
     * Handle a chat message.
     *
     * @param message the direct message to handle.
     */
    public static void handle(AbstractTransferObject message) throws IOException {

        // Send the direct message to all servers.
        sendProxyMessage(message);

    }

    /**
     * Send a message to all servers on a specific channel.
     *
     * @param message the {@link AbstractTransferObject} to send
     */
    private static void sendProxyMessage(AbstractTransferObject message) throws IOException {

        // Serialize the chat message and convert it to bytes.
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(stream, message);

        //Send message to all servers.
        Proxy.getInstance().getServer().getAllServers().forEach(server ->
                server.sendPluginMessage(MinecraftChannelIdentifier.create("uknet", "network"), stream.toByteArray()));
    }
}
