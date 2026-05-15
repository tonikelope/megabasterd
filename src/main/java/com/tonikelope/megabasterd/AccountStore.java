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
import static com.tonikelope.megabasterd.MiscTools.UrlBASE642Bin;
import static com.tonikelope.megabasterd.MiscTools.i32a2bin;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
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

    // ===================== MEGA login persistence =====================

    /**
     * Called after a successful {@link MegaAPI#login}. Handles every
     * follow-up step that used to be inlined in SettingsDialog's save loop:
     *
     * <ul>
     *   <li>serialise the {@code MegaAPI} to a session blob;</li>
     *   <li>encrypt the blob with the configured master pass (if any) and
     *       insert the {@code mega_sessions} row;</li>
     *   <li>build the {@code password / password_aes / user_hash} values in
     *       the historical on-disk storage format and insert the
     *       {@code mega_accounts} row;</li>
     *   <li>update the in-memory caches (
     *       {@link MainPanel#getMega_active_accounts()} and
     *       {@link MainPanel#getMega_accounts()}) so subsequent reads see
     *       the new state.</li>
     * </ul>
     *
     * <b>Historical storage quirk preserved:</b> when no master pass is
     * configured, {@code password_aes} is stored as the regular-base64
     * encoding of the raw int[]-to-byte[] conversion, and {@code user_hash}
     * is stored as the URL-safe base64 string MegaAPI returned. When a
     * master pass is configured, both fields are stored as the regular-
     * base64 encoding of their AES-CBC-at-rest ciphertext. The
     * migration logic in {@link #migrateMasterPass} relies on this
     * asymmetry.
     *
     * @param email account email (assumed already validated by caller)
     * @param plaintextPassword raw user-entered password; the store
     *     encrypts at-rest as needed and never expects an already-encrypted
     *     value here
     * @param loggedInMa a {@code MegaAPI} instance that has just returned
     *     from a successful {@code login(...)}; its {@code getPassword_aes()}
     *     and {@code getUser_hash()} accessors must be ready to read
     */
    public void persistMegaLogin(String email, String plaintextPassword, MegaAPI loggedInMa) throws Exception {

        // 1) Serialise + persist the session blob.
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        try (ObjectOutputStream os = new ObjectOutputStream(bs)) {
            os.writeObject(loggedInMa);
        }
        byte[] sessionBytes = bs.toByteArray();

        if (isEncrypted()) {
            DBTools.insertMegaSession(email,
                    CryptTools.aes_cbc_encrypt_at_rest(sessionBytes, _main_panel.getMaster_pass()),
                    true);
        } else {
            DBTools.insertMegaSession(email, sessionBytes, false);
        }

        _main_panel.getMega_active_accounts().put(email, loggedInMa);

        // 2) Persist the account row in the historical storage format.
        String storedPassword;
        String storedPasswordAes;
        String storedUserHash;

        if (isEncrypted()) {
            byte[] mp = _main_panel.getMaster_pass();
            storedPassword = Bin2BASE64(CryptTools.aes_cbc_encrypt_at_rest(
                    plaintextPassword.getBytes(StandardCharsets.UTF_8), mp));
            storedPasswordAes = Bin2BASE64(CryptTools.aes_cbc_encrypt_at_rest(
                    i32a2bin(loggedInMa.getPassword_aes()), mp));
            storedUserHash = Bin2BASE64(CryptTools.aes_cbc_encrypt_at_rest(
                    UrlBASE642Bin(loggedInMa.getUser_hash()), mp));
        } else {
            storedPassword = plaintextPassword;
            storedPasswordAes = Bin2BASE64(i32a2bin(loggedInMa.getPassword_aes()));
            storedUserHash = loggedInMa.getUser_hash();
        }

        DBTools.insertMegaAccount(email, storedPassword, storedPasswordAes, storedUserHash);

        // 3) Update the in-memory cache to match what we just wrote.
        HashMap<String, Object> data = new HashMap<>();
        data.put("password", storedPassword);
        data.put("password_aes", storedPasswordAes);
        data.put("user_hash", storedUserHash);
        _main_panel.getMega_accounts().put(email, data);
    }

    // ===================== Master-pass migration =====================

    /**
     * Re-encrypts every persisted MEGA + ELC account when the master pass
     * changes (enabling, disabling, or rotating).
     *
     * <p>Caller protocol:
     * <ol>
     *   <li>Update {@link MainPanel#setMaster_pass_hash} and
     *       {@link MainPanel#setMaster_pass} to the NEW values (or to
     *       {@code null/null} when disabling encryption).</li>
     *   <li>Truncate {@code mega_sessions} (sessions are not re-encrypted;
     *       they get refetched on next use).</li>
     *   <li>Call this method, passing the OLD master pass bytes (may be
     *       {@code null} if encryption was previously off) and the OLD
     *       hash (used only as a "was encryption on?" flag).</li>
     * </ol>
     *
     * <p>For each account, the stored fields are decrypted with the old
     * master pass (or taken verbatim when there was no old master pass)
     * and then re-encrypted with the new master pass (or stored verbatim
     * when disabling encryption). The in-memory caches are updated so
     * subsequent reads see the migrated values.
     *
     * <p>The {@code password_aes} / {@code user_hash} storage-format
     * quirks documented on {@link #persistMegaLogin} are preserved.
     */
    public void migrateMasterPass(byte[] oldMasterPass, String oldMasterPassHash) throws Exception {

        boolean wasEncrypted = oldMasterPassHash != null;
        boolean isNowEncrypted = isEncrypted();
        byte[] newMasterPass = isNowEncrypted ? _main_panel.getMaster_pass() : null;

        // MEGA accounts.
        for (Map.Entry<String, Object> pair : _main_panel.getMega_accounts().entrySet()) {
            @SuppressWarnings("unchecked")
            HashMap<String, Object> data = (HashMap<String, Object>) pair.getValue();
            String email = pair.getKey();

            String password;
            String password_aes;
            String user_hash;

            if (wasEncrypted) {
                password = new String(CryptTools.aes_cbc_decrypt_at_rest(
                        BASE642Bin((String) data.get("password")), oldMasterPass),
                        StandardCharsets.UTF_8);
                password_aes = Bin2BASE64(CryptTools.aes_cbc_decrypt_at_rest(
                        BASE642Bin((String) data.get("password_aes")), oldMasterPass));
                user_hash = Bin2BASE64(CryptTools.aes_cbc_decrypt_at_rest(
                        BASE642Bin((String) data.get("user_hash")), oldMasterPass));
            } else {
                password = (String) data.get("password");
                password_aes = (String) data.get("password_aes");
                user_hash = (String) data.get("user_hash");
            }

            if (isNowEncrypted) {
                password = Bin2BASE64(CryptTools.aes_cbc_encrypt_at_rest(
                        password.getBytes(StandardCharsets.UTF_8), newMasterPass));
                password_aes = Bin2BASE64(CryptTools.aes_cbc_encrypt_at_rest(
                        BASE642Bin(password_aes), newMasterPass));
                // user_hash arrives here in one of two forms:
                //   - URL-base64 (when never encrypted; that's what MegaAPI gives us)
                //   - regular-base64 (when just decrypted above)
                // The two replace() calls map URL-base64 to regular-base64;
                // they are no-ops on regular-base64 (which doesn't contain
                // - or _).
                user_hash = Bin2BASE64(CryptTools.aes_cbc_encrypt_at_rest(
                        BASE642Bin(user_hash.replace('-', '+').replace('_', '/')),
                        newMasterPass));
            }

            data.put("password", password);
            data.put("password_aes", password_aes);
            data.put("user_hash", user_hash);
            DBTools.insertMegaAccount(email, password, password_aes, user_hash);
        }

        // ELC accounts.
        for (Map.Entry<String, Object> pair : _main_panel.getElc_accounts().entrySet()) {
            @SuppressWarnings("unchecked")
            HashMap<String, Object> data = (HashMap<String, Object>) pair.getValue();
            String host = pair.getKey();

            String user;
            String apikey;

            if (wasEncrypted) {
                user = new String(CryptTools.aes_cbc_decrypt_at_rest(
                        BASE642Bin((String) data.get("user")), oldMasterPass),
                        StandardCharsets.UTF_8);
                apikey = new String(CryptTools.aes_cbc_decrypt_at_rest(
                        BASE642Bin((String) data.get("apikey")), oldMasterPass),
                        StandardCharsets.UTF_8);
            } else {
                user = (String) data.get("user");
                apikey = (String) data.get("apikey");
            }

            if (isNowEncrypted) {
                user = Bin2BASE64(CryptTools.aes_cbc_encrypt_at_rest(
                        user.getBytes(StandardCharsets.UTF_8), newMasterPass));
                apikey = Bin2BASE64(CryptTools.aes_cbc_encrypt_at_rest(
                        apikey.getBytes(StandardCharsets.UTF_8), newMasterPass));
            }

            data.put("user", user);
            data.put("apikey", apikey);
            DBTools.insertELCAccount(host, user, apikey);
        }
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
