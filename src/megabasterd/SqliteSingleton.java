package megabasterd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public final class SqliteSingleton {

    public static final String SQLITE_FILE = "megabasterd.db";
    
    public static final int VALID_TIMEOUT = 15;

    private Connection conn = null;

    public static SqliteSingleton getInstance() {

        return LazyHolder.INSTANCE;
    }

    private SqliteSingleton() {
    }

    public Connection getConn() {

        try {
            if (conn == null || !conn.isValid(VALID_TIMEOUT)) {
                
                if(conn != null && !conn.isClosed()) {
                    
                    conn.close();
                }

                Class.forName("org.sqlite.JDBC");

                conn = DriverManager.getConnection("jdbc:sqlite:" + SQLITE_FILE);
            }
        } catch (SQLException | ClassNotFoundException ex) {
            Logger.getLogger(SqliteSingleton.class.getName()).log(Level.SEVERE, null, ex);
        }

        return conn;
    }

    private final static class LazyHolder {

        private static final SqliteSingleton INSTANCE = new SqliteSingleton();
    }

}
