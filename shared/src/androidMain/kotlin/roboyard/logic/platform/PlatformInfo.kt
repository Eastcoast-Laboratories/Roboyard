package roboyard.logic.platform

import android.os.Build
import android.content.Context

/**
 * Android implementation of PlatformInfo.
 * Uses Context to obtain app version and package info at runtime,
 * avoiding a dependency on the app module's BuildConfig.
 */
actual object PlatformInfo {
    private var appContext: Context? = null
    private var cachedVersionName: String? = null
    private var cachedPlayGamesEnabled: Boolean? = null

    /**
     * Initialize PlatformInfo with the app Context.
     * Called by the platform factory at app start.
     */
    fun init(context: Context, versionName: String, playGamesEnabled: Boolean) {
        appContext = context.applicationContext
        cachedVersionName = versionName
        cachedPlayGamesEnabled = playGamesEnabled
    }

    actual fun getOsVersionString(): String {
        return Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")"
    }

    actual fun getSystemLanguageTag(): String? {
        return try {
            val config = android.content.res.Resources.getSystem().configuration
            val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val locales = config.locales
                if (locales.isEmpty()) return null
                locales[0]
            } else {
                @Suppress("DEPRECATION")
                config.locale
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                locale.toLanguageTag()
            } else {
                val lang = locale.language
                val country = locale.country
                if (country.isNullOrEmpty()) lang else "$lang-$country"
            }
        } catch (e: Exception) {
            null
        }
    }

    actual fun getAppVersionName(): String = cachedVersionName ?: "unknown"

    actual fun isPlayGamesEnabled(): Boolean = cachedPlayGamesEnabled ?: false
}
