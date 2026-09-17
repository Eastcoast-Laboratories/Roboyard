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

    actual fun getAppVersionName(): String = roboyard.logic.core.Constants.APP_VERSION_NAME

    actual fun isPlayGamesEnabled(): Boolean = false
}
