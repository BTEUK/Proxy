package net.bteuk.proxy;

import lombok.Getter;
import net.bteuk.network.lib.dto.UserUpdate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.bteuk.proxy.utils.Constants.RECONNECT_MESSAGE;

/**
 * Class to manage the users on the network.
 */
public class UserManager {

    @Getter
    List<User> users = new ArrayList<>();

    public void addUser(String uuid) {

        // See is user instance still exists.
        User user = getUserByUuid(uuid);
        if (user != null) {

            // Cancel disconnect task.
            user.reconnect();

            // Send reconnect message to servers and discord.
            Proxy.getInstance().getDiscord().sendConnectEmbed(RECONNECT_MESSAGE, user.getName(), user.getUuid(), user.getPlayerSkin(), null);

        } else {

            // Create new user.

            // Send connect message, or welcome message if this is their first time joining.

        }


    }

    /**
     * A user has disconnected, start their removal timer.
     * @param uuid the uuid of the {@link User}
     */
    public void disconnectUser(String uuid) {
        // Get the user.
        User user = getUserByUuid(uuid);

        if (user != null) {
            user.disconnect(() -> removeUser(user));
        } else {
            Proxy.getInstance().getLogger().warn(String.format("Disconnect event for %s was started, but no User exists.", uuid));
        }
    }

    /**
     * Check if a user has another user muted.
     * @param userUuid uuid of user
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
     * Remove a user from the proxy.
     * @param user the user to remove
     */
    private void removeUser(User user) {
        // Remove the user from the list.
        users.remove(user);
        // Remove the user from the list of muted users for all players, if they had this player muted.
        users.forEach(u -> u.unmute(user));
    }

    /**
     * Get a user by uuid.
     *
     * @param uuid the uuid of the user to get
     * @return the {@link User} or null if not exists
     */
    private User getUserByUuid(String uuid) {
        return users.stream().filter(user -> Objects.equals(user.getUuid(), uuid)).findFirst().orElse(null);
    }
}
