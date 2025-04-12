package roboyard.eclabs;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

import roboyard.logic.core.Preferences;
import timber.log.Timber;

/**
 * Custom Application class for Roboyard app.
 * Initializes app-wide components like Timber logging.
 */
public class RoboyardApplication extends Application {
    
    private static Context appContext;
    
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
        
        // Initialize Timber for logging - always enable it for debugging
        // Plant a debug tree that includes line numbers and method names
        Timber.plant(new Timber.DebugTree());
        Timber.d("Timber initialized for logging");
        
        // Initialize the Preferences system at app startup
        Preferences.initialize(appContext);
        Timber.d("[PREFERENCES] Initialized at application startup");
        
        // Lokalisierung initialisieren
        updateAppContextLocale();
    }
}
