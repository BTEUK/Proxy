package net.bteuk.proxy.socket;

import net.bteuk.network.lib.dto.AbstractTransferObject;
import net.bteuk.network.lib.dto.ChatMessage;
import net.bteuk.network.lib.dto.DirectMessage;
import net.bteuk.network.lib.dto.DiscordDirectMessage;
import net.bteuk.network.lib.dto.DiscordEmbed;
import net.bteuk.network.lib.dto.DiscordLinking;
import net.bteuk.network.lib.dto.DiscordRole;
import net.bteuk.network.lib.dto.FocusEvent;
import net.bteuk.network.lib.dto.ModerationEvent;
import net.bteuk.network.lib.dto.MuteEvent;
import net.bteuk.network.lib.dto.ServerShutdown;
import net.bteuk.network.lib.dto.ServerStartup;
import net.bteuk.network.lib.dto.SwitchServerEvent;
import net.bteuk.network.lib.dto.UserConnectRequest;
import net.bteuk.network.lib.dto.UserDisconnect;
import net.bteuk.network.lib.dto.UserUpdate;
import net.bteuk.network.lib.socket.SocketHandler;
import net.bteuk.proxy.Proxy;
import net.bteuk.proxy.chat.ChatManager;

public class ProxySocketHandler implements SocketHandler {

    private final ChatManager manager;

    public ProxySocketHandler(ChatManager manager) {
        this.manager = manager;
    }

    @Override
    public synchronized AbstractTransferObject handle(AbstractTransferObject abstractTransferObject) {
        // Handle the different objects.
        if (abstractTransferObject instanceof ChatMessage chatMessage) {
            manager.handle(chatMessage);
            Proxy.getInstance().getDiscord().handle(chatMessage);
        } else if (abstractTransferObject instanceof DirectMessage directMessage) {
            manager.handle(directMessage);
        } else if (abstractTransferObject instanceof DiscordDirectMessage discordDirectMessage) {
            Proxy.getInstance().getDiscord().handle(discordDirectMessage);
        } else if (abstractTransferObject instanceof DiscordEmbed discordEmbed) {
            Proxy.getInstance().getDiscord().handle(discordEmbed);
        } else if (abstractTransferObject instanceof DiscordLinking discordLinking) {
            Proxy.getInstance().getDiscord().handle(discordLinking);
        } else if (abstractTransferObject instanceof DiscordRole discordRole) {
            Proxy.getInstance().getDiscord().handle(discordRole);
        } else if (abstractTransferObject instanceof UserConnectRequest userConnect) {
            Proxy.getInstance().getUserManager().handleUserConnect(userConnect);
        } else if (abstractTransferObject instanceof UserDisconnect userDisconnect) {
            Proxy.getInstance().getUserManager().handleUserDisconnect(userDisconnect);
        } else if (abstractTransferObject instanceof UserUpdate userUpdate) {
            Proxy.getInstance().getUserManager().handleUserUpdate(userUpdate);
        } else if (abstractTransferObject instanceof SwitchServerEvent switchServerEvent) {
            Proxy.getInstance().getUserManager().handleSwitchServerEvent(switchServerEvent);
        } else if (abstractTransferObject instanceof MuteEvent muteEvent) {
            Proxy.getInstance().getUserManager().handleMuteEvent(muteEvent);
        } else if (abstractTransferObject instanceof ModerationEvent moderationEvent) {
            // Currently the moderation is handled on the servers, this is event is purely to update Tab for (un)muting.
            Proxy.getInstance().getTabManager().updatePlayerByUuid(moderationEvent.getUuid());
        } else if (abstractTransferObject instanceof FocusEvent focusEvent) {
            Proxy.getInstance().getUserManager().handleFocusEvent(focusEvent);
        } else if (abstractTransferObject instanceof ServerStartup serverStart) {
            Proxy.getInstance().getServerManager().addServer(serverStart);
        } else if (abstractTransferObject instanceof ServerShutdown serverClose) {
            Proxy.getInstance().getServerManager().removeServer(serverClose);
        } else {
            Proxy.getInstance().getLogger().warn(String.format("Socket object has an unrecognised type %s", abstractTransferObject.getClass().getTypeName()));
        }
        return null;
    }
}
