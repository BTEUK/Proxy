package net.bteuk.proxy;

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
import lombok.Getter;
import net.bteuk.network.lib.socket.InputSocket;
import net.bteuk.network.lib.socket.SocketHandler;
import net.bteuk.proxy.chat.ChatManager;
import net.bteuk.proxy.config.Config;
import net.bteuk.proxy.events.CommandListener;
import net.bteuk.proxy.socket.ProxySocketHandler;
import net.bteuk.proxy.sql.DatabaseInit;
import net.bteuk.proxy.sql.GlobalSQL;
import net.bteuk.proxy.sql.PlotSQL;
import net.bteuk.proxy.sql.RegionSQL;
import net.bteuk.proxy.utils.Linked;
import net.bteuk.proxy.utils.ReviewStatus;
import net.dv8tion.jda.api.EmbedBuilder;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static net.bteuk.proxy.utils.Constants.LEAVE_MESSAGE;

@Plugin(id = "proxy", name = "Proxy", version = "1.7.2",
        url = "https://github.com/BTEUK/Proxy", description = "Proxy plugin, managed chat, discord and server related actions.", authors = {"ELgamer"})
public class Proxy {

    @Getter
    private final ProxyServer server;
    @Getter
    private final Logger logger;

    @Getter
    private static Proxy instance;
    private InputSocket inputSocket;

    @Getter
    private Config config;

    private File dataFolder;

    @Getter
    private Discord discord;

    @Getter
    private ArrayList<Linked> linking;

    @Getter
    private GlobalSQL globalSQL;
    @Getter
    private PlotSQL plotSQL;
    @Getter
    private RegionSQL regionSQL;

    private HashMap<UUID, String> last_server;

    @Getter
    private UserManager userManager;

    @Getter
    private ChatManager chatManager;

    @Getter
    private TabManager tabManager;

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

        last_server = new HashMap<>();

        //Load command listener to forward /server to the servers.
        new CommandListener(server, this);

        int socket_port = Proxy.getInstance().getConfig().getInt("socket_port");

        userManager = new UserManager();

        chatManager = new ChatManager(userManager);

        tabManager = new TabManager();

        // Start socket.
        if (socket_port == 0) {
            logger.error("Socket port is not set in config or is set to 0. Please set a valid port!");
        } else {
            // Create the socket handler.
            SocketHandler handler = new ProxySocketHandler(chatManager);

            // Create the input socket.
            inputSocket = new InputSocket(socket_port);
            inputSocket.start(handler);
        }

        this.dataFolder = getDataFolder();

        loadLastServer();

        //Setup MySQL
        try {

            DatabaseInit init = new DatabaseInit();

            //Global Database
            String global_database = config.getString("database.global");
            BasicDataSource global_dataSource = init.mysqlSetup(global_database);
            globalSQL = new GlobalSQL(global_dataSource);

            //Region Database
            String region_database = config.getString("database.region");
            BasicDataSource region_dataSource = init.mysqlSetup(region_database);
            regionSQL = new RegionSQL(region_dataSource);

            //Plot Database
            String plot_database = config.getString("database.plot");
            BasicDataSource plot_dataSource = init.mysqlSetup(plot_database);
            plotSQL = new PlotSQL(plot_dataSource);

        } catch (SQLException | RuntimeException | ClassNotFoundException e) {
            logger.error("Failed to connect to the database, please check that you have set the config values correctly.");
            logger.error("Disabling Proxy");
            return;
        }

        // To make sure no players were left online when the Proxy was closed, clear the list of online users.
        globalSQL.update("DELETE FROM online_users;");

        //Setup review status message.
        new ReviewStatus();

        logger.info("Loaded Proxy");

    }

    @Subscribe
    public void onProxyShutDown(ProxyShutdownEvent event) {

        ArrayList<String> online_users = globalSQL.getStringList("SELECT uuid FROM online_users;");
        AtomicInteger users = new AtomicInteger(online_users.size());

        //Store the last server players are connected to.
        updateLastServer();

        if (inputSocket != null) {
            inputSocket.close();
        }

        // Get start time.
        long startTime = System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();

        // Show disconnect message for all players in discord.
        for (User user : userManager.getUsers()) {
            discord.sendConnectEmbed(LEAVE_MESSAGE, user.getName(), user.getUuid(), user.getPlayerSkin(), (reply) -> users.decrementAndGet());
        }

        // Stop if it takes longer than 15 seconds.
        while (users.get() > 0 && (currentTime - startTime) < 15000) {
            // Get the time for a pontetial timeout.
            currentTime = System.currentTimeMillis();
        }

        if (!online_users.isEmpty()) {
            getLogger().info("Sent disconnect message to online users!");
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
            } catch (NoClassDefFoundError ignored) {
            }
        }
    }

    @Subscribe
    public void choose(PlayerChooseInitialServerEvent e) {

        Player player = e.getPlayer();


        String prev = getLastServer(player.getUniqueId());
        RegisteredServer server;
        //Not null check
        if (prev != null) {
            //Get the RegisteredServer
            server = getServer(prev);
            //Not null check
            if (server != null) {
                //Check if server is online.
                if (globalSQL.hasRow("SELECT name FROM server_data WHERE name='" + server.getServerInfo().getName() + "' AND online=1;")) {
                    e.setInitialServer(server);
                    return;
                }
            }
        }

        //Try default server.
        String default_server = config.getString("default_server");

        //Try to set the default server.
        if (default_server != null) {

            RegisteredServer registeredServer = getServer(default_server);

            if (registeredServer != null) {

                //Set the default server.
                //Check if server is online.
                if (globalSQL.hasRow("SELECT name FROM server_data WHERE name='" + registeredServer.getServerInfo().getName() + "' AND online=1;")) {
                    e.setInitialServer(registeredServer);
                    return;
                }
            }
        }

        RegisteredServer random_server = getRandomOnlineServer();

        //Check if any server exists.
        if (random_server == null) {
            return;
        }

        //Set the server.
        e.setInitialServer(random_server);

    }

    private RegisteredServer getRandomOnlineServer() {

        Collection<RegisteredServer> servers = getServer().getAllServers();

        for (RegisteredServer server : servers) {
            //Check if server is online.
            if (globalSQL.hasRow("SELECT name FROM server_data WHERE name='" + server.getServerInfo().getName() + "' AND online=1;")) {
                return server;
            }
        }

        //Return null if no servers can be found.
        return null;

    }

    @Subscribe
    public void change(ServerConnectedEvent e) {
        //Store server as last server.
        setLastServer(e.getPlayer().getUniqueId(), e.getServer().getServerInfo().getName());
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
            logger.info("Loaded last_server.properties with " + last_server.size() + " entries.");

        } catch (IOException ignored) {
            logger.info("last_server.properties does not exist, if this is the first time loading the plugin this is normal behaviour.");
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

    public String getAvatarUrl(String uuid, String texture) {
        return constructAvatarUrl(uuid, texture);
    }

    private String constructAvatarUrl(String uuid, String texture) {

        String defaultUrl = "https://crafatar.com/avatars/{uuid-nodashes}.png?size={size}&overlay#{texture}";

        defaultUrl = defaultUrl
                .replace("{texture}", texture != null ? texture : "")
                .replace("{uuid-nodashes}", Objects.requireNonNull(uuid).replace("-", ""))
                .replace("{size}", "128");

        return defaultUrl;
    }
}
