package roboyard.logic.core

import roboyard.logic.storage.PlatformStorage
import roboyard.logic.util.RLog
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Central preferences manager for Roboyard.
 * Provides static access to all game preferences.
 * This class should be initialized at app start and updated only from the settings screen.
 */
object Preferences {
    private var storage: PlatformStorage? = null
    private val log = RLog.tag("Preferences")

    /**
     * Provider for lazy storage initialization, set by the platform at app start.
     * Lets commonMain obtain a PlatformStorage without a platform Context.
     */
    @JvmField
    var storageProvider: (() -> PlatformStorage?)? = null

    // Preference keys
    private const val KEY_ROBOT_COUNT = "robot_count"
    private const val KEY_TARGET_COLORS = "target_colors"
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_DIFFICULTY = "difficulty"

    // Use the same keys as BoardSizeManager for compatibility
    private const val KEY_BOARD_SIZE_WIDTH = "boardSizeX"
    private const val KEY_BOARD_SIZE_HEIGHT = "boardSizeY"
    private const val KEY_GENERATE_NEW_MAP = "generate_new_map"
    private const val KEY_ACCESSIBILITY_MODE = "accessibility_mode"
    private const val KEY_APP_LANGUAGE = "app_language"
    private const val KEY_TALKBACK_LANGUAGE = "talkback_language"
    private const val KEY_GAME_MODE = "game_mode"
    private const val KEY_FULLSCREEN_ENABLED = "fullscreen_enabled"
    private const val KEY_MIN_SOLUTION_MOVES = "min_solution_moves"
    private const val KEY_MAX_SOLUTION_MOVES = "max_solution_moves"
    private const val KEY_ALLOW_MULTICOLOR_TARGET = "allow_multicolor_target"
    private const val KEY_HIGH_CONTRAST_MODE = "high_contrast_mode"
    private const val KEY_BACKGROUND_SOUND_VOLUME = "background_sound_volume"
    private const val KEY_LIVE_MOVE_COUNTER_ENABLED = "live_move_counter_enabled"
    private const val KEY_HINT_AUTO_MOVE_ENABLED = "hint_auto_move_enabled"
    private const val KEY_HINT_AUTO_MOVE_MODE = "hint_auto_move_mode"
    private const val KEY_SOUND_EFFECTS_VOLUME = "sound_effects_volume"

    // Hint auto-move modes
    const val HINT_AUTO_MOVE_MANUAL: Int = 0 // Manual: user moves robots manually
    const val HINT_AUTO_MOVE_FULL_AUTO: Int =
        1 // Full-Auto: robot moves automatically when hint shown
    const val HINT_AUTO_MOVE_SEMI_AUTO: Int =
        2 // Semi-Auto: robot moves when next-hint button clicked

    // Default values
    const val DEFAULT_ROBOT_COUNT: Int = 1
    const val DEFAULT_TARGET_COLORS: Int = 1
    const val DEFAULT_SOUND_ENABLED: Boolean = true
    val DEFAULT_DIFFICULTY: Int = Constants.DIFFICULTY_BEGINNER
    const val DEFAULT_BOARD_SIZE_WIDTH: Int = 12
    const val DEFAULT_BOARD_SIZE_HEIGHT: Int = 14
    const val DEFAULT_GENERATE_NEW_MAP: Boolean = true
    const val DEFAULT_ACCESSIBILITY_MODE: Boolean = false
    const val DEFAULT_APP_LANGUAGE: String = "en"
    const val DEFAULT_TALKBACK_LANGUAGE: String = "same"
    val DEFAULT_GAME_MODE: Int = Constants.GAME_MODE_STANDARD
    const val DEFAULT_FULLSCREEN_ENABLED: Boolean = true
    const val DEFAULT_MIN_SOLUTION_MOVES: Int = 4
    const val DEFAULT_MAX_SOLUTION_MOVES: Int = 6
    const val DEFAULT_ALLOW_MULTICOLOR_TARGET: Boolean = true
    const val DEFAULT_HIGH_CONTRAST_MODE: Boolean = false
    const val DEFAULT_BACKGROUND_SOUND_VOLUME: Int = 1
    const val DEFAULT_LIVE_MOVE_COUNTER_ENABLED: Boolean = false
    const val DEFAULT_HINT_AUTO_MOVE_ENABLED: Boolean = false
    val DEFAULT_HINT_AUTO_MOVE_MODE: Int = HINT_AUTO_MOVE_MANUAL
    const val DEFAULT_SOUND_EFFECTS_VOLUME: Int = 20

    // Cached values - accessible as static fields
    @JvmField
    var robotCount: Int = 0
    @JvmField
    var targetColors: Int = 0
    @JvmField
    var soundEnabled: Boolean = false
    @JvmField
    var difficulty: Int = 0
    @JvmField
    var boardSizeWidth: Int = 0
    @JvmField
    var boardSizeHeight: Int = 0
    @JvmField
    var generateNewMapEachTime: Boolean = false
    @JvmField
    var accessibilityMode: Boolean = false
    @JvmField
    var appLanguage: String? = null
    @JvmField
    var talkbackLanguage: String? = null
    @JvmField
    var gameMode: Int = 0
    @JvmField
    var fullscreenEnabled: Boolean = false
    @JvmField
    var minSolutionMoves: Int = 0
    @JvmField
    var maxSolutionMoves: Int = 0
    @JvmField
    var allowMulticolorTarget: Boolean = false
    @JvmField
    var highContrastMode: Boolean = false
    @JvmField
    var backgroundSoundVolume: Int = 0
    @JvmField
    var liveMoveCounterEnabled: Boolean = false
    var hintAutoMoveEnabled: Boolean = false
    @JvmField
    var hintAutoMoveMode: Int = 0
    @JvmField
    var soundEffectsVolume: Int = 0

