package net.bteuk.proxy.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.bteuk.network.lib.dto.AbstractTransferObject;
import net.bteuk.proxy.Proxy;
import net.bteuk.proxy.exceptions.ServerNotFoundException;
import org.apache.logging.log4j.core.jmx.Server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

public class ChatHandler {

    private static final ChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("uknet", "network");

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
     * Handle a message, send it to a specific server.
     *
     * @param message the message to handle.
     * @param server the server to send the message to.
     *
     * @throws ServerNotFoundException if the server can not be found
     */
    public static void handle(AbstractTransferObject message, String server) throws IOException, ServerNotFoundException {

        // Send the direct message to the specified server.
        sendProxyMessage(message, server);

    }

    /**
     * Send a message to all servers on a specific channel.
     *
     * @param message the {@link AbstractTransferObject} to send
     */
    private static void sendProxyMessage(AbstractTransferObject message) throws IOException {

        ByteArrayOutputStream stream = writeToStream(message);

        //Send message to all servers.
        Proxy.getInstance().getServer().getAllServers().forEach(server ->
                server.sendPluginMessage(CHANNEL, stream.toByteArray()));
    }

    private static void sendProxyMessage(AbstractTransferObject message, String serverName) throws IOException, ServerNotFoundException {

        // Serialize the chat message and convert it to bytes.
        ByteArrayOutputStream stream = writeToStream(message);

        //Send message to all servers.
        Optional<RegisteredServer> optionalServer = Proxy.getInstance().getServer().getServer(serverName);

        if (optionalServer.isPresent()) {
            optionalServer.get().sendPluginMessage(CHANNEL, stream.toByteArray());
        } else {
            throw new ServerNotFoundException(serverName);
        }
    }

    private static ByteArrayOutputStream writeToStream(AbstractTransferObject message) throws IOException {
        // Serialize the chat message and convert it to bytes.
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(stream, message);

        return stream;
    }
}
