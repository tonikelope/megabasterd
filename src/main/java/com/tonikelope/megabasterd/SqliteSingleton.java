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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
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
    private final ConcurrentHashMap<Thread, Connection> _connections_map;

    private SqliteSingleton() {

        _connections_map = new ConcurrentHashMap();

        File database_path = new File(MainPanel.MEGABASTERD_HOME_DIR + "/.megabasterd" + MainPanel.VERSION);

        database_path.mkdirs();
    }

    public Connection getConn() {

        Connection conn = null;

        try {

            if (!_connections_map.containsKey(Thread.currentThread()) || !(conn = _connections_map.get(Thread.currentThread())).isValid(VALIDATION_TIMEOUT)) {

                Class.forName("org.sqlite.JDBC");

                conn = DriverManager.getConnection("jdbc:sqlite:" + MainPanel.MEGABASTERD_HOME_DIR + "/.megabasterd" + MainPanel.VERSION + "/" + SQLITE_FILE + "?journal_mode=WAL&synchronous=OFF&journal_size_limit=500");

                _connections_map.put(Thread.currentThread(), conn);
            }

        } catch (ClassNotFoundException | SQLException ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        }

        return conn;
    }

    private static class LazyHolder {

        private static final SqliteSingleton INSTANCE = new SqliteSingleton();

        private LazyHolder() {
        }
    }

}
