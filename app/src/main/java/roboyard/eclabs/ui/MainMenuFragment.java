package roboyard.eclabs.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;

import roboyard.eclabs.Constants;
import roboyard.eclabs.R;
import timber.log.Timber;

/**
 * Main menu screen implemented as a Fragment with native Android UI components.
 * Replaces the canvas-based MainScreen.
 */
public class MainMenuFragment extends BaseGameFragment {
    
    private Button newRandomGameButton;
    private Button levelGameButton;
    private Button legacyLevelButton;
    private Button loadGameButton;
    private Button settingsButton;
    private Button helpButton;
    private Button modernUIButton; // New button for modern UI game
    // private Button exitButton; // Can be removed since we're not using it anymore
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_menu, container, false);
        
        // Set up UI elements
        newRandomGameButton = view.findViewById(R.id.new_game_button);
        // Set the correct text for Legacy Game button
        newRandomGameButton.setText(R.string.legacy_game);
        
        // Use existing level game button from XML layout
        levelGameButton = view.findViewById(R.id.level_game_button);
        legacyLevelButton = view.findViewById(R.id.legacy_level_button);
        loadGameButton = view.findViewById(R.id.load_game_button);
        settingsButton = view.findViewById(R.id.settings_button);
        helpButton = view.findViewById(R.id.help_button);
        modernUIButton = view.findViewById(R.id.modern_ui_button); // Initialize modern UI button
        
        // Set up button listeners with proper accessibility support
        setupButtons();
        
        return view;
    }
    
    /**
     * Set up button click listeners
     */
    private void setupButtons() {
        // New Random Game button - start a random game
        newRandomGameButton.setOnClickListener(v -> {
            Timber.d("MainMenuFragment: New Random Game button clicked");
            
            // Start a new random game
            Timber.d("MainMenuFragment: Calling gameStateManager.startNewGame()");
            gameStateManager.startNewGame();
            
            // Navigate to game screen
            Timber.d("MainMenuFragment: Navigating to game play screen with R.id.actionMainMenuToGamePlay");
            navigateTo(R.id.actionMainMenuToGamePlay);
            Timber.d("MainMenuFragment: Navigation to game play completed");
        });
        
        // Level Game button - go to level selection screen in modern UI
        levelGameButton.setOnClickListener(v -> {
            // Navigate to the level selection screen in modern UI
            Timber.d("MainMenuFragment: Level Game button clicked");
            
            try {
                // Create a new LevelSelectionFragment instance
                LevelSelectionFragment levelFragment = new LevelSelectionFragment();
                
                // Perform the fragment transaction
                requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, levelFragment)
                    .addToBackStack(null)
                    .commit();
                
                Timber.d("Navigation to level selection screen completed using fragment transaction");
            } catch (Exception e) {
                Timber.e(e, "Error navigating to level selection screen");
                Toast.makeText(requireContext(), "Cannot navigate to level selection: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        // Legacy Level button - go to legacy level selection screen
        legacyLevelButton.setOnClickListener(v -> {
            // Navigate to the legacy level selection screen
            Timber.d("MainMenuFragment: Legacy Level button clicked");
            Bundle args = new Bundle();
            args.putInt("levelScreen", Constants.SCREEN_LEVEL_BEGINNER); // Level selection screen 1
            
            // First call GameStateManager to perform any needed state setup
            gameStateManager.navigateToLevelScreen(Constants.SCREEN_LEVEL_BEGINNER);
            
            try {
                // Create a new GameCanvasFragment instance and pass the arguments
                GameCanvasFragment gameCanvasFragment = new GameCanvasFragment();
                gameCanvasFragment.setArguments(args);
                
                // Perform the fragment transaction
                requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, gameCanvasFragment)
                    .addToBackStack(null)
                    .commit();
                
                Timber.d("Navigation to legacy game screen completed using fragment transaction");
            } catch (Exception e) {
                Timber.e(e, "Error navigating to legacy game screen");
                Toast.makeText(requireContext(), "Cannot navigate to legacy game: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        // Load Game button - go to save game screen in load mode
        loadGameButton.setOnClickListener(v -> {
            try {
                // Create a new SaveGameFragment instance
                SaveGameFragment saveGameFragment = new SaveGameFragment();
                
                // Set the load mode argument
                Bundle args = new Bundle();
                args.putBoolean("isLoadMode", true);
                saveGameFragment.setArguments(args);
                
                // Perform the fragment transaction
                requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, saveGameFragment)
                    .addToBackStack(null)
                    .commit();
                
                Timber.d("Navigation to save game screen in load mode completed using fragment transaction");
            } catch (Exception e) {
                Timber.e(e, "Error navigating to save game screen");
                Toast.makeText(requireContext(), "Cannot navigate to save game screen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        // Settings button - go to settings screen
        settingsButton.setOnClickListener(v -> {
            try {
                // Create a new SettingsFragment instance
                SettingsFragment settingsFragment = new SettingsFragment();
                
                // Perform the fragment transaction
                requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, settingsFragment)
                    .addToBackStack(null)
                    .commit();
                
                Timber.d("Navigation to settings screen completed using fragment transaction");
            } catch (Exception e) {
                Timber.e(e, "Error navigating to settings screen");
                Toast.makeText(requireContext(), "Cannot navigate to settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        // Help button - go to help screen
        helpButton.setOnClickListener(v -> {
            try {
                // Create a new HelpFragment instance
                HelpFragment helpFragment = new HelpFragment();
                
                // Perform the fragment transaction
                requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, helpFragment)
                    .addToBackStack(null)
                    .commit();
                
                Timber.d("Navigation to help screen completed using fragment transaction");
            } catch (Exception e) {
                Timber.e(e, "Error navigating to help screen");
                Toast.makeText(requireContext(), "Cannot navigate to help: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        // Modern UI button - start a modern UI game
        modernUIButton.setOnClickListener(v -> {
            // Start a new modern UI game
            Timber.d("MainMenuFragment: Calling gameStateManager.startModernGame()");
            gameStateManager.startModernGame();
            
            try {
                // Create a new ModernGameFragment instance
                ModernGameFragment gameFragment = new ModernGameFragment();
                
                // Perform the fragment transaction
                requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, gameFragment)
                    .addToBackStack(null)
                    .commit();
                
                Timber.d("Navigation to modern game screen completed using fragment transaction");
            } catch (Exception e) {
                Timber.e(e, "Error navigating to modern game screen");
                Toast.makeText(requireContext(), "Cannot navigate to game: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        // Exit button - exit the app (commented out since we removed the exit button)
        /* 
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
        */
    }
    
    @Override
    public String getScreenTitle() {
        return getString(R.string.main_menu_title);
    }
}
