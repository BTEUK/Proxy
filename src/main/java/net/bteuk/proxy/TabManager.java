package net.bteuk.proxy;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import net.bteuk.network.lib.dto.AddTeamEvent;
import net.bteuk.network.lib.dto.TabPlayer;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.proxy.chat.ChatHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Keeps track of all users and their tab information.
 * Sends updates to the servers when things change.
 */
public class TabManager {

    private final ProxyServer server;

    private final Set<TabPlayer> tabPlayers = new HashSet<>();

    private static final Component HEADER = Component.text("BTE ", NamedTextColor.AQUA, TextDecoration.BOLD)
            .append(Component.text("UK", NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
            .append(Component.newline());

    private static final Component FOOTER = Component.newline()
            .append(ChatUtils.line("Server Info: "))
            .append(Component.text("/help", NamedTextColor.GRAY))
            .append(Component.newline())
            .append(ChatUtils.line("More Info: "))
            .append(Component.text("/discord", NamedTextColor.GRAY));

    public TabManager(ProxyServer server) {
        this.server = server;

        // Update ping every 30 seconds.
        Proxy.getInstance().getServer().getScheduler().buildTask(Proxy.getInstance(), this::updatePing)
                .repeat(30L, TimeUnit.SECONDS)
                .schedule();
    }

    /**
     * Add the new user to the tablist.
     * Send the new player to all other users, excluding themselves.
     *
     * @param tabPlayer the {@link TabPlayer} to add
     */
    public void addPlayer(TabPlayer tabPlayer) {
        tabPlayers.add(tabPlayer);
        Proxy.getInstance().getUserManager().getUsers().forEach(user -> {
            if (!user.getUuid().equals(tabPlayer.getUuid()) && user.getPlayer() != null) {
                user.getPlayer().getTabList().addEntry(createTabPlayer(user, tabPlayer));
            }
        });
        // Send add team event to servers.
        sendAddTeam(tabPlayer);
    }

    /**
     * Remove a user from the tablist.
     * Remove the user from the tablist of all users.
     *
     * @param uuid of the {@link TabPlayer} to remove
     */
    public void removePlayer(String uuid) {
        TabPlayer tabPlayer = findTabPlayerByUuid(uuid);
        if (tabPlayer != null) {
            tabPlayers.remove(tabPlayer);
            Proxy.getInstance().getUserManager().getUsers().forEach(user -> {
                if (user.getPlayer() != null) {
                    // Find the entries that match the player name.
                    Collection<TabListEntry> tablist = user.getPlayer().getTabList().getEntries();
                    List<TabListEntry> entriesToRemove = tablist.stream().filter(tabListEntry -> tabListEntry.getProfile().getName().equals(tabPlayer.getName())).toList();
                    // Remove the entries by UUID.
                    entriesToRemove.forEach(tabListEntry -> user.getPlayer().getTabList().removeEntry(tabListEntry.getProfile().getId()));
                }
            });
        }
    }

    /**
     * Update a player in the tablist for all players.
     * This is used when the players displayname changes.
     * Triggers can be change in role, afk, mute.
     *
     * @param tabPlayer the tabplayer to update
     */
    public void updatePlayer(TabPlayer tabPlayer) {
        // Find the tab player by uuid.
        TabPlayer currentTabPlayer = findTabPlayerByUuid(tabPlayer.getUuid());
        // If null, add the tab player.
        if (currentTabPlayer == null) {
            addPlayer(tabPlayer);
        } else {
            // Update the display name and ping.
            String name = tabPlayer.getName();
            updatePlayerPing(name, findPingForPlayer(tabPlayer.getUuid()));
            updatePlayerDisplayName(name, tabPlayer);
            // If the primary role has changed update the players team.
            if (!tabPlayer.getPrimaryGroup().equals(currentTabPlayer.getPrimaryGroup())) {
                currentTabPlayer.setPrimaryGroup(tabPlayer.getPrimaryGroup());
                currentTabPlayer.setPrefix(tabPlayer.getPrefix());
                sendAddTeam(tabPlayer);
            }
        }
    }

    public void updatePlayerByUuid(String uuid) {
        // Find the tab player by uuid.
        TabPlayer currentTabPlayer = findTabPlayerByUuid(uuid);
        if (currentTabPlayer != null) {
            updatePlayer(currentTabPlayer);
        }
    }

    /**
     * Send the full tablist to a user.
     * This is used when a user connects to a server.
     * Adjust display names for muted players.
     */
    public void sendTablist(User user) {
        // Player must exist.
        if (user.getPlayer() != null) {
            List<TabListEntry> tabListEntries = new ArrayList<>();
            tabPlayers.forEach(tabPlayer -> {
                TabListEntry entry = createTabPlayer(user, tabPlayer);
                if (entry != null) {
                    tabListEntries.add(entry);
                }
            });
            user.getPlayer().getTabList().addEntries(tabListEntries);

            // Send header and footer.
            user.getPlayer().sendPlayerListHeaderAndFooter(HEADER, FOOTER);
        }
    }

    public void sendAddTeam(TabPlayer tabPlayer) {
        try {
            ChatHandler.handle(new AddTeamEvent(tabPlayer.getName(), tabPlayer.getPrimaryGroup()));
        } catch (IOException e) {
            // TODO: Exception handling.
        }
    }

    /**
     * Update a specific user in the tablist of another user.
     * This can be used specifically when you do a personal mute of a player.
     *
     * @param userToGetTablist the user to update the tablist for
     * @param userToUpdate the user to update in the tablist
     */
    public void updatePlayerInTablistOfPlayer(User userToGetTablist, User userToUpdate) {
        TabPlayer tabPlayer = findTabPlayerByUuid(userToUpdate.getUuid());
        TabListEntry tabEntry = findTabListEntryForPlayer(userToGetTablist.getPlayer().getTabList().getEntries(), userToUpdate.getName());
        if (tabPlayer != null && tabEntry != null) {
            // Update the display name.
            tabEntry.setDisplayName(formattedName(userToGetTablist, tabPlayer));
        }
    }

    /**
     * Update the ping in tab for all players.
     */
    private void updatePing() {
        server.getAllPlayers().forEach(player -> updatePingForTabList(player.getTabList().getEntries()));
    }

    /**
     * Update the ping in tab for a specific player.
     * @param playerName the name of the player
     */
    private void updatePlayerPing(String playerName, int ping) {
        server.getAllPlayers().forEach(player -> {
            TabListEntry tabEntry = findTabListEntryForPlayer(player.getTabList().getEntries(), playerName);
            if (tabEntry != null) {
                updateLatency(tabEntry, ping);
            }
        });
    }

    private void updatePlayerDisplayName(String playerName, TabPlayer tabPlayer) {
        server.getAllPlayers().forEach(player -> {
            TabListEntry tabEntry = findTabListEntryForPlayer(player.getTabList().getEntries(), playerName);
            User user = Proxy.getInstance().getUserManager().getUserByUuid(String.valueOf(player.getUniqueId()));
            if (tabEntry != null && user != null) {
                updateDisplayName(tabEntry, formattedName(user, tabPlayer));
            }
        });
    }

    private void updatePingForTabList(Collection<TabListEntry> tabEntries) {
        tabEntries.forEach(tabEntry -> {
            int ping = findPingForPlayer(tabEntry.getProfile().getId().toString());
            updateLatency(tabEntry, ping);
        });
    }

    private void updateLatency(TabListEntry tabEntry, int ping) {
        tabEntry.setLatency(ping);
    }
    
    private void updateDisplayName(TabListEntry tabEntry, Component displayName) {
        tabEntry.setDisplayName(displayName);
    }

    private TabListEntry findTabListEntryForPlayer(Collection<TabListEntry> tabEntries, String playerName) {
        return tabEntries.stream().filter(tabEntry -> tabEntry.getProfile().getName().equals(playerName)).findFirst().orElse(null);
    }

    private int findPingForPlayer(String uuid) {
        return (int) server.getAllPlayers().stream().filter(player -> player.getUniqueId().toString().equals(uuid)).mapToLong(Player::getPing).findFirst().orElse(-1);
    }

    private TabPlayer findTabPlayerByUuid(String uuid) {
        return tabPlayers.stream().filter(tabPlayer -> tabPlayer.getUuid().equals(uuid)).findFirst().orElse(null);
    }

    private TabListEntry createTabPlayer(User user, TabPlayer tabPlayer) {
        // Find player instance of TabPlayer.
        Optional<Player> optionalPlayer = server.getAllPlayers().stream().filter(p -> p.getUniqueId().toString().equals(tabPlayer.getUuid())).findFirst();
        if (optionalPlayer.isPresent()) {
            Player player = optionalPlayer.get();
            return TabListEntry.builder()
                    .tabList(user.getPlayer().getTabList())
                    .gameMode(1) // All players will be shown in creative
                    .displayName(formattedName(user, tabPlayer))
                    .profile(GameProfile.forOfflinePlayer(player.getUsername()).withProperties(player.getGameProfileProperties()))
                    .latency((int) player.getPing())
                    .listed(true)
                    .build();
        } else {
            return null;
        }
    }


    private Component formattedName(User user, TabPlayer tabPlayer) {

        // Find the user.
        User userToAdd = Proxy.getInstance().getUserManager().getUserByUuid(tabPlayer.getUuid());

        Component name = ChatUtils.line(tabPlayer.getName());

        if (userToAdd != null) {
            if (user.isMuted(userToAdd) /* TODO: Check if player is globally muted */) {
                name = name.color(NamedTextColor.RED);
            }
            if (userToAdd.isAfk()) {
                name = name.decorate(TextDecoration.ITALIC);
            }
        }

        // Add the prefix.
        if (tabPlayer.getPrefix() != null) {
            name = tabPlayer.getPrefix()
                    .append(Component.space())
                    .append(name);
        }

        return name;
    }
}
