package me.bteuk.proxy;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import java.io.*;
import java.net.Socket;

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

            for(RegisteredServer server : Proxy.getInstance().getServer().getAllServers()) {
                if(!server.getPlayersConnected().isEmpty()) {
                    server.sendPluginMessage(MinecraftChannelIdentifier.create("uknet", channelName), stream.toByteArray());
                }
            }

            stream.close();
            out.close();
            socket.close();
        } catch (Exception ex) {
            Proxy.getInstance().getLogger().warn("Could not handle socket message from server!");
        }
    }
}