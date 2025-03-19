package roboyard.eclabs.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import roboyard.eclabs.R;

/**
 * Main activity for the game, hosts the fragment-based UI.
 * Acts as the container for all game fragments and provides access to the GameStateManager.
 */
public class MainActivity extends AppCompatActivity {
    
    private GameStateManager gameStateManager;
    private NavController navController;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
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
}
