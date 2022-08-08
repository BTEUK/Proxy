package me.bteuk.chatsocket;

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
            String playerServer = (String) objectInput.readObject();

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(stream);
            out.writeUTF(message);

            for(RegisteredServer server : ChatSocket.getInstance().getServer().getAllServers()) {
                ServerInfo serverInfo = server.getServerInfo();
                if(!serverInfo.getName().equals(playerServer) && !server.getPlayersConnected().isEmpty()) {
                    server.sendPluginMessage(ChatSocket.getInstance().getChannel(), stream.toByteArray());
                }
            }

            stream.close();
            out.close();
            socket.close();
        } catch (Exception ex) {
            ChatSocket.getInstance().getLogger().warn("Could not handle socket message from server!");
        }
    }
}
