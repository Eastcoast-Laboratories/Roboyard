package roboyard.logic.platform

import roboyard.logic.storage.PlatformStorage
import java.io.File

/**
 * Desktop implementation of PlatformStorage using local filesystem.
 * Stores data in ~/.roboyard/ directory.
 */
class DesktopStorage : PlatformStorage {
    private val appDir = File(System.getProperty("user.home"), ".roboyard").apply {
        if (!exists()) mkdirs()
    }

    private val prefsFile = File(appDir, "preferences.properties")
    private val javaPrefs = java.util.prefs.Preferences.userRoot().node("roboyard")

    override fun getString(key: String, defaultValue: String?): String? {
        return javaPrefs.get(key, defaultValue)
    }

    override fun putString(key: String, value: String) {
        javaPrefs.put(key, value)
        javaPrefs.flush()
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return javaPrefs.getInt(key, defaultValue)
    }

    override fun putInt(key: String, value: Int) {
        javaPrefs.putInt(key, value)
        javaPrefs.flush()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return javaPrefs.getLong(key, defaultValue)
    }

    override fun putLong(key: String, value: Long) {
        javaPrefs.putLong(key, value)
        javaPrefs.flush()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return javaPrefs.getBoolean(key, defaultValue)
    }

    override fun putBoolean(key: String, value: Boolean) {
        javaPrefs.putBoolean(key, value)
        javaPrefs.flush()
    }

    override fun remove(key: String) {
        javaPrefs.remove(key)
        javaPrefs.flush()
    }

    override fun clear() {
        javaPrefs.clear()
        javaPrefs.flush()
    }

    override fun readFile(fileName: String): String {
        val file = File(appDir, fileName)
        return if (file.exists()) {
            file.readText()
        } else {
            ""
        }
    }

    override fun writeFile(fileName: String, content: String): Boolean {
        return try {
            val file = File(appDir, fileName)
            // Create parent directories if they don't exist
            file.parentFile?.mkdirs()
            file.writeText(content)
            println("[STORAGE] writeFile SUCCESS: $fileName -> ${file.absolutePath}")
            true
        } catch (e: Exception) {
            println("[STORAGE] writeFile ERROR: $fileName - ${e.message}")
            e.printStackTrace()
            false
        }
    }

    override fun fileExists(fileName: String): Boolean {
        return File(appDir, fileName).exists()
    }

    override fun deleteFile(fileName: String): Boolean {
        return File(appDir, fileName).delete()
    }

    override fun getFilePath(fileName: String): String {
        return File(appDir, fileName).absolutePath
    }

    override fun getFileTimestamp(fileName: String): Long? {
        val file = File(appDir, fileName)
        return if (file.exists()) file.lastModified() else null
    }

    override fun hasSavedGames(): Boolean {
        val savesDir = File(appDir, "saves")
        return savesDir.exists() && savesDir.listFiles()?.isNotEmpty() == true
    }

    override fun listFiles(prefix: String, suffix: String): List<String> {
        val files = appDir.listFiles() ?: return emptyList()
        return files.map { it.name }
            .filter { it.startsWith(prefix) && it.endsWith(suffix) }
            .sorted()
    }

    override fun listFilesInDir(dirName: String): List<String> {
        val dir = File(appDir, dirName)
        return dir.list()?.toList() ?: emptyList()
    }

    override fun listAssetFiles(dirName: String): List<String> {
        // Classpath lookup: handles both exploded resources (dev runs) and jars
        val classLoader = Thread.currentThread().contextClassLoader
            ?: DesktopStorage::class.java.classLoader
        val names = mutableListOf<String>()
        try {
            val urls = classLoader.getResources(dirName)
            while (urls.hasMoreElements()) {
                val url = urls.nextElement()
                when (url.protocol) {
                    "file" -> File(url.toURI()).list()?.let { names.addAll(it) }
                    "jar" -> {
                        val conn = url.openConnection() as? java.net.JarURLConnection ?: continue
                        val prefix = conn.entryName?.let { "$it/" } ?: "$dirName/"
                        conn.jarFile.entries().asSequence()
                            .map { it.name }
                            .filter { it.startsWith(prefix) && it.length > prefix.length }
                            .map { it.substring(prefix.length).substringBefore('/') }
                            .let { names.addAll(it) }
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("[DESKTOP_STORAGE] listAssetFiles failed for $dirName: ${e.message}")
        }
        return names.distinct().sorted()
    }

    override fun readBitmap(fileName: String): Any? {
        // Desktop doesn't need bitmap operations for now
        return null
    }

    override fun writeBitmap(fileName: String, bitmap: Any?): Boolean {
        // Desktop doesn't need bitmap operations for now
        return false
    }
}
