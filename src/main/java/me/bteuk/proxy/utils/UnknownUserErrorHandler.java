package me.bteuk.proxy.utils;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.bteuk.proxy.Proxy;
import net.dv8tion.jda.api.exceptions.ErrorHandler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static net.dv8tion.jda.api.requests.ErrorResponse.UNKNOWN_MEMBER;
import static net.dv8tion.jda.api.requests.ErrorResponse.UNKNOWN_USER;

/**
 * Error handler for when the user is unknown, this implies they have left the discord and their link must therefore be removed.
 */
public class UnknownUserErrorHandler extends ErrorHandler {
    public UnknownUserErrorHandler(long userID) {
        super();
        handle(Arrays.asList(UNKNOWN_USER, UNKNOWN_MEMBER),
                e -> {
                    //Remove the user from the discord link table.
                    if (Proxy.getInstance().getGlobalSQL().hasRow("SELECT discord_id FROM discord WHERE discord_id='" + userID + "';")) {
                        Proxy.getInstance().getGlobalSQL().update("DELETE FROM discord WHERE discord_id=" + userID + ";");
                        Proxy.getInstance().getLogger().info(("Removed discord link for " + userID + ", they are no longer in the discord server."));
                    } else {
                        //The link does not exist in the database, make sure it's removed for the Network also.
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        DataOutputStream out = new DataOutputStream(stream);
                        try {
                            out.writeUTF("unlink " + userID);
                        } catch (IOException ex) {
                            Proxy.getInstance().getLogger().info("Unable to send unlink message for " + userID);
                        }
                        for (RegisteredServer server : Proxy.getInstance().getServer().getAllServers()) {
                            server.sendPluginMessage(MinecraftChannelIdentifier.create("uknet", "discord_linking"), stream.toByteArray());
                        }
                    }
                });
    }
}
