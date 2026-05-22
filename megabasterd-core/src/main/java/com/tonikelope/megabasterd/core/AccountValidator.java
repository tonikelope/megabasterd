package com.tonikelope.megabasterd.core;

public final class AccountValidator {

    private AccountValidator() {
    }

    public static String requireEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email cannot be blank");
        }
        return email;
    }

    public static String requireHost(String host) {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("ELC host cannot be blank");
        }
        return host;
    }
}
