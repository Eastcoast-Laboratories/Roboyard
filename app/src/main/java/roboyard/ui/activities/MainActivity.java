package roboyard.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import android.view.View;

import roboyard.eclabs.R;
import roboyard.logic.managers.SyncManager;
import roboyard.logic.core.Constants;
import roboyard.logic.core.GameState;
import roboyard.logic.core.Preferences;
import roboyard.logic.managers.GameStateManager;
import roboyard.logic.network.DeepLinkHandler;
import timber.log.Timber;

import java.util.Locale;

/**
 * Main activity for the game, hosts the fragment-based UI.
 * Acts as the container for all game fragments and provides access to the GameStateManager.
 */
public class MainActivity extends AppCompatActivity {
    
    private GameStateManager gameStateManager;
    private NavController navController;
    
    // Current board size - can be changed at runtime
    public static int boardSizeX = Constants.DEFAULT_BOARD_SIZE_X;
    public static int boardSizeY = Constants.DEFAULT_BOARD_SIZE_Y;

    // Get board dimensions
    public static int getBoardWidth() {
        int width = boardSizeX;
        Timber.d("[BOARD_SIZE_DEBUG] getBoardWidth() called, returning: %d", width);
        return width;
    }

    public static int getBoardHeight() {
        int height = boardSizeY;
        Timber.d("[BOARD_SIZE_DEBUG] getBoardHeight() called, returning: %d", height);
        return height;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge display for modern Android versions
        // This is safe for all versions as WindowCompat handles compatibility
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        // Apply fullscreen mode if enabled
        applyFullscreenMode();
        
        setContentView(R.layout.activity_main);

        // Verify auth token on app start to maintain login session
        roboyard.logic.network.RoboyardApiClient apiClient = roboyard.logic.network.ApiClientProvider.api(getApplicationContext());
        apiClient.verifyToken(new roboyard.logic.network.RoboyardApiClient.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isValid) {
                if (isValid) {
                    Timber.d("[AUTO_LOGIN] Token verified, user is logged in");
                } else {
                    Timber.d("[AUTO_LOGIN] No valid token, user needs to login");
                }
            }
            
            @Override
            public void onError(String error) {
                Timber.e("[AUTO_LOGIN] Error verifying token: %s", error);
            }
        });
        
        // Start background sound service if volume > 0
        Timber.d("[SOUND_SERVICE] MainActivity.onCreate: backgroundSoundVolume = %d", 
                Preferences.backgroundSoundVolume);
        startBackgroundSoundService(Preferences.backgroundSoundVolume);
        
        // Log the board size at startup
        Timber.d("[BOARD_SIZE_DEBUG] UI MainActivity onCreate - Current board size: %dx%d", 
                 getBoardWidth(), getBoardHeight());
        
        // Initialize the GameStateManager as a ViewModel
        gameStateManager = new ViewModelProvider(this).get(GameStateManager.class);
        
        // Note: Daily login is now recorded when starting a new game (random or level)
        // instead of at app startup, to ensure the user actually plays
        
        // Set up the Navigation controller with proper error handling
        try {
            androidx.fragment.app.Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (fragment instanceof NavHostFragment) {
                NavHostFragment navHostFragment = (NavHostFragment) fragment;
                navController = navHostFragment.getNavController();
                Timber.d("[NAV] Navigation controller initialized successfully");
            } else {
                Timber.w("[NAV] Fragment with id nav_host_fragment is not a NavHostFragment: %s", 
                        fragment != null ? fragment.getClass().getSimpleName() : "null");
            }
        } catch (ClassCastException e) {
            Timber.e(e, "[NAV] ClassCastException when setting up navigation controller");
        } catch (Exception e) {
            Timber.e(e, "[NAV] Unexpected error when setting up navigation controller");
        }
        
        // Set up accessibility services
        setupAccessibility();
        
        applyLanguageSettings();
        
        // Handle deep link intent
        handleIntent(getIntent());
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // Pause background sound when app goes to background
        Intent pauseIntent = new Intent(this, roboyard.SoundService.class);
        pauseIntent.setAction(roboyard.SoundService.ACTION_PAUSE);
        startService(pauseIntent);
        Timber.d("[LIFECYCLE] App paused - background sound paused");
        
        // Pause timer in GameFragment if active
        if (gameStateManager != null) {
            gameStateManager.pauseTimer();
            Timber.d("[LIFECYCLE] App paused - game timer paused");
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Re-apply fullscreen mode when resuming
        // This is needed in case the user changed the setting in the settings screen
        applyFullscreenMode();
        
        // Resume background sound when app comes to foreground
        Intent resumeIntent = new Intent(this, roboyard.SoundService.class);
        resumeIntent.setAction(roboyard.SoundService.ACTION_RESUME);
        startService(resumeIntent);
        Timber.d("[LIFECYCLE] App resumed - background sound resumed");
        
        // Resume timer in GameFragment if it was running
        if (gameStateManager != null) {
            gameStateManager.resumeTimer();
            Timber.d("[LIFECYCLE] App resumed - game timer resumed");
        }
        
        // Auto-sync when coming back online (uploads offline achievements/saves/history/streak)
        roboyard.logic.network.ApiClientProvider.sync(this).syncOnResume();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        // Log orientation change for debugging
        String orientation = "unknown";
        switch (newConfig.orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                orientation = "portrait";
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                orientation = "landscape";
                break;
        }
        Timber.d("[ORIENTATION] Configuration changed to: %s", orientation);
        
        // Re-apply fullscreen mode on configuration changes (e.g. rotation)
        applyFullscreenMode();
        
        // Force layout refresh to ensure proper layout selection
        if (getCurrentFocus() != null) {
            getCurrentFocus().clearFocus();
        }
    }
    
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Handle the new intent, e.g., if the app was already running and a new deep link is clicked
        handleIntent(intent);
    }
    
    /**
     * Handle incoming intents, including deep links
     * @param intent The intent to handle
     */
    /** Current supported deep-link protocol version. Increment when breaking changes are introduced. */
    private static final int DEEPLINK_API_VERSION = 1;

    // Size limits for deep-link map data to prevent denial-of-service via crafted links
    private static final int DEEPLINK_MAX_WIDTH   = 64;
    private static final int DEEPLINK_MAX_HEIGHT  = 64;
    private static final int DEEPLINK_MAX_CELLS   = 5000;
    private static final int DEEPLINK_MAX_ROBOTS  = 16;
    private static final int DEEPLINK_MAX_TARGETS = 16;

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        Uri data = intent.getData();

        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            // This is a deep link - delegate all parsing to the shared DeepLinkHandler
            DeepLinkHandler.DeepLinkResult result = DeepLinkHandler.INSTANCE.parse(data.toString());

            if (result instanceof DeepLinkHandler.DeepLinkResult.Random) {
                handleRandomGameDeepLink(data.toString());
                return;
            }
            if (result instanceof DeepLinkHandler.DeepLinkResult.UnsupportedVersion) {
                int ver = ((DeepLinkHandler.DeepLinkResult.UnsupportedVersion) result).getVersion();
                Timber.w("[DEEPLINK] Unsupported deep-link version %d (max %d), redirecting to menu", ver, DEEPLINK_API_VERSION);
                android.widget.Toast.makeText(this, R.string.needs_update_toast, android.widget.Toast.LENGTH_LONG).show();
                if (navController != null) navController.navigate(R.id.mainMenuFragment);
                return;
            }
            if (result instanceof DeepLinkHandler.DeepLinkResult.MapTooLarge) {
                Timber.e("[DEEPLINK] Map data rejected: size limits exceeded");
                android.widget.Toast.makeText(this, R.string.deeplink_map_too_large, android.widget.Toast.LENGTH_LONG).show();
                return;
            }
            if (result instanceof DeepLinkHandler.DeepLinkResult.Map) {
                DeepLinkHandler.DeepLinkResult.Map map = (DeepLinkHandler.DeepLinkResult.Map) result;
                GameState gameState = map.getGameState();
                int difficulty = map.getDifficulty();

                // Override the difficulty if specified in the deep link
                if (difficulty >= 0) {
                    gameStateManager.setDifficulty(difficulty);
                    Timber.d("[DEEPLINK_PROCESS] Set map difficulty: %d", difficulty);
                }

                // Navigate to the game fragment if we're not already there
                if (navController != null && navController.getCurrentDestination() != null &&
                    navController.getCurrentDestination().getId() != R.id.gameFragment) {
                    Timber.d("[DEEPLINK_NAV] Navigating to game fragment");
                    navController.navigate(R.id.gameFragment);
                }

                // Set the game state in the GameStateManager
                Timber.d("[DEEPLINK_PROCESS] Setting game state in GameStateManager");
                gameStateManager.setGameState(gameState);
                return;
            }
            Timber.w("[DEEPLINK] No usable map data in deep link");
        }
    }
    
    
    /**
     * Handle random game deep link
     * Start random game directly like the Play button
     */
    private void handleRandomGameDeepLink(String source) {
        Timber.d("[DEEPLINK] Random game deep link detected (%s)", source);
        if (gameStateManager != null) {
            // Record daily login when starting a new game
            roboyard.logic.achievements.StreakManagerFactory.getInstance(this).recordDailyLogin();
            Timber.d("[STREAK] Daily login recorded on random game deep link");
            
            // Reset achievement game session flags for new game
            roboyard.logic.achievements.AchievementManagerFactory.getInstance(this).onNewGameStarted();
            
            // Start a new game
            gameStateManager.startGame();
            Timber.d("[DEEPLINK] Started random game from deep link");
            
            // Navigate to game screen
            if (navController != null) {
                navController.navigate(R.id.mainMenuFragment);
                // Navigate to game fragment after menu is loaded
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    navController.navigate(R.id.gameFragment);
                }, 300);
            }
        }
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

    
    private void applyLanguageSettings() {
        try {
            // Get saved language setting
            String languageCode = roboyard.logic.core.Preferences.appLanguage;
            Timber.d("ROBOYARD_LANGUAGE: Setting app language on application level: %s", languageCode);
            
            if (languageCode != null && !languageCode.isEmpty()) {
                // Apply language change
                Locale locale = new Locale(languageCode);
                Locale.setDefault(locale);
                
                Resources resources = getResources();
                Configuration config = new Configuration(resources.getConfiguration());
                config.setLocale(locale); // Verwende die neuere Methode statt config.locale = locale
                
                resources.updateConfiguration(config, resources.getDisplayMetrics());
                
                Timber.d("ROBOYARD_LANGUAGE: Successfully applied language %s at application level", languageCode);
            }
        } catch (Exception e) {
            Timber.e(e, "ROBOYARD_LANGUAGE: Error applying language settings at application level");
        }
    }
    
    private void applyFullscreenMode() {
        // Check if fullscreen is enabled in preferences
        boolean fullscreenEnabled = Preferences.fullscreenEnabled;

        if (fullscreenEnabled) {
            // Apply fullscreen mode for edge-to-edge compatibility
            applyFullscreenModeForEdgeToEdge();
        } else {
            // Apply normal mode for edge-to-edge compatibility
            applyNormalModeForEdgeToEdge();
        }
    }

    private void applyFullscreenModeForEdgeToEdge() {
        try {
            // Use modern WindowInsets API for API 30+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                WindowInsetsControllerCompat windowInsetsController =
                    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
                windowInsetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            } else {
                // Fallback for older Android versions
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
            Timber.d("[FULLSCREEN] Edge-to-edge fullscreen mode enabled");
        } catch (Exception e) {
            Timber.e(e, "[FULLSCREEN] Error applying edge-to-edge fullscreen mode, falling back to legacy");
            // Fallback to legacy fullscreen
            applyLegacyFullscreen();
        }
    }

    private void applyNormalModeForEdgeToEdge() {
        try {
            // Use modern WindowInsets API for API 30+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                WindowInsetsControllerCompat windowInsetsController =
                    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
            } else {
                // Fallback for older Android versions
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
            Timber.d("[FULLSCREEN] Edge-to-edge normal mode - system bars visible");
        } catch (Exception e) {
            Timber.e(e, "[FULLSCREEN] Error applying edge-to-edge normal mode, falling back to legacy");
            // Fallback to legacy normal mode
            applyLegacyNormalMode();
        }
    }

    private void applyLegacyFullscreen() {
        try {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
            Timber.d("[FULLSCREEN] Legacy fullscreen mode applied");
        } catch (Exception e) {
            Timber.e(e, "[FULLSCREEN] Error applying legacy fullscreen");
        }
    }

    private void applyLegacyNormalMode() {
        try {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            Timber.d("[FULLSCREEN] Legacy normal mode applied");
        } catch (Exception e) {
            Timber.e(e, "[FULLSCREEN] Error applying legacy normal mode");
        }
    }

    /**
     * Start, update, or stop the background sound service based on volume level.
     * Replicates exact logic from MainActivity and SettingsFragment
     * @param volume Volume level 0-100 (0 stops the service)
     */
    private void startBackgroundSoundService(int volume) {
        Intent intent = new Intent(this, roboyard.SoundService.class);
        if (volume > 0) {
            intent.putExtra(roboyard.SoundService.EXTRA_VOLUME, volume);
            startService(intent);
            Timber.d("[SOUND_SERVICE] Started background sound service with volume %d", volume);
        } else {
            stopService(intent);
            Timber.d("[SOUND_SERVICE] Stopped background sound service");
        }
    }
}
