package roboyard.eclabs;

/**
 * Constants for z-index values to control drawing order of game objects
 */
public class ZIndexConstants {
    // Background elements (lowest z-index)
    public static final int BACKGROUND = 10;
    
    // Grid elements
    public static final int GRID = 20;
    
    // Target markers
    public static final int TARGET = 30;
    
    // Robot markers (the transparent markers showing starting positions)
    public static final int ROBOT_MARKER = 35;
    
    // Base z-index for game objects (walls and robots)
    // Actual z-index will be calculated based on position
    // each element gets one z-index value lower than the previous one
    public static final int GAME_OBJECT_BASE = 9000;
    
    // UI elements (highest z-index)
    public static final int UI_ELEMENT = 10000;
}
