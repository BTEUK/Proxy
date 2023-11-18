package me.bteuk.proxy.utils;

import me.bteuk.proxy.Proxy;
import net.dv8tion.jda.api.exceptions.ErrorHandler;

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
                    }
                });
    }
}
