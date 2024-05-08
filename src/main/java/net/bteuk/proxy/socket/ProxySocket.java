package net.bteuk.proxy.socket;

import net.bteuk.proxy.Proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CompletableFuture;

public class ProxySocket {

    private final int port;

    private ServerSocket serverSocket;

    public ProxySocket(int port) {
        this.port = port;
    }

    public void start() {
        CompletableFuture.runAsync(() -> {
            try {
                serverSocket = new ServerSocket(port);
                while (true)
                    new SocketHandler(serverSocket.accept()).start();
            } catch (IOException ex) {
                if (serverSocket == null) Proxy.getInstance().getLogger().warn("Could not bind port to socket!");
            }
        });
    }
}
