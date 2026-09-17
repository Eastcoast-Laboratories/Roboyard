package roboyard.logic.storage

import android.content.Context

private var registeredStorage: PlatformStorage? = null

/**
 * Register the app's PlatformStorage so common code can access it via
 * getPlatformStorage() without needing a Context. Must be called once during
 * Application.onCreate (see RoboyardApplication).
 */
fun initPlatformStorage(storage: PlatformStorage) {
    registeredStorage = storage
}

/**
 * Android implementation of getPlatformStorage.
 * Returns the storage registered via initPlatformStorage(). Throws if the app
 * did not register a storage — this is an initialization bug, not a missing
 * file, so it must fail loudly.
 */
actual fun getPlatformStorage(): PlatformStorage {
    return registeredStorage
        ?: throw IllegalStateException(
            "No PlatformStorage registered. Call initPlatformStorage(AndroidStorage.getInstance(context)) during Application.onCreate."
        )
}
