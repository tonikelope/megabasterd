package com.tonikelope.megabasterd.core;

public final class ElcAccount {

    private final String host;
    private final String user;
    private final String apiKey;

    public ElcAccount(String host, String user, String apiKey) {
        AccountValidator.requireHost(host);
        if (user == null || user.trim().isEmpty()) {
            throw new IllegalArgumentException("ELC user cannot be blank");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("ELC API key cannot be blank");
        }
        this.host = host;
        this.user = user;
        this.apiKey = apiKey;
    }

    public String host() {
        return host;
    }

    public String user() {
        return user;
    }

    public String apiKey() {
        return apiKey;
    }
}
