package com.tonikelope.megabasterd.db

import com.tonikelope.megabasterd.Download
import com.tonikelope.megabasterd.MainPanel
import com.tonikelope.megabasterd.MiscTools
import com.tonikelope.megabasterd.SqliteSingleton
import com.tonikelope.megabasterd.Upload
import org.apache.logging.log4j.LogManager
import org.intellij.lang.annotations.Language
import java.io.File
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import java.util.concurrent.ConcurrentHashMap

typealias AccountSettings = HashMap<String, String>
typealias AccountSet = HashMap<String, AccountSettings>

/**
 * Port of tonikelope's DBTools to Kotlin.
 *
 * @author tonikelope
 * @author DavidArthurCole
 */
@Suppress("SameParameterValue", "Unused")
object KDBTools {

    @JvmStatic private val LOG = LogManager.getLogger(KDBTools::class)
    @JvmStatic private val DB_LOCK = Object()
    @JvmStatic private val DB_FILE = File(MainPanel.MEGABASTERD_HOME_DIR + "/.megabasterd" + MainPanel.VERSION + "/" + SqliteSingleton.SQLITE_FILE)

    @JvmStatic private val SETTINGS_CACHE = ConcurrentHashMap<String, Any?>()

    // <editor-fold desc="Helpers for DB ops">
    /**
     * Use this method to perform operations with a database connection.
     * The connection will be automatically closed after the action is performed.
     * This is synchronized on a lock to prevent concurrent access to the database.
     *
     * @param action The action to perform with the connection.
     * @throws SQLException if a database access error occurs
     */
    @JvmStatic
    @Throws(SQLException::class)
    private fun <T> withDbConnection(action: Connection.() -> T): T = synchronized(DB_LOCK) {
        KSqliteSingleton.getConnection().use { it.action() }
    }

    /**
     * A safe version of [withDbConnection] that catches and logs exceptions.
     * @param action The action to perform with the connection.
     */
    @JvmStatic
    private fun <T> withDbConnectionSafe(
        action: Connection.() -> T,
    ): T? = try {
        withDbConnection(action)
    } catch (e: Exception) {
        LOG.error("Database operation failed: {}", e.message, e)
        null
    }

    /**
     * Use this method to perform operations with a database statement.
     * The statement will be automatically closed after the action is performed.
     * This is synchronized on a lock to prevent concurrent access to the database.
     *
     * @param action The action to perform with the statement.
     * @throws SQLException if a database access error occurs
     */
    @JvmStatic
    @Throws(SQLException::class)
    private fun <T> withDbStatement(action: Statement.() -> T): T = synchronized(DB_LOCK) {
        withDbConnection { createStatement().use { it.action() } }
    }

    /**
     * A safe version of [withDbStatement] that catches and logs exceptions.
     * @param action The action to perform with the statement.
     */
    @JvmStatic
    private fun <T> withDbStatementSafe(
        action: Statement.() -> T,
    ): T? = try {
        withDbStatement(action)
    } catch (e: Exception) {
        LOG.error("Database statement operation failed: {}", e.message, e)
        null
    }

    /**
     * Use this method to perform operations with a prepared statement.
     * The prepared statement will be automatically closed after the action is performed.
     * This is synchronized on a lock to prevent concurrent access to the database.
     *
     * @param sql The SQL query for the prepared statement.
     * @param args The arguments to set in the prepared statement.
     * @param action The action to perform with the prepared statement.
     */
    @JvmStatic
    @Throws(SQLException::class)
    private fun <T>  withDbPreparedStatement(
        @Language("SQL") sql: String,
        args: List<Any> = emptyList(),
        action: java.sql.PreparedStatement.() -> T,
    ): T = synchronized(DB_LOCK) {
        withDbConnection {
            prepareStatement(sql).use {
                args.forEachIndexed { index, arg ->
                    when (arg) {
                        is String -> if (arg.isEmpty()) {
                            it.setString(index + 1, null)
                        } else it.setString(index + 1, arg)
                        is Int -> it.setInt(index + 1, arg)
                        is Long -> it.setLong(index + 1, arg)
                        is Double -> it.setDouble(index + 1, arg)
                        is ByteArray -> it.setBytes(index + 1, arg)
                        else -> throw IllegalArgumentException("Unsupported argument type: ${arg::class.java}")
                    }
                }
                it.action()
            }
        }
    }

