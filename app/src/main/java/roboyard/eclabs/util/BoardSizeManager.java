package roboyard.eclabs.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages board size preferences for both legacy canvas-based UI
 * and modern fragment-based UI.
 * This ensures consistent board dimensions across both implementations.
 */
public class BoardSizeManager {
    private static final String PREFS_NAME = "RoboyardBoardPrefs";
    private static final String KEY_BOARD_WIDTH = "board_width";
    private static final String KEY_BOARD_HEIGHT = "board_height";
    
    // Default board dimensions
    private static final int DEFAULT_BOARD_WIDTH = 14;
    private static final int DEFAULT_BOARD_HEIGHT = 14;
    
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
        return prefs.getInt(KEY_BOARD_WIDTH, DEFAULT_BOARD_WIDTH);
    }
    
    /**
     * Get the current board height
     * @return Current board height
     */
    public int getBoardHeight() {
        return prefs.getInt(KEY_BOARD_HEIGHT, DEFAULT_BOARD_HEIGHT);
    }
    
    /**
     * Set the board width
     * @param width Board width to set
     */
    public void setBoardWidth(int width) {
        if (width < 8 || width > 20) {
            throw new IllegalArgumentException("Board width must be between 8 and 20");
        }
        
        prefs.edit().putInt(KEY_BOARD_WIDTH, width).apply();
    }
    
    /**
     * Set the board height
     * @param height Board height to set
     */
    public void setBoardHeight(int height) {
        if (height < 8 || height > 20) {
            throw new IllegalArgumentException("Board height must be between 8 and 20");
        }
        
        prefs.edit().putInt(KEY_BOARD_HEIGHT, height).apply();
    }
    
    /**
     * Set both board width and height
     * @param width Board width to set
     * @param height Board height to set
     */
    public void setBoardSize(int width, int height) {
        if (width < 8 || width > 20) {
            throw new IllegalArgumentException("Board width must be between 8 and 20");
        }
        if (height < 8 || height > 20) {
            throw new IllegalArgumentException("Board height must be between 8 and 20");
        }
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_BOARD_WIDTH, width);
        editor.putInt(KEY_BOARD_HEIGHT, height);
        editor.apply();
    }
    
    /**
     * Reset board size to default dimensions
     */
    public void resetToDefaults() {
        setBoardSize(DEFAULT_BOARD_WIDTH, DEFAULT_BOARD_HEIGHT);
    }
}
