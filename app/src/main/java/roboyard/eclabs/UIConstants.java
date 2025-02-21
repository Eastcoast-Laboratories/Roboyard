package roboyard.eclabs;

/**
 * Constants for UI layout positioning.
 * All values are in virtual coordinates (1080x1920).
 * Negative values mean position from right/bottom edge.
 */
public class UIConstants {
    // Bottom area layout
    public static final float BOTTOM_BUTTONS_Y = -255;   // Bottom row of buttons

    // Right side layout
    public static final float NEXT_BUTTON_X = -255;      // Next button from right edge
    public static final float BOARD_NAME_POS_X = -190;       // Level name aligned with next button
    
    // Text sizes
    public static final float TEXT_SIZE_SMALL = 55;      // Smaller text size
    public static final float TEXT_SIZE_NORMAL = 77;     // Normal text size
    public static final float TEXT_SIZE_LARGE = 99;      // Larger text size
    
    // Spacing
    public static final float BUTTON_GAP = 10;           // Gap between buttons

    // Colors (moved from GridGameScreen)
    public static final int TEXT_COLOR_HIGHLIGHT = 0xFFAAAAAA;  // Light gray
    public static final int TEXT_COLOR_NORMAL = 0xFF808080;     // Gray
}
