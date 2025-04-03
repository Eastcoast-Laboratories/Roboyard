package roboyard.logic.core;

import android.content.Context;
import android.content.SharedPreferences;

import timber.log.Timber;

/**
 * Central preferences manager for Roboyard.
 * Provides static access to all game preferences.
 * This class should be initialized at app start and updated only from the settings screen.
 */
public class Preferences {
    private static final String PREFS_NAME = "RoboYard";
    private static SharedPreferences prefs;
    
    // Preference keys
    private static final String KEY_ROBOT_COUNT = "robot_count";
    private static final String KEY_TARGET_COLORS = "target_colors";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_DIFFICULTY = "difficulty";
    private static final String KEY_BOARD_SIZE_WIDTH = "board_width";
    private static final String KEY_BOARD_SIZE_HEIGHT = "board_height";
    private static final String KEY_GENERATE_NEW_MAP = "generate_new_map";
    private static final String KEY_ACCESSIBILITY_MODE = "accessibility_mode";
    
    // Default values
    private static final int DEFAULT_ROBOT_COUNT = 1;
    private static final int DEFAULT_TARGET_COLORS = 4;
    private static final boolean DEFAULT_SOUND_ENABLED = true;
    private static final int DEFAULT_DIFFICULTY = 3;
    private static final int DEFAULT_BOARD_SIZE_WIDTH = 16;
    private static final int DEFAULT_BOARD_SIZE_HEIGHT = 16;
    private static final boolean DEFAULT_GENERATE_NEW_MAP = true;
    private static final boolean DEFAULT_ACCESSIBILITY_MODE = false;
    
    // Cached values - accessible as static fields
    public static int robotCount;
    public static int targetColors;
    public static boolean soundEnabled;
    public static int difficulty;
    public static int boardSizeWidth;
    public static int boardSizeHeight;
    public static boolean generateNewMap;
    public static boolean accessibilityMode;
    
    // For compatibility with existing code
    public static int boardSizeX;
    public static int boardSizeY;
    
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
     * Initialize the Preferences system. Must be called at app start.
     * @param context Application context
     */
    public static void initialize(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadCachedValues();
        
        Timber.d("[PREFERENCES] Initialized with robotCount: %d, targetColors: %d, boardSize: %dx%d", 
                robotCount, targetColors, boardSizeWidth, boardSizeHeight);
    }
    
    /**
     * Loads all preference values into static fields
     */
    private static void loadCachedValues() {
        if (prefs == null) {
            throw new IllegalStateException("Preferences not initialized. Call initialize() first.");
        }
        
        robotCount = prefs.getInt(KEY_ROBOT_COUNT, DEFAULT_ROBOT_COUNT);
        targetColors = prefs.getInt(KEY_TARGET_COLORS, DEFAULT_TARGET_COLORS);
        soundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, DEFAULT_SOUND_ENABLED);
        difficulty = prefs.getInt(KEY_DIFFICULTY, DEFAULT_DIFFICULTY);
        boardSizeWidth = prefs.getInt(KEY_BOARD_SIZE_WIDTH, DEFAULT_BOARD_SIZE_WIDTH);
        boardSizeHeight = prefs.getInt(KEY_BOARD_SIZE_HEIGHT, DEFAULT_BOARD_SIZE_HEIGHT);
        generateNewMap = prefs.getBoolean(KEY_GENERATE_NEW_MAP, DEFAULT_GENERATE_NEW_MAP);
        accessibilityMode = prefs.getBoolean(KEY_ACCESSIBILITY_MODE, DEFAULT_ACCESSIBILITY_MODE);
        
        // For compatibility with existing code
        boardSizeX = boardSizeWidth;
        boardSizeY = boardSizeHeight;
        
