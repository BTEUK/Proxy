package me.bteuk.bungee;

import net.md_5.bungee.api.config.ServerInfo;

import java.io.*;
import java.net.Socket;
import java.util.logging.Level;

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

            for(ServerInfo server : Bungee.getInstance().getProxy().getServers().values()) {
                if(!server.getPlayers().isEmpty()) {
                    server.sendData("uknet:globalchat", stream.toByteArray());
                }
            }

            stream.close();
            out.close();
            socket.close();
        } catch (Exception ex) {
            Bungee.getInstance().getLogger().log(Level.SEVERE, "Could not handle socket message from server!", ex);
        }
    }
}
