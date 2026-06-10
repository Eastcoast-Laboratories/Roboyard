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

        @Volatile
        private var instance: AndroidStorage? = null

        @JvmStatic
        fun getInstance(context: Context): AndroidStorage {
            return instance ?: AndroidStorage(context.applicationContext).also {
                instance = it
            }
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
            val output = FileOutputStream(context.getFileStreamPath(fileName))
            val writer = OutputStreamWriter(output, StandardCharsets.UTF_8)
            writer.write(content)
            writer.flush()
            writer.close()
            true
        } catch (e: Exception) {
            Timber.d("Exception writeFile: %s", e.message)
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