    /**
     * A safe version of [withDbPreparedStatement] that catches and logs exceptions.
     * @param sql The SQL query for the prepared statement.
     * @param args The arguments to set in the prepared statement.
     * @param action The action to perform with the prepared statement.
     */
    @JvmStatic
    private fun <T> withDbPreparedStatementSafe(
        @Language("SQL") sql: String,
        args: List<Any> = emptyList(),
        action: java.sql.PreparedStatement.() -> T,
    ): T? = try {
        withDbPreparedStatement(sql, args, action)
    } catch (e: Exception) {
        LOG.error("Database prepared statement operation failed: {}", e.message, e)
        null
    }

    @JvmStatic
    private fun <T> readTableAsCollection(
        @Language("SQL") tableName: String,
        @Language("SQL") query: String = "SELECT * FROM $tableName",
        @Language("SQL") orderBy: String = "",
        @Language("SQL") limit: String = "",
        @Language("SQL") where: String = "",
        rowMapper: java.sql.ResultSet.() -> T,
    ): List<T> = withDbStatement {
        val finalQuery = buildString {
            append(query)
            if (where.isNotEmpty()) append(" WHERE $where")
            if (orderBy.isNotEmpty()) append(" ORDER BY $orderBy")
            if (limit.isNotEmpty()) append(" LIMIT $limit")
        }
        @Suppress("SqlSourceToSinkFlow")
        val result = executeQuery(finalQuery)
        buildList {
            while (result.next()) {
                add(rowMapper(result))
            }
        }.toMutableList()
    }

    @JvmStatic
    @Throws(SQLException::class)
    private fun truncateTable(
        @Language("SQL") tableName: String
    ): Boolean = withDbStatement {
        execute("DELETE FROM $tableName")
    }
    // </editor-fold>

    fun deleteDbFile(): Boolean = synchronized(DB_LOCK) {
        return@deleteDbFile if (!DB_FILE.exists()) {
            LOG.warn("Cannot delete database file, does not exist: {}", DB_FILE.absolutePath)
            false
        } else try {
            DB_FILE.delete()
        } catch (e: Exception) {
            LOG.error("Failed to delete database file: {}", e.message, e)
            false
        }
    }

    @JvmStatic
    @Throws(SQLException::class)
    fun setupSqliteTables() = withDbStatement {
        executeUpdate("CREATE TABLE IF NOT EXISTS downloads(url TEXT, email TEXT, path TEXT, filename TEXT, filekey TEXT, filesize UNSIGNED BIG INT, filepass VARCHAR(64), filenoexpire VARCHAR(64), custom_chunks_dir TEXT, PRIMARY KEY ('url'), UNIQUE(path, filename));")
        executeUpdate("CREATE TABLE IF NOT EXISTS uploads(filename TEXT, email TEXT, url TEXT, ul_key TEXT, parent_node TEXT, root_node TEXT, share_key TEXT, folder_link TEXT, bytes_uploaded UNSIGNED BIG INT, meta_mac TEXT, PRIMARY KEY ('filename'), UNIQUE(filename, email));")
        executeUpdate("CREATE TABLE IF NOT EXISTS settings(key VARCHAR(255), value TEXT, PRIMARY KEY('key'));")
        executeUpdate("CREATE TABLE IF NOT EXISTS mega_accounts(email TEXT, password TEXT, password_aes TEXT, user_hash TEXT, PRIMARY KEY('email'));")
        executeUpdate("CREATE TABLE IF NOT EXISTS elc_accounts(host TEXT, user TEXT, apikey TEXT, PRIMARY KEY('host'));")
        executeUpdate("CREATE TABLE IF NOT EXISTS mega_sessions(email TEXT, ma BLOB, crypt INT, PRIMARY KEY('email'));")
        executeUpdate("CREATE TABLE IF NOT EXISTS downloads_queue(url TEXT, PRIMARY KEY('url'));")
        executeUpdate("CREATE TABLE IF NOT EXISTS uploads_queue(filename TEXT, PRIMARY KEY('filename'));")
    }

