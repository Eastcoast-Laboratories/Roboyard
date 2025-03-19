package roboyard.eclabs.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;

import roboyard.eclabs.R;

/**
 * Main menu screen implemented as a Fragment with native Android UI components.
 * Replaces the canvas-based MainScreen.
 */
public class MainMenuFragment extends BaseGameFragment {
    
    private Button newGameButton;
    private Button loadGameButton;
    private Button settingsButton;
    private Button helpButton;
    private Button exitButton;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_menu, container, false);
        
        // Set up UI elements
        newGameButton = view.findViewById(R.id.new_game_button);
        loadGameButton = view.findViewById(R.id.load_game_button);
        settingsButton = view.findViewById(R.id.settings_button);
        helpButton = view.findViewById(R.id.help_button);
        exitButton = view.findViewById(R.id.exit_button);
        
        // Set up button listeners with proper accessibility support
        setupButtons();
        
        return view;
    }
    
    /**
     * Set up button click listeners
     */
    private void setupButtons() {
        // New Game button - start a random game
        newGameButton.setOnClickListener(v -> {
            // Start a new random game
            gameStateManager.startNewGame();
            
            // Navigate to game screen
            navigateTo(R.id.actionMainMenuToGamePlay);
        });
        
        // Load Game button - go to save game screen in load mode
        loadGameButton.setOnClickListener(v -> {
            // Navigate to save screen in load mode using action ID instead of NavDirections
            // to avoid the boolean parameter issue
            navigateTo(R.id.actionMainMenuToSaveGame);
        });
        
        // Settings button - go to settings screen
        settingsButton.setOnClickListener(v -> {
            // Navigate to settings screen
            navigateTo(R.id.actionMainMenuToSettings);
        });
        
        // Help button - go to help screen
        helpButton.setOnClickListener(v -> {
            // Navigate to help screen
            navigateTo(R.id.actionMainMenuToHelp);
        });
        
        // Exit button - exit the app
        exitButton.setOnClickListener(v -> {
            // Show confirmation dialog
            new AlertDialog.Builder(requireContext())
                .setTitle(R.string.exit_confirm_title)
                .setMessage(R.string.exit_confirm_message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    requireActivity().finish();
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
        });
    }
    
    @Override
    public String getScreenTitle() {
        return getString(R.string.main_menu_title);
    }
}
