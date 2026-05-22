package com.tonikelope.megabasterd.core;

public final class CoreHealth {

    private final boolean running;
    private final long startedAtMillis;
    private final long checkedAtMillis;

    public CoreHealth(boolean running, long startedAtMillis, long checkedAtMillis) {
        this.running = running;
        this.startedAtMillis = startedAtMillis;
        this.checkedAtMillis = checkedAtMillis;
    }

    public boolean isRunning() {
        return running;
    }

    public long getStartedAtMillis() {
        return startedAtMillis;
    }

    public long getCheckedAtMillis() {
        return checkedAtMillis;
    }
}
