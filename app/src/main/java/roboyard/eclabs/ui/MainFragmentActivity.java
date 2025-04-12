package roboyard.eclabs.ui;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import roboyard.eclabs.GameManager;
import roboyard.eclabs.R;
import roboyard.logic.core.Preferences;
import roboyard.ui.components.GameStateManager;
import timber.log.Timber;

import java.util.Locale;

/**
 * Main activity for the game, hosts the fragment-based UI.
 * Acts as the container for all game fragments and provides access to the GameStateManager.
 */
public class MainFragmentActivity extends AppCompatActivity {
    
    private GameStateManager gameStateManager;
    private NavController navController;

    // Forward to the regular MainActivity's static methods
    public static int getBoardWidth() {
        int width = roboyard.ui.activities.MainActivity.getBoardWidth();
        Timber.d("[BOARD_SIZE_DEBUG] UI MainActivity.getBoardWidth() called, returning: %d", width);
        return width;
    }

    public static int getBoardHeight() {
        int height = roboyard.ui.activities.MainActivity.getBoardHeight();
        Timber.d("[BOARD_SIZE_DEBUG] UI MainActivity.getBoardHeight() called, returning: %d", height);
        return height;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize the static Preferences at app startup
        roboyard.logic.core.Preferences.initialize(getApplicationContext());
        Timber.d("[PREFERENCES] Initialized with robotCount=%d, targetColors=%d", 
                roboyard.logic.core.Preferences.robotCount, 
                roboyard.logic.core.Preferences.targetColors);
        
        // Log the board size at startup
        Timber.d("[BOARD_SIZE_DEBUG] UI MainActivity onCreate - Current board size: %dx%d", 
                 getBoardWidth(), getBoardHeight());
        
        // Initialize the GameStateManager as a ViewModel
        gameStateManager = new ViewModelProvider(this).get(GameStateManager.class);
        
        // Set up the Navigation controller
        NavHostFragment navHostFragment = 
            (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }
        
        // Set up accessibility services
        setupAccessibility();
        
        applyLanguageSettings();
    }
    
    /**
     * Configure accessibility features
     */
    private void setupAccessibility() {
        // Set content descriptions on key elements
        // This is in addition to the content descriptions set in XML and fragment code
    }
    
    /**
     * Get the game state manager
     * @return GameStateManager instance
     */
    public GameStateManager getGameStateManager() {
        return gameStateManager;
    }

    /**
     * LEGACY COMPATIBILITY METHOD
     * This method is provided for backward compatibility with code that still uses GameManager
     * In the new architecture, we don't use GameManager anymore
     * @return null - GameManager is no longer used
     * @deprecated Use getGameStateManager() instead
     */
    @Deprecated
    public GameManager getGameManager() {
        // Return null as we don't use GameManager anymore
        // Legacy code should be updated to use GameStateManager
        return null;
    }
    
    private void applyLanguageSettings() {
        try {
            // Get saved language setting
            String languageCode = roboyard.logic.core.Preferences.appLanguage;
            Timber.d("ROBOYARD_LANGUAGE: Setting app language on application level: %s", languageCode);
            
            if (languageCode != null && !languageCode.isEmpty()) {
                // Apply language change
                Locale locale = new Locale(languageCode);
                Locale.setDefault(locale);
                
                Resources resources = getResources();
                Configuration config = new Configuration(resources.getConfiguration());
                config.setLocale(locale); // Verwende die neuere Methode statt config.locale = locale
                
                resources.updateConfiguration(config, resources.getDisplayMetrics());
                
                Timber.d("ROBOYARD_LANGUAGE: Successfully applied language %s at application level", languageCode);
            }
        } catch (Exception e) {
            Timber.e(e, "ROBOYARD_LANGUAGE: Error applying language settings at application level");
        }
    }
}
