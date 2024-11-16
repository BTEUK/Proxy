package net.bteuk.proxy.chat;

import net.bteuk.network.lib.dto.ChatMessage;
import net.bteuk.network.lib.dto.DirectMessage;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.proxy.Proxy;
import net.bteuk.proxy.User;
import net.bteuk.proxy.UserManager;
import net.bteuk.proxy.utils.Analytics;
import net.bteuk.proxy.utils.Time;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.List;

import static net.bteuk.network.lib.enums.ChatChannels.GLOBAL;
import static net.bteuk.proxy.utils.Constants.DISCORD_SENDER;
import static net.bteuk.proxy.utils.Constants.SERVER_SENDER;

/**
 * The chat manager keeps track of all the channels, players and statuses.
 */
public class ChatManager {

    private final UserManager userManager;

    private static final List<String> SERVER_USERS = List.of(new String[]{SERVER_SENDER, DISCORD_SENDER});

    private static final String FOCUS_ENABLED_PRESET = "%s is in focus mode, unable to send message.";

    public ChatManager(UserManager userManager) {
        this.userManager = userManager;
    }


    /**
     * Handle a chat message.
     * A chat message must be split into direct messages for each player who must receive it.
     *
     * @param chatMessage the chat message
     */
    public void handle(ChatMessage chatMessage) {
        // Send a direct message to all players
        for (User user : userManager.getUsers()) {
            sendDirectMessage(new DirectMessage(chatMessage.getChannel(), user.getUuid(), chatMessage.getSender(), chatMessage.getComponent(), false));
        }
        if (!SERVER_USERS.contains(chatMessage.getSender())) {
            Analytics.addMessage(chatMessage.getSender(), Time.getDate(Time.currentTime()));
        }
    }

    /**
     * Handle a direct message.
     * A direct message will be sent to a specific player if they don't have the sender muted.
     *
     * @param directMessage the direct message
     */
    public void handle(DirectMessage directMessage) {
        // If the message is sent a by a player, and the recipient is in focus mode, block the message and let the sender know.
        if (!SERVER_USERS.contains(directMessage.getSender())) {
            User user = userManager.getUserByUuid(directMessage.getRecipient());
            if (user != null && user.isFocusEnabled()) {
                sendDirectMessage(new DirectMessage(GLOBAL.getChannelName(), directMessage.getSender(), SERVER_SENDER, ChatUtils.error(FOCUS_ENABLED_PRESET, user.getName()), false));
                return;
            }
            // Send message to both the send and recipient.
            sendDirectMessage(new DirectMessage(GLOBAL.getChannelName(), directMessage.getRecipient(), directMessage.getSender(), directMessage.getComponent(), false));
            sendDirectMessage(new DirectMessage(GLOBAL.getChannelName(), directMessage.getSender(), directMessage.getSender(), directMessage.getComponent(), false));
            return;
        }
        sendDirectMessage(directMessage);
    }

    /**
     * Handle a direct message.
     * A direct message will be sent to a specific player if they don't have the sender muted.
     *
     * @param directMessage the direct message
     */
    public void sendDirectMessage(DirectMessage directMessage) {
        // If the sender is muted for the recipient, don't send the message.
        if (!userManager.isMutedForUser(directMessage.getRecipient(), directMessage.getSender())) {
            User user = userManager.getUserByUuid(directMessage.getRecipient());
            if (user != null && user.isOnline()) {
                // Block the message is the player is in focus mode and the server is not the server. (Discord should also be blocked)
                if (!user.isFocusEnabled() || directMessage.getSender().equals(SERVER_SENDER)) {
                    Proxy.getInstance().getChatHandler().handle(directMessage);
                }
            } else if (directMessage.isOffline()) {
                // Send offline message.
                Proxy.getInstance().getGlobalSQL().insertMessage(directMessage.getRecipient(), GsonComponentSerializer.gson().serialize(directMessage.getComponent()));
            }
        }
    }
}
