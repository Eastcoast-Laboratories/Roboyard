package roboyard.eclabs.ui;

import android.content.Context;
import android.util.Log;

/**
 * Utility class to help identify and track usage of legacy code during the transition
 * from canvas-based UI to fragment-based UI.
 */
public class LegacyCodeTracker {
    private static final String TAG = "LegacyCodeTracker";
    
    /**
     * Log when a legacy component is used
     * 
     * @param context Context
     * @param componentName Name of the legacy component being used
     */
    public static void trackLegacyUsage(Context context, String componentName) {
        Log.d(TAG, "Legacy component used: " + componentName);
        
        // We could also store this information for later analysis
        // For example, counting occurrences to identify which legacy components
        // are still actively used
    }
    
    /**
     * List of legacy classes that can be safely removed once migration is complete
     */
    public static String[] getLegacyClasses() {
        return new String[] {
            // Main screens
            "roboyard.eclabs.MainScreen",
            "roboyard.eclabs.GridGameScreen",
            "roboyard.eclabs.SaveGameScreen",
            "roboyard.eclabs.SettingsScreen",
            "roboyard.eclabs.HelpScreen",
            
            // UI components
            "roboyard.eclabs.GameButton",
            "roboyard.eclabs.GameButtonGeneral",
            "roboyard.eclabs.GameButtonLevel",
            "roboyard.eclabs.GameButtonMenuLevel",
            "roboyard.eclabs.GameButtonGotoHistoryGame",
            "roboyard.eclabs.GameButtonGotoSavedGame",
            "roboyard.eclabs.GameButtonSaveScreen",
            
            // Legacy managers
            "roboyard.eclabs.ScreenManager",
            "roboyard.eclabs.RenderManager",
            "roboyard.ui.components.InputManager",
            
            // Legacy main activity
            "roboyard.eclabs.MainActivity"
        };
    }
}