    // For compatibility with existing code
    var boardSizeX: Int = 0
    var boardSizeY: Int = 0
    var generateNewMap: Boolean = false
    var accessibility: Boolean = false

    // Listener for preference changes
    private var preferenceChangeListener: PreferenceChangeListener? = null

    /**
     * Set a listener to be notified when preferences change
     * @param listener The listener to notify
     */
    @JvmStatic
    fun setPreferenceChangeListener(listener: PreferenceChangeListener?) {
        preferenceChangeListener = listener
    }

    /**
     * Initialize the Preferences system. Must be called at app start.
     * @param storage Platform storage implementation
     * @param accessibilityActive Whether a screen reader is active (platform-detected)
     */
    @JvmStatic
    @JvmOverloads
    fun initialize(storage: PlatformStorage?, accessibilityActive: Boolean = false) {
        if (storage == null) {
            log.e("Cannot initialize Preferences with null storage")
            return
        }

        try {
            this.storage = storage

            // Load all preferences
            loadCachedValues()

            // For compatibility with existing code
            boardSizeX = boardSizeWidth
            boardSizeY = boardSizeHeight

            // Check if it's a fresh install (no preferences saved yet)
            val isFreshInstall = storage.getString(KEY_ROBOT_COUNT, null) == null &&
                storage.getString(KEY_BOARD_SIZE_WIDTH, null) == null &&
                storage.getString(KEY_GENERATE_NEW_MAP, null) == null

            // For fresh installs with accessibility active, set appropriate defaults
            if (isFreshInstall && accessibilityActive) {
                storage.putInt(KEY_BOARD_SIZE_WIDTH, 8)
                storage.putInt(KEY_BOARD_SIZE_HEIGHT, 8)
                storage.putBoolean(KEY_GENERATE_NEW_MAP, false)
                storage.putBoolean(KEY_ACCESSIBILITY_MODE, true)

                boardSizeWidth = 8
                boardSizeHeight = 8
                boardSizeX = 8
                boardSizeY = 8
                generateNewMapEachTime = false
                accessibilityMode = true

                log.d("[PREFERENCES] Fresh install with accessibility detected. Setting defaults: board size=8x8, generate new map=false")
            }

            log.d("Preferences initialized successfully")
        } catch (e: Exception) {
            log.e(e, "Error initializing preferences")
            resetToDefaults()
        }
    }

