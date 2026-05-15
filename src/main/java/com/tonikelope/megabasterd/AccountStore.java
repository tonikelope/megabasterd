/*
 __  __                  _               _               _
|  \/  | ___  __ _  __ _| |__   __ _ ___| |_ ___ _ __ __| |
| |\/| |/ _ \/ _` |/ _` | '_ \ / _` / __| __/ _ \ '__/ _` |
| |  | |  __/ (_| | (_| | |_) | (_| \__ \ ||  __/ | | (_| |
|_|  |_|\___|\__, |\__,_|_.__/ \__,_|___/\__\___|_|  \__,_|
             |___/
© Perpetrated by tonikelope since 2016
 */
package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.MiscTools.BASE642Bin;
import static com.tonikelope.megabasterd.MiscTools.Bin2BASE64;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for MEGA / ELC account encryption + persistence.
 *
 * Centralises the master-pass / AES-CBC-at-rest dance that was previously
 * duplicated in 6+ places across SettingsDialog (load on dialog open,
 * decrypt on unlock, encrypt on save, encrypt on add-from-import, encrypt
 * on update, ...). Every duplication was a chance for the
 * "if master_pass_hash != null { encrypt } else { plaintext }" pattern
 * to drift; bugs #645, #699, #719 all touched this surface.
 *
 * Callers never deal with the encrypt/decrypt step directly: they pass
 * plaintext to persist*() and receive plaintext from list*() / get*().
 * The store handles the rest based on MainPanel.getMaster_pass_hash() /
 * MainPanel.getMaster_pass().
 *
 * Thread-safety: read methods are safe to call concurrently with each
 * other; write methods serialise through the underlying DBTools
 * synchronized statics.
 */
public final class AccountStore {

    private final MainPanel _main_panel;

    public AccountStore(MainPanel main_panel) {
        this._main_panel = main_panel;
    }

    /** True if a master pass has been configured (DB has master_pass_hash). */
    public boolean isEncrypted() {
        return _main_panel.getMaster_pass_hash() != null;
    }

    /**
     * True if accounts are encrypted AND the master pass is not currently in
     * memory. Callers should refuse plaintext reads in this state.
     */
    public boolean isLocked() {
        return isEncrypted() && _main_panel.getMaster_pass() == null;
    }

    // ===================== MEGA accounts =====================

    /**
     * Returns plaintext password for one MEGA account, or null if missing.
     * @throws IllegalStateException if the store is locked
     */
    public String getMegaPassword(String email) throws Exception {
        if (isLocked()) {
            throw new IllegalStateException("Account store is locked");
        }
        @SuppressWarnings("unchecked")
        HashMap<String, Object> data = (HashMap<String, Object>) _main_panel.getMega_accounts().get(email);
        if (data == null) {
            return null;
        }
        String stored = (String) data.get("password");
        return stored == null ? null : decryptIfNeeded(stored);
    }

