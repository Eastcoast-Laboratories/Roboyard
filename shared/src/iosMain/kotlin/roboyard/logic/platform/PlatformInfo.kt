package roboyard.logic.platform

import platform.Foundation.NSLocale
import platform.Foundation.NSBundle
import platform.Foundation.preferredLanguages
import platform.Foundation.systemLocale

/**
 * iOS implementation of PlatformInfo.
 */
actual object PlatformInfo {
    actual fun getOsVersionString(): String {
        return "iOS"
    }

    actual fun getSystemLanguageTag(): String? {
        return try {
            val languages = NSLocale.preferredLanguages
            if (languages != null && languages.isNotEmpty()) {
                languages.firstOrNull() as? String
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    actual fun getAppVersionName(): String {
        return try {
            NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    actual fun isPlayGamesEnabled(): Boolean = false
}
