package roboyard.eclabs;

import android.app.Application;
import android.content.Context;

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
    }
}
