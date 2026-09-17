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
    fun hasSavedGames(): Boolean

    /**
     * List files in the app storage root matching prefix/suffix
     * (e.g. "custom_level_", ".txt"). Returns file names only.
     */
    fun listFiles(prefix: String = "", suffix: String = ""): List<String> = emptyList()

    /** List file names inside a subdirectory of private storage (e.g. "saves"). */
    fun listFilesInDir(dirName: String): List<String> = emptyList()

    /**
     * List file names inside a bundled-asset directory (e.g. "Maps").
     * Used for level discovery like Android's context.assets.list("Maps").
     */
    fun listAssetFiles(dirName: String): List<String> = emptyList()

    /** Last-modified timestamp of a stored file in epoch millis, or null if unavailable. */
    fun getFileTimestamp(fileName: String): Long? = null

    // Bitmap operations (may be no-op on some platforms)
    fun readBitmap(fileName: String): Any?
    fun writeBitmap(fileName: String, bitmap: Any?): Boolean
}

/**
 * Factory function to get the platform-specific storage implementation.
 */
expect fun getPlatformStorage(): PlatformStorage
