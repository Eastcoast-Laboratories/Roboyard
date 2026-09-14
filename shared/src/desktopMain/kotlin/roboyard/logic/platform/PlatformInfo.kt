package roboyard.logic.platform

import java.util.Locale

/**
 * Desktop/JVM implementation of PlatformInfo.
 */
actual object PlatformInfo {
    actual fun getOsVersionString(): String {
        return "Desktop (JVM ${System.getProperty("java.version")})"
    }

    actual fun getSystemLanguageTag(): String? {
        return try {
            Locale.getDefault().toLanguageTag()
        } catch (e: Exception) {
            null
        }
    }

    actual fun getAppVersionName(): String = "unknown"

    actual fun isPlayGamesEnabled(): Boolean = false
}
