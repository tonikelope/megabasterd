package com.tonikelope.megabasterd.db

import com.tonikelope.megabasterd.MainPanel
import org.apache.logging.log4j.LogManager
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.concurrent.ConcurrentHashMap

/**
 * Port of tonikelope's SqliteSingleton to Kotlin.
 *
 * @author tonikelope
 * @author DavidArthurCole
 */
object KSqliteSingleton {
    @JvmStatic private val LOG = LogManager.getLogger(KSqliteSingleton::class.java)
    private val connectionsMap = ConcurrentHashMap<Thread, Connection>()

    @JvmStatic val SQLITE_FILE = "megabasterd.db"
    @JvmStatic val VALIDATION_TIMEOUT = 15

    @JvmStatic private val DB_DIR_PATH = "${MainPanel.MEGABASTERD_HOME_DIR}/.megabasterd${MainPanel.VERSION}"
    @JvmStatic private val DRIVER_OPTS = "?journal_mode=WAL&synchronous=OFF&journal_size_limit=500"
    @JvmStatic private val DRIVER_PATH = "jdbc:sqlite:${DB_DIR_PATH}/${SQLITE_FILE}$DRIVER_OPTS"

    init {
        val dbDir = File(DB_DIR_PATH)
        if (!dbDir.exists() && !dbDir.mkdirs()) {
            LOG.error("Failed to create directory for database: {}", DB_DIR_PATH)
        }
    }

    @Throws(SQLException::class)
    fun getConnection(): Connection = runCatching {
        val currentThread = Thread.currentThread()
        if (!connectionsMap.containsKey(currentThread) || connectionsMap[currentThread]?.isValid(VALIDATION_TIMEOUT) == false) {
            Class.forName("org.sqlite.JDBC")
            connectionsMap[currentThread] = DriverManager.getConnection(DRIVER_PATH)
        }
        connectionsMap[currentThread]!!
    }.onFailure {
        LOG.error("Failed to get database connection: {}", it.message, it)
    }.getOrThrow()

}