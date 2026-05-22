package com.tonikelope.megabasterd.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

final class DefaultAccountService implements AccountService {

    private static final Logger LOG = Logger.getLogger(DefaultAccountService.class.getName());

    private final AccountStorage storage;
    private final CoreEventPublisher events;

    DefaultAccountService(AccountStorage storage, CoreEventPublisher events) {
        this.storage = storage != null ? storage : new InMemoryAccountStorage();
        this.events = events;
    }

    @Override
    public boolean isEncrypted() {
        return storage.isEncrypted();
    }

    @Override
    public boolean isLocked() {
        return storage.isLocked();
    }

    @Override
    public String megaPassword(String email) {
        AccountValidator.requireEmail(email);
        try {
            return storage.getMegaPassword(email);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public Map<String, MegaAccount> megaAccounts() {
        try {
            return storage.loadMegaAccounts();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public ElcAccount elcAccount(String host) {
        AccountValidator.requireHost(host);
        try {
            return storage.getElcAccount(host);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public Map<String, ElcAccount> elcAccounts() {
        try {
            return storage.loadElcAccounts();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void saveElcAccount(ElcAccount account) {
        try {
            storage.saveElcAccount(account);
            publish(AccountEvent.Action.SAVED, account.host());
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void deleteMegaAccount(String email) {
        AccountValidator.requireEmail(email);
        try {
            storage.deleteMegaAccount(email);
            publish(AccountEvent.Action.DELETED, email);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void deleteElcAccount(String host) {
        AccountValidator.requireHost(host);
        try {
            storage.deleteElcAccount(host);
            publish(AccountEvent.Action.DELETED, host);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public List<String> exportMegaLines() {
        Map<String, MegaAccount> accounts = megaAccounts();
        List<String> lines = new ArrayList<>(accounts.size());
        for (MegaAccount account : accounts.values()) {
            if (account.password() != null) {
                lines.add(account.email() + "#" + account.password());
            }
        }
        return lines;
    }

    @Override
    public List<String> exportElcLines() {
        Map<String, ElcAccount> accounts = elcAccounts();
        List<String> lines = new ArrayList<>(accounts.size());
        for (ElcAccount account : accounts.values()) {
            lines.add(account.host() + "#" + account.user() + "#" + account.apiKey());
        }
        return lines;
    }

    private void publish(AccountEvent.Action action, String accountId) {
        if (events != null) {
            events.publish(new AccountEvent(action, accountId));
        }
    }
}
