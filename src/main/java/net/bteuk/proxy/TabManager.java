package net.bteuk.proxy;

import net.bteuk.network.lib.dto.TabEvent;
import net.bteuk.network.lib.dto.TabPlayer;
import net.bteuk.network.lib.enums.TabEventType;
import net.bteuk.proxy.chat.ChatHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Keeps track of all users and their tab information.
 * Sends updates to the servers when things change.
 */
public class TabManager {

    private final Set<TabPlayer> tabPlayers;

    public TabManager() {
        tabPlayers = new HashSet<>();
    }

    /**
     * Add the new user to the tablist.
     * Send the new player to all other users, excluding itself.
     *
     * @param tabPlayer the {@link TabPlayer} to add
     */
    public void addPlayer(TabPlayer tabPlayer) {
        tabPlayers.add(tabPlayer);
        Proxy.getInstance().getUserManager().getUsers().forEach(user -> {
            if (!user.getUuid().equals(tabPlayer.getUuid())) {
                TabEvent tabEvent = new TabEvent();
                tabEvent.setUuid(user.getUuid());
                tabEvent.setType(TabEventType.ADD);
                tabEvent.setPlayers(Collections.singleton(createTabPlayer(user, tabPlayer)));
                sendTabEvent(tabEvent);
            }
        });
    }

    /**
     * Send the full tablist to a user.
     * This is used when a user connects to a server.
     * Adjust display names for muted players.
     */
    public void sendTablist(User user) {

        TabEvent tabEvent = new TabEvent();
        tabEvent.setUuid(user.getUuid());
        tabEvent.setType(TabEventType.ADD);

        Set<TabPlayer> players = new HashSet<>();
        tabPlayers.forEach(tabPlayer -> players.add(createTabPlayer(user, tabPlayer)));
        tabEvent.setPlayers(players);

        sendTabEvent(tabEvent);
    }

    private void sendTabEvent(TabEvent tabEvent) {
        try {
            ChatHandler.handle(tabEvent);
        } catch (IOException e) {
            // TODO: Error handling
        }
    }

    private TabPlayer createTabPlayer(User user, TabPlayer tabPlayer) {
        return new TabPlayer(
                tabPlayer.getUuid(),
                formattedName(user, tabPlayer.getUuid(), tabPlayer.getName()),
                tabPlayer.getPrefix(),
                tabPlayer.getPing()
        );
    }

    private Component formattedName(User user, String uuid, Component name) {

        // Find the user.
        User userToAdd = Proxy.getInstance().getUserManager().getUserByUuid(uuid);

        if (userToAdd != null) {
            if (user.isMuted(userToAdd)) {
                name = name.color(NamedTextColor.RED);
            }
            if (userToAdd.isAfk()) {
                name = name.decorate(TextDecoration.ITALIC);
            }
        }
        return name;
    }
}
