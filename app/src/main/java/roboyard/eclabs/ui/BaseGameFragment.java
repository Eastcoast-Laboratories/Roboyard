package roboyard.eclabs.ui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import roboyard.eclabs.R;
import roboyard.ui.components.GameStateManager;
import timber.log.Timber;

/**
 * Base fragment class for all game screens.
 * Provides common functionality and access to the GameStateManager.
 */
public abstract class BaseGameFragment extends Fragment {
    
    protected GameStateManager gameStateManager;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the GameStateManager from the activity
        gameStateManager = new ViewModelProvider(requireActivity()).get(GameStateManager.class);
    }
    
    /**
     * Navigate to another screen using NavDirections
     * @param directions NavDirections object containing navigation information
     */
    protected void navigateTo(NavDirections directions) {
        Navigation.findNavController(requireView()).navigate(directions);
    }
    
    /**
     * Navigate to another screen using a navigation action resource ID
     * This provides an alternative to NavDirections for simpler navigation needs
     * @param actionId Resource ID of the navigation action
     */
    protected void navigateTo(@IdRes int actionId) {
        Navigation.findNavController(requireView()).navigate(actionId);
    }
    
    /**
     * Navigate to another screen using direct fragment transaction
     * This method should be used instead of Navigation component when mixing navigation approaches
     * @param fragment The fragment to navigate to
     * @param addToBackStack Whether to add the transaction to the back stack
     * @param tag Optional tag for the fragment transaction
     */
    protected void navigateToDirect(Fragment fragment, boolean addToBackStack, String tag) {
        try {
            // Create the transaction
            androidx.fragment.app.FragmentTransaction transaction = requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment);
            
            // Add to back stack if requested
            if (addToBackStack) {
                transaction.addToBackStack(tag);
            }
            
            // Commit the transaction
            transaction.commit();
            
            // Log success
            Timber.d("Navigation to %s completed using fragment transaction", 
                    fragment.getClass().getSimpleName());
        } catch (Exception e) {
            // Log error
            Timber.e(e, "Error navigating to %s", fragment.getClass().getSimpleName());
        }
    }
    
    /**
     * Simplified version of navigateToDirect that adds to back stack with null tag
     * @param fragment The fragment to navigate to
     */
    protected void navigateToDirect(Fragment fragment) {
        navigateToDirect(fragment, true, null);
    }
    
    /**
     * Shows a toast message
     * @param message Message to display
     */
    protected void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Get the screen title for accessibility and UI
     * @return The screen title
     */
    public abstract String getScreenTitle();
}
