package roboyard.eclabs.ui;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
     * Shows a toast message
     * @param message Message to display
     */
    protected void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Navigate to another screen using the Navigation component
     * @param actionId The navigation action ID from the nav graph
     */
    protected void navigateTo(int actionId) {
        Navigation.findNavController(requireView()).navigate(actionId);
    }
    
    /**
     * Navigate to another screen with arguments
     * @param directions NavDirections object with destination and arguments
     */
    protected void navigateTo(NavDirections directions) {
        Navigation.findNavController(requireView()).navigate(directions);
    }
    
    /**
     * Get the screen title for accessibility and UI
     * @return The screen title
     */
    public abstract String getScreenTitle();
}