    @JvmStatic
    @Throws(SQLException::class)
    fun vacuumDB() = withDbStatement { execute("VACUUM") }

    // <editor-fold desc="Downloads Queue">
    @JvmStatic
    @Throws(SQLException::class)
    fun insertDownloadsQueue(queue: List<String>): IntArray = withDbPreparedStatement(
        "INSERT OR REPLACE INTO downloads_queue (url) VALUES (?)"
    ) {
        if (queue.isEmpty()) return@withDbPreparedStatement IntArray(0)
        queue.forEach { url ->
            setString(1, url)
            addBatch()
        }
        executeBatch()
    }

    @JvmStatic
    @Throws(SQLException::class)
    fun selectDownloadsQueue(): ArrayList<String> = readTableAsCollection(
        "downloads_queue",
        orderBy = "rowid",
        rowMapper = { getString("url") }
    ).toMutableList() as ArrayList<String>

    @JvmStatic
    @Throws(SQLException::class)
    fun truncateDownloadsQueue() = truncateTable("downloads_queue")
    // </editor-fold>

    // <editor-fold desc="Uploads Queue">
    @JvmStatic
    @Throws(SQLException::class)
    fun insertUploadsQueue(queue: List<String>): IntArray = withDbPreparedStatement(
        "INSERT OR REPLACE INTO uploads_queue (filename) VALUES (?)"
    ) {
        if (queue.isEmpty()) return@withDbPreparedStatement IntArray(0)
        queue.forEach { filename ->
            setString(1, filename)
            addBatch()
        }
        executeBatch()
    }

    @JvmStatic
    @Throws(SQLException::class)
    fun selectUploadsQueue(): ArrayList<String> = readTableAsCollection(
        "uploads_queue",
        orderBy = "rowid",
        rowMapper = { getString("filename") }
    ).toMutableList() as ArrayList<String>

    @JvmStatic
    @Throws(SQLException::class)
    fun truncateUploadsQueue() = truncateTable("uploads_queue")
    // </editor-fold>

    // <editor-fold desc="MEGA Sessions">
    @JvmStatic
    @Throws(SQLException::class)
    fun insertMegaSession(email: String, ma: ByteArray, crypt: Boolean): Boolean = withDbPreparedStatement(
        "INSERT OR REPLACE INTO mega_sessions (email, ma, crypt) VALUES (?, ?, ?)"
    ) {
        setString(1, email)
        setBytes(2, ma)
        setInt(3, if (crypt) 1 else 0)
        executeUpdate() > 0
    }

    @JvmStatic
    fun selectMegaSession(email: String): HashMap<String, Any>? = withDbPreparedStatementSafe(
        "SELECT * from mega_sessions WHERE email=? LIMIT 1",
        listOf(email),
    ) {
        val result = executeQuery()
        if (result.next()) buildMap {
            put("email", email)
            put("ma", result.getBytes("ma"))
            put("crypt", result.getInt("crypt"))
        }.toMap<String, Any>() as HashMap<String, Any>
        else null
    }

    @JvmStatic
    @Throws(SQLException::class)
    fun truncateMegaSessions(): Boolean = truncateTable("mega_sessions")
    // </editor-fold>

    // <editor-fold desc="Downloads">
    @JvmStatic
    @Throws(SQLException::class)
    fun executeDownloadPreparedStatement(
        @Language("SQL") sql: String,
        download: Download,
    ): Int = withDbPreparedStatement(
        sql,
        with(download) {
            listOf(
                url.orEmpty(),
                ma.full_email.orEmpty(),
                download_path.orEmpty(),
                file_name.orEmpty(),
                file_key.orEmpty(),
                file_size,
                file_pass.orEmpty(),
                file_noexpire.orEmpty(),
                custom_chunks_dir.orEmpty(),
            )
        }
    ) { executeUpdate() }

