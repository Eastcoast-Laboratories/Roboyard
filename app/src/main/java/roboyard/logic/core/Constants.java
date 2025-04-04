package roboyard.logic.core;

public class Constants {
    // Screen indices (preserved for compatibility with existing code)
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
    
    // Native UI constants
    public static final String PREFS_NAME = "RoboYard";
    public static final String PREF_SOUND_ENABLED = "sound_enabled";
    
    // Game state cell types
    public static final int TYPE_EMPTY = 0;
    public static final int TYPE_ROBOT = 1;
    public static final int TYPE_TARGET = 2;
    public static final int TYPE_HORIZONTAL_WALL = 3;
    public static final int TYPE_VERTICAL_WALL = 4;
    
    // Difficulty levels
    public static final int DIFFICULTY_BEGINNER = 0;
    public static final int DIFFICULTY_ADVANCED = 1;
    public static final int DIFFICULTY_INSANE = 2;
    public static final int DIFFICULTY_IMPOSSIBLE = 3;

    // Color constants
    public static final int COLOR_PINK = 0;
    public static final int COLOR_GREEN = 1;
    public static final int COLOR_BLUE = 2;
    public static final int COLOR_YELLOW = 3;
    public static final int COLOR_SILVER = 4;
    public static final int COLOR_RED = 5;
    public static final int COLOR_BROWN = 6;
    public static final int COLOR_ORANGE = 7;
    public static final int COLOR_WHITE = 8;
    public static final int COLOR_MULTI = 9; // the multi target

    public static final int NUM_ROBOTS = 4; // number of robots
    
    // File and directory paths
    public static final String SAVE_DIRECTORY = "saves";
    public static final String AUTO_SAVE_FILENAME = "autosave.dat";
    public static final String SAVE_FILENAME_PREFIX = "save_";
    public static final String SAVE_FILENAME_EXTENSION = ".dat";
    public static final String HISTORY_DIRECTORY = "history";
    
    // Accessibility constants
    public static final long ACCESSIBILITY_FOCUS_DELAY_MS = 500;
    public static final int MIN_BOARD_SIZE = 8;
    public static final int MAX_BOARD_SIZE = 22;
}
