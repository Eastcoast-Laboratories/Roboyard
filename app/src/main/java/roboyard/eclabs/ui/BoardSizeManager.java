package roboyard.eclabs.ui;

import android.content.Context;
import android.content.SharedPreferences;
import timber.log.Timber;

/**
 * Manages board size preferences for both legacy canvas-based UI
 * and modern fragment-based UI.
 * This ensures consistent board dimensions across both implementations.
 */
public class BoardSizeManager {
    // Use the same preference file as MainActivity and util.BoardSizeManager
    private static final String PREFS_NAME = "RoboYard";
    
    // Use the same keys as MainActivity and util.BoardSizeManager
    private static final String KEY_BOARD_WIDTH = "boardSizeX";
    private static final String KEY_BOARD_HEIGHT = "boardSizeY";
    
    // Default board dimensions
    private static final int DEFAULT_BOARD_WIDTH = 14;
    private static final int DEFAULT_BOARD_HEIGHT = 16;
    
    private static BoardSizeManager instance;
    private final SharedPreferences prefs;
    
    /**
     * Get the singleton instance of BoardSizeManager
     * @param context Application context
     * @return BoardSizeManager instance
     */
    public static synchronized BoardSizeManager getInstance(Context context) {
        if (instance == null) {
            instance = new BoardSizeManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor to prevent direct instantiation
     * @param context Application context
     */
    private BoardSizeManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Get the current board width
     * @return Current board width
     */
    public int getBoardWidth() {
        // Get the value from string preference (MainActivity saves as String)
        String widthStr = prefs.getString(KEY_BOARD_WIDTH, null);
        Timber.d("[BOARD_SIZE_DEBUG] UI BoardSizeManager.getBoardWidth() string preference: %s", widthStr);
        
        int width = DEFAULT_BOARD_WIDTH;
        if (widthStr != null && !widthStr.isEmpty()) {
            try {
                width = Integer.parseInt(widthStr);
                Timber.d("[BOARD_SIZE_DEBUG] UI BoardSizeManager.getBoardWidth() parsed string: %d", width);
            } catch (NumberFormatException e) {
                Timber.e(e, "[BOARD_SIZE_DEBUG] UI BoardSizeManager.getBoardWidth() parse error, using default: %d", width);
            }
        } else {
            Timber.d("[BOARD_SIZE_DEBUG] UI BoardSizeManager.getBoardWidth() using default (empty string): %d", width);
        }
        
        Timber.d("[BOARD_SIZE_DEBUG] UI BoardSizeManager.getBoardWidth() returning: %d", width);
        return width;
    }
    
    /**
     * Get the current board height
     * @return Current board height
     */
    public int getBoardHeight() {
        // Get the value from string preference (MainActivity saves as String)
        String heightStr = prefs.getString(KEY_BOARD_HEIGHT, null);
        Timber.d("[BOARD_SIZE_DEBUG] UI BoardSizeManager.getBoardHeight() string preference: %s", heightStr);
        
        int height = DEFAULT_BOARD_HEIGHT;
        if (heightStr != null && !heightStr.isEmpty()) {
            try {
                height = Integer.parseInt(heightStr);
                Timber.d("[BOARD_SIZE_DEBUG] UI BoardSizeManager.getBoardHeight() parsed string: %d", height);
            } catch (NumberFormatException e) {
                Timber.e(e, "[BOARD_SIZE_DEBUG] UI BoardSizeManager.getBoardHeight() parse error, using default: %d", height);
            }
        } else {
            Timber.d("[BOARD_SIZE_DEBUG] UI BoardSizeManager.getBoardHeight() using default (empty string): %d", height);
        }
        
        Timber.d("[BOARD_SIZE_DEBUG] UI BoardSizeManager.getBoardHeight() returning: %d", height);
        return height;
    }
    
    /**
     * Set the board dimensions
     * @param width New board width
     * @param height New board height
     */
    public void setBoardSize(int width, int height) {
        Timber.d("[BOARD_SIZE_DEBUG] UI BoardSizeManager.setBoardSize() setting: %dx%d", width, height);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Save as strings to be consistent with MainActivity
        editor.putString(KEY_BOARD_WIDTH, String.valueOf(width));
        editor.putString(KEY_BOARD_HEIGHT, String.valueOf(height));
        editor.apply();
        
        // Also update MainActivity static fields for backward compatibility
        try {
            roboyard.eclabs.MainActivity.boardSizeX = width;
            roboyard.eclabs.MainActivity.boardSizeY = height;
            Timber.d("[BOARD_SIZE_DEBUG] UI BoardSizeManager.setBoardSize() updated MainActivity: %dx%d", width, height);
        } catch (Exception e) {
            Timber.e(e, "[BOARD_SIZE_DEBUG] UI BoardSizeManager.setBoardSize() error updating MainActivity");
        }
    }
}
