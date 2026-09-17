package roboyard.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import roboyard.logic.storage.PlatformStorage
import timber.log.Timber
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

/**
 * Android implementation of PlatformStorage using Context and SharedPreferences.
 */
class AndroidStorage(private val context: Context) : PlatformStorage {
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREFS_NAME = "roboyard_prefs"

        // Old SharedPreferences file used by LevelCompletionManager before KMP migration
        private const val OLD_LEVEL_COMPLETION_PREFS = "level_completion_prefs"

        @Volatile
        private var instance: AndroidStorage? = null

        @JvmStatic
        fun getInstance(context: Context): AndroidStorage {
            return instance ?: AndroidStorage(context.applicationContext).also {
                it.migrateOldPrefs(context.applicationContext)
                instance = it
            }
        }
    }

    /**
     * One-time migration: copy data from the old level_completion_prefs SharedPreferences file
     * to the unified roboyard_prefs file. Runs once, then deletes the old file.
     */
    private fun migrateOldPrefs(context: Context) {
        try {
            val oldPrefs = context.getSharedPreferences(OLD_LEVEL_COMPLETION_PREFS, Context.MODE_PRIVATE)
            if (!oldPrefs.contains("completion_data") && !oldPrefs.contains("last_played_level")) {
                return
            }

            Timber.d("[STORAGE_MIGRATION] Migrating data from $OLD_LEVEL_COMPLETION_PREFS to $PREFS_NAME")

            val editor = prefs.edit()
            val oldMap = oldPrefs.all
            for ((key, value) in oldMap) {
                when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Set<*> -> @Suppress("UNCHECKED_CAST") editor.putStringSet(key, value as Set<String>)
                }
            }
            editor.apply()

            // Delete the old prefs file to prevent re-migration
            oldPrefs.edit().clear().apply()
            Timber.d("[STORAGE_MIGRATION] Migration complete, old prefs cleared")
        } catch (e: Exception) {
            Timber.e(e, "[STORAGE_MIGRATION] Error migrating old prefs: %s", e.message)
        }
    }

    // Key-value storage
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

    // File I/O
    override fun readFile(fileName: String): String {
        return try {
            val file = context.getFileStreamPath(fileName)
            if (!file.exists()) return ""

            val input = FileInputStream(file)
            val reader = BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8))
            val buffer = StringBuilder()
            reader.forEachLine { buffer.append(it).append('\n') }
            reader.close()
            input.close()
            buffer.toString()
        } catch (e: Exception) {
            Timber.d("Exception readFile: %s", e.toString())
            ""
        }
    }

    override fun writeFile(fileName: String, content: String): Boolean {
        return try {
            val file = context.getFileStreamPath(fileName)
            // Create parent directories for subdirectory paths like "saves/save_0.dat"
            file.parentFile?.mkdirs()
            val output = FileOutputStream(file)
            val writer = OutputStreamWriter(output, StandardCharsets.UTF_8)
            writer.write(content)
            writer.flush()
            writer.close()
            Timber.d("[STORAGE] writeFile SUCCESS: $fileName -> ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Timber.d("[STORAGE] writeFile ERROR: $fileName - ${e.message}")
            e.printStackTrace()
            false
        }
    }

    override fun fileExists(fileName: String): Boolean {
        return try {
            context.getFileStreamPath(fileName).exists()
        } catch (e: Exception) {
            false
        }
    }

    override fun deleteFile(fileName: String): Boolean {
        return try {
            val file = context.getFileStreamPath(fileName)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    override fun getFilePath(fileName: String): String {
        return context.getFileStreamPath(fileName).absolutePath
    }

    override fun getFileTimestamp(fileName: String): Long? {
        return try {
            val file = context.getFileStreamPath(fileName)
            if (file.exists()) file.lastModified() else null
        } catch (e: Exception) {
            null
        }
    }

    override fun hasSavedGames(): Boolean {
        val savesDir = java.io.File(context.filesDir, "saves")
        return savesDir.exists() && savesDir.listFiles()?.isNotEmpty() == true
    }

    override fun listFiles(prefix: String, suffix: String): List<String> {
        val files = context.filesDir.listFiles() ?: return emptyList()
        return files.map { it.name }
            .filter { it.startsWith(prefix) && it.endsWith(suffix) }
            .sorted()
    }

    override fun listFilesInDir(dirName: String): List<String> {
        val dir = java.io.File(context.filesDir, dirName)
        return dir.list()?.toList() ?: emptyList()
    }

    override fun listAssetFiles(dirName: String): List<String> {
        return try {
            context.assets.list(dirName)?.toList() ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "listAssetFiles failed for dir: %s", dirName)
            emptyList()
        }
    }

    // Bitmap operations
    override fun readBitmap(fileName: String): Any? {
        return try {
            val file = context.getFileStreamPath(fileName)
            if (!file.exists()) return null
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            Timber.d("Exception readBitmap: %s", e.message)
            null
        }
    }

    override fun writeBitmap(fileName: String, bitmap: Any?): Boolean {
        return try {
            if (bitmap !is Bitmap) return false
            val file = context.getFileStreamPath(fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            true
        } catch (e: Exception) {
            Timber.d("Exception writeBitmap: %s", e.message)
            false
        }
    }
}
