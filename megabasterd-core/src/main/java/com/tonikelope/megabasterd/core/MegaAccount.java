package com.tonikelope.megabasterd.core;

public final class MegaAccount {

    private final String email;
    private final String password;

    public MegaAccount(String email, String password) {
        AccountValidator.requireEmail(email);
        this.email = email;
        this.password = password;
    }

    public String email() {
        return email;
    }

    public String password() {
        return password;
    }
}
