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

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public class SqliteSingleton {

    public static final String SQLITE_FILE = "megabasterd.db";

    public static final int VALIDATION_TIMEOUT = 15;
    private static final Logger LOG = Logger.getLogger(SqliteSingleton.class.getName());

    public static SqliteSingleton getInstance() {

        return LazyHolder.INSTANCE;
    }
    private final Object _conn_lock = new Object();
    private Connection _real_conn;
    private Connection _proxy_conn;

    private SqliteSingleton() {

        File database_path = new File(MainPanel.MEGABASTERD_HOME_DIR + "/.megabasterd" + MainPanel.VERSION);

        database_path.mkdirs();
    }

    public Connection getConn() {

        synchronized (_conn_lock) {

            try {

                if (_real_conn == null || !_real_conn.isValid(VALIDATION_TIMEOUT)) {

                    Class.forName("org.sqlite.JDBC");

                    if (_real_conn != null) {
                        try {
                            _real_conn.close();
                        } catch (SQLException ignore) {
                        }
                    }

                    _real_conn = DriverManager.getConnection("jdbc:sqlite:" + MainPanel.MEGABASTERD_HOME_DIR + "/.megabasterd" + MainPanel.VERSION + "/" + SQLITE_FILE + "?journal_mode=WAL&synchronous=OFF&journal_size_limit=500");

                    _proxy_conn = _wrap(_real_conn);
                }

            } catch (ClassNotFoundException | SQLException ex) {
                LOG.log(Level.SEVERE, ex.getMessage());
            }

            return _proxy_conn;
        }
    }

    public void shutdown() {

        synchronized (_conn_lock) {

            if (_real_conn != null) {

                try {
                    _real_conn.close();
                } catch (SQLException ex) {
                    LOG.log(Level.SEVERE, ex.getMessage());
                }

                _real_conn = null;
                _proxy_conn = null;
            }
        }
    }

    private static Connection _wrap(final Connection real) {

        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        switch (method.getName()) {
                            case "close":
                                return null;
                            case "isClosed":
                                return real.isClosed();
                            default:
                                try {
                                    return method.invoke(real, args);
                                } catch (java.lang.reflect.InvocationTargetException ite) {
                                    throw ite.getCause();
                                }
                        }
                    }
                });
    }

    private static class LazyHolder {

        private static final SqliteSingleton INSTANCE = new SqliteSingleton();

        private LazyHolder() {
        }
    }

}
