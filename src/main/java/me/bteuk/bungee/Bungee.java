package me.bteuk.bungee;

import net.md_5.bungee.api.plugin.Plugin;

import java.io.*;
import java.net.ServerSocket;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class Bungee extends Plugin {

    private static Bungee instance;
    private static ServerSocket serverSocket;

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void onEnable() {
        instance = this;

        CompletableFuture.runAsync(() -> {
            try {
                serverSocket = new ServerSocket(30589);
                while (true) new ChatHandler(serverSocket.accept()).start();
            } catch (IOException ex) {
                if (serverSocket == null) getLogger().log(Level.SEVERE, "Could not bind port to socket!", ex);
            }
        });

        getLogger().info("Enabled Bungee");
    }

    @Override
    public void onDisable() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, "Could not unbind port from socket!", ex);
            }
        }
    }

    public static Bungee getInstance() {
        return instance;
    }
}
