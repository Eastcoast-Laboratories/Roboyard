package roboyard.eclabs.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import roboyard.eclabs.R;
import timber.log.Timber;
import java.util.Locale;
import android.content.res.Resources;
import android.content.res.Configuration;

/**
 * Main menu screen implemented as a Fragment with modern Android UI components.
 */
public class MainMenuFragment extends BaseGameFragment {
    
    private Button newGameButton;
    private Button levelGameButton;
    private Button loadGameButton;
    private Button achievementsButton;
    private Button settingsButton;
    private Button helpButton;
    private Button levelEditorButton;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_menu, container, false);
        
        // Apply saved language settings
        applyLanguageSettings();
        
        // Set up UI elements - only use buttons
        newGameButton = view.findViewById(R.id.modern_ui_button);
        
        levelGameButton = view.findViewById(R.id.level_game_button);
        loadGameButton = view.findViewById(R.id.load_game_button);
        achievementsButton = view.findViewById(R.id.achievements_button);
        settingsButton = view.findViewById(R.id.settings_button);
        helpButton = view.findViewById(R.id.help_button);
        levelEditorButton = view.findViewById(R.id.level_editor_button);
        
        // Set up credits link
        TextView creditsLink = view.findViewById(R.id.credits_link);
        creditsLink.setPaintFlags(creditsLink.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        creditsLink.setOnClickListener(v -> {
            Timber.d("MainMenuFragment: Credits link clicked");
            // Create a new CreditsFragment instance
            CreditsFragment creditsFragment = new CreditsFragment();
            navigateToDirect(creditsFragment);
        });
        
        // Set up button listeners
        setupButtons();
        
        return view;
    }
    
    /**
     * Apply saved language settings from preferences
     */
    private void applyLanguageSettings() {
        try {
            // Get saved language setting
            String languageCode = roboyard.logic.core.Preferences.appLanguage;
            Timber.d("ROBOYARD_LANGUAGE: Loading saved language: %s", languageCode);
            
            // Apply language change
            Locale locale = new Locale(languageCode);
            Locale.setDefault(locale);
            
            Resources resources = requireContext().getResources();
            Configuration config = new Configuration(resources.getConfiguration());
            config.setLocale(locale);
            
            resources.updateConfiguration(config, resources.getDisplayMetrics());
        } catch (Exception e) {
            Timber.e(e, "ROBOYARD_LANGUAGE: Error loading language settings");
        }
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
            
            // Set the mode to load - set both parameters to ensure it works
            Bundle args = new Bundle();
            args.putString("mode", "load");
            args.putBoolean("saveMode", false);  // Explicitly set to false (load mode)
            saveGameFragment.setArguments(args);
            
            Timber.d("MainMenuFragment: Navigating to SaveGameFragment in LOAD mode");
            
            // Navigate to the save game fragment
            navigateToDirect(saveGameFragment);
        });
        
        // Achievements button - go to achievements screen
        achievementsButton.setOnClickListener(v -> {
            AchievementsFragment achievementsFragment = new AchievementsFragment();
            navigateToDirect(achievementsFragment);
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
        
        // Level Design Editor button - go to level design editor
        levelEditorButton.setOnClickListener(v -> {
            // Create the Level Design Editor fragment with a new level (ID 0)
            LevelDesignEditorFragment editorFragment = LevelDesignEditorFragment.newInstance(0);
            
            // Navigate to the editor fragment using the same pattern as other buttons
            navigateToDirect(editorFragment);
            
            Timber.d("MainMenuFragment: Opening Level Design Editor");
        });
    }
    
    @Override
    public String getScreenTitle() {
        return getString(R.string.main_menu_title);
    }
}
