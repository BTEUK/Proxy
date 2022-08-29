package me.bteuk.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.util.Properties;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Plugin(id = "proxy", name = "Proxy", version = "1.1.0",
        url = "https://github.com/BTEUK/Proxy", description = "Proxy plugin for UKnet, deals with the chat socket and chooses server for reconnecting.", authors = {"ELgamer"})
public class Proxy {

    private final ProxyServer server;
    private final Logger logger;

    private static Proxy instance;
    private static ServerSocket serverSocket;

    private File dataFolder;

    @Inject
    public Proxy(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;

        instance = this;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

        CompletableFuture.runAsync(() -> {
            try {
                serverSocket = new ServerSocket(30589);
                while (true) new ChatHandler(serverSocket.accept()).start();
            } catch (IOException ex) {
                if (serverSocket == null) logger.warn("Could not bind port to socket!");
            }
        });

        this.dataFolder = getDataFolder();

        logger.info("Loading Proxy");

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

    @Subscribe
    public void choose(PlayerChooseInitialServerEvent e) {
        Player player = e.getPlayer();
        String prev = getLastServer(player.getUniqueId().toString());
        RegisteredServer server;
        //Not null check
        if (prev != null) {
            //Get the RegisteredServer
            server = getServer(prev);
            //Not null check
            if (server != null) {
                try {
                    //Make sure they can join
                    server.ping();
                } catch (CancellationException | CompletionException exception) {
                    return;
                }
                e.setInitialServer(server);
            }
        }
    }

    @Subscribe
    public void change(ServerConnectedEvent e) {
        //Store server as last server.
        setLastServer(e.getPlayer().getUniqueId().toString(), e.getServer().getServerInfo().getName());
    }

    public static Proxy getInstance() {
        return instance;
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public static RegisteredServer getServer(String name) {
        for (RegisteredServer server : instance.getServer().getAllServers()) {
            if (server.getServerInfo().getName().equalsIgnoreCase(name)) {
                return server;
            }
        }
        return null;
    }

    public void setLastServer(String uuid, String servername) {

        try (OutputStream output = new FileOutputStream(dataFolder + "/config.properties")) {

            Properties prop = new Properties();

            prop.setProperty(uuid, servername);

            prop.store(output, null);

        } catch (IOException io) {
            io.printStackTrace();
        }

    }

    public String getLastServer(String uuid) {

        try (InputStream input = new FileInputStream(dataFolder + "/config.properties")) {

            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            return prop.getProperty(uuid);

        } catch (IOException e) {
            return null;
        }
    }

    public File getDataFolder() {
        File dataFolder = this.dataFolder;
        if (dataFolder == null) {
            String path = "plugins/proxy/";
            try {
                dataFolder = new File(path);
                dataFolder.mkdir();
                return dataFolder;
            } catch (Exception e) {
                return null;
            }
        } else {
            return dataFolder;
        }
    }
}