    /**
     * Loads all preference values into static fields
     */
    private fun loadCachedValues() {
        checkNotNull(storage) { "Preferences not initialized. Call initialize() first." }

        try {
            // Load preferences with error handling for each value
            try {
                robotCount = storage!!.getInt(KEY_ROBOT_COUNT, DEFAULT_ROBOT_COUNT)
            } catch (e: ClassCastException) {
                log.e("[PREFERENCES] Error loading robot count: %s", e.message)
                // Clear the invalid preference and use default
                storage!!.remove(KEY_ROBOT_COUNT)
                robotCount = DEFAULT_ROBOT_COUNT
            }

            try {
                targetColors = storage!!.getInt(KEY_TARGET_COLORS, DEFAULT_TARGET_COLORS)
            } catch (e: ClassCastException) {
                log.e("[PREFERENCES] Error loading target colors: %s", e.message)
                storage!!.remove(KEY_TARGET_COLORS)
                targetColors = DEFAULT_TARGET_COLORS
            }

            try {
                soundEnabled = storage!!.getBoolean(KEY_SOUND_ENABLED, DEFAULT_SOUND_ENABLED)
            } catch (e: ClassCastException) {
                log.e("[PREFERENCES] Error loading sound enabled: %s", e.message)
                storage!!.remove(KEY_SOUND_ENABLED)
                soundEnabled = DEFAULT_SOUND_ENABLED
            }

            try {
                difficulty = storage!!.getInt(KEY_DIFFICULTY, DEFAULT_DIFFICULTY)
            } catch (e: ClassCastException) {
                log.e("[PREFERENCES] Error loading difficulty: %s", e.message)
                storage!!.remove(KEY_DIFFICULTY)
                difficulty = DEFAULT_DIFFICULTY
            }

            try {
                boardSizeWidth = storage!!.getInt(KEY_BOARD_SIZE_WIDTH, DEFAULT_BOARD_SIZE_WIDTH)
            } catch (e: ClassCastException) {
                log.e("[PREFERENCES] Error loading board width: %s", e.message)
                storage!!.remove(KEY_BOARD_SIZE_WIDTH)
                boardSizeWidth = DEFAULT_BOARD_SIZE_WIDTH
            }

            try {
                boardSizeHeight = storage!!.getInt(KEY_BOARD_SIZE_HEIGHT, DEFAULT_BOARD_SIZE_HEIGHT)
            } catch (e: ClassCastException) {
                log.e("[PREFERENCES] Error loading board height: %s", e.message)
                storage!!.remove(KEY_BOARD_SIZE_HEIGHT)
                boardSizeHeight = DEFAULT_BOARD_SIZE_HEIGHT
            }

            try {
                generateNewMapEachTime =
                    storage!!.getBoolean(KEY_GENERATE_NEW_MAP, DEFAULT_GENERATE_NEW_MAP)
            } catch (e: ClassCastException) {
                log.e("[PREFERENCES] Error loading generate new map: %s", e.message)
                storage!!.remove(KEY_GENERATE_NEW_MAP)
                generateNewMapEachTime = DEFAULT_GENERATE_NEW_MAP
            }

            try {
                accessibilityMode =
                    storage!!.getBoolean(KEY_ACCESSIBILITY_MODE, DEFAULT_ACCESSIBILITY_MODE)
            } catch (e: ClassCastException) {
                log.e("[PREFERENCES] Error loading accessibility mode: %s", e.message)
                storage!!.remove(KEY_ACCESSIBILITY_MODE)
                accessibilityMode = DEFAULT_ACCESSIBILITY_MODE
            }

            try {
                appLanguage = storage!!.getString(KEY_APP_LANGUAGE, DEFAULT_APP_LANGUAGE)
            } catch (e: ClassCastException) {
                log.e("[PREFERENCES] Error loading app language: %s", e.message)
                storage!!.remove(KEY_APP_LANGUAGE)
                appLanguage = DEFAULT_APP_LANGUAGE
            }

            try {
                talkbackLanguage =
                    storage!!.getString(KEY_TALKBACK_LANGUAGE, DEFAULT_TALKBACK_LANGUAGE)
            } catch (e: ClassCastException) {
                log.e("[PREFERENCES] Error loading talkback language: %s", e.message)
                storage!!.remove(KEY_TALKBACK_LANGUAGE)
                talkbackLanguage = DEFAULT_TALKBACK_LANGUAGE
            }

            try {
                gameMode = storage!!.getInt(KEY_GAME_MODE, DEFAULT_GAME_MODE)
            } catch (e: ClassCastException) {
                log.e("[PREFERENCES] Error loading game mode: %s", e.message)
                storage!!.remove(KEY_GAME_MODE)
                gameMode = DEFAULT_GAME_MODE
            }

            try {
                fullscreenEnabled =
                    storage!!.getBoolean(KEY_FULLSCREEN_ENABLED, DEFAULT_FULLSCREEN_ENABLED)
            } catch (e: ClassCastException) {
                log.e("[PREFERENCES] Error loading fullscreen enabled: %s", e.message)
                storage!!.remove(KEY_FULLSCREEN_ENABLED)
                fullscreenEnabled = DEFAULT_FULLSCREEN_ENABLED
            }

            try {
                minSolutionMoves =
                    storage!!.getInt(KEY_MIN_SOLUTION_MOVES, DEFAULT_MIN_SOLUTION_MOVES)
            } catch (e: ClassCastException) {
                log.e("[PREFERENCES] Error loading min solution moves: %s", e.message)
                storage!!.remove(KEY_MIN_SOLUTION_MOVES)
                minSolutionMoves = DEFAULT_MIN_SOLUTION_MOVES
            }

            try {
                maxSolutionMoves =
                    storage!!.getInt(KEY_MAX_SOLUTION_MOVES, DEFAULT_MAX_SOLUTION_MOVES)
            } catch (e: ClassCastException) {
                log.e("[PREFERENCES] Error loading max solution moves: %s", e.message)
                storage!!.remove(KEY_MAX_SOLUTION_MOVES)
                maxSolutionMoves = DEFAULT_MAX_SOLUTION_MOVES
            }

            try {
                allowMulticolorTarget =
                    storage!!.getBoolean(KEY_ALLOW_MULTICOLOR_TARGET, DEFAULT_ALLOW_MULTICOLOR_TARGET)
            } catch (e: ClassCastException) {
                log.e("[PREFERENCES] Error loading allow multicolor target: %s", e.message)
                storage!!.remove(KEY_ALLOW_MULTICOLOR_TARGET)
                allowMulticolorTarget = DEFAULT_ALLOW_MULTICOLOR_TARGET
            }

            try {
                highContrastMode =
                    storage!!.getBoolean(KEY_HIGH_CONTRAST_MODE, DEFAULT_HIGH_CONTRAST_MODE)
            } catch (e: ClassCastException) {
                log.e("[PREFERENCES] Error loading high contrast mode: %s", e.message)
                storage!!.remove(KEY_HIGH_CONTRAST_MODE)
                highContrastMode = DEFAULT_HIGH_CONTRAST_MODE
            }

            try {
                backgroundSoundVolume =
                    storage!!.getInt(KEY_BACKGROUND_SOUND_VOLUME, DEFAULT_BACKGROUND_SOUND_VOLUME)
                log.d(
                    "[PREFERENCES] Loaded background sound volume: %d (default: %d)",
                    backgroundSoundVolume,
                    DEFAULT_BACKGROUND_SOUND_VOLUME
                )
            } catch (e: ClassCastException) {
                log.e("[PREFERENCES] Error loading background sound volume: %s", e.message)
                storage!!.remove(KEY_BACKGROUND_SOUND_VOLUME)
                backgroundSoundVolume = DEFAULT_BACKGROUND_SOUND_VOLUME
                log.d(
                    "[PREFERENCES] Reset background sound volume to default: %d",
                    DEFAULT_BACKGROUND_SOUND_VOLUME
                )
            }

            try {
                liveMoveCounterEnabled = storage!!.getBoolean(
                    KEY_LIVE_MOVE_COUNTER_ENABLED,
                    DEFAULT_LIVE_MOVE_COUNTER_ENABLED
                )
            } catch (e: ClassCastException) {
                log.e("[PREFERENCES] Error loading live move counter enabled: %s", e.message)
                storage!!.remove(KEY_LIVE_MOVE_COUNTER_ENABLED)
                liveMoveCounterEnabled = DEFAULT_LIVE_MOVE_COUNTER_ENABLED
            }

            try {
                hintAutoMoveEnabled =
                    storage!!.getBoolean(KEY_HINT_AUTO_MOVE_ENABLED, DEFAULT_HINT_AUTO_MOVE_ENABLED)
            } catch (e: ClassCastException) {
                log.e("[PREFERENCES] Error loading hint auto move enabled: %s", e.message)
                storage!!.remove(KEY_HINT_AUTO_MOVE_ENABLED)
                hintAutoMoveEnabled = DEFAULT_HINT_AUTO_MOVE_ENABLED
            }

            try {
                hintAutoMoveMode =
                    storage!!.getInt(KEY_HINT_AUTO_MOVE_MODE, DEFAULT_HINT_AUTO_MOVE_MODE)
            } catch (e: ClassCastException) {
                // Migration: Old version stored this as boolean, new version uses int (0, 1, 2)
                log.w("[PREFERENCES] Migrating hint auto move mode from boolean to int")
                try {
                    val oldBoolValue = storage!!.getBoolean(KEY_HINT_AUTO_MOVE_MODE, false)
                    hintAutoMoveMode = if (oldBoolValue) 1 else 0 // Convert: false->0, true->1
                    // Save migrated value as int
                    storage!!.remove(KEY_HINT_AUTO_MOVE_MODE)
                    storage!!.putInt(KEY_HINT_AUTO_MOVE_MODE, hintAutoMoveMode)
                    log.d(
                        "[PREFERENCES] Migrated hint auto move mode: %b -> %d",
                        oldBoolValue,
                        hintAutoMoveMode
                    )
                } catch (e2: Exception) {
                    log.e("[PREFERENCES] Failed to migrate hint auto move mode: %s", e2.message)
                    storage!!.remove(KEY_HINT_AUTO_MOVE_MODE)
                    hintAutoMoveMode = DEFAULT_HINT_AUTO_MOVE_MODE
                }
            }

            try {
                soundEffectsVolume =
                    storage!!.getInt(KEY_SOUND_EFFECTS_VOLUME, DEFAULT_SOUND_EFFECTS_VOLUME)
            } catch (e: ClassCastException) {
                log.e("[PREFERENCES] Error loading sound effects volume: %s", e.message)
                storage!!.remove(KEY_SOUND_EFFECTS_VOLUME)
                soundEffectsVolume = DEFAULT_SOUND_EFFECTS_VOLUME
            }


            // For compatibility with existing code
            boardSizeX = boardSizeWidth
            boardSizeY = boardSizeHeight
        } catch (e: Exception) {
            // Catch any other unexpected errors
            log.e("[PREFERENCES] Unexpected error loading preferences: %s", e.message)
            e.printStackTrace()


            // Reset to defaults
            resetToDefaults()
        }

        log.d(
            "[PREFERENCES] Cached values loaded - robotCount: %d, targetColors: %d, difficulty: %d, boardSize: %dx%d",
            robotCount, targetColors, difficulty, boardSizeWidth, boardSizeHeight
        )
    }

