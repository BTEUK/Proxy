package org.btuk.proxy.core.chat.automod;

import lombok.Getter;
import lombok.extern.java.Log;

import java.time.Duration;
import java.util.List;

@Log
public class AutoModFlagRule extends AutoModWordRule {

    @Getter
    private final int points;

    private final boolean deleteMessage;

    public AutoModFlagRule(String id, List<String> flaggedWords, int points, Duration duration, boolean deleteMessage) {
        super(id, flaggedWords, duration);
        this.points = points;
        this.deleteMessage = deleteMessage;
        log.info(String.format("Loaded flag rule, id: %s, flagged words: %d, delete message: %s", id, flaggedWords.size(), deleteMessage));
    }

    @Override
    public boolean blockMessage() {
        return deleteMessage;
    }
}
