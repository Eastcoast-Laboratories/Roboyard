package roboyard.eclabs;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Bundle;

import java.util.Locale;

import roboyard.logic.core.Preferences;
import timber.log.Timber;

/**
 * Custom Application class for Roboyard app.
 * Initializes app-wide components like Timber logging.
 */
public class RoboyardApplication extends Application implements Application.ActivityLifecycleCallbacks {
    
    /**
     * Custom Timber tree that filters out unwanted log messages.
     * 
     * This filter reduces noise from performance statistics like app_time_stats messages
     * but doesn't eliminate them entirely. The filtering works by checking message content
     * rather than log tags or sources, so it catches most repetitive performance logs
     * while still allowing some through for debugging purposes.
     * 
     * Why not all messages are filtered:
     * - Some app_time_stats messages may come from different sources with slightly different formats
     * - Android system logs and third-party libraries may use variations of the pattern
     * - The filter is intentionally conservative to avoid blocking legitimate debug information
     * - This approach reduces log spam while maintaining debugging capability
     */
    private static class FilteredDebugTree extends Timber.DebugTree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            // Filter out app_time_stats messages
            if (message != null && message.contains("app_time_stats:")) {
                return; // Don't log these messages
            }
            
            // Filter out other performance stats that might be noisy
            if (message != null && (message.contains("avg=") && message.contains("min=") && message.contains("max=") && message.contains("count="))) {
                return; // Don't log performance stats
            }
            
            // Log everything else normally
            super.log(priority, tag, message, t);
        }
    }
    
    private static Context appContext;
    private int startedActivityCount = 0;
    
    /**
     * Get the application context
     * @return Application context
     */
    public static Context getAppContext() {
        return appContext;
    }

    
    /**
     * Updates the app context with the current locale from preferences
     * This should be called whenever the locale changes
     */
    public static void updateAppContextLocale() {
        // Wenn der appContext noch nicht existiert, abbrechen
        if (appContext == null) return;
        
        // Sprache aus den Pru00e4ferenzen laden
        String language = Preferences.appLanguage;
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        
        // Konfiguration aktualisieren
        Resources resources = appContext.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        config.setLocale(locale);
        
        // Neuen Context erstellen
        appContext = appContext.createConfigurationContext(config);
        
        Timber.d("[LOCALE] Application context locale updated to %s", locale.getLanguage());
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Store the application context for global access
        appContext = getApplicationContext();

        registerActivityLifecycleCallbacks(this);
        
        // Initialize Timber for logging - always enable it for debugging
        Timber.plant(new FilteredDebugTree());

        // Initialize the Preferences system at app startup
        Preferences.initialize(appContext);
        Timber.d("Preferences initialized");
        
        // Set app language to match device locale on first launch
        if (isFirstLaunch()) {
            setAppLanguageToDeviceLocale();
        }
        
        // Set locale based on preferences
        updateAppContextLocale();
        
        // Initialize other components
        Timber.d("Application created");
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        // No-op
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (startedActivityCount == 0) {
            Timber.d("[STREAK_POPUP] App entered foreground");
        }
        startedActivityCount++;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        // No-op
    }

    @Override
    public void onActivityPaused(Activity activity) {
        // No-op
    }

    @Override
    public void onActivityStopped(Activity activity) {
        startedActivityCount = Math.max(0, startedActivityCount - 1);
        if (startedActivityCount == 0) {
            Timber.d("[STREAK_POPUP] App moved to background");
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        // No-op
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        // No-op
    }
    
    /**
     * Determines if this is the first app launch
     * @return true if first launch, false otherwise
     */
    private boolean isFirstLaunch() {
        SharedPreferences prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean("is_first_launch", true);
        
        if (isFirstLaunch) {
            // Update the flag for future launches
            prefs.edit().putBoolean("is_first_launch", false).apply();
            return true;
        }
        
        return false;
    }
    
    /**
     * Sets the app language to match the device locale
     */
    private void setAppLanguageToDeviceLocale() {
        // Get the device's current locale
        Locale deviceLocale = Locale.getDefault();
        String languageCode = deviceLocale.getLanguage();
        
        // Map the device language to supported app languages
        // Only set if the device language is supported
        if (languageCode.equals("en") || 
            languageCode.equals("de") || 
            languageCode.equals("fr") ||
            languageCode.equals("es") ||
            languageCode.equals("zh") ||
            languageCode.equals("ko")) {
            
            // Update the app language preference
            Preferences.appLanguage = languageCode;
            Timber.d("[LOCALE] Setting initial app language to match device: %s", languageCode);
        } else {
            // Default to English if language is not supported
            Preferences.appLanguage = "en";
            Timber.d("[LOCALE] Device language not supported, defaulting to English");
        }
    }
}
