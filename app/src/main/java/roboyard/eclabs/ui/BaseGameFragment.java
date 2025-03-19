package roboyard.eclabs.ui;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

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
