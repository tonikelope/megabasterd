package megabasterd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public final class DBTools {
    
    public static void setupSqliteTables() throws SQLException {
        
        try (Connection conn = SqliteSingleton.getInstance().getConn(); Statement stat = conn.createStatement()) {
            
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS downloads(url TEXT, path TEXT, filename TEXT, filekey TEXT, filesize UNSIGNED BIG INT, filepass VARCHAR(64), filenoexpire VARCHAR(64), PRIMARY KEY ('url'), UNIQUE(path, filename));");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS uploads(filename TEXT, email TEXT, url TEXT, ul_key TEXT, parent_node TEXT, root_node TEXT, share_key TEXT, folder_link TEXT, PRIMARY KEY ('filename'), UNIQUE(filename, email));");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS settings(key VARCHAR(255), value TEXT, PRIMARY KEY('key'));");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS mega_accounts(email TEXT, password TEXT, password_aes TEXT, user_hash TEXT, PRIMARY KEY('email'));");
        }
    }
    
    public static void insertDownload(String url, String path, String filename, String filekey, Long size, String filepass, String filenoexpire) throws SQLException {
        
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
    
    public static void deleteDownload(String url) throws SQLException {
        
        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("DELETE FROM downloads WHERE url=?")) {
            
            ps.setString(1, url);

            ps.executeUpdate();
            
        }
    }
    
    public static void insertUpload(String filename, String email, String parent_node, String ul_key, String root_node, String share_key, String folder_link) throws SQLException {
       
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
    
    public static void updateUploadUrl(String filename, String email, String ul_url) throws SQLException {
        
        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("UPDATE uploads SET url=? WHERE filename=? AND email=?")) {

            ps.setString(1, ul_url);
            ps.setString(2, filename);
            ps.setString(3, email);

            ps.executeUpdate();

        } 
    }
    
    public static void deleteUpload(String filename, String email) throws SQLException {

        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("DELETE FROM uploads WHERE filename=? AND email=?")) {
 
            ps.setString(1, filename);

            ps.setString(2, email);

            ps.executeUpdate();
        }
    }
    
    public static String selectSettingValueFromDB(String key) {
        
        String value=null;
        
        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("SELECT value from settings WHERE key=?")) {
            
            ps.setString(1, key);

            ResultSet res = ps.executeQuery();

            if(res.next()) {
                value = res.getString(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(DBTools.class.getName()).log(Level.SEVERE, null, ex);
        }
       
        return value;
    }
    
    public static void insertSettingValueInDB(String key, String value) throws SQLException  {
        
        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO settings (key,value) VALUES (?, ?)")) {

            ps.setString(1, key);

            ps.setString(2, value);

            ps.executeUpdate();
        }
    }
    
    public static ArrayList<HashMap<String,Object>> selectDownloads() throws SQLException {
        
        ArrayList<HashMap<String,Object>> downloads = new ArrayList<>();
        
        ResultSet res;
                
        try (Connection conn = SqliteSingleton.getInstance().getConn(); Statement stat = conn.createStatement()) {
             
            res = stat.executeQuery("SELECT * FROM downloads");
            
            while(res.next()) {
                
                HashMap<String,Object> download = new HashMap<>();
                
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
    
    public static ArrayList<HashMap<String,Object>> selectUploads() throws SQLException {
        
        ArrayList<HashMap<String,Object>> uploads = new ArrayList<>();
        
        ResultSet res;
                
        try (Connection conn = SqliteSingleton.getInstance().getConn(); Statement stat = conn.createStatement()) {
             
            res = stat.executeQuery("SELECT * FROM uploads");
            
            while(res.next()) {
                
                HashMap<String,Object> upload = new HashMap<>();
                
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
    
    public static HashMap<String,Object> selectMegaAccounts() throws SQLException {
        
        HashMap<String, Object> accounts = new HashMap<>();
        
        ResultSet res;
                
        try (Connection conn = SqliteSingleton.getInstance().getConn(); Statement stat = conn.createStatement()) {
             
            res = stat.executeQuery("SELECT * FROM mega_accounts");
            
            while(res.next()) {
                
                HashMap<String,Object> account_data = new HashMap<>();
                
                account_data.put("password", res.getString("password"));
                account_data.put("password_aes", res.getString("password_aes"));
                account_data.put("user_hash", res.getString("user_hash"));
                
                accounts.put(res.getString("email"), account_data);
            }
        }
  
        return accounts;
    }
    
 
    public static void insertMegaAccount(String email, String password, String password_aes, String user_hash) throws SQLException {
        
        try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO mega_accounts (email,password,password_aes,user_hash) VALUES (?, ?, ?, ?)")) {
            
            ps.setString(1, email);
                
            ps.setString(2, password);

            ps.setString(3, password_aes);

            ps.setString(4, user_hash);

            ps.executeUpdate();
            
        }      

    }
    
    public static void deleteMegaAccount(String email) throws SQLException {
        
         try (Connection conn = SqliteSingleton.getInstance().getConn(); PreparedStatement ps = conn.prepareStatement("DELETE from mega_accounts WHERE email=?")) {

            ps.setString(1, email);
                
            ps.executeUpdate();
        }
    }

    private DBTools() {
    }
    
}