    @JvmStatic
    @Throws(SQLException::class)
    fun insertDownload(download: Download): Int = executeDownloadPreparedStatement(
        "INSERT INTO downloads (url, email, path, filename, filekey, filesize, filepass, filenoexpire, custom_chunks_dir) VALUES (?,?,?,?,?,?,?,?,?)",
        download,
    )

    @JvmStatic
    @Throws(SQLException::class)
    fun insertOrReplaceDownload(download: Download): Int = executeDownloadPreparedStatement(
        "INSERT OR REPLACE INTO downloads (url, email, path, filename, filekey, filesize, filepass, filenoexpire, custom_chunks_dir) VALUES (?,?,?,?,?,?,?,?,?)",
        download,
    )

    @JvmStatic
    @Throws(SQLException::class)
    fun selectDownloads(): HashMap<String, HashMap<String, Any?>> = readTableAsCollection(
        "downloads",
        rowMapper = {
            getString("url") to buildMap<String, Any?> {
                put("email", getString("email"))
                put("path", getString("path"))
                put("filename", getString("filename"))
                put("filekey", getString("filekey"))
                put("filesize", getLong("filesize"))
                put("filepass", getString("filepass"))
                put("filenoexpire", getString("filenoexpire"))
                put("custom_chunks_dir", getString("custom_chunks_dir"))
            }.toMutableMap() as HashMap<String, Any?>
        }
    ).associate { it }.toMutableMap() as HashMap<String, HashMap<String, Any?>>

    @JvmStatic
    @Throws(SQLException::class)
    fun deleteDownload(url: String): Int = withDbPreparedStatement(
        "DELETE FROM downloads WHERE url=?",
        listOf(url)
    ) { executeUpdate() }

    @JvmStatic
    @Throws(SQLException::class)
    fun deleteDownloads(urls: List<String>): IntArray = withDbPreparedStatement(
        "DELETE FROM downloads WHERE url=?"
    ) {
        if (urls.isEmpty()) return@withDbPreparedStatement IntArray(0)
        urls.forEach { url ->
            setString(1, url)
            addBatch()
        }
        executeBatch()
    }
    // </editor-fold>

    // <editor-fold desc="Uploads">
    @JvmStatic
    @Throws(SQLException::class)
    fun insertUpload(upload: Upload): Int = withDbPreparedStatement(
        "INSERT INTO uploads (filename, email, parent_node, ul_key, root_node, share_key, folder_link, bytes_uploaded, meta_mac) VALUES (?,?,?,?,?,?,?,?,?)",
        with(upload) {
            listOf(
                file_name.orEmpty(),
                ma.full_email.orEmpty(),
                parent_node.orEmpty(),
                MiscTools.Bin2BASE64(MiscTools.i32a2bin(ul_key)),
                root_node.orEmpty(),
                MiscTools.Bin2BASE64(share_key),
                folder_link.orEmpty(),
                0L,
                "",
            )
        }
    ) { executeUpdate() }

    @JvmStatic
    @Throws(SQLException::class)
    fun updateUploadUrl(upload: Upload): Int = withDbPreparedStatement(
        "UPDATE uploads SET url=? WHERE filename=? AND email=?",
        listOf(
            upload.ul_url.orEmpty(),
            upload.file_name.orEmpty(),
            upload.ma.full_email.orEmpty(),
        )
    ) { executeUpdate() }

    @JvmStatic
    @Throws(SQLException::class)
    fun updateUploadProgress(fileName: String, email: String, bytesUploaded: Long, metaMac: String): Int = withDbPreparedStatement(
        "UPDATE uploads SET bytes_uploaded=?, meta_mac=? WHERE filename=? AND email=?",
        listOf(
            bytesUploaded,
            metaMac,
            fileName,
            email,
        )
    ) { executeUpdate() }

