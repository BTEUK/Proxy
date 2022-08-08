package me.bteuk.chatsocket;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CompletableFuture;

@Plugin(id = "chatsocket", name = "ChatSocket", version = "1.1.0",
        url = "https://github.com/BTEUK/ChatSocket", description = "Handles incoming and outgoing chat between servers on UKnet", authors = {"ELgamer"})
public class ChatSocket {

    private final ProxyServer server;
    private final Logger logger;

    private static ChatSocket instance;
    private static ServerSocket serverSocket;

    private ChannelIdentifier channel;

    @Inject
    public ChatSocket(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;

        this.instance = this;

        logger.info("Constructing ChatSocket");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

        this.channel = MinecraftChannelIdentifier.create("uknet","globalchat");

        CompletableFuture.runAsync(() -> {
            try {
                serverSocket = new ServerSocket(30589);
                while (true) new ChatHandler(serverSocket.accept()).start();
            } catch (IOException ex) {
                if (serverSocket == null) logger.warn("Could not bind port to socket!");
            }
        });

        logger.info("Loading ChatSocket");

    }

    @Subscribe
    public void onProxyShutDown(ProxyShutdownEvent event) {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                logger.warn("Could not unbind port from socket!");
            }
        }
    }

    public static ChatSocket getInstance() {
        return instance;
    }

    public ProxyServer getServer() {
        return server;
    }

    public ChannelIdentifier getChannel() {
        return channel;
    }

    public Logger getLogger() {
        return logger;
    }
}
