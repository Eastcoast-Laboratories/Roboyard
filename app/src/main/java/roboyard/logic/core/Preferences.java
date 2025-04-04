package roboyard.logic.core;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Map;

import timber.log.Timber;

/**
 * Central preferences manager for Roboyard.
 * Provides static access to all game preferences.
 * This class should be initialized at app start and updated only from the settings screen.
 */
public class Preferences {
    // Use a consistent name for SharedPreferences across the entire app
    private static final String PREFS_NAME = Constants.PREFS_NAME;
    private static SharedPreferences prefs;
    
    // Preference keys
    private static final String KEY_ROBOT_COUNT = "robot_count";
    private static final String KEY_TARGET_COLORS = "target_colors";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_DIFFICULTY = "difficulty";
    // Use the same keys as BoardSizeManager for compatibility
    private static final String KEY_BOARD_SIZE_WIDTH = "boardSizeX";
    private static final String KEY_BOARD_SIZE_HEIGHT = "boardSizeY";
    private static final String KEY_GENERATE_NEW_MAP = "generate_new_map";
    private static final String KEY_ACCESSIBILITY_MODE = "accessibility_mode";
    
    // Default values
    public static final int DEFAULT_ROBOT_COUNT = 1;
    public static final int DEFAULT_TARGET_COLORS = 1;
    public static final boolean DEFAULT_SOUND_ENABLED = true;
    public static final int DEFAULT_DIFFICULTY = Constants.DIFFICULTY_BEGINNER;
    public static final int DEFAULT_BOARD_SIZE_WIDTH = 12;
    public static final int DEFAULT_BOARD_SIZE_HEIGHT = 14;
    public static final boolean DEFAULT_GENERATE_NEW_MAP = true;
    public static final boolean DEFAULT_ACCESSIBILITY_MODE = false;
    
    // Cached values - accessible as static fields
    public static int robotCount;
    public static int targetColors;
    public static boolean soundEnabled;
    public static int difficulty;
    public static int boardSizeWidth;
    public static int boardSizeHeight;
    public static boolean generateNewMapEachTime;
    public static boolean accessibilityMode;
    
    // For compatibility with existing code
    public static int boardSizeX;
    public static int boardSizeY;
    public static boolean generateNewMap;
    
    // Application context for accessing SharedPreferences
    private static Context applicationContext;
    
    // Listener for preference changes
    private static PreferenceChangeListener preferenceChangeListener;
    
    /**
     * Interface for listening to preference changes
     */
    public interface PreferenceChangeListener {
        void onPreferencesChanged();
    }
    
    /**
     * Set a listener to be notified when preferences change
     * @param listener The listener to notify
     */
    public static void setPreferenceChangeListener(PreferenceChangeListener listener) {
        preferenceChangeListener = listener;
    }
    
    /**
     * Set the application context for accessing SharedPreferences
     * @param context Application context
     */
    public static void setContext(Context context) {
        if (context != null) {
            applicationContext = context.getApplicationContext();
            Timber.d("Application context set for Preferences");
        }
    }
    
    /**
     * Get the application context
     * @return Application context or null if not set
     */
    public static Context getContext() {
        return applicationContext;
    }
    
    /**
     * Initialize the Preferences system. Must be called at app start.
     * @param context Application context
     */
    public static void initialize(Context context) {
        if (context == null) {
            Timber.e("Cannot initialize Preferences with null context");
            return;
        }
        
        // Store the application context for later use
        setContext(context);
        
        try {
            // Get the app's shared preferences
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            
            // Load all preferences
            loadCachedValues();
            
            // For compatibility with existing code
            boardSizeX = boardSizeWidth;
            boardSizeY = boardSizeHeight;
            
            Timber.d("Preferences initialized successfully");
        } catch (Exception e) {
            Timber.e(e, "Error initializing preferences");
            // Use defaults if there's an error
            resetToDefaults();
        }
    }
    
