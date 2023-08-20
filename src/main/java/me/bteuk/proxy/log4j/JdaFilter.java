package me.bteuk.proxy.log4j;

import me.bteuk.proxy.Proxy;
import net.dv8tion.jda.api.JDA;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;

public class JdaFilter implements Filter {

    public Result check(String loggerName, Level level, String message, Throwable throwable) {
        // only listen for JDA logs
        if (!loggerName.startsWith("github.scarsz.discordsrv.dependencies.jda")) return Result.NEUTRAL;

        switch (level.name()) {
            case "INFO" -> Proxy.getInstance().getLogger().info("[JDA] " + message);
            case "WARN" -> {
                if (message.contains("Encountered 429")) {
                    Proxy.getInstance().getLogger().debug(message);
                    break;
                }
                Proxy.getInstance().getLogger().warn("[JDA] " + message);
            }
            case "ERROR" -> {
                if (message.contains("Requester timed out while executing a request")) {
                    Proxy.getInstance().getLogger().error("[JDA] " + message + ". This is either a issue on Discord's end (https://discordstatus.com) or with your server's connection");
                    Proxy.getInstance().getLogger().debug(ExceptionUtils.getStackTrace(throwable));
                    break;
                }

                // JDA forcefully logs this :(
                JDA jda = Proxy.getInstance().getDiscord().getJda();
                if (message.contains("There was an I/O error while executing a REST request: null")
                        && jda != null && (jda.getStatus() == JDA.Status.SHUTDOWN || jda.getStatus() == JDA.Status.SHUTTING_DOWN)) {
                    // Ignore InterruptedIOException's during shutdown, we can't hold up the server from stopping forever,
                    // so some requests are cancelled during shutdown. Logging errors for those request failures isn't important.
                    return Result.DENY;
                }
                if (throwable != null) {
                    Proxy.getInstance().getLogger().error("[JDA] " + message + "\n" + ExceptionUtils.getStackTrace(throwable));
                } else {
                    Proxy.getInstance().getLogger().error("[JDA] " + message);
                }
            }
            default -> Proxy.getInstance().getLogger().debug("[JDA] " + message);
        }

        // all JDA messages should be denied because we handle them ourselves
        return Result.DENY;
    }

    @Override
    public Result filter(LogEvent logEvent) {
        return check(
                logEvent.getLoggerName(),
                logEvent.getLevel(),
                logEvent.getMessage()
                        .getFormattedMessage(),
                logEvent.getThrown());
    }
    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object... parameters) {
        return check(
                logger.getName(),
                level,
                message,
                null);
    }
    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object message, Throwable throwable) {
        return check(
                logger.getName(),
                level,
                message.toString(),
                throwable);
    }
    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message message, Throwable throwable) {
        return check(
                logger.getName(),
                level,
                message.getFormattedMessage(),
                throwable);
    }

    @Override
    public State getState() {
        return State.STARTED;
    }

    public void start() {}
    public void stop() {}
    public boolean isStarted() {
        return true;
    }
    public boolean isStopped() {
        return false;
    }

    @Override
    public Result getOnMismatch() {
        return Result.NEUTRAL;
    }
    @Override
    public Result getOnMatch() {
        return Result.NEUTRAL;
    }

}