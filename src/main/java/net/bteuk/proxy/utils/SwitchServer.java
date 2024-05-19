package net.bteuk.proxy.utils;

import com.velocitypowered.api.scheduler.ScheduledTask;
import lombok.Getter;
import net.bteuk.proxy.Proxy;
import net.bteuk.proxy.User;

import java.util.concurrent.TimeUnit;

/**
 * Class to handle the switching of servers.
 * An instance is created when a user switches server.
 */
public class SwitchServer {

    private final User user;

    private final String fromServer;

    @Getter
    private final String toServer;

    private final long switchTime;

    private final ScheduledTask switchTask;

    public SwitchServer(User user, String fromServer, String toServer) {
        this.user = user;
        this.fromServer = fromServer;
        this.toServer = toServer;
        this.switchTime = Time.currentTime();

        switchTask = Proxy.getInstance().getServer().getScheduler().buildTask(Proxy.getInstance(), this::onTimeout)
                .delay(10L, TimeUnit.SECONDS)
                .schedule();
    }

    public void cancelTimeout() {
        if (switchTask != null) {
            switchTask.cancel();
        }
    }

    private void onTimeout() {
        // TODO: Run leave event.
    }
}