    /**
     * Reset all preferences to default values
     */
    private fun resetToDefaults() {
        robotCount = DEFAULT_ROBOT_COUNT
        targetColors = DEFAULT_TARGET_COLORS
        soundEnabled = DEFAULT_SOUND_ENABLED
        difficulty = DEFAULT_DIFFICULTY
        boardSizeWidth = DEFAULT_BOARD_SIZE_WIDTH
        boardSizeHeight = DEFAULT_BOARD_SIZE_HEIGHT
        boardSizeX = boardSizeWidth
        boardSizeY = boardSizeHeight
        generateNewMapEachTime = DEFAULT_GENERATE_NEW_MAP
        accessibilityMode = DEFAULT_ACCESSIBILITY_MODE
        appLanguage = DEFAULT_APP_LANGUAGE
        talkbackLanguage = DEFAULT_TALKBACK_LANGUAGE
        gameMode = DEFAULT_GAME_MODE
        fullscreenEnabled = DEFAULT_FULLSCREEN_ENABLED
        minSolutionMoves = DEFAULT_MIN_SOLUTION_MOVES
        maxSolutionMoves = DEFAULT_MAX_SOLUTION_MOVES
        allowMulticolorTarget = DEFAULT_ALLOW_MULTICOLOR_TARGET
        highContrastMode = DEFAULT_HIGH_CONTRAST_MODE
        backgroundSoundVolume = DEFAULT_BACKGROUND_SOUND_VOLUME
        liveMoveCounterEnabled = DEFAULT_LIVE_MOVE_COUNTER_ENABLED
        soundEffectsVolume = DEFAULT_SOUND_EFFECTS_VOLUME


        // Clear all preferences
        if (storage != null) {
            storage!!.clear()
            log.d("[PREFERENCES] Reset all preferences to defaults")
        }
    }

