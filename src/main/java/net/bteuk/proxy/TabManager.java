package net.bteuk.proxy;

import net.bteuk.network.lib.dto.TabEvent;
import net.bteuk.network.lib.dto.TabPlayer;

import java.util.Set;

/**
 * Keeps track of all users and their tab information.
 * Sends updates to the servers when things change.
 */
public class TabManager {

    private final Set<TabPlayer> tabPlayers;

    public void handle(TabEvent tabEvent) {

    }

}
