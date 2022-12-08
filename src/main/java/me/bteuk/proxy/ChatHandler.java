package me.bteuk.proxy;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ChatHandler extends Thread {
    protected final Socket socket;

    public ChatHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try (InputStream input = socket.getInputStream()) {
            ObjectInputStream objectInput = new ObjectInputStream(input);
            String message = (String) objectInput.readObject();
            String channelName = (String) objectInput.readObject();

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(stream);
            out.writeUTF(message);

            //Check if the chat channel is not meant to be sent back.
            if (channelName.equalsIgnoreCase("uknet:discord")) {
                //Split message.
                String[] args = message.split(" ");

                if (args[0].equalsIgnoreCase("addrole")) {

                    long user_id = Long.parseLong(args[1]);
                    long role_id = Long.parseLong(args[2]);
                    Proxy.getInstance().getDiscord().addRole(user_id, role_id);

                } else if (args[0].equalsIgnoreCase("removerole")) {

                    long user_id = Long.parseLong(args[1]);
                    long role_id = Long.parseLong(args[2]);
                    Proxy.getInstance().getDiscord().removeRole(user_id, role_id);

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

            } else {

                for (RegisteredServer server : Proxy.getInstance().getServer().getAllServers()) {
                    if (!server.getPlayersConnected().isEmpty()) {
                        server.sendPluginMessage(MinecraftChannelIdentifier.create("uknet", channelName.split(":")[1]), stream.toByteArray());
                    }
                }

                //Send a message to discord.
                Proxy.getInstance().getDiscord().sendMessage(channelName, stream.toByteArray());
            }

            stream.close();
            out.close();
            socket.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            Proxy.getInstance().getLogger().warn("Could not handle socket message from server!");
        }
    }
}
