package megabasterd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author tonikelope
 */
public final class SqliteSingleton {

    public static final String SQLITE_FILE = "megabasterd.db";

    public static SqliteSingleton getInstance() {

        return LazyHolder.INSTANCE;
    }

    private SqliteSingleton() {
    }

    public Connection getConn() {

        Connection conn = null;

        try {

            Class.forName("org.sqlite.JDBC");

            conn = DriverManager.getConnection("jdbc:sqlite:" + SQLITE_FILE);

        } catch (ClassNotFoundException | SQLException ex) {
            getLogger(SqliteSingleton.class.getName()).log(Level.SEVERE, null, ex);
        }

        return conn;
    }

    private final static class LazyHolder {

        private static final SqliteSingleton INSTANCE = new SqliteSingleton();
    }

}
