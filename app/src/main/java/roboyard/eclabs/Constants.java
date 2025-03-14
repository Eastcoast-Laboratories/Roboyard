package roboyard.eclabs;

public class Constants {
    // Screen indices
    public static final int SCREEN_START = 0;
    public static final int SCREEN_SETTINGS = 2;
    public static final int SCREEN_CREDITS = 3;
    public static final int SCREEN_GAME = 4;
    public static final int SCREEN_LEVEL_BEGINNER = 5;  // Beginner levels (1-35)
    public static final int SCREEN_LEVEL_INTERMEDIATE = 6;  // Intermediate levels (36-70)
    public static final int SCREEN_LEVEL_ADVANCED = 7;  // Advanced levels (71-105)
    public static final int SCREEN_LEVEL_EXPERT = 8;  // Expert levels (106-140)
    public static final int SCREEN_SAVE_GAMES = 9;

    // Movement directions
    public static final int NORTH = 0; // up
    public static final int EAST = 1; // right
    public static final int SOUTH = 2; // down
    public static final int WEST = 3; // left

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
