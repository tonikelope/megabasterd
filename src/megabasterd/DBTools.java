package megabasterd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author tonikelope
 */
public final class DBTools {

    public static final int MAX_TRANSFERENCES_QUERY = 100;

    public static synchronized void setupSqliteTables() throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); Statement stat = conn.createStatement()) {

            stat.executeUpdate("CREATE TABLE IF NOT EXISTS downloads(url TEXT, path TEXT, filename TEXT, filekey TEXT, filesize UNSIGNED BIG INT, filepass VARCHAR(64), filenoexpire VARCHAR(64), PRIMARY KEY ('url'), UNIQUE(path, filename));");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS uploads(filename TEXT, email TEXT, url TEXT, ul_key TEXT, parent_node TEXT, root_node TEXT, share_key TEXT, folder_link TEXT, PRIMARY KEY ('filename'), UNIQUE(filename, email));");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS settings(key VARCHAR(255), value TEXT, PRIMARY KEY('key'));");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS mega_accounts(email TEXT, password TEXT, password_aes TEXT, user_hash TEXT, PRIMARY KEY('email'));");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS elc_accounts(host TEXT, user TEXT, apikey TEXT, PRIMARY KEY('host'));");
        }
    }

    public static synchronized void vaccum() throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); Statement stat = conn.createStatement()) {

            stat.execute("VACUUM");
        }
    }

    public static synchronized void insertDownload(String url, String path, String filename, String filekey, Long size, String filepass, String filenoexpire) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("INSERT INTO downloads (url, path, filename, filekey, filesize, filepass, filenoexpire) VALUES (?,?,?,?,?,?,?)")) {

            ps.setString(1, url);
            ps.setString(2, path);
            ps.setString(3, filename);
            ps.setString(4, filekey);
            ps.setLong(5, size);
            ps.setString(6, filepass);
            ps.setString(7, filenoexpire);

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

        for (int n = 0, t = 0; t < urls.length; n++) {

            String[] sub_array = Arrays.copyOfRange(urls, n * MAX_TRANSFERENCES_QUERY, t + Math.min(MAX_TRANSFERENCES_QUERY, urls.length - t));

            t += sub_array.length;

            String whereClause = String.format("url in (%s)", String.join(",", Collections.nCopies(sub_array.length, "?")));

            try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("DELETE FROM downloads WHERE " + whereClause)) {

                int i = 1;

                for (String value : sub_array) {

                    ps.setString(i, value);

                    i++;
                }

                ps.executeUpdate();
            }
        }
    }

    public static synchronized void insertUpload(String filename, String email, String parent_node, String ul_key, String root_node, String share_key, String folder_link) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("INSERT INTO uploads (filename, email, parent_node, ul_key, root_node, share_key, folder_link) VALUES (?,?,?,?,?,?,?)")) {

            ps.setString(1, filename);
            ps.setString(2, email);
            ps.setString(3, parent_node);
            ps.setString(4, ul_key);
            ps.setString(5, root_node);
            ps.setString(6, share_key);
            ps.setString(7, folder_link);

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

    public static synchronized void deleteUpload(String filename, String email) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("DELETE FROM uploads WHERE filename=? AND email=?")) {

            ps.setString(1, filename);

            ps.setString(2, email);

            ps.executeUpdate();
        }
    }

    public static synchronized void deleteUploads(String[][] uploads) throws SQLException {

        for (int n = 0, t = 0; t < uploads.length; n++) {

            String[][] sub_array = Arrays.copyOfRange(uploads, n * MAX_TRANSFERENCES_QUERY, t + Math.min(MAX_TRANSFERENCES_QUERY, uploads.length - t));

            t += sub_array.length;

            String whereClause = String.join(" OR ", Collections.nCopies(sub_array.length, "(filename=? AND email=?)"));

            try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("DELETE FROM uploads WHERE " + whereClause)) {

                int i = 1;

                for (String[] pair : sub_array) {

                    ps.setString(i, pair[0]);

                    ps.setString(i + 1, pair[1]);

                    i += 2;
                }

                ps.executeUpdate();
            }
        }
    }

    public static synchronized String selectSettingValueFromDB(String key) {

        String value = null;

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("SELECT value from settings WHERE key=?")) {

            ps.setString(1, key);

            ResultSet res = ps.executeQuery();

            if (res.next()) {
                value = res.getString(1);
            }
        } catch (SQLException ex) {
            getLogger(DBTools.class.getName()).log(Level.SEVERE, null, ex);
        }

        return value;
    }

    public static synchronized void insertSettingValueInDB(String key, String value) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO settings (key,value) VALUES (?, ?)")) {

            ps.setString(1, key);

            ps.setString(2, value);

            ps.executeUpdate();
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
                download.put("path", res.getString("path"));
                download.put("filename", res.getString("filename"));
                download.put("filekey", res.getString("filekey"));
                download.put("filesize", res.getLong("filesize"));
                download.put("filepass", res.getString("filepass"));
                download.put("filenoexpire", res.getString("filenoexpire"));

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

    private DBTools() {
    }

}
