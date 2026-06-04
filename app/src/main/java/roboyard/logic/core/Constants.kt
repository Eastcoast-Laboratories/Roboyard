package roboyard.logic.core

import android.graphics.Color

object Constants {
    // Screen indices (preserved for compatibility with existing code)
    const val SCREEN_START: Int = 0
    const val SCREEN_SETTINGS: Int = 2
    const val SCREEN_CREDITS: Int = 3
    const val SCREEN_GAME: Int = 4
    const val SCREEN_LEVEL_BEGINNER: Int = 5 // Beginner levels (1-35)
    const val SCREEN_LEVEL_INTERMEDIATE: Int = 6 // Intermediate levels (36-70)
    const val SCREEN_LEVEL_ADVANCED: Int = 7 // Advanced levels (71-105)
    const val SCREEN_LEVEL_EXPERT: Int = 8 // Expert levels (106-140)
    const val SCREEN_SAVE_GAMES: Int = 9

    // Movement directions
    const val NORTH: Int = 0 // up
    const val EAST: Int = 1 // right
    const val SOUTH: Int = 2 // down
    const val WEST: Int = 3 // left

    // Background elements (lowest z-index)
    const val BACKGROUND: Int = 10

    // Grid elements
    const val GRID: Int = 20

    // Target markers
    const val TARGET: Int = 30

    // Robot markers (the transparent markers showing starting positions)
    const val ROBOT_MARKER: Int = 35

    // Base z-index for game objects (walls and robots)
    // Actual z-index will be calculated based on position
    // each element gets one z-index value lower than the previous one
    const val GAME_OBJECT_BASE: Int = 9000

    // UI elements (highest z-index)
    const val UI_ELEMENT: Int = 10000

    // Native UI constants
    const val PREFS_NAME: String = "roboyard_prefs"
    const val PREF_SOUND_ENABLED: String = "sound_enabled"

    // Game state cell types
    const val TYPE_EMPTY: Int = 0
    const val TYPE_ROBOT: Int = 1
    const val TYPE_TARGET: Int = 2
    const val TYPE_HORIZONTAL_WALL: Int = 3
    const val TYPE_VERTICAL_WALL: Int = 4

    // Game modes
    const val GAME_MODE_STANDARD: Int = 0
    const val GAME_MODE_MULTI_TARGET: Int = 1

    // Difficulty levels
    const val DIFFICULTY_BEGINNER: Int = 0
    const val DIFFICULTY_ADVANCED: Int = 1
    const val DIFFICULTY_INSANE: Int = 2
    const val DIFFICULTY_IMPOSSIBLE: Int = 3

    // Min moves for difficulty
    const val MIN_MOVES_BEGINNER: Int = 4
    const val MIN_MOVES_ADVANCED: Int = 6
    const val MIN_MOVES_INSANE: Int = 10
    const val MIN_MOVES_IMPOSSIBLE: Int = 17

    // Color constants
    const val COLOR_PINK: Int = 0
    const val COLOR_GREEN: Int = 1
    const val COLOR_BLUE: Int = 2
    const val COLOR_YELLOW: Int = 3
    const val COLOR_SILVER: Int = 4
    const val COLOR_RED: Int = 5
    const val COLOR_BROWN: Int = 6
    const val COLOR_ORANGE: Int = 7
    const val COLOR_WHITE: Int = 8
    @JvmField
    val COLOR_MULTI: Int = -1 // the multi target

    const val NUM_ROBOTS: Int = 4 // number of robots
    const val MAX_NUM_ROBOTS: Int = 5 // maximal allowed num robots

    // Default board sizes
    const val DEFAULT_BOARD_SIZE_X: Int = 12
    const val DEFAULT_BOARD_SIZE_Y: Int = 14

    // Debug settings
    const val DEBUG_SCREEN_LONG_PRESS_TIMEOUT_MS: Long = 3000 // milliseconds to open debug screen

    // Pre-computation solver timeout per solve (seconds)
    const val PRECOMP_SOLVER_TIMEOUT_SECONDS: Int = 120

    // RGB color values for robots (used in solver)
    val colors_rgb: IntArray = intArrayOf(
        Color.MAGENTA,  // Pink (COLOR_PINK)
        Color.GREEN,  // Green (COLOR_GREEN)
        Color.BLUE,  // Blue (COLOR_BLUE)
        Color.YELLOW,  // Yellow (COLOR_YELLOW)
        Color.GRAY,  // Silver (COLOR_SILVER)
        Color.RED,  // Red (COLOR_RED)
        -0x5ad5d6,  // Brown (COLOR_BROWN)
        -0x5b00,  // Orange (COLOR_ORANGE)
        Color.WHITE // White (COLOR_WHITE)
    )

    // File and directory paths
    const val SAVE_DIRECTORY: String = "saves"
    const val AUTO_SAVE_FILENAME: String = "autosave.dat"
    const val SAVE_FILENAME_PREFIX: String = "save_"
    const val SAVE_FILENAME_EXTENSION: String = ".dat"
    const val HISTORY_DIRECTORY: String = "history"

    // Accessibility constants
    const val ACCESSIBILITY_FOCUS_DELAY_MS: Long = 500
    const val MIN_BOARD_SIZE: Int = 6
    const val MAX_BOARD_SIZE: Int = 22

    // Level completion stars configuration
    // Levels 1-10: always earn at least 1 star (beginner-friendly)
    // Levels 11+: must earn stars based on performance (no guaranteed star)
    const val MIN_STAR_GUARANTEE_LEVEL: Int = 10
}
