package org.btuk.proxy.core.chat.automod;

import lombok.Getter;

import java.time.Duration;

public abstract class AutoModRule {

    @Getter
    private final String id;

    @Getter
    private final Duration duration;

    public AutoModRule(String id, Duration duration) {
        this.id = id;
        this.duration = duration;
    }

    public abstract boolean blockMessage();

    public int getPoints() {
        return 0;
    }

    @Override
    public String toString() {
        return String.format("AutoModRule: %s, id: %s", getClass().getSimpleName(), id);
    }
}
