package com.tonikelope.megabasterd.core;

import java.util.LinkedHashMap;
import java.util.Map;

final class InMemoryAccountStorage implements AccountStorage {

    private final Map<String, MegaAccount> megaAccounts = new LinkedHashMap<>();
    private final Map<String, ElcAccount> elcAccounts = new LinkedHashMap<>();

    @Override
    public boolean isEncrypted() {
        return false;
    }

    @Override
    public boolean isLocked() {
        return false;
    }

    @Override
    public String getMegaPassword(String email) {
        MegaAccount account = megaAccounts.get(email);
        return account != null ? account.password() : null;
    }

    @Override
    public Map<String, MegaAccount> loadMegaAccounts() {
        return new LinkedHashMap<>(megaAccounts);
    }

    @Override
    public ElcAccount getElcAccount(String host) {
        return elcAccounts.get(host);
    }

    @Override
    public Map<String, ElcAccount> loadElcAccounts() {
        return new LinkedHashMap<>(elcAccounts);
    }

    @Override
    public void saveElcAccount(ElcAccount account) {
        elcAccounts.put(account.host(), account);
    }

    @Override
    public void deleteMegaAccount(String email) {
        megaAccounts.remove(email);
    }

    @Override
    public void deleteElcAccount(String host) {
        elcAccounts.remove(host);
    }
}
