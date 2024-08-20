package net.bteuk.proxy.chat;

import net.bteuk.network.lib.dto.ChatMessage;
import net.bteuk.network.lib.dto.DirectMessage;
import net.bteuk.network.lib.enums.ChatChannels;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.proxy.Proxy;
import net.bteuk.proxy.User;
import net.bteuk.proxy.UserManager;
import net.bteuk.proxy.utils.Analytics;
import net.bteuk.proxy.utils.Time;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.io.IOException;
import java.util.List;

import static net.bteuk.network.lib.enums.ChatChannels.GLOBAL;
import static net.bteuk.proxy.utils.Constants.DISCORD_SENDER;
import static net.bteuk.proxy.utils.Constants.SERVER_SENDER;

/**
 * The chat manager keeps track of all the channels, players and statuses.
 */
public class ChatManager {

    private final UserManager userManager;

    private final List<String> SERVER_USERS = List.of(new String[]{SERVER_SENDER, DISCORD_SENDER});

    private final String FOCUS_MODE_PREFIX = "%s is unable to receive the message since they are in focus mode.";

    public ChatManager(UserManager userManager) {
        this.userManager = userManager;
    }


    /**
     * Handle a chat message.
     * A chat message must be split into direct messages for each player who must receive it.
     *
     * @param chatMessage the chat message
     */
    public void handle(ChatMessage chatMessage) throws IOException {
        // Send a direct message to all players
        for (User user : userManager.getUsers()) {
            handle(new DirectMessage(chatMessage.getChannel(), user.getUuid(), chatMessage.getSender(), chatMessage.getComponent(), false));
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
    public void handle(DirectMessage directMessage) throws IOException {
        // If the sender is muted for the recipient, don't send the message.
        if (!userManager.isMutedForUser(directMessage.getRecipient(), directMessage.getSender())) {
            User user = userManager.getUserByUuid(directMessage.getRecipient());
            if (userManager.getUserByUuid(directMessage.getRecipient()) != null) {
                if (!user.isFocusEnabled() || SERVER_USERS.contains(directMessage.getSender())) {
                    ChatHandler.handle(directMessage);
                }
            } else if (directMessage.isOffline()) {
                // Send offline message.
                Proxy.getInstance().getGlobalSQL().update("INSERT INTO messages(recipient,message) VALUES(" +
                        directMessage.getRecipient() + "," +
                        GsonComponentSerializer.gson().serialize(directMessage.getComponent()) + ");");
            }
        }
    }
}
