package roboyard.eclabs.ui;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility class to migrate preferences from the old canvas-based UI to the new fragment-based UI.
 * This enables a smooth transition by preserving user settings during the architecture change.
 */
public class PreferencesMigrator {
    
    private static final String OLD_PREFS_NAME = "preferencestorage";
    private static final String NEW_PREFS_NAME = "roboyard_preferences";
    
    // Key mappings between old and new preference keys
    private static final String[][] KEY_MAPPINGS = {
        // Format: {oldKey, newKey}
        {"difficulty", "game_difficulty"},
        {"boardSize", "board_size"},
        {"sound", "sound_enabled"},
        {"accessibilityMode", "accessibility_mode"},
        // Add more mappings as needed
    };
    
    /**
     * Migrate all preferences from the old system to the new one.
     * 
     * @param context Application context
     * @return true if migration was performed, false if already migrated
     */
    public static boolean migratePreferences(Context context) {
        SharedPreferences oldPrefs = context.getSharedPreferences(OLD_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences newPrefs = context.getSharedPreferences(NEW_PREFS_NAME, Context.MODE_PRIVATE);
        
        // Check if migration already happened
        if (newPrefs.getBoolean("preferences_migrated", false)) {
            return false; // Already migrated
        }
        
        // Get editor for new preferences
        SharedPreferences.Editor editor = newPrefs.edit();
        
        // Migrate all mapped preferences
        for (String[] mapping : KEY_MAPPINGS) {
            String oldKey = mapping[0];
            String newKey = mapping[1];
            
            if (oldPrefs.contains(oldKey)) {
                String value = oldPrefs.getString(oldKey, "");
                editor.putString(newKey, value);
            }
        }
        
        // Mark as migrated
        editor.putBoolean("preferences_migrated", true);
        
        // Commit changes (using apply for better performance)
        editor.apply();
        
        return true;
    }
    
    /**
     * Get a preference value, trying the new preferences first, then falling back to old if needed.
     * This ensures a smooth transition period where both systems can work together.
     * 
     * @param context Application context
     * @param newKey New preference key
     * @param oldKey Old preference key
     * @param defaultValue Default value if preference not found
     * @return The preference value
     */
    public static String getPreference(Context context, String newKey, String oldKey, String defaultValue) {
        SharedPreferences newPrefs = context.getSharedPreferences(NEW_PREFS_NAME, Context.MODE_PRIVATE);
        
        // First try to get from new preferences
        if (newPrefs.contains(newKey)) {
            return newPrefs.getString(newKey, defaultValue);
        }
        
        // Fall back to old preferences
        SharedPreferences oldPrefs = context.getSharedPreferences(OLD_PREFS_NAME, Context.MODE_PRIVATE);
        return oldPrefs.getString(oldKey, defaultValue);
    }
}
