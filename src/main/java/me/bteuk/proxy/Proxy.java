package me.bteuk.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.bteuk.proxy.config.Config;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.*;

@Plugin(id = "proxy", name = "Proxy", version = "1.3.0",
        url = "https://github.com/BTEUK/Proxy", description = "Proxy plugin for UKnet, deals with the chat and server selection.", authors = {"ELgamer"})
public class Proxy {

    private final ProxyServer server;
    private final Logger logger;

    private static Proxy instance;
    private static ServerSocket serverSocket;

    private Config config;

    private File dataFolder;

    private Discord discord;

    private ArrayList<Linked> linking;

    public GlobalSQL globalSQL;

    @Inject
    public Proxy(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;

        instance = this;

        try {
            config = new Config();
        } catch (IOException e) {
            getLogger().warn("An error occurred while loading the config.");
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

        discord = new Discord();

        linking = new ArrayList<>();

        CompletableFuture.runAsync(() -> {
            try {
                serverSocket = new ServerSocket(30589);
                while (true) new ChatHandler(serverSocket.accept()).start();
            } catch (IOException ex) {
                if (serverSocket == null) logger.warn("Could not bind port to socket!");
            }
        });

        this.dataFolder = getDataFolder();

        //Setup MySQL
        try {

            //Global Database
            String global_database = config.getString("database.global");
            BasicDataSource global_dataSource = mysqlSetup(global_database);
            globalSQL = new GlobalSQL(global_dataSource);

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            getLogger().error("Failed to connect to the database, please check that you have set the config values correctly.");
            return;
        }

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

        // Clear JDA listeners
        if (discord.getJda() != null) {
            //Unregister listners.
            discord.getJda().getEventManager().getRegisteredListeners().forEach(listener -> discord.getJda().getEventManager().unregister(listener));
        }

        // try to shut down jda gracefully
        if (discord.getJda() != null) {
            try {
                discord.getJda().shutdownNow();
                discord.setJda(null);
            } catch (NoClassDefFoundError e) {
            }
        }
    }

    @Subscribe
    public void choose(PlayerChooseInitialServerEvent e) {

        Player player = e.getPlayer();

        //Set players protocol version in the database.
        //This will allow other servers to check and notify the player is using a suboptimal version.
        ProtocolVersion version = player.getProtocolVersion();
        version.getProtocol();
        //TODO set protocol version in database.

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

    //Creates the mysql connection.
    private BasicDataSource mysqlSetup(String database) throws SQLException, ClassNotFoundException {

        Class.forName("com.mysql.cj.jdbc.Driver");

        String host = config.getString("host");
        int port = config.getInt("port");
        String username = config.getString("username");
        String password = config.getString("password");

        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?&useSSL=false&");
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        testDataSource(dataSource);
        return dataSource;

    }

    public void testDataSource(BasicDataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(1000)) {
                throw new SQLException("Could not establish database connection.");
            }
        }
    }

    public Config getConfig() {
        return config;
    }

    public Discord getDiscord() {
        return discord;
    }

    public ArrayList<Linked> getLinking() {
        return linking;
    }
}
