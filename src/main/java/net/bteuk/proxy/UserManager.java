package net.bteuk.proxy;

import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import net.bteuk.network.lib.dto.ChatMessage;
import net.bteuk.network.lib.dto.TabEvent;
import net.bteuk.network.lib.dto.UserConnectReply;
import net.bteuk.network.lib.dto.UserConnectRequest;
import net.bteuk.network.lib.dto.UserUpdate;
import net.bteuk.proxy.utils.SwitchServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static net.bteuk.network.lib.enums.ChatChannels.GLOBAL;
import static net.bteuk.proxy.utils.Constants.JOIN_MESSAGE;
import static net.bteuk.proxy.utils.Constants.RECONNECT_MESSAGE;
import static net.bteuk.proxy.utils.Constants.SERVER_SENDER;
import static net.bteuk.proxy.utils.Constants.WELCOME_MESSAGE;

/**
 * Class to manage the users on the network.
 */
@Getter
public class UserManager {

    private final ProxyServer server;

    private final List<User> users = new ArrayList<>();

    public UserManager(ProxyServer server) {
        this.server = server;
    }

    public UserConnectReply handleUserConnect(UserConnectRequest request) {

        User user = addUser(request);

        // Get the information for the reply.
        return user.createUserConnectReply();

    }

    public User addUser(UserConnectRequest request) {

        String joinMessage = null;

        // See is user instance still exists.
        User user = getUserByUuid(request.getUuid());
        if (user != null) {

            SwitchServer switchServer = user.getSwitchServer();
            if (switchServer != null) {

                // Check if the user is switching the server they are actually connecting to.
                // Else cancel their join events, since they weren't meant for this server.
                // Cancel the switch server task either way.
                if (!switchServer.getToServer().equals(request.getServer())) {
                    user.clearJoinEvent();
                }

                switchServer.cancelTimeout();
                user.setSwitchServer(null);

            } else {
                // Cancel disconnect task.
                user.reconnect();

                // Send reconnect message to servers and discord.
                joinMessage = RECONNECT_MESSAGE;
            }

        } else {

            // Add user.
            user = new User(request.getUuid(), request.getName(), request.getPlayerSkin());
            users.add(user);

            if (!Proxy.getInstance().getGlobalSQL().hasRow("SELECT uuid FROM player_data WHERE uuid='" + request.getUuid() + "';")) {
                // Send welcome message.
                joinMessage = WELCOME_MESSAGE;

                // Set the user as a new user.
                user.setNewUser(true);
            } else {
                // Send connect message.
                joinMessage = JOIN_MESSAGE;
            }
        }

        // Set the server.
        user.setServer(request.getServer());

        // Set the proxy player.
        user.setPlayer(server.getAllPlayers().stream().filter(player -> player.getUniqueId().toString().equals(request.getUuid())).findFirst().orElse(null));

        // Send join message, if not null.
        if (joinMessage != null) {
            sendConnectMessage(joinMessage, user);

            // Add the user to tab for other players.
            Proxy.getInstance().getTabManager().addPlayer(request.getTabPlayer());
        }

        // Send the tab list to the user.
        Proxy.getInstance().getTabManager().sendTablist(user);

        return user;
    }

    /**
     * A user has disconnected, start their removal timer.
     *
     * @param uuid the uuid of the {@link User}
     */
    public void disconnectUser(String uuid) {
        // Get the user.
        User user = getUserByUuid(uuid);

        if (user != null) {
            user.disconnect(() -> removeUser(user));

            // TODO: Run leave events.
        } else {
            Proxy.getInstance().getLogger().warn(String.format("Disconnect event for %s was started, but no User exists.", uuid));
        }
    }

    /**
     * Check if a user has another user muted.
     *
     * @param userUuid      uuid of user
     * @param otherUserUuid uuid of user to check
     * @return boolean if user has otherUser muted
     */
    public boolean isMutedForUser(String userUuid, String otherUserUuid) {
        User user = getUserByUuid(userUuid);
        if (user != null) {
            User otherUser = getUserByUuid(otherUserUuid);
            if (otherUser != null) {
                return user.isMuted(otherUser);
            }
        }
        return false;
    }

    public void updateUser(UserUpdate update) {

    }

    /**
     * Get a user by uuid.
     *
     * @param uuid the uuid of the user to get
     * @return the {@link User} or null if not exists
     */
    public User getUserByUuid(String uuid) {
        return users.stream().filter(user -> user.getUuid().equals(uuid)).findFirst().orElse(null);
    }

    /**
     * Remove a user from the proxy.
     *
     * @param user the user to remove
     */
    private void removeUser(User user) {
        // Remove the user from the list.
        users.remove(user);
        // Remove the user from the list of muted users for all players, if they had this player muted.
        users.forEach(u -> u.unmute(user));
        // TODO: Send message to frontend for them to delete the user instance.
    }

    private void sendConnectMessage(String message, User user) {
        Proxy.getInstance().getDiscord().sendConnectEmbed(message, user.getName(), user.getUuid(), user.getPlayerSkin(), null);
        sendConnectMessageToServer(message, user.getName());
    }

    private void sendConnectMessageToServer(String message, String name) {
        // Construct a chat message to send to the servers.
        Component component = Component.text(message.replace("%player%", name), NamedTextColor.YELLOW);
        ChatMessage chatMessage = new ChatMessage(GLOBAL.getChannelName(), SERVER_SENDER, component);
        try {
            Proxy.getInstance().getChatManager().handle(chatMessage);
        } catch (IOException e) {
            // TODO: Exception handling.
        }
    }
}
