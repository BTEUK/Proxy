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
import me.bteuk.proxy.sql.GlobalSQL;
import me.bteuk.proxy.sql.PlotSQL;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
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
    public PlotSQL plotSQL;

    private HashMap<UUID, Integer> protocol_version;

    private HashMap<UUID, String> last_server;

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

        protocol_version = new HashMap<>();

        last_server = new HashMap<>();
        loadLastServer();

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

            //Plot Database
            String plot_database = config.getString("database.plot");
            BasicDataSource plot_dataSource = mysqlSetup(plot_database);
            plotSQL = new PlotSQL(plot_dataSource);

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            getLogger().error("Failed to connect to the database, please check that you have set the config values correctly.");
            return;
        }

        //Setup review status message.
        new ReviewStatus();

        getServer().getScheduler()
                .buildTask(this, () -> {

                    getServer().getAllPlayers().forEach(player -> {

                        String uuid = player.getUniqueId().toString();

                        if (globalSQL.hasRow("SELECT uuid FROM online_users WHERE uuid='" + uuid + "';")) {

                            //Update last ping.
                            globalSQL.update("UPDATE online_users SET last_ping=" + System.currentTimeMillis() + " WHERE uuid='" + uuid + "';");

                        }
                    });
                })
                .repeat(1L, TimeUnit.MINUTES)
                .schedule();

        logger.info("Loading Proxy");

    }

    @Subscribe
    public void onProxyShutDown(ProxyShutdownEvent event) {

        //Store the last server players are connected to.
        updateLastServer();

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

        //Get the protocol version of the player.
        //This will allow other servers to check and notify the player is using a suboptimal version.
        ProtocolVersion version = player.getProtocolVersion();

        //Store the protocol version in a map.
        if (protocol_version.containsKey(player.getUniqueId())) {
            //Update protocol version.
            protocol_version.replace(player.getUniqueId(), version.getProtocol());
        } else {
            protocol_version.put(player.getUniqueId(), version.getProtocol());
        }

        String prev = getLastServer(player.getUniqueId());
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
        setLastServer(e.getPlayer().getUniqueId(), e.getServer().getServerInfo().getName());
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

    private void setLastServer(UUID uuid, String serverName) {
        last_server.put(uuid, serverName);
    }

    private String getLastServer(UUID uuid) {
        return last_server.get(uuid);
    }

    //Store the last server data in the properties file when the server closes.
    private void updateLastServer() {

        try (OutputStream output = new FileOutputStream(dataFolder + "/last_server.properties")) {

            Properties prop = new Properties();

            //Store all entries of the array.
            for (Map.Entry<UUID, String> entry : last_server.entrySet()) {
                prop.setProperty(entry.getKey().toString(), entry.getValue());
            }

            prop.store(output, null);

        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    //Sets the hashmap with entries from the properties file on server load.
    private void loadLastServer() {

        try (InputStream input = new FileInputStream(dataFolder + "/last_server.properties")) {

            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            prop.forEach((uuid, server) -> last_server.put(UUID.fromString((String) uuid), (String) server));

        } catch (IOException ignored) {
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
