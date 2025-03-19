package roboyard.eclabs.ui.mock;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import roboyard.eclabs.ui.GameStateManager;

/**
 * Mock MainActivity for testing fragments without needing the full application context.
 * Provides minimal implementation to host fragments for testing purposes.
 */
public class MockMainActivity extends FragmentActivity {

    private MockGameStateManager gameStateManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create a simple layout with just a fragment container
        android.widget.FrameLayout root = new android.widget.FrameLayout(this);
        root.setId(android.R.id.content);
        setContentView(root);
        
        // Initialize the game state manager
        gameStateManager = new MockGameStateManager();
    }
    
    /**
     * Get the mock game state manager for testing
     */
    public GameStateManager getGameStateManager() {
        return gameStateManager;
    }
    
    /**
     * Load a fragment for testing
     * @param fragment The fragment to test
     */
    public void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit();
        fragmentManager.executePendingTransactions();
    }

    /**
     * Navigate between fragments (simulated navigation)
     * @param fragment The fragment to navigate to
     */
    public void navigateTo(Fragment fragment) {
        loadFragment(fragment);
    }
}
