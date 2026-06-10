package roboyard.logic.platform

import roboyard.logic.storage.PlatformStorage

/**
 * iOS placeholder implementation of PlatformStorage.
 * Uses NSUserDefaults under the hood (to be implemented with kotlin.native).
 * TODO: Implement with platform.Foundation.NSUserDefaults for production iOS.
 */
class IosStorage : PlatformStorage {
    private val store = mutableMapOf<String, Any?>()

    override fun getString(key: String, defaultValue: String?): String? =
        store[key] as? String ?: defaultValue

    override fun putString(key: String, value: String) { store[key] = value }

    override fun getInt(key: String, defaultValue: Int): Int =
        (store[key] as? Int) ?: defaultValue

    override fun putInt(key: String, value: Int) { store[key] = value }

    override fun getLong(key: String, defaultValue: Long): Long =
        (store[key] as? Long) ?: defaultValue

    override fun putLong(key: String, value: Long) { store[key] = value }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        (store[key] as? Boolean) ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) { store[key] = value }

    override fun remove(key: String) { store.remove(key) }

    override fun clear() { store.clear() }
}
