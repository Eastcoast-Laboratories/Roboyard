package roboyard.eclabs.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import roboyard.eclabs.R;
import timber.log.Timber;

/**
 * Main menu screen implemented as a Fragment with modern Android UI components.
 */
public class MainMenuFragment extends BaseGameFragment {
    
    private Button newGameButton;
    private Button levelGameButton;
    private Button loadGameButton;
    private Button settingsButton;
    private Button helpButton;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_menu, container, false);
        
        // Set up UI elements - only use buttons
        newGameButton = view.findViewById(R.id.modern_ui_button);
        newGameButton.setText("New Game");
        
        levelGameButton = view.findViewById(R.id.level_game_button);
        loadGameButton = view.findViewById(R.id.load_game_button);
        settingsButton = view.findViewById(R.id.settings_button);
        helpButton = view.findViewById(R.id.help_button);
        
        // Set up button listeners
        setupButtons();
        
        return view;
    }
    
    /**
     * Set up button click listeners
     */
    private void setupButtons() {
        // New Game button - start a game
        newGameButton.setOnClickListener(v -> {
            // Start a new game
            Timber.d("MainMenuFragment: Calling gameStateManager.startModernGame()");
            gameStateManager.startModernGame();
            
            // Create a new ModernGameFragment instance
            ModernGameFragment gameFragment = new ModernGameFragment();
            navigateToDirect(gameFragment);
        });
        
        // Level Game button - go to level selection screen
        levelGameButton.setOnClickListener(v -> {
            LevelSelectionFragment levelSelectionFragment = new LevelSelectionFragment();
            navigateToDirect(levelSelectionFragment);
        });
        
        // Load Game button - go to save game screen in load mode
        loadGameButton.setOnClickListener(v -> {
            // Create a new SaveGameFragment instance
            SaveGameFragment saveGameFragment = new SaveGameFragment();
            
            // Set the mode to load
            Bundle args = new Bundle();
            args.putString("mode", "load");
            saveGameFragment.setArguments(args);
            
            // Navigate to the save game fragment
            navigateToDirect(saveGameFragment);
        });
        
        // Settings button - go to settings screen
        settingsButton.setOnClickListener(v -> {
            // Create a new SettingsFragment instance
            SettingsFragment settingsFragment = new SettingsFragment();
            navigateToDirect(settingsFragment);
        });
        
        // Help button - go to help screen
        helpButton.setOnClickListener(v -> {
            // Create a new HelpFragment instance
            HelpFragment helpFragment = new HelpFragment();
            navigateToDirect(helpFragment);
        });
    }
    
    @Override
    public String getScreenTitle() {
        return getString(R.string.main_menu_title);
    }
}
