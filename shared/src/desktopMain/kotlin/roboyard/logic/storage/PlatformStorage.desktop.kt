package roboyard.logic.storage

import roboyard.logic.platform.DesktopStorage

/**
 * Desktop implementation of getPlatformStorage.
 */
actual fun getPlatformStorage(): PlatformStorage {
    return DesktopStorage()
}
