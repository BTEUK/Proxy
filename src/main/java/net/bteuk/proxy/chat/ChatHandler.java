package net.bteuk.proxy.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.bteuk.network.lib.dto.AbstractTransferObject;
import net.bteuk.proxy.Proxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ChatHandler {

    /**
     * Handle a message.
     *
     * @param message the message to handle.
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
