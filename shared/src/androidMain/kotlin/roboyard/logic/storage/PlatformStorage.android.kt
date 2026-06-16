package roboyard.logic.storage

import android.content.Context
import roboyard.logic.platform.AndroidStorage

/**
 * Android implementation of getPlatformStorage.
 * Note: This requires a Context, which should be passed from the Android app.
 */
actual fun getPlatformStorage(): PlatformStorage {
    throw IllegalStateException("AndroidStorage requires Context. Use AndroidStorage(context) directly or provide a singleton pattern.")
}
