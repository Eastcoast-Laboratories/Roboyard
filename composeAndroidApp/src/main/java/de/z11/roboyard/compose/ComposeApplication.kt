package de.z11.roboyard.compose

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.view.accessibility.AccessibilityManager
import roboyard.logic.audio.AndroidSoundManager
import roboyard.logic.audio.initSoundManager
import roboyard.logic.core.Preferences
import roboyard.logic.platform.PlatformInfo
import roboyard.logic.platform.appContextProvider
import roboyard.logic.storage.initPlatformStorage
import roboyard.logic.ui.setStringProvider
import roboyard.platform.AndroidStorage
import roboyard.platform.AndroidStringProvider
import timber.log.Timber
import java.util.Locale

/**
 * Application class for the Compose Multiplatform Android app.
 * Replicates the platform wiring done by the Android app's RoboyardApplication.
 */
class ComposeApplication : Application() {

    private lateinit var appContext: Context

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        appContextProvider = { appContext }

        Timber.plant(Timber.DebugTree())

        val storage = AndroidStorage.getInstance(appContext)
        Preferences.storageProvider = { storage }
        initPlatformStorage(storage)

        setStringProvider(AndroidStringProvider.getInstance(appContext))

        PlatformInfo.init(appContext, BuildConfig.VERSION_NAME, false)

        Preferences.initialize(storage, isScreenReaderActive(appContext))
        initSoundManager(AndroidSoundManager(appContext))

        if (isFirstLaunch()) {
            setAppLanguageToDeviceLocale()
        }
        updateAppContextLocale()
    }

    private fun isScreenReaderActive(context: Context): Boolean {
        return try {
            val manager = context.getSystemService(ACCESSIBILITY_SERVICE) as? AccessibilityManager
            manager != null && manager.isEnabled && manager.isTouchExplorationEnabled
        } catch (e: Exception) {
            false
        }
    }

    private fun isFirstLaunch(): Boolean {
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val firstLaunch = prefs.getBoolean("is_first_launch", true)
        if (firstLaunch) {
            prefs.edit().putBoolean("is_first_launch", false).apply()
        }
        return firstLaunch
    }

    private fun setAppLanguageToDeviceLocale() {
        val languageCode = Locale.getDefault().language
        val supported = setOf("en", "de", "fr", "es", "zh", "ko")
        val language = if (languageCode in supported) languageCode else "en"
        Preferences.setAppLanguage(language)
        Timber.d("[LOCALE] Setting initial app language to match device: %s", language)
    }

    private fun updateAppContextLocale() {
        val language = Preferences.appLanguage ?: return
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(appContext.resources.configuration)
        config.setLocale(locale)
        appContext = appContext.createConfigurationContext(config)
    }
}
