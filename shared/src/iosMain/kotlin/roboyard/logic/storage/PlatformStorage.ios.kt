package roboyard.logic.storage

import roboyard.logic.platform.IosStorage

/**
 * iOS implementation of getPlatformStorage backed by NSUserDefaults/NSFileManager.
 */
actual fun getPlatformStorage(): PlatformStorage {
    return IosStorage()
}
