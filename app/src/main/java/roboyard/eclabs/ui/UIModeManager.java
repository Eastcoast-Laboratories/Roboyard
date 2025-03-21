package roboyard.eclabs.ui;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages UI mode preferences for switching between legacy canvas-based UI
 * and modern fragment-based UI.
 */
public class UIModeManager {
    private static final String PREFS_NAME = "RoboyardUIPrefs";
    private static final String KEY_UI_MODE = "ui_mode";
    
    // UI mode constants
    public static final int MODE_LEGACY = 0;  // Canvas-based UI
    public static final int MODE_MODERN = 1;  // Fragment-based UI
    
    private static UIModeManager instance;
    private final SharedPreferences prefs;
    
    /**
     * Get the singleton instance of UIModeManager
     * @param context Application context
     * @return UIModeManager instance
     */
    public static synchronized UIModeManager getInstance(Context context) {
        if (instance == null) {
            instance = new UIModeManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor to prevent direct instantiation
     * @param context Application context
     */
    private UIModeManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Get the current UI mode
     * @return Current UI mode (MODE_LEGACY or MODE_MODERN)
     */
    public int getUIMode() {
        return prefs.getInt(KEY_UI_MODE, MODE_LEGACY); // Default to legacy mode
    }
    
    /**
     * Set the UI mode
     * @param mode UI mode to set (MODE_LEGACY or MODE_MODERN)
     */
    public void setUIMode(int mode) {
        if (mode != MODE_LEGACY && mode != MODE_MODERN) {
            throw new IllegalArgumentException("Invalid UI mode: " + mode);
        }
        
        prefs.edit().putInt(KEY_UI_MODE, mode).apply();
    }
    
    /**
     * Toggle the UI mode between legacy and modern
     * @return The new UI mode after toggling
     */
    public int toggleUIMode() {
        int currentMode = getUIMode();
        int newMode = (currentMode == MODE_LEGACY) ? MODE_MODERN : MODE_LEGACY;
        setUIMode(newMode);
        return newMode;
    }
    
    /**
     * Check if the current UI mode is legacy
     * @return true if in legacy mode, false otherwise
     */
    public boolean isLegacyMode() {
        return getUIMode() == MODE_LEGACY;
    }
    
    /**
     * Check if the current UI mode is modern
     * @return true if in modern mode, false otherwise
     */
    public boolean isModernMode() {
        return getUIMode() == MODE_MODERN;
    }
}