    @JvmStatic
    @Throws(SQLException::class)
    fun selectUploads(): HashMap<String, HashMap<String, Any?>> = readTableAsCollection(
        "uploads",
        rowMapper = {
            getString("filename") to buildMap<String, Any?> {
                put("email", getString("email"))
                put("url", getString("url"))
                put("ul_key", getString("ul_key"))
                put("parent_node", getString("parent_node"))
                put("root_node", getString("root_node"))
                put("share_key", MiscTools.BASE642Bin(getString("share_key")))
                put("folder_link", getString("folder_link"))
                put("bytes_uploaded", getLong("bytes_uploaded"))
                put("meta_mac", getString("meta_mac"))
            }.toMutableMap() as HashMap<String, Any?>
        }
    ).associate { it }.toMutableMap() as HashMap<String, HashMap<String, Any?>>

    @JvmStatic
    @Throws(SQLException::class)
    fun selectUploadProgress(fileName: String, email: String): HashMap<String, Any>? = withDbPreparedStatement(
        "SELECT bytes_uploaded, meta_mac FROM uploads WHERE filename=? AND email=? LIMIT 1",
        listOf(fileName, email)
    ) {
        val result = executeQuery()
        if (result.next()) buildMap<String, Any> {
            put("bytes_uploaded", result.getLong("bytes_uploaded"))
            put("meta_mac", result.getString("meta_mac"))
        }.toMutableMap() as HashMap<String, Any>
        else null
    }

    @JvmStatic
    @Throws(SQLException::class)
    fun deleteUpload(fileName: String, email: String): Int = withDbPreparedStatement(
        "DELETE FROM uploads WHERE filename=? AND email=?",
        listOf(fileName, email)
    ) { executeUpdate() }

    @JvmStatic
    @Throws(SQLException::class)
    fun deleteUploads(uploads: List<Array<String>>): IntArray = withDbPreparedStatement(
        "DELETE FROM uploads WHERE filename=? AND email=?"
    ) {
        if (uploads.isEmpty()) return@withDbPreparedStatement IntArray(0)
        uploads.forEach { upload ->
            if (upload.size != 2) return@forEach
            val (fileName, email) = upload
            setString(1, fileName)
            setString(2, email)
            addBatch()
        }
        executeBatch()
    }
    // </editor-fold>

    // <editor-fold desc="Settings">
    @JvmStatic
    fun selectSettingValue(key: String): String? = SETTINGS_CACHE.getOrPut(key) {
        val dbValue = try {
            selectSettingValueFromDb(key)
        } catch (e: Exception) {
            LOG.error("Failed to select setting value for key '{}': {}", key, e.message, e)
            return@getOrPut null
        }
        dbValue ?: return null
    } as? String

    @JvmStatic
    @Throws(SQLException::class)
    fun selectSettingsValues(): Map<String, Any> = readTableAsCollection(
        "settings",
        rowMapper = { getString("key") to getString("value") }
    ).associate { it }.toMutableMap()

    @JvmStatic
    @Throws(SQLException::class)
    private fun selectSettingValueFromDb(key: String): String? = withDbPreparedStatementSafe(
        "SELECT value FROM settings WHERE key=?",
        listOf(key),
    ) {
        val result = executeQuery()
        if (result.next()) result.getString("value")
        else null
    }

    @JvmStatic
    @Throws(SQLException::class)
    fun insertOrReplaceSettingValue(key: String, value: String?): Int = withDbPreparedStatement(
        "INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)",
        listOf(key, value.orEmpty())
    ) { executeUpdate() }.also { SETTINGS_CACHE[key] = value }

    @JvmStatic
    @Throws(SQLException::class)
    fun insertOrReplaceSettingValues(settings: Map<String, Any?>): IntArray = withDbPreparedStatement(
        "INSERT OR REPLACE INTO settings (key,value) VALUES (?, ?)"
    ) {
        if (settings.isEmpty()) return@withDbPreparedStatement IntArray(0)
        settings.forEach { (key, value) ->
            setString(1, key)
            setString(2, value?.toString().orEmpty())
            addBatch()
        }
        executeBatch()
    }
    // </editor-fold>

