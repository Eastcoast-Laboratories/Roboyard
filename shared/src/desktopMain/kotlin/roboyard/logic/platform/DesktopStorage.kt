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
            file.writeText(content)
            true
        } catch (e: Exception) {
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

    override fun hasSavedGames(): Boolean {
        val savesDir = File(appDir, "saves")
        return savesDir.exists() && savesDir.listFiles()?.isNotEmpty() == true
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
