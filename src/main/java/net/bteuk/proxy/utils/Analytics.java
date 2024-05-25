package net.bteuk.proxy.utils;

import net.bteuk.proxy.Proxy;
import net.bteuk.proxy.User;

import java.util.List;

/**
 * This class handles all the statistics gathered by the server.
 * The reason for gathering this information is, so we can tell what the behaviour of a player is like
 * and tailor the experience of the server to better accommodate their style of play.
 */
public class Analytics {

    // Log the player count, which is the number of online users.
    public static void logPlayerCount(List<User> users) {
        Proxy.getInstance().getGlobalSQL().update("INSERT INTO player_count(log_time,players) VALUES(" + Time.currentTime() + "," + users.stream().filter(User::isOnline).count() + ");");
    }

}
