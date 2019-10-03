package com.tonikelope.megabasterd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public final class DBTools {

    private static final Logger LOG = Logger.getLogger(DBTools.class.getName());

    public static synchronized void setupSqliteTables() throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); Statement stat = conn.createStatement()) {

            stat.executeUpdate("CREATE TABLE IF NOT EXISTS downloads(url TEXT, email TEXT, path TEXT, filename TEXT, filekey TEXT, filesize UNSIGNED BIG INT, filepass VARCHAR(64), filenoexpire VARCHAR(64), custom_chunks_dir TEXT, PRIMARY KEY ('url'), UNIQUE(path, filename));");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS uploads(filename TEXT, email TEXT, url TEXT, ul_key TEXT, parent_node TEXT, root_node TEXT, share_key TEXT, folder_link TEXT, bytes_uploaded UNSIGNED BIG INT, meta_mac TEXT, PRIMARY KEY ('filename'), UNIQUE(filename, email));");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS settings(key VARCHAR(255), value TEXT, PRIMARY KEY('key'));");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS mega_accounts(email TEXT, password TEXT, password_aes TEXT, user_hash TEXT, PRIMARY KEY('email'));");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS elc_accounts(host TEXT, user TEXT, apikey TEXT, PRIMARY KEY('host'));");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS mega_sessions(email TEXT, ma BLOB, crypt INT, PRIMARY KEY('email'));");
        }
    }

    public static synchronized void vaccum() throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); Statement stat = conn.createStatement()) {

            stat.execute("VACUUM");
        }
    }

    public static synchronized void insertMegaSession(String email, byte[] ma, boolean crypt) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO mega_sessions (email, ma, crypt) VALUES (?,?,?)")) {

            ps.setString(1, email);
            ps.setBytes(2, ma);
            ps.setInt(3, crypt ? 1 : 0);

            ps.executeUpdate();
        }
    }

    public static synchronized void truncateMegaSessions() throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); Statement stat = conn.createStatement()) {

            stat.execute("DELETE FROM mega_sessions");
        }
    }

    public static synchronized HashMap<String, Object> selectMegaSession(String email) {

        HashMap<String, Object> session = null;

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("SELECT * from mega_sessions WHERE email=?")) {

            ps.setString(1, email);

            ResultSet res = ps.executeQuery();

            if (res.next()) {

                session = new HashMap<>();

                session.put("email", email);
                session.put("ma", res.getBytes(2));
                session.put("crypt", (res.getInt(3) == 1));

                return session;
            }
        } catch (SQLException ex) {
            Logger.getLogger(DBTools.class.getName()).log(Level.SEVERE, ex.getMessage());
        }

        return session;
    }

    public static synchronized void insertDownload(String url, String email, String path, String filename, String filekey, Long size, String filepass, String filenoexpire, String custom_chunks_dir) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("INSERT INTO downloads (url, email, path, filename, filekey, filesize, filepass, filenoexpire, custom_chunks_dir) VALUES (?,?,?,?,?,?,?,?,?)")) {

            ps.setString(1, url);
            ps.setString(2, email);
            ps.setString(3, path);
            ps.setString(4, filename);
            ps.setString(5, filekey);
            ps.setLong(6, size);
            ps.setString(7, filepass);
            ps.setString(8, filenoexpire);
            ps.setString(9, custom_chunks_dir);

            ps.executeUpdate();
        }
    }

    public static synchronized void deleteDownload(String url) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("DELETE FROM downloads WHERE url=?")) {

            ps.setString(1, url);

            ps.executeUpdate();

        }
    }

    public static synchronized void deleteDownloads(String[] urls) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("DELETE FROM downloads WHERE url=?")) {

            for (String url : urls) {

                ps.setString(1, url);

                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    public static synchronized void insertUpload(String filename, String email, String parent_node, String ul_key, String root_node, String share_key, String folder_link) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("INSERT INTO uploads (filename, email, parent_node, ul_key, root_node, share_key, folder_link, bytes_uploaded, meta_mac) VALUES (?,?,?,?,?,?,?,?,?)")) {

            ps.setString(1, filename);
            ps.setString(2, email);
            ps.setString(3, parent_node);
            ps.setString(4, ul_key);
            ps.setString(5, root_node);
            ps.setString(6, share_key);
            ps.setString(7, folder_link);
            ps.setLong(8, 0L);
            ps.setString(9, null);

            ps.executeUpdate();
        }
    }

    public static synchronized void updateUploadUrl(String filename, String email, String ul_url) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("UPDATE uploads SET url=? WHERE filename=? AND email=?")) {

            ps.setString(1, ul_url);
            ps.setString(2, filename);
            ps.setString(3, email);

            ps.executeUpdate();
        }
    }

    public static synchronized void updateUploadProgress(String filename, String email, Long bytes_uploaded, String meta_mac) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("UPDATE uploads SET bytes_uploaded=?,meta_mac=? WHERE filename=? AND email=?")) {

            ps.setLong(1, bytes_uploaded);
            ps.setString(2, meta_mac);
            ps.setString(3, filename);
            ps.setString(4, email);

            ps.executeUpdate();
        }
    }

    public static synchronized HashMap<String, Object> selectUploadProgress(String filename, String email) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("SELECT bytes_uploaded,meta_mac FROM uploads WHERE filename=? AND email=?")) {

            ps.setString(1, filename);
            ps.setString(2, email);

            ResultSet res = ps.executeQuery();

            if (res.next()) {
                HashMap<String, Object> upload = new HashMap<>();
                upload.put("bytes_uploaded", res.getLong("bytes_uploaded"));
                upload.put("meta_mac", res.getString("meta_mac"));
                return upload;
            }
        }

        return null;
    }

    public static synchronized void deleteUpload(String filename, String email) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("DELETE FROM uploads WHERE filename=? AND email=?")) {

            ps.setString(1, filename);

            ps.setString(2, email);

            ps.executeUpdate();
        }
    }

    public static synchronized void deleteUploads(String[][] uploads) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("DELETE FROM uploads WHERE filename=? AND email=?")) {

            for (String[] upload : uploads) {

                ps.setString(1, upload[0]);
                ps.setString(2, upload[1]);
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    public static synchronized String selectSettingValue(String key) {

        String value = null;

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("SELECT value from settings WHERE key=?")) {

            ps.setString(1, key);

            ResultSet res = ps.executeQuery();

            if (res.next()) {
                value = res.getString(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(DBTools.class.getName()).log(Level.SEVERE, ex.getMessage());
        }

        return value;
    }

    public static synchronized void insertSettingValue(String key, String value) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO settings (key,value) VALUES (?, ?)")) {

            ps.setString(1, key);

            ps.setString(2, value);

            ps.executeUpdate();
        }
    }

    public static synchronized HashMap<String, Object> selectSettingsValues() throws SQLException {

        HashMap<String, Object> settings = new HashMap<>();

        ResultSet res;

        try (Connection conn = SqliteSingleton.getInstance().getConn(); Statement stat = conn.createStatement()) {

            res = stat.executeQuery("SELECT * FROM settings");

            while (res.next()) {

                settings.put(res.getString("key"), res.getString("value"));
            }
        }

        return settings;
    }

    public static synchronized void insertSettingsValues(HashMap<String, Object> settings) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO settings (key,value) VALUES (?, ?)")) {

            for (Map.Entry<String, Object> entry : settings.entrySet()) {

                ps.setString(1, entry.getKey());
                ps.setString(2, (String) entry.getValue());
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    public static synchronized ArrayList<HashMap<String, Object>> selectDownloads() throws SQLException {

        ArrayList<HashMap<String, Object>> downloads = new ArrayList<>();

        ResultSet res;

        try (Connection conn = SqliteSingleton.getInstance().getConn(); Statement stat = conn.createStatement()) {

            res = stat.executeQuery("SELECT * FROM downloads");

            while (res.next()) {

                HashMap<String, Object> download = new HashMap<>();

                download.put("url", res.getString("url"));
                download.put("email", res.getString("email"));
                download.put("path", res.getString("path"));
                download.put("filename", res.getString("filename"));
                download.put("filekey", res.getString("filekey"));
                download.put("filesize", res.getLong("filesize"));
                download.put("filepass", res.getString("filepass"));
                download.put("filenoexpire", res.getString("filenoexpire"));
                download.put("custom_chunks_dir", res.getString("custom_chunks_dir"));

                downloads.add(download);
            }
        }

        return downloads;
    }

    public static synchronized ArrayList<HashMap<String, Object>> selectUploads() throws SQLException {

        ArrayList<HashMap<String, Object>> uploads = new ArrayList<>();

        ResultSet res;

        try (Connection conn = SqliteSingleton.getInstance().getConn(); Statement stat = conn.createStatement()) {

            res = stat.executeQuery("SELECT * FROM uploads");

            while (res.next()) {

                HashMap<String, Object> upload = new HashMap<>();

                upload.put("filename", res.getString("filename"));
                upload.put("email", res.getString("email"));
                upload.put("url", res.getString("url"));
                upload.put("ul_key", res.getString("ul_key"));
                upload.put("parent_node", res.getString("parent_node"));
                upload.put("root_node", res.getString("root_node"));
                upload.put("share_key", res.getString("share_key"));
                upload.put("folder_link", res.getString("folder_link"));
                upload.put("bytes_uploaded", res.getLong("bytes_uploaded"));
                upload.put("meta_mac", res.getString("meta_mac"));
                uploads.add(upload);
            }
        }

        return uploads;
    }

    public static synchronized HashMap<String, Object> selectMegaAccounts() throws SQLException {

        HashMap<String, Object> accounts = new HashMap<>();

        ResultSet res;

        try (Connection conn = SqliteSingleton.getInstance().getConn(); Statement stat = conn.createStatement()) {

            res = stat.executeQuery("SELECT * FROM mega_accounts");

            while (res.next()) {

                HashMap<String, Object> account_data = new HashMap<>();

                account_data.put("password", res.getString("password"));
                account_data.put("password_aes", res.getString("password_aes"));
                account_data.put("user_hash", res.getString("user_hash"));

                accounts.put(res.getString("email"), account_data);
            }
        }

        return accounts;
    }

    public static synchronized void insertMegaAccounts(HashMap<String, Object> accounts) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO mega_accounts (email,password,password_aes,user_hash) VALUES (?, ?, ?, ?)")) {

            if (!accounts.isEmpty()) {

                for (Map.Entry<String, Object> entry : accounts.entrySet()) {

                    ps.setString(1, entry.getKey());

                    ps.setString(2, (String) ((Map<String, Object>) entry.getValue()).get("password"));

                    ps.setString(3, (String) ((Map<String, Object>) entry.getValue()).get("password_aes"));

                    ps.setString(4, (String) ((Map<String, Object>) entry.getValue()).get("user_hash"));

                    ps.addBatch();
                }

                ps.executeBatch();
            }
        }
    }

    public static synchronized void insertELCAccounts(HashMap<String, Object> accounts) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO elc_accounts (host,user,apikey) VALUES (?, ?, ?)")) {

            if (!accounts.isEmpty()) {

                for (Map.Entry<String, Object> entry : accounts.entrySet()) {

                    ps.setString(1, entry.getKey());

                    ps.setString(2, (String) ((Map<String, Object>) entry.getValue()).get("user"));

                    ps.setString(3, (String) ((Map<String, Object>) entry.getValue()).get("apikey"));

                    ps.addBatch();
                }

                ps.executeBatch();

            }
        }
    }

    public static synchronized HashMap<String, Object> selectELCAccounts() throws SQLException {

        HashMap<String, Object> accounts = new HashMap<>();

        ResultSet res;

        try (Connection conn = SqliteSingleton.getInstance().getConn(); Statement stat = conn.createStatement()) {

            res = stat.executeQuery("SELECT * FROM elc_accounts");

            while (res.next()) {

                HashMap<String, Object> account_data = new HashMap<>();

                account_data.put("user", res.getString("user"));
                account_data.put("apikey", res.getString("apikey"));

                accounts.put(res.getString("host"), account_data);
            }
        }

        return accounts;
    }

    public static synchronized void insertMegaAccount(String email, String password, String password_aes, String user_hash) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO mega_accounts (email,password,password_aes,user_hash) VALUES (?, ?, ?, ?)")) {

            ps.setString(1, email);

            ps.setString(2, password);

            ps.setString(3, password_aes);

            ps.setString(4, user_hash);

            ps.executeUpdate();

        }

    }

    public static synchronized void insertELCAccount(String host, String user, String apikey) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO elc_accounts (host,user,apikey) VALUES (?, ?, ?)")) {

            ps.setString(1, host);

            ps.setString(2, user);

            ps.setString(3, apikey);

            ps.executeUpdate();
        }

    }

    public static synchronized void deleteMegaAccount(String email) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("DELETE from mega_accounts WHERE email=?")) {

            ps.setString(1, email);

            ps.executeUpdate();
        }
    }

    public static synchronized void deleteELCAccount(String host) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("DELETE from elc_accounts WHERE host=?")) {

            ps.setString(1, host);

            ps.executeUpdate();
        }
    }

    public static synchronized void truncateMegaAccounts() throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); Statement stat = conn.createStatement()) {

            stat.execute("DELETE FROM mega_accounts");
        }
    }

    public static synchronized void truncateELCAccounts() throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); Statement stat = conn.createStatement()) {

            stat.execute("DELETE FROM elc_accounts");
        }
    }

    private DBTools() {
    }

}
