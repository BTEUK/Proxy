package me.bteuk.proxy;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.bteuk.proxy.socket.ChatMessage;
import me.bteuk.proxy.utils.ChatFormatter;
import me.bteuk.proxy.utils.Linked;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ChatHandler {

    /**
     * Handle a chat message.
     * @param message the chat message to handle.
     */
    public static void handle(ChatMessage message) throws IOException {
        switch (message.getChannel()) {

            case "uknet:globalchat", "uknet:reviewer", "uknet:staff", "uknet:connect", "uknet:disconnect", "uknet:tab" -> {

                // Convert the component back to json.
                String jsonMessage = GsonComponentSerializer.gson().serialize(message.getComponent());

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(stream);
                out.writeUTF(jsonMessage);

                //Send message to all servers.
                for (RegisteredServer server : Proxy.getInstance().getServer().getAllServers()) {
                    server.sendPluginMessage(MinecraftChannelIdentifier.create("uknet", message.getChannel().split(":")[1]), stream.toByteArray());
                }
            }

            case "uknet:discord_chat" -> {

                // Convert the component to plain text.
                String plain = PlainTextComponentSerializer.plainText().serialize(message.getComponent());

                //Send message to public discord chat.
                Proxy.getInstance().getDiscord().sendMessage(ChatFormatter.escapeDiscordFormatting(plain));

            }

            case "uknet:discord_announcements" -> {

                // Convert the component to plain text.
                String plain = PlainTextComponentSerializer.plainText().serialize(message.getComponent());

                //Send announcement to discord, this will be formatted before sending.
                Proxy.getInstance().getDiscord().sendAnnouncement(plain);

            }

            case "uknet:discord_staff" -> {

                // Convert the component to plain text.
                String plain = PlainTextComponentSerializer.plainText().serialize(message.getComponent());

                //Send message to start chat.
                Proxy.getInstance().getDiscord().sendStaffMessage(ChatFormatter.escapeDiscordFormatting(plain));

            }

            case "uknet:discord_reviewer" -> //Tell discord to update the reviewer channel.
                    Proxy.getInstance().getDiscord().updateReviewerChannel();

            case "uknet:discord_linking" -> {

                // Convert the component to plain text.
                String plain = PlainTextComponentSerializer.plainText().serialize(message.getComponent());

                //Split message.
                String[] args = plain.split(" ");

                if (args[0].equalsIgnoreCase("addrole")) {

                    long user_id = Long.parseLong(args[1]);
                    long role_id = Long.parseLong(args[2]);
                    Proxy.getInstance().getDiscord().addRole(user_id, role_id, true);

                } else if (args[0].equalsIgnoreCase("removerole")) {

                    long user_id = Long.parseLong(args[1]);
                    long role_id = Long.parseLong(args[2]);
                    Proxy.getInstance().getDiscord().removeRole(user_id, role_id, true);

                } else if (args[0].equalsIgnoreCase("link")) {

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
}
