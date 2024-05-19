package net.bteuk.proxy;

import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;
import lombok.Getter;
import lombok.Setter;
import net.bteuk.network.lib.dto.UserConnectReply;
import net.bteuk.proxy.sql.GlobalSQL;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * User object, stored specific information about the user.
 */
public class User {

    private boolean online = true;

    /** Indicator for new users, so the database object is created when fetching information. */
    @Setter
    private boolean newUser = false;

    @Getter
    private String uuid;

    @Getter
    private String name;

    @Getter
    private String playerSkin;

    @Getter
    @Setter
    private String server;

    private String primaryRole;

    /** List of muted users for this session */
    private Set<User> mutedUsers = new HashSet<>();

    /** List of channels the user can read */
    private Set<String> channels = new HashSet<>();

    private ScheduledTask disconnectTask;

    /** Utility reference to the database. */
    private final GlobalSQL globalSQL;

    public User(String uuid, String name, String playerSkin) {
        this.uuid = uuid;
        this.name = name;
        this.playerSkin = playerSkin;

        this.globalSQL = Proxy.getInstance().getGlobalSQL();
    }

    /**
     * The user has disconnected from the network.
     * Store their user for 5 minutes before removing it.
     * This allows their local settings to remain stored in case they reconnect.
     */
    public void disconnect(Runnable runnable) {
        online = false;
        //Run a delayed task to remove the user.
        disconnectTask = Proxy.getInstance().getServer().getScheduler().buildTask(Proxy.getInstance(), runnable)
                .delay(5L, TimeUnit.MINUTES)
                .schedule();
    }

    /**
     * The user has reconnected to the network.
     * This is fired because their user instance was still stored.
     * Cancel the disconnect task that was scheduled.
     */
    public void reconnect() {
        online = true;
        if (disconnectTask != null && disconnectTask.status() == TaskStatus.SCHEDULED) {
            disconnectTask.cancel();
        }
        disconnectTask = null;
    }

    /**
     * Delete the user instance.
     */
    public void delete() {
        // If the disconnectTask is running cancel.
        if (disconnectTask != null && disconnectTask.status() == TaskStatus.SCHEDULED) {
            disconnectTask.cancel();
        }
    }

    public boolean mute(User user) {
        return mutedUsers.add(user);
    }

    public boolean unmute(User user) {
        return mutedUsers.remove(user);
    }

    /**
     * Check if the user is muted for this user.
     *
     * @param user the user to check
     * @return boolean if the user is muted by this user
     */
    public boolean isMuted(User user) {
        return mutedUsers.contains(user);
    }

    /**
     * Create a {@link UserConnectReply} for the user.
     * If the User object in the database is missing, create it.
     *
     * @return the {@link UserConnectReply}
     */
    public UserConnectReply createUserConnectReply() {

        // Create database object if not exists.
        if (newUser && globalSQL.createUser(uuid, name, playerSkin)) {
            setNewUser(false);
        }

        return new UserConnectReply(
                uuid,
                isNavigatorEnabled(),
                isTeleportEnabled(),
                isNightvisionEnabled(),
                getChatChannel(),
                isTipsEnabled()
        );
    }

    private boolean isNavigatorEnabled() {
        return globalSQL.getBoolean("SELECT navigator FROM player_data WHERE uuid='" + uuid + "';");
    }

    private boolean isTeleportEnabled() {
        return globalSQL.getBoolean("SELECT teleport_enabled FROM player_data WHERE uuid='" + uuid + "';");
    }

    private boolean isNightvisionEnabled() {
        return globalSQL.getBoolean("SELECT nightvision_enabled FROM player_data WHERE uuid='" + uuid + "';");
    }

    private String getChatChannel() {
        return globalSQL.getString("SELECT chat_channel FROM player_data WHERE uuid='" + uuid + "';");
    }

    private boolean isTipsEnabled() {
        return globalSQL.getBoolean("SELECT tips_enabled FROM player_data WHERE uuid='" + uuid + "';");
    }
}
