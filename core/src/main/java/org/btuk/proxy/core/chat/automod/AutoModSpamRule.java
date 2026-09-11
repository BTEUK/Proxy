package org.btuk.proxy.core.chat.automod;

import lombok.Getter;
import lombok.extern.java.Log;

import java.time.Duration;

import org.btuk.proxy.core.user.User;

@Log
public class AutoModSpamRule extends AutoModRule {

    private final int maxMessages;

    private final long window;

    private final boolean deleteMessages;

    @Getter
    private final int points;

    public AutoModSpamRule(String id, Duration duration, int maxMessages, Duration window, int points, boolean deleteMessages) {
        super(id, duration);
        this.maxMessages = maxMessages;
        this.window = window.toMillis();
        this.points = points;
        this.deleteMessages = deleteMessages;
        log.info(String.format("Loaded spam rule, id: %s, max messages: %d in %d ms", id, maxMessages, this.window));

    }

    @Override
    public boolean blockMessage() {
        return deleteMessages;
    }

    public boolean checkSpam(User user, String message, long timestamp) {
        return user.addMessage(message, timestamp, window, maxMessages);
    }
}