    /**
     * Loads all preference values into static fields
     */
    private static void loadCachedValues() {
        if (prefs == null) {
            throw new IllegalStateException("Preferences not initialized. Call initialize() first.");
        }
        
        try {
            // Load preferences with error handling for each value
            try {
                robotCount = prefs.getInt(KEY_ROBOT_COUNT, DEFAULT_ROBOT_COUNT);
            } catch (ClassCastException e) {
                Timber.e("[PREFERENCES] Error loading robot count: %s", e.getMessage());
                // Clear the invalid preference and use default
                prefs.edit().remove(KEY_ROBOT_COUNT).apply();
                robotCount = DEFAULT_ROBOT_COUNT;
            }
            
            try {
                targetColors = prefs.getInt(KEY_TARGET_COLORS, DEFAULT_TARGET_COLORS);
            } catch (ClassCastException e) {
                Timber.e("[PREFERENCES] Error loading target colors: %s", e.getMessage());
                prefs.edit().remove(KEY_TARGET_COLORS).apply();
                targetColors = DEFAULT_TARGET_COLORS;
            }
            
            try {
                soundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, DEFAULT_SOUND_ENABLED);
            } catch (ClassCastException e) {
                Timber.e("[PREFERENCES] Error loading sound enabled: %s", e.getMessage());
                prefs.edit().remove(KEY_SOUND_ENABLED).apply();
                soundEnabled = DEFAULT_SOUND_ENABLED;
            }
            
            try {
                difficulty = prefs.getInt(KEY_DIFFICULTY, DEFAULT_DIFFICULTY);
            } catch (ClassCastException e) {
                Timber.e("[PREFERENCES] Error loading difficulty: %s", e.getMessage());
                prefs.edit().remove(KEY_DIFFICULTY).apply();
                difficulty = DEFAULT_DIFFICULTY;
            }
            
            try {
                boardSizeWidth = prefs.getInt(KEY_BOARD_SIZE_WIDTH, DEFAULT_BOARD_SIZE_WIDTH);
            } catch (ClassCastException e) {
                Timber.e("[PREFERENCES] Error loading board width: %s", e.getMessage());
                prefs.edit().remove(KEY_BOARD_SIZE_WIDTH).apply();
                boardSizeWidth = DEFAULT_BOARD_SIZE_WIDTH;
            }
            
            try {
                boardSizeHeight = prefs.getInt(KEY_BOARD_SIZE_HEIGHT, DEFAULT_BOARD_SIZE_HEIGHT);
            } catch (ClassCastException e) {
                Timber.e("[PREFERENCES] Error loading board height: %s", e.getMessage());
                prefs.edit().remove(KEY_BOARD_SIZE_HEIGHT).apply();
                boardSizeHeight = DEFAULT_BOARD_SIZE_HEIGHT;
            }
            
            try {
                generateNewMapEachTime = prefs.getBoolean(KEY_GENERATE_NEW_MAP, DEFAULT_GENERATE_NEW_MAP);
            } catch (ClassCastException e) {
                Timber.e("[PREFERENCES] Error loading generate new map: %s", e.getMessage());
                prefs.edit().remove(KEY_GENERATE_NEW_MAP).apply();
                generateNewMapEachTime = DEFAULT_GENERATE_NEW_MAP;
            }
            
            try {
                accessibilityMode = prefs.getBoolean(KEY_ACCESSIBILITY_MODE, DEFAULT_ACCESSIBILITY_MODE);
            } catch (ClassCastException e) {
                Timber.e("[PREFERENCES] Error loading accessibility mode: %s", e.getMessage());
                prefs.edit().remove(KEY_ACCESSIBILITY_MODE).apply();
                accessibilityMode = DEFAULT_ACCESSIBILITY_MODE;
            }
            
            // For compatibility with existing code
            boardSizeX = boardSizeWidth;
            boardSizeY = boardSizeHeight;
        } catch (Exception e) {
            // Catch any other unexpected errors
            Timber.e("[PREFERENCES] Unexpected error loading preferences: %s", e.getMessage());
            e.printStackTrace();
            
            // Reset to defaults
            resetToDefaults();
        }
        