    // <editor-fold desc="MEGA Accounts">
    @JvmStatic
    @Throws(SQLException::class)
    fun insertOrReplaceMegaAccount(
        email: String,
        password: String,
        passwordAes: String,
        userHash: String
    ): Int = withDbPreparedStatement(
        "INSERT OR REPLACE INTO mega_accounts (email, password, password_aes, user_hash) VALUES (?, ?, ?, ?)",
        listOf(email, password, passwordAes, userHash)
    ) { executeUpdate() }

    @JvmStatic
    @Throws(SQLException::class)
    fun insertOrReplaceMegaAccounts(accounts: Map<String, Map<String, String>>): IntArray = withDbPreparedStatement(
        "INSERT OR REPLACE INTO mega_accounts (email,password,password_aes,user_hash) VALUES (?, ?, ?, ?)"
    ) {
        if (accounts.isEmpty()) return@withDbPreparedStatement IntArray(0)
        accounts.forEach { (email, account) ->
            setString(1, email)
            setString(2, account["password"].orEmpty())
            setString(3, account["password_aes"].orEmpty())
            setString(4, account["user_hash"].orEmpty())
            addBatch()
        }
        executeBatch()
    }

    @JvmStatic
    @Throws(SQLException::class)
    fun selectMegaAccounts(): AccountSet = readTableAsCollection(
        "mega_accounts",
        rowMapper = {
            getString("email") to buildMap {
                put("password", getString("password"))
                put("password_aes", getString("password_aes"))
                put("user_hash", getString("user_hash"))
            }.toMutableMap() as AccountSettings
        }
    ).associate { it }.toMutableMap() as AccountSet

    @JvmStatic
    @Throws(SQLException::class)
    fun deleteMegaAccount(email: String): Int = withDbPreparedStatement(
        "DELETE FROM mega_accounts WHERE email=?",
        listOf(email)
    ) { executeUpdate() }

    @JvmStatic
    @Throws(SQLException::class)
    fun truncateMegaAccounts(): Boolean = truncateTable("mega_accounts")
    // </editor-fold>

    // <editor-fold desc="ELC Accounts">
    @JvmStatic
    @Throws(SQLException::class)
    fun insertOrReplaceELCAccount(
        host: String,
        user: String,
        apikey: String
    ): Int = withDbPreparedStatement(
        "INSERT OR REPLACE INTO elc_accounts (host, user, apikey) VALUES (?, ?, ?)",
        listOf(host, user, apikey)
    ) { executeUpdate() }

    @JvmStatic
    @Throws(SQLException::class)
    fun insertOrReplaceELCAccounts(accounts: Map<String, Map<String, String>>): IntArray = withDbPreparedStatement(
        "INSERT OR REPLACE INTO elc_accounts (host, user, apikey) VALUES (?, ?, ?)"
    ) {
        if (accounts.isEmpty()) return@withDbPreparedStatement IntArray(0)
        accounts.forEach { (host, account) ->
            setString(1, host)
            setString(2, account["user"].orEmpty())
            setString(3, account["apikey"].orEmpty())
            addBatch()
        }
        executeBatch()
    }

    @JvmStatic
    @Throws(SQLException::class)
    fun selectELCAccounts(): AccountSet = readTableAsCollection(
        "elc_accounts",
        rowMapper = {
            getString("host") to buildMap {
                put("user", getString("user"))
                put("apikey", getString("apikey"))
            }.toMutableMap() as AccountSettings
        }
    ).associate { it }.toMutableMap() as AccountSet

    @JvmStatic
    @Throws(SQLException::class)
    fun deleteELCAccount(host: String): Int = withDbPreparedStatement(
        "DELETE FROM elc_accounts WHERE host=?",
        listOf(host)
    ) { executeUpdate() }

    @JvmStatic
    @Throws(SQLException::class)
    fun truncateELCAccounts(): Boolean = truncateTable("elc_accounts")
    // </editor-fold>
}