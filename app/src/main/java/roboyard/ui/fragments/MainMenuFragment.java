package roboyard.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import roboyard.eclabs.R;
import roboyard.ui.components.RoboyardApiClient;
import roboyard.ui.achievements.Achievement;
import roboyard.ui.achievements.AchievementCategory;
import roboyard.ui.achievements.AchievementManager;
import roboyard.ui.achievements.AchievementPopup;
import roboyard.ui.achievements.StreakManager;
import timber.log.Timber;
import java.util.Locale;
import android.content.res.Resources;
import android.content.res.Configuration;
import android.content.Intent;
import android.net.Uri;
import roboyard.ui.components.LoginDialogHelper;

/**
 * Main menu screen
 */
public class MainMenuFragment extends BaseGameFragment {
    
    private Button newGameButton;
    private Button levelGameButton;
    private Button loadGameButton;
    private Button levelEditorButton;
    private ImageButton helpIconButton;
    private ImageButton settingsIconButton;
    private ImageButton achievementsIconButton;
    private Button creditsButton;
    private Button userProfileButton;
    private ViewGroup rootViewGroup;
    private AchievementPopup achievementPopup;
    private Integer prevNewGameVisibility;
    private Integer prevLevelGameVisibility;
    private Integer prevLoadGameVisibility;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_menu, container, false);
        
        // Apply saved language settings
        applyLanguageSettings();
        
        // Set up UI elements - only use buttons
        newGameButton = view.findViewById(R.id.ui_button);
        
        levelGameButton = view.findViewById(R.id.level_game_button);
        loadGameButton = view.findViewById(R.id.load_game_button);
        levelEditorButton = view.findViewById(R.id.level_editor_button);
        
        // Set up icon buttons
        helpIconButton = view.findViewById(R.id.help_icon_button);
        settingsIconButton = view.findViewById(R.id.settings_icon_button);
        achievementsIconButton = view.findViewById(R.id.achievements_icon_button);
        creditsButton = view.findViewById(R.id.credits_button);
        userProfileButton = view.findViewById(R.id.user_profile_button);

        if (view instanceof ViewGroup) {
            rootViewGroup = (ViewGroup) view;
            achievementPopup = new AchievementPopup(requireContext(), rootViewGroup);
            achievementPopup.setPopupVisibilityListener((isVisible, isStreakPopup) -> {
                if (!isStreakPopup) {
                    return;
                }
                if (isVisible) {
                    prevNewGameVisibility = newGameButton.getVisibility();
                    prevLevelGameVisibility = levelGameButton.getVisibility();
                    prevLoadGameVisibility = loadGameButton.getVisibility();

                    newGameButton.setVisibility(View.INVISIBLE);
                    levelGameButton.setVisibility(View.INVISIBLE);
                    loadGameButton.setVisibility(View.INVISIBLE);
                    Timber.d("[STREAK_POPUP] Hid main menu navigation buttons while streak popup is visible");
                } else {
                    if (prevNewGameVisibility != null) {
                        newGameButton.setVisibility(prevNewGameVisibility);
                    }
                    if (prevLevelGameVisibility != null) {
                        levelGameButton.setVisibility(prevLevelGameVisibility);
                    }
                    if (prevLoadGameVisibility != null) {
                        loadGameButton.setVisibility(prevLoadGameVisibility);
                    }
                    Timber.d("[STREAK_POPUP] Restored main menu navigation buttons after streak popup hidden");
                }
            });
        } else {
            rootViewGroup = null;
            achievementPopup = null;
            Timber.w("[MAIN_MENU] Root view is not a ViewGroup; achievement popups disabled");
        }
        
        // Set up button listeners
        setupButtons();
        
        // Update user profile button UI
        updateUserProfileButton();
        
        // Apply outline effect to title
        applyTitleOutline(view);
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Stop any running solver when returning to main menu (via back button or navigation)
        roboyard.ui.util.SolverManager solverManager = roboyard.ui.util.SolverManager.getInstance();
        solverManager.cancelSolver();
        solverManager.resetInitialization();
        
        // Also cancel solver via GameStateManager to stop background threads
        // AND stop map regeneration to prevent infinite solver restarts
        if (gameStateManager != null) {
            gameStateManager.cancelSolver();
            gameStateManager.stopRegeneration();
        }
        
        Timber.d("[SOLVER] Cancelled all solvers and stopped regeneration when entering main menu");
    }

    @Override
    public void onResume() {
        super.onResume();
        AchievementManager.getInstance(requireContext()).setUnlockListener(achievement -> {
            if (achievementPopup == null) {
                Timber.w("[ACHIEVEMENT_POPUP] Root view missing, cannot show achievement %s", achievement.getId());
                return;
            }
            achievementPopup.show(achievement);
            Timber.d("[ACHIEVEMENT_POPUP] Main menu displayed achievement: %s", achievement.getId());
        });
        maybeShowDailyStreakPopup();
    }

    @Override
    public void onPause() {
        AchievementManager.getInstance(requireContext()).setUnlockListener(null);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (achievementPopup != null) {
            achievementPopup.setPopupVisibilityListener(null);
        }
        achievementPopup = null;
        rootViewGroup = null;
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
    
    private boolean showDailyStreakPopup() {
        if (achievementPopup == null || rootViewGroup == null) {
            Timber.w("[STREAK_POPUP] Root view unavailable, skipping streak popup");
            return false;
        }
        int streakDays = StreakManager.getInstance(requireContext()).getCurrentStreak();
        
        // If streak is 0 (first app launch), treat it as day 1
        // because recordDailyLogin() will set it to 1 when called
        if (streakDays == 0) {
            streakDays = 1;
            Timber.d("[STREAK_POPUP] Streak was 0, treating as day 1 for first launch");
        }
        
        // Determine headline based on streak day
        // For days 1-31, use specific headlines; for day 31+, always use "Legend status"
        String headlineKey;
        if (streakDays >= 31) {
            headlineKey = "streak_popup_day_31_headline";
        } else {
            headlineKey = "streak_popup_day_" + streakDays + "_headline";
        }
        
        // Use day 1 message for first day, regular message for other days
        String messageKey = (streakDays == 1) ? "streak_popup_day_1_message" : "streak_popup_message";
        
        Achievement streakAchievement = new Achievement(
                AchievementPopup.STREAK_POPUP_ID,
                headlineKey,
                messageKey,
                AchievementCategory.PROGRESSION,
                "icon_46_flame");
        streakAchievement.setDescriptionFormatArgs(new Object[]{streakDays});
        achievementPopup.show(streakAchievement);
        Timber.d("[STREAK_POPUP] Displayed streak popup for %d days with headline: %s", streakDays, headlineKey);
        return true;
    }

    private void maybeShowDailyStreakPopup() {
        if (!StreakManager.getInstance(requireContext()).shouldShowStreakPopupToday()) {
            return;
        }
        if (showDailyStreakPopup()) {
            StreakManager.getInstance(requireContext()).markStreakPopupShownToday();
        }
    }
    
    /**
     * Set up button click listeners
     */
    private void setupButtons() {
        // Play button - resume auto-save if available, otherwise start new game
        newGameButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.new_random_game_scaled_large, 0, 0, 0);
        newGameButton.setOnClickListener(v -> {
            // Record daily login when starting a new game
            StreakManager.getInstance(requireContext()).recordDailyLogin();
            Timber.d("[STREAK] Daily login recorded on new random game start");
            
            // Reset achievement game session flags for new game
            AchievementManager.getInstance(requireContext()).onNewGameStarted();
            
            // Check if auto-save (slot 0) exists and load it
            String autosavePath = roboyard.ui.components.FileReadWrite.getSaveGamePath(requireActivity(), 0);
            java.io.File autosaveFile = new java.io.File(autosavePath);
            if (autosaveFile.exists()) {
                // Check if settings changed since autosave was created
                if (gameStateManager.autosaveSettingsMatch()) {
                    Timber.d("[PLAY] Auto-save found and settings match, loading from slot 0");
                    gameStateManager.loadGame(0);
                    if (gameStateManager.getCurrentState().getValue() != null) {
                        Timber.d("[PLAY] Auto-save loaded successfully, resuming game");
                        GameFragment gameFragment = new GameFragment();
                        navigateToDirect(gameFragment);
                        return;
                    }
                    Timber.w("[PLAY] Auto-save load failed, starting new game instead");
                } else {
                    Timber.d("[PLAY] Auto-save found but settings changed (board size or target count), starting new game");
                    // don't delete stale autosave
                    // autosaveFile.delete();
                    // gameStateManager.clearAutosaveMetadata();
                }
            } else {
                Timber.d("[PLAY] No auto-save found, starting new game");
            }
            
            // Start a new game
            Timber.d("MainMenuFragment: Calling gameStateManager.startGame()");
            gameStateManager.startGame();
            
            // Create a new GameFragment instance
            GameFragment gameFragment = new GameFragment();
            navigateToDirect(gameFragment);
        });
        
        // Level Game button - go to level selection screen
        levelGameButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.level_game_scaled_large, 0, 0, 0);
        levelGameButton.setOnClickListener(v -> {
            // Record daily login when starting a level game
            StreakManager.getInstance(requireContext()).recordDailyLogin();
            Timber.d("[STREAK] Daily login recorded on level game start");
            
            LevelSelectionFragment levelSelectionFragment = new LevelSelectionFragment();
            navigateToDirect(levelSelectionFragment);
        });
        
        // Load Game button - go to save game screen in load mode
        // Check if there are saved games and only show button if there are
        checkAndUpdateLoadGameButton();
        loadGameButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_load_game_scaled_large, 0, 0, 0);
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
        
        // Level Design Editor button - go to level design editor
        levelEditorButton.setOnClickListener(v -> {
            // Create the Level Design Editor fragment with a new level (ID 0)
            LevelDesignEditorFragment editorFragment = LevelDesignEditorFragment.newInstance(0);
            
            // Navigate to the editor fragment using the same pattern as other buttons
            navigateToDirect(editorFragment);
            
            Timber.d("MainMenuFragment: Opening Level Design Editor");
        });
        
        // Credits button - go to credits screen
        creditsButton.setOnClickListener(v -> {
            Timber.d("MainMenuFragment: Credits button clicked");
            CreditsFragment creditsFragment = new CreditsFragment();
            navigateToDirect(creditsFragment);
        });
        
        // Help icon button - go to help screen
        helpIconButton.setOnClickListener(v -> {
            Timber.d("MainMenuFragment: Help icon button clicked");
            HelpFragment helpFragment = new HelpFragment();
            navigateToDirect(helpFragment);
        });
        
        // Settings icon button - go to settings screen
        settingsIconButton.setOnClickListener(v -> {
            Timber.d("MainMenuFragment: Settings icon button clicked");
            SettingsFragment settingsFragment = new SettingsFragment();
            navigateToDirect(settingsFragment);
        });
        
        // Achievements icon button - go to achievements screen
        achievementsIconButton.setOnClickListener(v -> {
            Timber.d("MainMenuFragment: Achievements icon button clicked");
            AchievementsFragment achievementsFragment = new AchievementsFragment();
            navigateToDirect(achievementsFragment);
        });
        
        // User profile button - login or open profile
        userProfileButton.setOnClickListener(v -> {
            Timber.d("MainMenuFragment: User profile button clicked");
            RoboyardApiClient apiClient = RoboyardApiClient.getInstance(requireContext());
            if (apiClient.isLoggedIn()) {
                // Open profile in browser with auto-login token
                String url = apiClient.buildAutoLoginUrl("https://roboyard.z11.de/profile");
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } else {
                // Show login dialog
                LoginDialogHelper.showLoginDialog(requireContext(), new LoginDialogHelper.LoginCallback() {
                    @Override
                    public void onLoginSuccess(RoboyardApiClient.LoginResult result) {
                        updateUserProfileButton();
                    }
                    
                    @Override
                    public void onLoginError(String error) {
                        // Error handling is done in LoginDialogHelper
                    }
                });
            }
        });
    }
    
    /**
     * Apply white outline effect to the title
     */
    private void applyTitleOutline(View view) {
        TextView titleView = view.findViewById(R.id.main_menu_title);
        if (titleView != null) {
            // White text with black shadow
            titleView.setTextColor(0xFFFFFFFF);
            titleView.setShadowLayer(3f, 2f, 2f, 0xFF000000);
            titleView.invalidate();
        }
    }
    
    /**
     * Check if there are saved games and update Load Game button visibility
     */
    private void checkAndUpdateLoadGameButton() {
        try {
            // Check if there are any saved games
            java.io.File savesDir = new java.io.File(requireContext().getFilesDir(), "saves");
            boolean hasSavedGames = savesDir.exists() && savesDir.listFiles() != null && savesDir.listFiles().length > 0;
            
            // Show/hide load game button based on saved games
            loadGameButton.setVisibility(hasSavedGames ? View.VISIBLE : View.GONE);
            Timber.d("MainMenuFragment: Load Game button visibility set to %s (hasSavedGames=%s)", 
                hasSavedGames ? "VISIBLE" : "GONE", hasSavedGames);
        } catch (Exception e) {
            Timber.e(e, "MainMenuFragment: Error checking for saved games");
            loadGameButton.setVisibility(View.GONE);
        }
    }
    
    /**
     * Update user profile button based on login state
     */
    private void updateUserProfileButton() {
        RoboyardApiClient apiClient = RoboyardApiClient.getInstance(requireContext());
        if (apiClient.isLoggedIn()) {
            String userName = apiClient.getUserName();
            if (userName == null) userName = apiClient.getUserEmail();
            if (userName != null && !userName.isEmpty()) {
                String initials = String.valueOf(userName.charAt(0)).toUpperCase();
                userProfileButton.setText(initials);
                userProfileButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                userProfileButton.setGravity(android.view.Gravity.CENTER);
                userProfileButton.setContentDescription(initials);
            }
        } else {
            userProfileButton.setText("");
            userProfileButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_user_profile, 0, 0);
            userProfileButton.setGravity(android.view.Gravity.CENTER);
        }
    }
    
    @Override
    public String getScreenTitle() {
        return getString(R.string.main_menu_title);
    }
}
