package roboyard.eclabs;

import android.app.Application;

import timber.log.Timber;

/**
 * Custom Application class for Roboyard app.
 * Initializes app-wide components like Timber logging.
 */
public class RoboyardApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Timber for logging - always enable it for debugging
        // Plant a debug tree that includes line numbers and method names
        Timber.plant(new Timber.DebugTree());
        Timber.d("Timber initialized for logging");
    }
}
