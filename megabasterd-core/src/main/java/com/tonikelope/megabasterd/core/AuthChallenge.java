package com.tonikelope.megabasterd.core;

public final class AuthChallenge {

    public enum Type {
        PASSWORD,
        TWO_FACTOR_CODE,
        MASTER_PASSWORD
    }

    private final Type type;
    private final String accountId;

    public AuthChallenge(Type type, String accountId) {
        if (type == null) {
            throw new IllegalArgumentException("Auth challenge type cannot be null");
        }
        this.type = type;
        this.accountId = accountId;
    }

    public Type type() {
        return type;
    }

    public String accountId() {
        return accountId;
    }
}