        Timber.d("[PREFERENCES] Cached values loaded - robotCount: %d, targetColors: %d, difficulty: %d, boardSize: %dx%d",
                robotCount, targetColors, difficulty, boardSizeWidth, boardSizeHeight);
    }
    
    /**
     * Reset all preferences to default values
     */
    private static void resetToDefaults() {
        robotCount = DEFAULT_ROBOT_COUNT;
        targetColors = DEFAULT_TARGET_COLORS;
        soundEnabled = DEFAULT_SOUND_ENABLED;
        difficulty = DEFAULT_DIFFICULTY;
        boardSizeWidth = DEFAULT_BOARD_SIZE_WIDTH;
        boardSizeHeight = DEFAULT_BOARD_SIZE_HEIGHT;
        boardSizeX = boardSizeWidth;
        boardSizeY = boardSizeHeight;
        generateNewMapEachTime = DEFAULT_GENERATE_NEW_MAP;
        accessibilityMode = DEFAULT_ACCESSIBILITY_MODE;
        
        // Clear all preferences
        if (prefs != null) {
            prefs.edit().clear().apply();
            Timber.d("[PREFERENCES] Reset all preferences to defaults");
        }
    }
    
    /**
     * Notify listeners that preferences have changed
     */
    private static void notifyPreferencesChanged() {
        if (preferenceChangeListener != null) {
            preferenceChangeListener.onPreferencesChanged();
        }
    }
    
    /**
     * Set the robot count and save to preferences
     * @param count Number of robots (1-4)
     */
    public static void setRobotCount(int count) {
        // Ensure preferences are initialized
        if (prefs == null) {
            Timber.w("[PREFERENCES] SharedPreferences is null in setRobotCount, attempting to initialize");
            if (roboyard.eclabs.RoboyardApplication.getAppContext() != null) {
                initialize(roboyard.eclabs.RoboyardApplication.getAppContext());
            } else {
                Timber.e("[PREFERENCES] Cannot initialize preferences: context is null");
                // Set the static field but don't save to preferences
                robotCount = Math.max(1, Math.min(4, count));
                return;
            }
        }
        
        // Ensure count is between 1 and 4
        int validCount = Math.max(1, Math.min(4, count));
        
        // Save to preferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_ROBOT_COUNT, validCount);
        editor.apply();
        
        // Update static field
        robotCount = validCount;
        
        // Notify listeners
        notifyPreferencesChanged();
        Timber.d("[PREFERENCES] Robot count set to %d", validCount);
    }
    
    /**
     * Set the target colors count and save to preferences
     * @param count Number of target colors (1-4)
     */
    public static void setTargetColors(int count) {
        // Ensure preferences are initialized
        if (prefs == null) {
            Timber.w("[PREFERENCES] SharedPreferences is null in setTargetColors, attempting to initialize");
            if (roboyard.eclabs.RoboyardApplication.getAppContext() != null) {
                initialize(roboyard.eclabs.RoboyardApplication.getAppContext());
            } else {
                Timber.e("[PREFERENCES] Cannot initialize preferences: context is null");
                // Set the static field but don't save to preferences
                targetColors = Math.max(1, Math.min(4, count));
                return;
            }
        }
        
        // Ensure count is between 1 and 4
        int validCount = Math.max(1, Math.min(4, count));
        
        // Save to preferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_TARGET_COLORS, validCount);
        editor.apply();
        
        // Update static field
        targetColors = validCount;
        
        // Notify listeners
        notifyPreferencesChanged();
        Timber.d("[PREFERENCES] Target colors set to %d", validCount);
    }
    
    /**
     * Set the sound enabled state and save to preferences
     * @param enabled True to enable sound, false to disable
     */
    public static void setSoundEnabled(boolean enabled) {
        // Ensure preferences are initialized
        if (prefs == null) {
            Timber.w("[PREFERENCES] SharedPreferences is null in setSoundEnabled, attempting to initialize");
            if (roboyard.eclabs.RoboyardApplication.getAppContext() != null) {
                initialize(roboyard.eclabs.RoboyardApplication.getAppContext());
            } else {
                Timber.e("[PREFERENCES] Cannot initialize preferences: context is null");
                // Set the static field but don't save to preferences
                soundEnabled = enabled;
                return;
            }
        }
        
        // Save to preferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_SOUND_ENABLED, enabled);
        editor.apply();
        
        // Update static field
        soundEnabled = enabled;
        
        // Notify listeners
        notifyPreferencesChanged();
        Timber.d("[PREFERENCES] Sound enabled set to %s", enabled);
    }
    
    /**
     * Set the difficulty level and save to preferences
     * @param difficultyLevel Difficulty level (0-3)
     */
    public static void setDifficulty(int difficultyLevel) {
        // Ensure preferences are initialized
        if (prefs == null) {
            Timber.w("[PREFERENCES] SharedPreferences is null in setDifficulty, attempting to initialize");
            if (roboyard.eclabs.RoboyardApplication.getAppContext() != null) {
                initialize(roboyard.eclabs.RoboyardApplication.getAppContext());
            } else {
                Timber.e("[PREFERENCES] Cannot initialize preferences: context is null");
                // Set the static field but don't save to preferences
                difficulty = Math.max(Constants.DIFFICULTY_BEGINNER, Math.min(Constants.DIFFICULTY_IMPOSSIBLE, difficultyLevel));
                return;
            }
        }
        
        // Ensure difficulty level is between DIFFICULTY_BEGINNER and DIFFICULTY_IMPOSSIBLE
        int validDifficulty = Math.max(Constants.DIFFICULTY_BEGINNER, Math.min(Constants.DIFFICULTY_IMPOSSIBLE, difficultyLevel));
        
        // Save to preferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_DIFFICULTY, validDifficulty);
        editor.apply();
        
        // Update static field
        difficulty = validDifficulty;
        
        // Notify listeners
        notifyPreferencesChanged();
        Timber.d("[PREFERENCES] Difficulty set to %d", validDifficulty);
    }
    
    /**
     * Set the board size and save to preferences
     * @param width Board width
     * @param height Board height
     */
    public static void setBoardSize(int width, int height) {
        // Ensure preferences are initialized
        if (prefs == null) {
            Timber.w("[PREFERENCES] SharedPreferences is null in setBoardSize, attempting to initialize");
            if (roboyard.eclabs.RoboyardApplication.getAppContext() != null) {
                initialize(roboyard.eclabs.RoboyardApplication.getAppContext());
            } else {
                Timber.e("[PREFERENCES] Cannot initialize preferences: context is null");
                // Set the static field but don't save to preferences
                boardSizeWidth = Math.max(Constants.MIN_BOARD_SIZE, Math.min(Constants.MAX_BOARD_SIZE, width));
                boardSizeHeight = Math.max(Constants.MIN_BOARD_SIZE, Math.min(Constants.MAX_BOARD_SIZE, height));
                boardSizeX = boardSizeWidth;
                boardSizeY = boardSizeHeight;
                return;
            }
        }
        
        // Ensure width and height are between MIN_BOARD_SIZE and MAX_BOARD_SIZE
        int validWidth = Math.max(Constants.MIN_BOARD_SIZE, Math.min(Constants.MAX_BOARD_SIZE, width));
        int validHeight = Math.max(Constants.MIN_BOARD_SIZE, Math.min(Constants.MAX_BOARD_SIZE, height));
        
        // Save to preferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_BOARD_SIZE_WIDTH, validWidth);
        editor.putInt(KEY_BOARD_SIZE_HEIGHT, validHeight);
        editor.apply();
        
        // Update static fields
        boardSizeWidth = validWidth;
        boardSizeHeight = validHeight;
        
        // For compatibility with existing code
        boardSizeX = validWidth;
        boardSizeY = validHeight;
        
        // Notify listeners
        notifyPreferencesChanged();
        Timber.d("[PREFERENCES] Board size set to %dx%d", validWidth, validHeight);
    }
    
    /**
     * Set whether to generate a new map each time
     * @param generateNewMapEachTime Whether to generate a new map each time
     */
    public static void setGenerateNewMapEachTime(boolean generateNewMapEachTime) {
        if (prefs == null) {
            Timber.e("[PREFERENCES] Cannot save preference: SharedPreferences not initialized");
            return;
        }
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_GENERATE_NEW_MAP, generateNewMapEachTime);
        
        // Apply changes asynchronously
        editor.apply();
        
        // Update cached value
        Preferences.generateNewMapEachTime = generateNewMapEachTime;
        // For compatibility with existing code
        Preferences.generateNewMap = generateNewMapEachTime;
        
        Timber.d("[PREFERENCES] Generate new map set to %s", generateNewMapEachTime);
    }
    
    /**
     * Set whether to generate a new map each time and save to preferences
     * @param generateNewMapEachTime Whether to generate a new map each time
     */
    public static void setgenerateNewMapEachTime(boolean generateNewMapEachTime) {
        // Ensure preferences are initialized
        if (prefs == null) {
            Timber.w("[PREFERENCES] SharedPreferences is null in setgenerateNewMapEachTime, attempting to initialize");
            if (roboyard.eclabs.RoboyardApplication.getAppContext() != null) {
                initialize(roboyard.eclabs.RoboyardApplication.getAppContext());
            } else {
                Timber.e("[PREFERENCES] Cannot initialize preferences: context is null");
                // Set the static field but don't save to preferences
                Preferences.generateNewMapEachTime = generateNewMapEachTime;
                return;
            }
        }
        
        // Save to preferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_GENERATE_NEW_MAP, generateNewMapEachTime);
        editor.apply();
        
        // Update static field
        Preferences.generateNewMapEachTime = generateNewMapEachTime;
        
        // Notify listeners
        notifyPreferencesChanged();
        Timber.d("[PREFERENCES] Generate new map set to %s", generateNewMapEachTime);
    }
    
    /**
     * Set whether accessibility mode is enabled and save to preferences
     * @param enabled True if accessibility mode is enabled, false otherwise
     */
    public static void setAccessibilityMode(boolean enabled) {
        // Ensure preferences are initialized
        if (prefs == null) {
            Timber.w("[PREFERENCES] SharedPreferences is null in setAccessibilityMode, attempting to initialize");
            if (roboyard.eclabs.RoboyardApplication.getAppContext() != null) {
                initialize(roboyard.eclabs.RoboyardApplication.getAppContext());
            } else {
                Timber.e("[PREFERENCES] Cannot initialize preferences: context is null");
                // Set the static field but don't save to preferences
                accessibilityMode = enabled;
                return;
            }
        }
        
        // Save to preferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_ACCESSIBILITY_MODE, enabled);
        editor.apply();
        
        // Update static field
        accessibilityMode = enabled;
        
        // Notify listeners
        notifyPreferencesChanged();
        Timber.d("[PREFERENCES] Accessibility mode set to %s", enabled);
    }
    
    /**
     * Reload all preference values from disk
     * Call this if preferences might have been changed by another component
     */
    public static void reloadPreferences() {
        loadCachedValues();
        Timber.d("[PREFERENCES] Preferences reloaded from disk");
    }
    
    /**
     * For backward compatibility: Get a preference value as a string
     * @param context Context to use for accessing preferences
     * @param key Preference key
     * @return Preference value as a string, or null if not found
     */
    public static String getPreferenceValue(Context context, String key) {
        if (prefs == null) {
            initialize(context);
        }
        
        // Map old keys to new keys
        String mappedKey = key;
        switch (key) {
            case "robot_count":
                mappedKey = KEY_ROBOT_COUNT;
                return String.valueOf(robotCount);
            case "target_colors":
                mappedKey = KEY_TARGET_COLORS;
                return String.valueOf(targetColors);
            case "sound":
                mappedKey = KEY_SOUND_ENABLED;
                return soundEnabled ? "true" : "false";
            case "difficulty":
                mappedKey = KEY_DIFFICULTY;
                return String.valueOf(difficulty);
            case "boardSizeX":
                mappedKey = KEY_BOARD_SIZE_WIDTH;
                return String.valueOf(boardSizeWidth);
            case "boardSizeY":
                mappedKey = KEY_BOARD_SIZE_HEIGHT;
                return String.valueOf(boardSizeHeight);
            case "newMapEachTime":
                mappedKey = KEY_GENERATE_NEW_MAP;
                return generateNewMapEachTime ? "true" : "false";
            case "accessibilityMode":
                mappedKey = KEY_ACCESSIBILITY_MODE;
                return accessibilityMode ? "true" : "false";
        }
        
        // For any other keys, try to get the value directly
        return prefs.getString(mappedKey, null);
    }
    
    /**
     * For backward compatibility: Set a preference value as a string
     * @param context Context to use for accessing preferences
     * @param key Preference key
     * @param value Preference value as a string
     */
    public static void setPreferences(Context context, String key, String value) {
        if (prefs == null) {
            initialize(context);
        }
        
        // Map old keys to new methods
        switch (key) {
            case "robot_count":
                try {
                    setRobotCount(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    Timber.e(e, "Error parsing robot count: %s", value);
                }
                return;
            case "target_colors":
                try {
                    setTargetColors(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    Timber.e(e, "Error parsing target colors: %s", value);
                }
                return;
            case "sound":
                setSoundEnabled(value.equals("true"));
                return;
            case "difficulty":
                try {
                    setDifficulty(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    Timber.e(e, "Error parsing difficulty: %s", value);
                }
                return;
            case "boardSizeX":
                try {
                    int x = Integer.parseInt(value);
                    int y = boardSizeHeight; // Keep existing height
                    setBoardSize(x, y);
                } catch (NumberFormatException e) {
                    Timber.e(e, "Error parsing board width: %s", value);
                }
                return;
            case "boardSizeY":
                try {
                    int x = boardSizeWidth; // Keep existing width
                    int y = Integer.parseInt(value);
                    setBoardSize(x, y);
                } catch (NumberFormatException e) {
                    Timber.e(e, "Error parsing board height: %s", value);
                }
                return;
            case "newMapEachTime":
                setgenerateNewMapEachTime(value.equals("true"));
                return;
            case "accessibilityMode":
                setAccessibilityMode(value.equals("true"));
                return;
        }
        
        // For any other keys, save the value directly
        prefs.edit().putString(key, value).apply();
        Timber.d("[PREFERENCES] Set custom preference %s = %s", key, value);
    }
}
