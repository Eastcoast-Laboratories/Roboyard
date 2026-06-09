package roboyard.logic.storage

/**
 * Platform-agnostic storage interface for KMP compatibility.
 * Abstracts Android-specific Context/SharedPreferences.
 */
interface PlatformStorage {
    // Key-value storage (like SharedPreferences)
    fun getString(key: String, defaultValue: String? = null): String?
    fun putString(key: String, value: String)
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun putInt(key: String, value: Int)
    fun getLong(key: String, defaultValue: Long = 0L): Long
    fun putLong(key: String, value: Long)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun remove(key: String)
    fun clear()

    // File I/O operations
    fun readFile(fileName: String): String
    fun writeFile(fileName: String, content: String): Boolean
    fun fileExists(fileName: String): Boolean
    fun deleteFile(fileName: String): Boolean
    fun getFilePath(fileName: String): String

    // Bitmap operations (may be no-op on some platforms)
    fun readBitmap(fileName: String): Any?
    fun writeBitmap(fileName: String, bitmap: Any?): Boolean
}