    /**
     * Notify listeners that preferences have changed
     */
    private fun notifyPreferencesChanged() {
        if (preferenceChangeListener != null) {
            preferenceChangeListener!!.onPreferencesChanged()
        }
    }

    /**
     * Set the robot count and save to preferences
     * @param count Number of robots
     */
    @JvmStatic
    fun setRobotCount(count: Int) {
        // Ensure preferences are initialized
        if (storage == null) {
            log.w("[PREFERENCES] Storage is null in setRobotCount, attempting to initialize")
            val provided = storageProvider?.invoke()
            if (provided != null) {
                initialize(provided)
            } else {
                log.e("[PREFERENCES] Cannot initialize preferences: context is null")
                // Set the static field but don't save to preferences
                robotCount = max(1, min(Constants.NUM_ROBOTS, count))
                return
            }
        }


        // Ensure count is between 1 and NUM_ROBOTS
        val validCount = max(1, min(Constants.NUM_ROBOTS, count))


        // Save to preferences
        storage!!.putInt(KEY_ROBOT_COUNT, validCount)


        // Update static field
        robotCount = validCount


        // Notify listeners
        notifyPreferencesChanged()
        log.d("[PREFERENCES] Robot count set to %d", validCount)
    }

    /**
     * Set the target colors count and save to preferences
     * @param count Number of target colors
     */
    @JvmStatic
    fun setTargetColors(count: Int) {
        // Ensure preferences are initialized
        if (storage == null) {
            log.w("[PREFERENCES] Storage is null in setTargetColors, attempting to initialize")
            val provided = storageProvider?.invoke()
            if (provided != null) {
                initialize(provided)
            } else {
                log.e("[PREFERENCES] Cannot initialize preferences: context is null")
                // Set the static field but don't save to preferences
                targetColors = max(1, min(Constants.NUM_ROBOTS, count))
                return
            }
        }


        // Ensure count is between 1 and Constants.NUM_ROBOTS
        val validCount = max(1, min(Constants.NUM_ROBOTS, count))


        // Save to preferences
        storage!!.putInt(KEY_TARGET_COLORS, validCount)


        // Update static field
        targetColors = validCount


        // Notify listeners
        notifyPreferencesChanged()
        log.d("[PREFERENCES] Target colors set to %d", validCount)
    }

    /**
     * Set the sound enabled state and save to preferences
     * @param enabled True to enable sound, false to disable
     */
    @JvmStatic
    fun setSoundEnabled(enabled: Boolean) {
        // Ensure preferences are initialized
        if (storage == null) {
            log.w("[PREFERENCES] Storage is null in setSoundEnabled, attempting to initialize")
            val provided = storageProvider?.invoke()
            if (provided != null) {
                initialize(provided)
            } else {
                log.e("[PREFERENCES] Cannot initialize preferences: context is null")
                // Set the static field but don't save to preferences
                soundEnabled = enabled
                return
            }
        }


        // Save to preferences
        storage!!.putBoolean(KEY_SOUND_ENABLED, enabled)


        // Update static field
        soundEnabled = enabled


        // Notify listeners
        notifyPreferencesChanged()
        log.d("[PREFERENCES] Sound enabled set to %s", enabled)
    }

    /**
     * Set the background sound volume and save to preferences
     * @param volume Volume level 0-100 (0 = off)
     */
    @JvmStatic
    fun setBackgroundSoundVolume(volume: Int) {
        if (storage == null) {
            log.w("[PREFERENCES] SharedPreferences is null in setBackgroundSoundVolume, attempting to initialize")
            val provided = storageProvider?.invoke()
            if (provided != null) {
                initialize(provided)
            } else {
                log.e("[PREFERENCES] Cannot initialize preferences: context is null")
                backgroundSoundVolume = volume
                return
            }
        }

        storage!!.putInt(KEY_BACKGROUND_SOUND_VOLUME, volume)

        backgroundSoundVolume = volume

        notifyPreferencesChanged()
        log.d("[PREFERENCES] Background sound volume set to %d", volume)
    }

    /**
     * Set the live move counter enabled state and save to preferences
     * @param enabled True to enable live move counter, false to disable
     */
    @JvmStatic
    fun setLiveMoveCounterEnabled(enabled: Boolean) {
        if (storage == null) {
            log.w("[PREFERENCES] SharedPreferences is null in setLiveMoveCounterEnabled, attempting to initialize")
            val provided = storageProvider?.invoke()
            if (provided != null) {
                initialize(provided)
            } else {
                log.e("[PREFERENCES] Cannot initialize preferences: context is null")
                liveMoveCounterEnabled = enabled
                return
            }
        }

        storage!!.putBoolean(KEY_LIVE_MOVE_COUNTER_ENABLED, enabled)

        liveMoveCounterEnabled = enabled

        log.d("[PREFERENCES] Live move counter enabled set to %s", enabled)
    }

    /**
     * Set the hint auto-move mode and save to preferences
     * @param mode 0=Manual, 1=Full-Auto, 2=Semi-Auto (move on next-hint button)
     */
    @JvmStatic
    fun setHintAutoMoveMode(mode: Int) {
        if (storage == null) {
            log.w("[PREFERENCES] SharedPreferences is null in setHintAutoMoveMode, attempting to initialize")
            val provided = storageProvider?.invoke()
            if (provided != null) {
                initialize(provided)
            } else {
                log.e("[PREFERENCES] Cannot initialize preferences: context is null")
                hintAutoMoveMode = mode
                return
            }
        }

        storage!!.putInt(KEY_HINT_AUTO_MOVE_MODE, mode)

        hintAutoMoveMode = mode

        notifyPreferencesChanged()
        log.d("[PREFERENCES] Hint auto-move mode set to %d", mode)
    }

