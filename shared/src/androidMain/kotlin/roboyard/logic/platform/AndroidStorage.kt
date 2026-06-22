package roboyard.logic.platform

import android.content.Context
import android.content.SharedPreferences
import roboyard.logic.storage.PlatformStorage
import java.io.File

/**
 * Android implementation of PlatformStorage using SharedPreferences and Context.
 */
class AndroidStorage(private val context: Context) : PlatformStorage {
    private val prefs: SharedPreferences = context.getSharedPreferences("roboyard", Context.MODE_PRIVATE)
    private val appDir = File(context.filesDir, "saves").apply {
        if (!exists()) mkdirs()
    }

    override fun getString(key: String, defaultValue: String?): String? {
        return prefs.getString(key, defaultValue)
    }

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }

    override fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return prefs.getLong(key, defaultValue)
    }

    override fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
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

    override fun hasSavedGames(): Boolean {
        return appDir.exists() && appDir.listFiles()?.isNotEmpty() == true
    }

    override fun readBitmap(fileName: String): Any? {
        // Android-specific bitmap loading would go here
        return null
    }

    override fun writeBitmap(fileName: String, bitmap: Any?): Boolean {
        // Android-specific bitmap saving would go here
        return false
    }
}
