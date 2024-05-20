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
        sendAddTeam(tabPlayer.getName(), tabPlayer.getPrimaryGroup());
    }

    /**
     * Remove a user from the tablist.
     * Remove the user from the tablist of all users.
     *
     * @param tabPlayer the {@link TabPlayer} to remove
     */
    public void removePlayer(TabPlayer tabPlayer) {
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

    public void updatePlayer(TabPlayer tabPlayer) {

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
                    tabListEntries.add(createTabPlayer(user, tabPlayer));
                }
            });
            user.getPlayer().getTabList().addEntries(tabListEntries);

            // Send header and footer.
            user.getPlayer().sendPlayerListHeaderAndFooter(HEADER, FOOTER);
        }
    }

    public void sendAddTeam(String name, String primaryGroup) {
        try {
            ChatHandler.handle(new AddTeamEvent(name, primaryGroup));
        } catch (IOException e) {
            // TODO: Exception handling.
        }
    }

    private TabListEntry createTabPlayer(User user, TabPlayer tabPlayer) {
        // Find player instance of TabPlayer.
        Optional<Player> optionalPlayer = server.getAllPlayers().stream().filter(p -> p.getUniqueId().toString().equals(tabPlayer.getUuid())).findFirst();
        if (optionalPlayer.isPresent()) {
            Player player = optionalPlayer.get();
            return TabListEntry.builder()
                    .tabList(user.getPlayer().getTabList())
                    .gameMode(1) // All players will be shown in creative
                    .displayName(formattedName(user, tabPlayer.getUuid(), tabPlayer.getName()))
                    .profile(GameProfile.forOfflinePlayer(player.getUsername()).withProperties(player.getGameProfileProperties()))
                    .latency((int) player.getPing())
                    .listed(true)
                    .build();
        } else {
            return null;
        }
    }


    private Component formattedName(User user, String uuid, String sName) {

        // Find the user.
        User userToAdd = Proxy.getInstance().getUserManager().getUserByUuid(uuid);

        Component name = Component.text(sName);

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
