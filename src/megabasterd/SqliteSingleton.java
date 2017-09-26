package megabasterd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author tonikelope
 */
public final class SqliteSingleton {

    public static final String SQLITE_FILE = "megabasterd.db";

    public static final int VALIDATION_TIMEOUT = 15;

    private final ConcurrentHashMap<Thread, Connection> _connections_map;

    public static SqliteSingleton getInstance() {

        return LazyHolder.INSTANCE;
    }

    private SqliteSingleton() {

        _connections_map = new ConcurrentHashMap();
    }

    public Connection getConn() {

        Connection conn = null;

        try {

            if (!_connections_map.containsKey(Thread.currentThread()) || !(conn = _connections_map.get(Thread.currentThread())).isValid(VALIDATION_TIMEOUT)) {

                Class.forName("org.sqlite.JDBC");

                conn = DriverManager.getConnection("jdbc:sqlite:" + SQLITE_FILE + "?journal_mode=WAL&synchronous=OFF&journal_size_limit=500");

                _connections_map.put(Thread.currentThread(), conn);
            }

        } catch (ClassNotFoundException | SQLException ex) {
            getLogger(SqliteSingleton.class.getName()).log(Level.SEVERE, null, ex);
        }

        return conn;
    }

    private final static class LazyHolder {

        private static final SqliteSingleton INSTANCE = new SqliteSingleton();
    }

}
