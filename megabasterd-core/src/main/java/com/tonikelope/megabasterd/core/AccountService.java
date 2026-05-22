package com.tonikelope.megabasterd.core;

import java.util.List;
import java.util.Map;

public interface AccountService {

    boolean isEncrypted();

    boolean isLocked();

    String megaPassword(String email);

    Map<String, MegaAccount> megaAccounts();

    ElcAccount elcAccount(String host);

    Map<String, ElcAccount> elcAccounts();

    void saveElcAccount(ElcAccount account);

    void deleteMegaAccount(String email);

    void deleteElcAccount(String host);

    List<String> exportMegaLines();

    List<String> exportElcLines();
}
