package roboyard.logic.platform

/**
 * Platform-specific information for KMP compatibility.
 * Provides OS version, language tag, and app configuration values.
 */
expect object PlatformInfo {
    /**
     * Get the OS version string (e.g., "Android 14 (API 34)", "iOS 17.0", "Desktop").
     */
    fun getOsVersionString(): String

    /**
     * Get the system language tag (e.g., "en-US", "de-DE").
     * Returns null if the language tag cannot be determined.
     */
    fun getSystemLanguageTag(): String?

    /**
     * Get the app version name (e.g., "4.0.1").
     */
    fun getAppVersionName(): String

    /**
     * Check if Google Play Games integration is enabled.
     */
    fun isPlayGamesEnabled(): Boolean
}
