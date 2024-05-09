package net.bteuk.proxy.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.bteuk.network.lib.dto.AbstractTransferObject;
import net.bteuk.network.lib.dto.ChatMessage;
import net.bteuk.network.lib.socket.SocketHandler;
import net.bteuk.proxy.Proxy;
import net.bteuk.proxy.ChatHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ProxySocketHandler implements SocketHandler {

    @Override
    public void handle(AbstractTransferObject abstractTransferObject) {
        // Handle the different objects.
        if (abstractTransferObject instanceof ChatMessage chatMessage) {
            try {
                ChatHandler.handle(chatMessage);
            } catch (IOException e) {
                // Ignored
            }
        } else {
            Proxy.getInstance().getLogger().warn(String.format("Socket object has an unrecognised type %s", abstractTransferObject.getClass().getTypeName()));
        }
    }
}