    /**
     * Set the sound effects volume and save to preferences
     * @param volume Volume level 0-100
     */
    @JvmStatic
    fun setSoundEffectsVolume(volume: Int) {
        if (storage == null) {
            log.w("[PREFERENCES] SharedPreferences is null in setSoundEffectsVolume, attempting to initialize")
            val provided = storageProvider?.invoke()
            if (provided != null) {
                initialize(provided)
            } else {
                log.e("[PREFERENCES] Cannot initialize preferences: context is null")
                soundEffectsVolume = volume
                return
            }
        }

        storage!!.putInt(KEY_SOUND_EFFECTS_VOLUME, volume)

        soundEffectsVolume = volume

        notifyPreferencesChanged()
        log.d("[PREFERENCES] Sound effects volume set to %d", volume)
    }

    /**
     * Set the difficulty level and save to preferences
     * @param difficultyLevel Difficulty level (0-3)
     */
    @JvmStatic
    fun setDifficulty(difficultyLevel: Int) {
        // Ensure preferences are initialized
        if (storage == null) {
            log.w("[PREFERENCES] SharedPreferences is null in setDifficulty, attempting to initialize")
            val provided = storageProvider?.invoke()
            if (provided != null) {
                initialize(provided)
            } else {
                log.e("[PREFERENCES] Cannot initialize preferences: context is null")
                // Set the static field but don't save to preferences
                difficulty = max(
                    Constants.DIFFICULTY_BEGINNER,
                    min(Constants.DIFFICULTY_IMPOSSIBLE, difficultyLevel)
                )
                return
            }
        }


        // Ensure difficulty level is between DIFFICULTY_BEGINNER and DIFFICULTY_IMPOSSIBLE
        val validDifficulty = max(
            Constants.DIFFICULTY_BEGINNER,
            min(Constants.DIFFICULTY_IMPOSSIBLE, difficultyLevel)
        )


        // Save to preferences
        storage!!.putInt(KEY_DIFFICULTY, validDifficulty)


        // Update static field
        difficulty = validDifficulty


        // Notify listeners
        notifyPreferencesChanged()
        log.d("[PREFERENCES] Difficulty set to %d", validDifficulty)
    }

    /**
     * Set the board size and save to preferences
     * @param width Board width
     * @param height Board height
     */
    @JvmStatic
    fun setBoardSize(width: Int, height: Int) {
        // Ensure preferences are initialized
        if (storage == null) {
            log.w("[PREFERENCES] SharedPreferences is null in setBoardSize, attempting to initialize")
            val provided = storageProvider?.invoke()
            if (provided != null) {
                initialize(provided)
            } else {
                log.e("[PREFERENCES] Cannot initialize preferences: context is null")
                // Set the static field but don't save to preferences
                boardSizeWidth = max(Constants.MIN_BOARD_SIZE, min(Constants.MAX_BOARD_SIZE, width))
                boardSizeHeight =
                    max(Constants.MIN_BOARD_SIZE, min(Constants.MAX_BOARD_SIZE, height))
                boardSizeX = boardSizeWidth
                boardSizeY = boardSizeHeight
                return
            }
        }


        // Ensure width and height are between MIN_BOARD_SIZE and MAX_BOARD_SIZE
        val validWidth = max(Constants.MIN_BOARD_SIZE, min(Constants.MAX_BOARD_SIZE, width))
        val validHeight = max(Constants.MIN_BOARD_SIZE, min(Constants.MAX_BOARD_SIZE, height))


        // Save to preferences
        storage!!.putInt(KEY_BOARD_SIZE_WIDTH, validWidth)
        storage!!.putInt(KEY_BOARD_SIZE_HEIGHT, validHeight)


        // Update static fields
        boardSizeWidth = validWidth
        boardSizeHeight = validHeight


        // For compatibility with existing code
        boardSizeX = validWidth
        boardSizeY = validHeight


        // Notify listeners
        notifyPreferencesChanged()
        log.d("[PREFERENCES] Board size set to %dx%d", validWidth, validHeight)
    }

    /**
     * Set whether to generate a new map each time
     * @param generateNewMapEachTime Whether to generate a new map each time
     */
    @JvmStatic
    fun setGenerateNewMapEachTime(generateNewMapEachTime: Boolean) {
        if (storage == null) {
            log.e("[PREFERENCES] Cannot save preference: SharedPreferences not initialized")
            return
        }

        storage!!.putBoolean(KEY_GENERATE_NEW_MAP, generateNewMapEachTime)


        // Update cached value
        Preferences.generateNewMapEachTime = generateNewMapEachTime

        log.d("[PREFERENCES] Generate new map set to %s", generateNewMapEachTime)
    }

