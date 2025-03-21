package roboyard.eclabs.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import roboyard.eclabs.GameManager;
import roboyard.eclabs.R;

/**
 * Main activity for the game, hosts the fragment-based UI.
 * Acts as the container for all game fragments and provides access to the GameStateManager.
 */
public class MainActivity extends AppCompatActivity {
    
    private GameStateManager gameStateManager;
    private NavController navController;

    // Forward to the regular MainActivity's static methods
    public static int getBoardWidth() {
        return roboyard.eclabs.MainActivity.getBoardWidth();
    }

    public static int getBoardHeight() {
        return roboyard.eclabs.MainActivity.getBoardHeight();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Migrate preferences from old implementation to new
        PreferencesMigrator.migratePreferences(this);
        
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
}
