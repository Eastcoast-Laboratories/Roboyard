package roboyard.eclabs;

/**
 * Helper class for calculating screen positions and sizes.
 * Uses a virtual coordinate system (1080x1920) that gets mapped to actual screen dimensions.
 * Supports positioning from both edges (positive = from top/left, negative = from bottom/right)
 */
public class ScreenLayout {
    // Virtual screen dimensions (reference resolution)
    private static final int VIRTUAL_WIDTH = 1080;
    private static final int VIRTUAL_HEIGHT = 1920;
    
    // Minimum sizes for UI elements (in dp)
    private static final int MIN_BUTTON_SIZE = 14;
    private static final int MIN_TEXT_SIZE = 4;
    
    private final int screenWidth;
    private final int screenHeight;
    private final float density;  // Screen density for dp conversion
    
    public ScreenLayout(int screenWidth, int screenHeight, float density) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.density = density;
    }
    
    /**
     * Convert virtual X coordinate to actual screen coordinate.
     * Negative values are calculated from the right edge.
     */
    public int x(float virtualX) {
        if (virtualX >= 0) {
            return (int)(virtualX * screenWidth / VIRTUAL_WIDTH);
        } else {
            return screenWidth - (int)(Math.abs(virtualX) * screenWidth / VIRTUAL_WIDTH);
        }
    }
    
    /**
     * Convert virtual Y coordinate to actual screen coordinate.
     * Negative values are calculated from the bottom edge.
     */
    public int y(float virtualY) {
        if (virtualY >= 0) {
            return (int)(virtualY * screenHeight / VIRTUAL_HEIGHT);
        } else {
            return screenHeight - (int)(Math.abs(virtualY) * screenHeight / VIRTUAL_HEIGHT);
        }
    }
    
    /**
     * Calculate button size that maintains aspect ratio but won't get too small
     */
    public int getButtonSize() {
        // Calculate size based on screen width (25% of width)
        int calculated = Math.min(x(VIRTUAL_WIDTH/4), y(VIRTUAL_WIDTH/4));
        int minimum = (int)(MIN_BUTTON_SIZE * density);
        return Math.max(calculated, minimum);
    }
    
    /**
     * Calculate text size that scales with screen but won't get too small
     */
    public int getTextSize(float virtualSize) {
        int calculated = (int)(virtualSize * Math.min(screenWidth / (float)VIRTUAL_WIDTH, 
                                                    screenHeight / (float)VIRTUAL_HEIGHT));
        int minimum = (int)(MIN_TEXT_SIZE * density);
        return Math.max(calculated, minimum);
    }
    
    /**
     * Get screen width
     */
    public int getScreenWidth() {
        return screenWidth;
    }
    
    /**
     * Get screen height
     */
    public int getScreenHeight() {
        return screenHeight;
    }
}