    /**
     * Set whether to generate a new map each time and save to preferences
     * @param generateNewMapEachTime Whether to generate a new map each time
     */
    fun setgenerateNewMapEachTime(generateNewMapEachTime: Boolean) {
        // Ensure preferences are initialized
        if (storage == null) {
            log.w("[PREFERENCES] SharedPreferences is null in setgenerateNewMapEachTime, attempting to initialize")
            val provided = storageProvider?.invoke()
            if (provided != null) {
                initialize(provided)
            } else {
                log.e("[PREFERENCES] Cannot initialize preferences: context is null")
                // Set the static field but don't save to preferences
                Preferences.generateNewMapEachTime = generateNewMapEachTime
                return
            }
        }


        // Save to preferences
        storage!!.putBoolean(KEY_GENERATE_NEW_MAP, generateNewMapEachTime)


        // Update static field
        Preferences.generateNewMapEachTime = generateNewMapEachTime


        // Notify listeners
        notifyPreferencesChanged()
        log.d("[PREFERENCES] Generate new map set to %s", generateNewMapEachTime)
    }

    /**
     * Set whether accessibility mode is enabled and save to preferences
     * @param enabled True if accessibility mode is enabled, false otherwise
     */
    @JvmStatic
    fun setAccessibilityMode(enabled: Boolean) {
        // Ensure preferences are initialized
        if (storage == null) {
            log.w("[PREFERENCES] SharedPreferences is null in setAccessibilityMode, attempting to initialize")
            val provided = storageProvider?.invoke()
            if (provided != null) {
                initialize(provided)
            } else {
                log.e("[PREFERENCES] Cannot initialize preferences: context is null")
                // Set the static field but don't save to preferences
                accessibilityMode = enabled
                return
            }
        }


        // Save to preferences
        storage!!.putBoolean(KEY_ACCESSIBILITY_MODE, enabled)


        // If accessibility mode is being enabled, also set accessibility-friendly defaults
        if (enabled) {
            // For accessibility mode, use a smaller 8x8 board size
            storage!!.putInt(KEY_BOARD_SIZE_WIDTH, 8)
            storage!!.putInt(KEY_BOARD_SIZE_HEIGHT, 8)


            // For accessibility mode, disable "Generate new map each time"
            storage!!.putBoolean(KEY_GENERATE_NEW_MAP, false)


            // Update cached values
            boardSizeWidth = 8
            boardSizeHeight = 8
            boardSizeX = 8
            boardSizeY = 8
            generateNewMapEachTime = false

            log.d("[PREFERENCES] Setting accessibility-friendly defaults: board size=8x8, generate new map=false")
        }


        // Update static field
        accessibilityMode = enabled


        // Notify listeners
        notifyPreferencesChanged()
        log.d("[PREFERENCES] Accessibility mode set to %s", enabled)
    }

    /**
     * Set the app language and save to preferences
     * @param language App language
     */
    @JvmStatic
    fun setAppLanguage(language: String?) {
        // Ensure preferences are initialized
        if (storage == null) {
            log.w("[PREFERENCES] SharedPreferences is null in setAppLanguage, attempting to initialize")
            val provided = storageProvider?.invoke()
            if (provided != null) {
                initialize(provided)
            } else {
                log.e("[PREFERENCES] Cannot initialize preferences: context is null")
                // Set the static field but don't save to preferences
                appLanguage = language
                return
            }
        }


        // Save to preferences
        if (language != null) {
            storage!!.putString(KEY_APP_LANGUAGE, language)
        }


        // Update static field
        appLanguage = language


        // Notify listeners
        notifyPreferencesChanged()
        log.d("[PREFERENCES] App language set to %s", language)
    }

    /**
     * Set the talkback language and save to preferences
     * @param language Talkback language
     */
    @JvmStatic
    fun setTalkbackLanguage(language: String?) {
        // Ensure preferences are initialized
        if (storage == null) {
            log.w("[PREFERENCES] SharedPreferences is null in setTalkbackLanguage, attempting to initialize")
            val provided = storageProvider?.invoke()
            if (provided != null) {
                initialize(provided)
            } else {
                log.e("[PREFERENCES] Cannot initialize preferences: context is null")
                // Set the static field but don't save to preferences
                talkbackLanguage = language
                return
            }
        }


        // Save to preferences
        if (language != null) {
            storage!!.putString(KEY_TALKBACK_LANGUAGE, language)
        }


        // Update static field
        talkbackLanguage = language


        // Notify listeners
        notifyPreferencesChanged()
        log.d("[PREFERENCES] Talkback language set to %s", language)
    }

    /**
     * Set the game mode and save to preferences
     * @param gameMode Game mode
     */
    @JvmStatic
    fun setGameMode(gameMode: Int) {
        // Ensure preferences are initialized
        if (storage == null) {
            log.w("[PREFERENCES] SharedPreferences is null in setGameMode, attempting to initialize")
            val provided = storageProvider?.invoke()
            if (provided != null) {
                initialize(provided)
            } else {
                log.e("[PREFERENCES] Cannot initialize preferences: context is null")
                // Set the static field but don't save to preferences
                Preferences.gameMode = gameMode
                return
            }
        }


        // Save to preferences
        storage!!.putInt(KEY_GAME_MODE, gameMode)


        // Update static field
        Preferences.gameMode = gameMode


        // Notify listeners
        notifyPreferencesChanged()
        log.d("[PREFERENCES] Game mode set to %d", gameMode)
    }