    /**
     * Returns an email -> plaintext-password map for every persisted MEGA
     * account. Insertion order matches MainPanel.getMega_accounts().
     * @throws IllegalStateException if the store is locked
     */
    public LinkedHashMap<String, String> listMegaPlaintext() throws Exception {
        if (isLocked()) {
            throw new IllegalStateException("Account store is locked");
        }
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : _main_panel.getMega_accounts().entrySet()) {
            @SuppressWarnings("unchecked")
            HashMap<String, Object> data = (HashMap<String, Object>) e.getValue();
            String stored = (String) data.get("password");
            result.put(e.getKey(), stored == null ? null : decryptIfNeeded(stored));
        }
        return result;
    }

    /**
     * Insert-or-update a MEGA account. The plaintext password is encrypted
     * at-rest before persisting if a master pass is configured. Updates the
     * in-memory cache so subsequent reads see the new value.
     *
     * @param email caller-validated MEGA email
     * @param plaintextPassword the plaintext password (NEVER encrypted form)
     * @param password_aes MegaAPI password-aes blob from a successful login
     * @param user_hash MegaAPI user-hash from a successful login
     */
    public void persistMegaAccount(String email, String plaintextPassword, String password_aes, String user_hash) throws Exception {
        String storedPassword = encryptIfNeeded(plaintextPassword);
        DBTools.insertMegaAccount(email, storedPassword, password_aes, user_hash);

        HashMap<String, Object> data = new HashMap<>();
        data.put("password", storedPassword);
        data.put("password_aes", password_aes);
        data.put("user_hash", user_hash);
        _main_panel.getMega_accounts().put(email, data);
    }

    /**
     * Remove a MEGA account from the DB and the in-memory caches (both the
     * persisted-accounts map and the active-sessions map).
     */
    public void deleteMegaAccount(String email) throws SQLException {
        DBTools.deleteMegaAccount(email);
        _main_panel.getMega_accounts().remove(email);
        _main_panel.getMega_active_accounts().remove(email);
    }

    // ===================== ELC accounts =====================

    /**
     * Returns plaintext {user, apikey} for one ELC host, or null if missing.
     * @throws IllegalStateException if the store is locked
     */
    public String[] getElcCredentials(String host) throws Exception {
        if (isLocked()) {
            throw new IllegalStateException("Account store is locked");
        }
        @SuppressWarnings("unchecked")
        HashMap<String, Object> data = (HashMap<String, Object>) _main_panel.getElc_accounts().get(host);
        if (data == null) {
            return null;
        }
        String user = (String) data.get("user");
        String apikey = (String) data.get("apikey");
        if (user == null || apikey == null) {
            return null;
        }
        return new String[]{decryptIfNeeded(user), decryptIfNeeded(apikey)};
    }

    /**
     * Returns host -> {plaintext-user, plaintext-apikey} for every persisted
     * ELC account.
     * @throws IllegalStateException if the store is locked
     */
    public LinkedHashMap<String, String[]> listElcPlaintext() throws Exception {
        if (isLocked()) {
            throw new IllegalStateException("Account store is locked");
        }
        LinkedHashMap<String, String[]> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : _main_panel.getElc_accounts().entrySet()) {
            @SuppressWarnings("unchecked")
            HashMap<String, Object> data = (HashMap<String, Object>) e.getValue();
            String user = (String) data.get("user");
            String apikey = (String) data.get("apikey");
            if (user == null || apikey == null) {
                continue;
            }
            result.put(e.getKey(), new String[]{decryptIfNeeded(user), decryptIfNeeded(apikey)});
        }
        return result;
    }

    /**
     * Insert-or-update an ELC account. Plaintext user / apikey are encrypted
     * at-rest before persisting if a master pass is configured.
     */
    public void persistElcAccount(String host, String plaintextUser, String plaintextApikey) throws Exception {
        String storedUser = encryptIfNeeded(plaintextUser);
        String storedApikey = encryptIfNeeded(plaintextApikey);
        DBTools.insertELCAccount(host, storedUser, storedApikey);

        HashMap<String, Object> data = new HashMap<>();
        data.put("user", storedUser);
        data.put("apikey", storedApikey);
        _main_panel.getElc_accounts().put(host, data);
    }

    public void deleteElcAccount(String host) throws SQLException {
        DBTools.deleteELCAccount(host);
        _main_panel.getElc_accounts().remove(host);
    }

    // ===================== Export =====================

    /**
     * Returns a list of EMAIL#PASSWORD lines matching the import format,
     * one per persisted MEGA account. Plaintext.
     * @throws IllegalStateException if the store is locked
     */
    public List<String> exportMegaLines() throws Exception {
        LinkedHashMap<String, String> accounts = listMegaPlaintext();
        List<String> lines = new ArrayList<>(accounts.size());
        for (Map.Entry<String, String> e : accounts.entrySet()) {
            // Skip null-password rows defensively; they shouldn't exist but
            // a corrupted DB row would otherwise produce a "user@x.com#null".
            if (e.getValue() == null) {
                continue;
            }
            lines.add(e.getKey() + "#" + e.getValue());
        }
        return lines;
    }

    /**
     * Returns a list of HOST#USER#APIKEY lines, one per persisted ELC
     * account. Plaintext.
     * @throws IllegalStateException if the store is locked
     */
    public List<String> exportElcLines() throws Exception {
        LinkedHashMap<String, String[]> accounts = listElcPlaintext();
        List<String> lines = new ArrayList<>(accounts.size());
        for (Map.Entry<String, String[]> e : accounts.entrySet()) {
            String[] creds = e.getValue();
            lines.add(e.getKey() + "#" + creds[0] + "#" + creds[1]);
        }
        return lines;
    }

    // ===================== Internal: encryption =====================

    /**
     * Encrypts to base64-of-AES-at-rest if a master pass is configured,
     * otherwise passes the plaintext through unchanged. The master-pass-hash
     * check is read once per call; callers that mix encrypted-and-unencrypted
     * accounts in the same operation are unsupported.
     */
    private String encryptIfNeeded(String plaintext) throws Exception {
        if (plaintext == null || !isEncrypted()) {
            return plaintext;
        }
        return Bin2BASE64(CryptTools.aes_cbc_encrypt_at_rest(
                plaintext.getBytes(StandardCharsets.UTF_8),
                _main_panel.getMaster_pass()));
    }

    /**
     * Inverse of encryptIfNeeded. Treats a stored value as ciphertext only
     * when a master pass is configured; otherwise returns it untouched.
     */
    private String decryptIfNeeded(String stored) throws Exception {
        if (stored == null || !isEncrypted()) {
            return stored;
        }
        return new String(CryptTools.aes_cbc_decrypt_at_rest(
                BASE642Bin(stored),
                _main_panel.getMaster_pass()),
                StandardCharsets.UTF_8);
    }
}