        Timber.d("[PREFERENCES] Cached values loaded - robotCount: %d, targetColors: %d, difficulty: %d, boardSize: %dx%d",
                robotCount, targetColors, difficulty, boardSizeWidth, boardSizeHeight);
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
            if (roboyard.ui.activities.MainActivity.getAppContext() != null) {
                initialize(roboyard.ui.activities.MainActivity.getAppContext());
            } else {
                Timber.e("[PREFERENCES] Cannot initialize preferences: context is null");
                // Set the static field but don't save to preferences
                robotCount = Math.max(1, count);
                return;
            }
        }
        
        // Ensure value is within valid range - allow for future expansion beyond 4 robots
        int validCount = Math.max(1, count);
        prefs.edit().putInt(KEY_ROBOT_COUNT, validCount).apply();
        robotCount = validCount;
        notifyPreferencesChanged();
        Timber.d("[PREFERENCES] Set robot count: %d", validCount);
    }
    
    /**
     * Set the target colors count and save to preferences
     * @param count Number of target colors (1-4)
     */
    public static void setTargetColors(int count) {
        // Ensure preferences are initialized
        if (prefs == null) {
            Timber.w("[PREFERENCES] SharedPreferences is null in setTargetColors, attempting to initialize");
            if (roboyard.ui.activities.MainActivity.getAppContext() != null) {
                initialize(roboyard.ui.activities.MainActivity.getAppContext());
            } else {
                Timber.e("[PREFERENCES] Cannot initialize preferences: context is null");
                // Set the static field but don't save to preferences
                targetColors = Math.max(1, Math.min(4, count));
                return;
            }
        }
        
        // Ensure value is within valid range
        int validCount = Math.max(1, Math.min(4, count));
        prefs.edit().putInt(KEY_TARGET_COLORS, validCount).apply();
        targetColors = validCount;
        notifyPreferencesChanged();
        Timber.d("[PREFERENCES] Set target colors: %d", validCount);
    }
    
    /**
     * Set the sound enabled state and save to preferences
     * @param enabled True to enable sound, false to disable
     */
    public static void setSoundEnabled(boolean enabled) {
        // Ensure preferences are initialized
        if (prefs == null) {
            Timber.w("[PREFERENCES] SharedPreferences is null in setSoundEnabled, attempting to initialize");
            if (roboyard.ui.activities.MainActivity.getAppContext() != null) {
                initialize(roboyard.ui.activities.MainActivity.getAppContext());
            } else {
                Timber.e("[PREFERENCES] Cannot initialize preferences: context is null");
                // Set the static field but don't save to preferences
                soundEnabled = enabled;
                return;
            }
        }
        
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
        soundEnabled = enabled;
        notifyPreferencesChanged();
        Timber.d("[PREFERENCES] Set sound enabled: %s", enabled);
    }
    
    /**
     * Set the difficulty level and save to preferences
     * @param difficultyLevel Difficulty level (1-5)
     */
    public static void setDifficulty(int difficultyLevel) {
        // Ensure preferences are initialized
        if (prefs == null) {
            Timber.w("[PREFERENCES] SharedPreferences is null in setDifficulty, attempting to initialize");
            if (roboyard.ui.activities.MainActivity.getAppContext() != null) {
                initialize(roboyard.ui.activities.MainActivity.getAppContext());
            } else {
                Timber.e("[PREFERENCES] Cannot initialize preferences: context is null");
                // Set the static field but don't save to preferences
                difficulty = Math.max(1, Math.min(5, difficultyLevel));
                return;
            }
        }
        
        // Ensure value is within valid range
        int validDifficulty = Math.max(1, Math.min(5, difficultyLevel));
        prefs.edit().putInt(KEY_DIFFICULTY, validDifficulty).apply();
        difficulty = validDifficulty;
        notifyPreferencesChanged();
        Timber.d("[PREFERENCES] Set difficulty: %d", validDifficulty);
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
            if (roboyard.ui.activities.MainActivity.getAppContext() != null) {
                initialize(roboyard.ui.activities.MainActivity.getAppContext());
            } else {
                Timber.e("[PREFERENCES] Cannot initialize preferences: context is null");
                // Set the static field but don't save to preferences
                boardSizeWidth = Math.max(8, Math.min(32, width));
                boardSizeHeight = Math.max(8, Math.min(32, height));
                boardSizeX = boardSizeWidth;
                boardSizeY = boardSizeHeight;
                return;
            }
        }
        
        // Ensure values are within valid range
        int validWidth = Math.max(8, Math.min(32, width));
        int validHeight = Math.max(8, Math.min(32, height));
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_BOARD_SIZE_WIDTH, validWidth);
        editor.putInt(KEY_BOARD_SIZE_HEIGHT, validHeight);
        editor.apply();
        
        boardSizeWidth = validWidth;
        boardSizeHeight = validHeight;
        
        // For compatibility with existing code
        boardSizeX = validWidth;
        boardSizeY = validHeight;
        
        notifyPreferencesChanged();
        Timber.d("[PREFERENCES] Set board size: %dx%d", validWidth, validHeight);
    }
    
    /**
     * Set whether to generate a new map each time and save to preferences
     * @param generateNewMap Whether to generate a new map each time
     */
    public static void setGenerateNewMap(boolean generateNewMap) {
        // Ensure preferences are initialized
        if (prefs == null) {
            Timber.w("[PREFERENCES] SharedPreferences is null in setGenerateNewMap, attempting to initialize");
            if (roboyard.ui.activities.MainActivity.getAppContext() != null) {
                initialize(roboyard.ui.activities.MainActivity.getAppContext());
            } else {
                Timber.e("[PREFERENCES] Cannot initialize preferences: context is null");
                // Set the static field but don't save to preferences
                Preferences.generateNewMap = generateNewMap;
                return;
            }
        }
        
        prefs.edit().putBoolean(KEY_GENERATE_NEW_MAP, generateNewMap).apply();
        Preferences.generateNewMap = generateNewMap;
        notifyPreferencesChanged();
        Timber.d("[PREFERENCES] Set generateNewMap: %s", generateNewMap);
    }
    
    /**
     * Set whether accessibility mode is enabled and save to preferences
     * @param enabled True if accessibility mode is enabled, false otherwise
     */
    public static void setAccessibilityMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_ACCESSIBILITY_MODE, enabled).apply();
        accessibilityMode = enabled;
        notifyPreferencesChanged();
        Timber.d("[PREFERENCES] Set accessibility mode: %s", enabled);
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
                return generateNewMap ? "true" : "false";
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
                setGenerateNewMap(value.equals("true"));
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
