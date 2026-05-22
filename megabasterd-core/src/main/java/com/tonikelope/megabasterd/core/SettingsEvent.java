package com.tonikelope.megabasterd.core;

public final class SettingsEvent implements CoreEvent {

    public enum Action {
        LOADED,
        SAVED,
        CHANGED
    }

    private final long timestampMillis;
    private final Action action;
    private final String key;

    public SettingsEvent(Action action, String key) {
        this(System.currentTimeMillis(), action, key);
    }

    public SettingsEvent(long timestampMillis, Action action, String key) {
        this.timestampMillis = timestampMillis;
        this.action = action != null ? action : Action.CHANGED;
        this.key = key != null ? key : "";
    }

    @Override
    public String type() {
        return "settings";
    }

    @Override
    public long timestampMillis() {
        return timestampMillis;
    }

    public Action action() {
        return action;
    }

    public String key() {
        return key;
    }
}
