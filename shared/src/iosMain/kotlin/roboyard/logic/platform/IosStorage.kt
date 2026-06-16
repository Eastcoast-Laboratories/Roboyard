package roboyard.logic.platform

import platform.Foundation.NSUserDefaults
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.stringWithString
import roboyard.logic.storage.PlatformStorage

/**
 * iOS implementation of PlatformStorage using NSUserDefaults and NSFileManager.
 */
class IosStorage : PlatformStorage {
    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val fileManager = NSFileManager.defaultManager

    // Key-value storage using NSUserDefaults
    override fun getString(key: String, defaultValue: String?): String? {
        val value = userDefaults.stringForKey(key)
        return value ?: defaultValue
    }

    override fun putString(key: String, value: String) {
        userDefaults.setObject(value, key)
        userDefaults.synchronize()
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return userDefaults.integerForKey(key).toInt()
    }

    override fun putInt(key: String, value: Int) {
        userDefaults.setInteger(value.toLong(), key)
        userDefaults.synchronize()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return userDefaults.integerForKey(key)
    }

    override fun putLong(key: String, value: Long) {
        userDefaults.setInteger(value, key)
        userDefaults.synchronize()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return userDefaults.boolForKey(key)
    }

    override fun putBoolean(key: String, value: Boolean) {
        userDefaults.setBool(value, key)
        userDefaults.synchronize()
    }

    override fun remove(key: String) {
        userDefaults.removeObjectForKey(key)
        userDefaults.synchronize()
    }

    override fun clear() {
        val domain = userDefaults.persistentDomainForName("app.roboyard.shared")
        userDefaults.removePersistentDomainForName("app.roboyard.shared")
        userDefaults.synchronize()
    }

    // File I/O operations using NSFileManager
    override fun readFile(fileName: String): String {
        val filePath = getFilePath(fileName)
        val content = fileManager.contentsAtPath(filePath)
        return content?.toByteArray()?.decodeToString() ?: ""
    }

    override fun writeFile(fileName: String, content: String): Boolean {
        val filePath = getFilePath(fileName)
        return content.encodeToByteArray().let { data ->
            fileManager.createFileAtPath(filePath, data, null)
        }
    }

    override fun fileExists(fileName: String): Boolean {
        val filePath = getFilePath(fileName)
        return fileManager.fileExistsAtPath(filePath)
    }

    override fun deleteFile(fileName: String): Boolean {
        val filePath = getFilePath(fileName)
        return fileManager.removeItemAtPath(filePath, null)
    }

    override fun getFilePath(fileName: String): String {
        val urls = fileManager.URLsForDirectory(
            NSDocumentDirectory,
            NSUserDomainMask
        )
        val documentsDirectory = urls.firstOrNull() as? NSURL
        return documentsDirectory?.path + "/$fileName"
    }

    // Bitmap operations (placeholder for iOS)
    override fun readBitmap(fileName: String): Any? {
        // TODO: Implement UIImage loading when needed
        return null
    }

    override fun writeBitmap(fileName: String, bitmap: Any?): Boolean {
        // TODO: Implement UIImage saving when needed
        return false
    }
}
