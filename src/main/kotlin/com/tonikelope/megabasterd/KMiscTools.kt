package com.tonikelope.megabasterd

import com.tonikelope.megabasterd.db.KDBTools
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.IOException
import java.lang.management.ManagementFactory
import kotlin.system.exitProcess

object KMiscTools {

    @JvmStatic
    private val LOG = LogManager.getLogger(KMiscTools::class.java)

    private fun getJavaExecutable(): String =
        System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"

    private fun getTempDir(): File =
        File(System.getProperty("java.io.tmpdir"))

    @JvmStatic
    fun byeByeNow(
        restart: Boolean = false,
        deleteDb: Boolean = false,
        purgeFolderCache: Boolean = false,
    ) {
        if (purgeFolderCache) purgeFolderCache()
        MainPanel.removeTrayIcon()
        if (deleteDb) KDBTools.deleteDbFile()
        else try {
            KDBTools.vacuumDB()
        } catch (e: Exception) {
            LOG.error("Failed to vacuum database: {}", e.message, e)
        }

        if (restart) restartApplication()
        else exitProcess(0)
    }

    @JvmStatic
    fun purgeFolderCache() = with(getTempDir()) {
        if (!exists()) return@with LOG.warn("Folder cache dir ({}) does not exist", absolutePath)
        val files = listFiles()?.takeIf { it.isNotEmpty() }
            ?: return@with LOG.warn("Folder cache dir ({}) does not contain any files", absolutePath)
        val ourFiles = files.filter {
            it.isFile && it.name.startsWith("megabasterd_folder_cache_")
        }
        ourFiles.forEach { file ->
            try {
                if (file.delete()) LOG.info("REMOVING FOLDER CACHE FILE: {}", file.absolutePath)
                else LOG.warn("Failed to delete folder cache file: {}", file.absolutePath)
            } catch (e: Exception) {
                LOG.error("Error deleting folder cache file {}: {}", file.absolutePath, e.message, e)
            }
        }
    }

    @JvmStatic
    fun restartApplication() {
        val restartCommand = buildString {
            append(getJavaExecutable())
            ManagementFactory.getRuntimeMXBean().inputArguments.forEach {
                append("$it ")
            }
            append(" -cp ${ManagementFactory.getRuntimeMXBean().classPath} ")
            append(MainPanel::class.java.name)
            append(" native 1")
        }
        try {
            runSystemProcess(restartCommand)
        } catch (e: IOException) {
            LOG.fatal("Failed to restart application {}!", e.message, e)
        }
    }

    @Throws(IOException::class)
    fun runSystemProcess(command: String): Process  {
        val processBuilder = ProcessBuilder(command.split(" "))
        processBuilder.redirectErrorStream(true)
        return processBuilder.start()
    }

    @Throws(IOException::class)
    fun runSystemProcess(vararg command: String): Process {
        val processBuilder = ProcessBuilder(*command)
        processBuilder.redirectErrorStream(true)
        return processBuilder.start()
    }

    @Throws(IOException::class)
    fun runSystemProcess(builderBlock: (ProcessBuilder) -> Unit): Process {
        val processBuilder = ProcessBuilder()
        processBuilder.redirectErrorStream(true)
        builderBlock(processBuilder)
        return processBuilder.start()
    }

}