package me.bteuk.proxy.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.bteuk.proxy.ChatHandler;
import me.bteuk.proxy.Proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class SocketHandler extends Thread {

    private final Socket clientSocket;

    public SocketHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void run() {
        try (
                //PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        ) {

            ObjectMapper mapper = new ObjectMapper();
            SocketObject object = mapper.readValue(in, SocketObject.class);

            // Handle the different objects.
            if (object instanceof ChatMessage chatMessage) {
                ChatHandler.handle(chatMessage);
            } else {
                Proxy.getInstance().getLogger().warn(String.format("Socket object has an unrecognised type %s", object.getClass().getTypeName()));
            }

            // Close the socket;
            clientSocket.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
