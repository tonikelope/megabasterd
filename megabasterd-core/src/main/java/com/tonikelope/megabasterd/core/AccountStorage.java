package com.tonikelope.megabasterd.core;

import java.util.Map;

public interface AccountStorage {

    boolean isEncrypted();

    boolean isLocked();

    String getMegaPassword(String email) throws Exception;

    Map<String, MegaAccount> loadMegaAccounts() throws Exception;

    ElcAccount getElcAccount(String host) throws Exception;

    Map<String, ElcAccount> loadElcAccounts() throws Exception;

    void saveElcAccount(ElcAccount account) throws Exception;

    void deleteMegaAccount(String email) throws Exception;

    void deleteElcAccount(String host) throws Exception;
}
