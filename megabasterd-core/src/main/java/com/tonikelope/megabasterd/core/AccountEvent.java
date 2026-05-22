package com.tonikelope.megabasterd.core;

public final class AccountEvent implements CoreEvent {

    public enum Action {
        SAVED,
        DELETED,
        AUTH_CHALLENGE
    }

    private final Action action;
    private final String accountId;
    private final long timestampMillis;

    public AccountEvent(Action action, String accountId) {
        this.action = action;
        this.accountId = accountId;
        this.timestampMillis = System.currentTimeMillis();
    }

    public Action action() {
        return action;
    }

    public String accountId() {
        return accountId;
    }

    @Override
    public String type() {
        return "account";
    }

    @Override
    public long timestampMillis() {
        return timestampMillis;
    }
}