    /**
     * Set whether fullscreen mode is enabled and save to preferences
     * @param enabled True if fullscreen mode is enabled, false otherwise
     */
    @JvmStatic
    fun setFullscreenEnabled(enabled: Boolean) {
        // Ensure preferences are initialized
        if (storage == null) {
            log.w("[PREFERENCES] SharedPreferences is null in setFullscreenEnabled, attempting to initialize")
            val provided = storageProvider?.invoke()
            if (provided != null) {
                initialize(provided)
            } else {
                log.e("[PREFERENCES] Cannot initialize preferences: context is null")
                // Set the static field but don't save to preferences
                fullscreenEnabled = enabled
                return
            }
        }


        // Save to preferences
        storage!!.putBoolean(KEY_FULLSCREEN_ENABLED, enabled)


        // Update static field
        fullscreenEnabled = enabled


        // Notify listeners
        notifyPreferencesChanged()
        log.d("[PREFERENCES] Fullscreen enabled set to %s", enabled)
    }

    /**
     * Reload all preference values from disk
     * Call this if preferences might have been changed by another component
     */
    @JvmStatic
    fun reloadPreferences() {
        loadCachedValues()
        log.d("[PREFERENCES] Preferences reloaded from disk")
    }

    /**
     * Set the minimum solution moves and save to preferences
     * @param moves Minimum number of moves required for a solution
     */
    @JvmStatic
    fun setMinSolutionMoves(moves: Int) {
        if (storage == null) {
            log.w("[PREFERENCES] SharedPreferences is null in setMinSolutionMoves, attempting to initialize")
            val provided = storageProvider?.invoke()
            if (provided != null) {
                initialize(provided)
            } else {
                log.e("[PREFERENCES] Cannot initialize preferences: context is null")
                minSolutionMoves = moves
                return
            }
        }

        storage!!.putInt(KEY_MIN_SOLUTION_MOVES, moves)

        minSolutionMoves = moves
        notifyPreferencesChanged()
        log.d("[PREFERENCES] Min solution moves set to %d", moves)
    }

    /**
     * Set the maximum solution moves and save to preferences
     * @param moves Maximum number of moves required for a solution
     */
    @JvmStatic
    fun setMaxSolutionMoves(moves: Int) {
        if (storage == null) {
            log.w("[PREFERENCES] SharedPreferences is null in setMaxSolutionMoves, attempting to initialize")
            val provided = storageProvider?.invoke()
            if (provided != null) {
                initialize(provided)
            } else {
                log.e("[PREFERENCES] Cannot initialize preferences: context is null")
                maxSolutionMoves = moves
                return
            }
        }

        storage!!.putInt(KEY_MAX_SOLUTION_MOVES, moves)

        maxSolutionMoves = moves
        notifyPreferencesChanged()
        log.d("[PREFERENCES] Max solution moves set to %d", moves)
    }

    /**
     * Set whether multicolor targets are allowed and save to preferences
     * @param allowed True to allow multicolor targets, false to disallow
     */
    @JvmStatic
    fun setAllowMulticolorTarget(allowed: Boolean) {
        if (storage == null) {
            log.w("[PREFERENCES] SharedPreferences is null in setAllowMulticolorTarget, attempting to initialize")
            val provided = storageProvider?.invoke()
            if (provided != null) {
                initialize(provided)
            } else {
                log.e("[PREFERENCES] Cannot initialize preferences: context is null")
                allowMulticolorTarget = allowed
                return
            }
        }

        storage!!.putBoolean(KEY_ALLOW_MULTICOLOR_TARGET, allowed)

        allowMulticolorTarget = allowed
        notifyPreferencesChanged()
        log.d("[PREFERENCES] Allow multicolor target set to %b", allowed)
    }

    /**
     * Set high contrast mode and save to preferences
     * @param enabled True to enable high contrast mode, false to disable
     */
    @JvmStatic
    fun setHighContrastMode(enabled: Boolean) {
        if (storage == null) {
            log.w("[PREFERENCES] SharedPreferences is null in setHighContrastMode, attempting to initialize")
            val provided = storageProvider?.invoke()
            if (provided != null) {
                initialize(provided)
            } else {
                log.e("[PREFERENCES] Cannot initialize preferences: context is null")
                highContrastMode = enabled
                return
            }
        }

        storage!!.putBoolean(KEY_HIGH_CONTRAST_MODE, enabled)

        highContrastMode = enabled
        notifyPreferencesChanged()
        log.d("[PREFERENCES] High contrast mode set to %b", enabled)
    }

    /**
     * Convert linear slider value (0-100) to logarithmic volume (0.0-1.0).
     * This provides a more natural volume curve where most of the range is quiet,
     * and only the right side of the slider produces louder volumes.
     * 
     * Formula: volume = (2^(x/50) - 1) / 1.93
     * Where x is the slider value 0-100
     * At 50% slider = ~20% volume, at 100% = 100% volume
     * 
     * @param sliderValue Linear slider value 0-100
     * @return Logarithmic volume 0.0-1.0
     */
    @JvmStatic
    fun getLogarithmicVolume(sliderValue: Int): Float {
        if (sliderValue <= 0) return 0.0f
        if (sliderValue >= 100) return 1.0f


        // Normalize slider value to 0-1 range
        val normalized = sliderValue / 100.0f


        // Apply steeper logarithmic curve: (2^(2*x) - 1) / 3
        // This creates a much steeper curve where 50% slider ≈ 20% volume
        val logVolume = ((2.0.pow((2 * normalized).toDouble()) - 1) / 3).toFloat()


        // Clamp to 0-1 range
        return max(0.0f, min(1.0f, logVolume))
    }

    /**
     * Interface for listening to preference changes
     */
    interface PreferenceChangeListener {
        fun onPreferencesChanged()
    }
}
