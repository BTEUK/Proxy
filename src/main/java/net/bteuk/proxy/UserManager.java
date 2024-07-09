package net.bteuk.proxy;

import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import net.bteuk.network.lib.dto.ChatMessage;
import net.bteuk.network.lib.dto.DirectMessage;
import net.bteuk.network.lib.dto.MuteEvent;
import net.bteuk.network.lib.dto.SwitchServerEvent;
import net.bteuk.network.lib.dto.UserConnectReply;
import net.bteuk.network.lib.dto.UserConnectRequest;
import net.bteuk.network.lib.dto.UserDisconnect;
import net.bteuk.network.lib.dto.UserRemove;
import net.bteuk.network.lib.dto.UserUpdate;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.proxy.chat.ChatHandler;
import net.bteuk.proxy.eventing.listeners.ServerConnectListener;
import net.bteuk.proxy.exceptions.ErrorMessage;
import net.bteuk.proxy.exceptions.ServerNotFoundException;
import net.bteuk.proxy.utils.SwitchServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static net.bteuk.network.lib.enums.ChatChannels.GLOBAL;
import static net.bteuk.proxy.utils.Analytics.logPlayerCount;
import static net.bteuk.proxy.utils.Constants.JOIN_MESSAGE;
import static net.bteuk.proxy.utils.Constants.LEAVE_MESSAGE;
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

    public void handleUserConnect(UserConnectRequest request) {

        User user = addUser(request);
        Proxy.getInstance().getLogger().info(String.format("UserConnectRequest for %s received.", request.getName()));

        // Get the information for the reply.
        UserConnectReply reply = user.createUserConnectReply();

        // Send the reply to the server.
        try {
            ChatHandler.handle(reply, request.getServer());
        } catch (IOException | ServerNotFoundException e) {
            // TODO: Handle exception
        }
    }

    public void handleUserDisconnect(UserDisconnect disconnect) {

        // Get the user.
        User user = getUserByUuid(disconnect.getUuid());

        if (user != null) {
            // Disconnect.
            disconnectUser(user);

            // Save information about the user.
            user.setNavigatorEnabled(disconnect.isNavigatorEnabled());
            user.setNightvisionEnabled(disconnect.isNightvisionEnabled());
            user.setTipsEnabled(disconnect.isTipsEnabled());
            user.setChatChannel(disconnect.getChatChannel());
            user.setTeleportEnabled(disconnect.isTeleportEnabled());
        } else {
            Proxy.getInstance().getLogger().warn(String.format("Disconnect event for %s was started, but no User exists by that uuid.", disconnect.getUuid()));
        }
    }

    public void handleUserUpdate(UserUpdate userUpdate) {

        // Get the user.
        User user = getUserByUuid(userUpdate.getUuid());

        if (user != null) {
            updateUser(user, userUpdate);
        } else {
            Proxy.getInstance().getLogger().warn(String.format("Update event for %s was received, but no User exists by that uuid.", userUpdate.getUuid()));
        }
    }

    /**
     * Handler for switch server events.
     * On receiving, switch the server of a user.
     * If the user does not switch within 10 seconds, cancel.
     * The {@link ServerConnectListener} will be able to check if the switch has actually happened.
     *
     * @param switchServerEvent the event
     */
    public void handleSwitchServerEvent(SwitchServerEvent switchServerEvent) {

        User user = getUserByUuid(switchServerEvent.getUuid());

        if (user != null) {
            SwitchServer switchServer = user.getSwitchServer();
            if (switchServer != null) {
                // Cancel the existing switch server event.
                switchServer.cancelTimeout();
            }
            user.setSwitchServer(new SwitchServer(user, switchServerEvent.getFrom_server(), switchServerEvent.getTo_server()));
        } else {
            Proxy.getInstance().getLogger().warn(String.format("Switch server event was received for non-existing user %s", switchServerEvent.getUuid()));
        }
    }

    public void handleMuteEvent(MuteEvent muteEvent) {

        String muteType = muteEvent.isMute() ? "mute" : "unmute";
        Component returnMessage;

        try {
            User user = getUserByUuid(muteEvent.getUuid());
            User userToMute = getUserByUuid(muteEvent.getUuidToMute());

            if (user == null) {
                Proxy.getInstance().getLogger().warn(String.format("Mute event was received from non-existing user %s", muteEvent.getUuid()));
                throw new ErrorMessage(ChatUtils.error("An error has occurred, please rejoin the server."));
            } else if (userToMute == null) {
                Proxy.getInstance().getLogger().warn(String.format("Mute event was received for non-existing user %s", muteEvent.getUuidToMute()));
                throw new ErrorMessage(ChatUtils.error("The selected player to %s is no longer online.", muteType));
            }

            // Check if the player is already muted or unmuted.
            // Prevent from muting yourself.
            if (muteEvent.isMute() && user == userToMute) {
                throw new ErrorMessage(ChatUtils.error("You can't mute yourself, just stop sending messages."));
            } else if (muteEvent.isMute() && user.isMuted(userToMute)) {
                throw new ErrorMessage(ChatUtils.error("%s is already muted.", userToMute.getName()));
            } else if (!muteEvent.isMute() && !user.isMuted(userToMute)) {
                throw new ErrorMessage(ChatUtils.error("%s is not muted.", userToMute.getName()));
            }

            if (muteEvent.isMute()) {
                user.mute(userToMute);
                returnMessage = ChatUtils.success("Muted %s for this session.", userToMute.getName());
            } else {
                user.unmute(userToMute);
                returnMessage = ChatUtils.success("Unmuted %s", userToMute.getName());
            }

        } catch (ErrorMessage errorMessage) {
            // Set the error message as the return message.
            returnMessage = errorMessage.getError();
        }

        DirectMessage directMessage = new DirectMessage(muteEvent.getUuid(), muteEvent.getUuid(), returnMessage);
        try {
            Proxy.getInstance().getChatManager().handle(directMessage);
        } catch (IOException e) {
            // TODO Error handling.
        }
    }

    public User addUser(UserConnectRequest request) {

        String joinMessage = null;

        // See is user instance still exists.
        User user = getUserByUuid(request.getUuid());
        if (user != null) {

            SwitchServer switchServer = user.getSwitchServer();
            if (switchServer != null) {

                // Check if the user is switching the server they are actually connecting to.
                // Else cancel their join eventing, since they weren't meant for this server.
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
            user = new User(request.getUuid(), request.getName(), request.getPlayerSkin(), request.getChannels());
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

        // Set primary role.
        user.setPrimaryRole(request.getTabPlayer().getPrimaryGroup());

        // Send join message, if not null.
        if (joinMessage != null) {
            sendConnectMessage(joinMessage, user, Color.GREEN);

            // Add the user to tab for other players.
            Proxy.getInstance().getTabManager().addPlayer(request.getTabPlayer());
        }

        // Log the player count.
        logPlayerCount(getUsers());

        // Send the tab list to the user.
        Proxy.getInstance().getTabManager().sendTablist(user);

        return user;
    }

    /**
     * A user has disconnected, start their removal timer.
     *
     * @param user the {@link User}
     */
    public void disconnectUser(User user) {

        user.disconnect(() -> removeUser(user, false));

        // Log the player count.
        logPlayerCount(getUsers());

        // Remove the player from tab.
        Proxy.getInstance().getTabManager().removePlayer(user.getUuid());

        sendConnectMessage(LEAVE_MESSAGE, user, Color.RED);
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

    public void updateUser(User user, UserUpdate update) {

        // Check what needs updating.
        if (update.getChannels() != null && !user.getChannels().equals(update.getChannels())) {
            user.getChannels().clear();
            user.getChannels().addAll(update.getChannels());
        }

        if (update.getAfk() != null && user.isAfk() != update.getAfk()) {
            user.setAfk(update.getAfk());
            Proxy.getInstance().getTabManager().updatePlayerByUuid(update.getUuid());
        }

        if (update.getTabPlayer() != null && !update.getTabPlayer().getPrimaryGroup().equals(user.getPrimaryRole())) {
            user.setPrimaryRole(update.getTabPlayer().getPrimaryGroup());
            Proxy.getInstance().getTabManager().updatePlayer(update.getTabPlayer());
        }
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
     * Removes all users from the user list.
     * The removal is run as if they disconnected.
     * This is to be used on Proxy-shutdown.
     */
    public void removeAllUsers() {
        while (!getUsers().isEmpty()) {
            removeUser(getUsers().get(0), true);
        }
    }

    /**
     * Remove a user from the proxy.
     *
     * @param user the user to remove
     */
    private void removeUser(User user, boolean shutdown) {
        // Remove the user from the list.
        users.remove(user);
        user.delete();
        // Remove the user from the list of muted users for all players, if they had this player muted.
        users.forEach(u -> u.unmute(user));
        UserRemove userRemoveEvent = new UserRemove(user.getUuid());
        try {
            ChatHandler.handle(userRemoveEvent);
        } catch (IOException e) {
            // TODO Exception handling
        }
        if (!shutdown) {
            Proxy.getInstance().getLogger().info(String.format("Removed user %s from the proxy, they have been offline for more than 5 minutes", user.getName()));
        } else {
            Proxy.getInstance().getLogger().info(String.format("Removed user %s from the proxy due to shutdown", user.getName()));

        }
    }

    private void sendConnectMessage(String message, User user, Color colour) {
        Proxy.getInstance().getDiscord().sendConnectEmbed(message, user.getName(), user.getUuid(), user.getPlayerSkin(), colour, null);
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
