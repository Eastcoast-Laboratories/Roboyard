package roboyard.logic.core

import kotlin.math.max
import kotlin.math.min

data class BoardSizeOption(val width: Int, val height: Int) {
    override fun toString(): String = "${width}x${height}"
}

data class LanguageOption(val code: String, val displayName: String)

data class SettingsState(
    val robotCount: Int,
    val targetColors: Int,
    val soundEnabled: Boolean,
    val difficulty: Int,
    val boardSizeWidth: Int,
    val boardSizeHeight: Int,
    val generateNewMapEachTime: Boolean,
    val accessibilityMode: Boolean,
    val appLanguage: String,
    val talkbackLanguage: String,
    val gameMode: Int,
    val fullscreenEnabled: Boolean,
    val minSolutionMoves: Int,
    val maxSolutionMoves: Int,
    val allowMulticolorTarget: Boolean,
    val highContrastMode: Boolean,
    val backgroundSoundVolume: Int,
    val liveMoveCounterEnabled: Boolean,
    val hintAutoMoveEnabled: Boolean,
    val hintAutoMoveMode: Int,
    val soundEffectsVolume: Int
)

object SettingsManager {

    @JvmField
    val ALL_BOARD_SIZES: List<BoardSizeOption> = listOf(
        BoardSizeOption(8, 7), BoardSizeOption(8, 8), BoardSizeOption(8, 12),
        BoardSizeOption(10, 8), BoardSizeOption(10, 10), BoardSizeOption(10, 12), BoardSizeOption(10, 14),
        BoardSizeOption(12, 12), BoardSizeOption(12, 14), BoardSizeOption(12, 16), BoardSizeOption(12, 18),
        BoardSizeOption(14, 14), BoardSizeOption(14, 16), BoardSizeOption(14, 18),
        BoardSizeOption(16, 14), BoardSizeOption(16, 16), BoardSizeOption(16, 18), BoardSizeOption(16, 20), BoardSizeOption(16, 22),
        BoardSizeOption(18, 18), BoardSizeOption(18, 20), BoardSizeOption(18, 22)
    )

    @JvmField
    val LANGUAGES = listOf(
        LanguageOption("en", "English"), LanguageOption("de", "Deutsch"), LanguageOption("fr", "Français"),
        LanguageOption("es", "Español"), LanguageOption("zh", "中文"), LanguageOption("ko", "한국어"),
        LanguageOption("ja", "日本語"), LanguageOption("pt", "Português (Brasil)"), LanguageOption("pl", "Polski")
    )

    @JvmField
    val TALKBACK_LANGUAGES = listOf(LanguageOption("same", "Same as app language")) + LANGUAGES

    @JvmStatic
    fun currentState(): SettingsState = SettingsState(
        robotCount = Preferences.robotCount,
        targetColors = Preferences.targetColors,
        soundEnabled = Preferences.soundEnabled,
        difficulty = Preferences.difficulty,
        boardSizeWidth = Preferences.boardSizeWidth,
        boardSizeHeight = Preferences.boardSizeHeight,
        generateNewMapEachTime = Preferences.generateNewMapEachTime,
        accessibilityMode = Preferences.accessibilityMode,
        appLanguage = Preferences.appLanguage ?: Preferences.DEFAULT_APP_LANGUAGE,
        talkbackLanguage = Preferences.talkbackLanguage ?: Preferences.DEFAULT_TALKBACK_LANGUAGE,
        gameMode = Preferences.gameMode,
        fullscreenEnabled = Preferences.fullscreenEnabled,
        minSolutionMoves = Preferences.minSolutionMoves,
        maxSolutionMoves = Preferences.maxSolutionMoves,
        allowMulticolorTarget = Preferences.allowMulticolorTarget,
        highContrastMode = Preferences.highContrastMode,
        backgroundSoundVolume = Preferences.backgroundSoundVolume,
        liveMoveCounterEnabled = Preferences.liveMoveCounterEnabled,
        hintAutoMoveEnabled = Preferences.hintAutoMoveEnabled,
        hintAutoMoveMode = Preferences.hintAutoMoveMode,
        soundEffectsVolume = Preferences.soundEffectsVolume
    )

    @JvmStatic
    fun validBoardSizes(displayRatio: Float, isLandscape: Boolean): List<BoardSizeOption> {
        if (isLandscape) {
            return ALL_BOARD_SIZES
        }
        val maxBoardRatio = when {
            displayRatio <= 1.5f -> 1.2f
            displayRatio >= 2.0f -> 1.8f
            else -> 1.2f + ((displayRatio - 1.5f) / 0.5f) * 0.6f
        }
        return ALL_BOARD_SIZES.filter { (it.height.toFloat() / it.width) * 1.3 <= maxBoardRatio }
    }

    @JvmStatic
    fun setBoardSize(width: Int, height: Int): SettingsState {
        Preferences.setBoardSize(width, height)
        return currentState()
    }

    @JvmStatic
    fun setDifficulty(difficulty: Int): SettingsState {
        val previousDifficulty = Preferences.difficulty
        val validDifficulty = max(
            Constants.DIFFICULTY_BEGINNER,
            min(Constants.DIFFICULTY_IMPOSSIBLE, difficulty)
        )
        Preferences.setDifficulty(validDifficulty)

        if (validDifficulty == Constants.DIFFICULTY_BEGINNER &&
            previousDifficulty != Constants.DIFFICULTY_BEGINNER &&
            (Preferences.boardSizeWidth > 12 || Preferences.boardSizeHeight > 14)
        ) {
            Preferences.setBoardSize(12, 14)
        }

        when (validDifficulty) {
            Constants.DIFFICULTY_BEGINNER -> {
                Preferences.setMinSolutionMoves(4)
                Preferences.setMaxSolutionMoves(6)
                Preferences.setAllowMulticolorTarget(true)
                Preferences.setGenerateNewMapEachTime(true)
            }
            Constants.DIFFICULTY_ADVANCED -> {
                Preferences.setMinSolutionMoves(6)
                Preferences.setMaxSolutionMoves(10)
                Preferences.setAllowMulticolorTarget(false)
                Preferences.setGenerateNewMapEachTime(true)
            }
            Constants.DIFFICULTY_INSANE -> {
                Preferences.setMinSolutionMoves(10)
                Preferences.setMaxSolutionMoves(99)
                Preferences.setAllowMulticolorTarget(false)
                Preferences.setGenerateNewMapEachTime(false)
            }
            Constants.DIFFICULTY_IMPOSSIBLE -> {
                Preferences.setMinSolutionMoves(17)
                Preferences.setMaxSolutionMoves(99)
                Preferences.setAllowMulticolorTarget(false)
            }
        }
        return currentState()
    }

    @JvmStatic
    fun setGenerateNewMapEachTime(enabled: Boolean): SettingsState {
        Preferences.setGenerateNewMapEachTime(enabled)
        return currentState()
    }

    @JvmStatic
    fun setSoundEnabled(enabled: Boolean): SettingsState {
        Preferences.setSoundEnabled(enabled)
        return currentState()
    }

    @JvmStatic
    fun setBackgroundSoundVolume(volume: Int): SettingsState {
        Preferences.setBackgroundSoundVolume(max(0, min(100, volume)))
        return currentState()
    }

    @JvmStatic
    fun setSoundEffectsVolume(volume: Int): SettingsState {
        Preferences.setSoundEffectsVolume(max(0, min(100, volume)))
        return currentState()
    }

    @JvmStatic
    fun setAccessibilityMode(enabled: Boolean): SettingsState {
        Preferences.setAccessibilityMode(enabled)
        return currentState()
    }

    @JvmStatic
    fun setHintAutoMoveMode(mode: Int): SettingsState {
        Preferences.setHintAutoMoveMode(
            max(Preferences.HINT_AUTO_MOVE_MANUAL, min(Preferences.HINT_AUTO_MOVE_SEMI_AUTO, mode))
        )
        return currentState()
    }

    @JvmStatic
    fun setFullscreenEnabled(enabled: Boolean): SettingsState {
        Preferences.setFullscreenEnabled(enabled)
        return currentState()
    }

    @JvmStatic
    fun setGameMode(mode: Int): SettingsState {
        if (mode == Constants.GAME_MODE_MULTI_TARGET) {
            Preferences.setGameMode(Constants.GAME_MODE_MULTI_TARGET)
            val targetColors = max(2, Preferences.targetColors)
            Preferences.setTargetColors(targetColors)
            if (Preferences.robotCount > targetColors) {
                Preferences.setRobotCount(targetColors)
            }
        } else {
            Preferences.setGameMode(Constants.GAME_MODE_STANDARD)
            Preferences.setRobotCount(1)
            Preferences.setTargetColors(1)
        }
        return currentState()
    }

    @JvmStatic
    fun setRobotCount(count: Int): SettingsState {
        var validCount = max(1, min(Constants.NUM_ROBOTS, count))
        if (validCount > Preferences.targetColors) {
            validCount = Preferences.targetColors
        }
        Preferences.setRobotCount(validCount)
        return currentState()
    }

    @JvmStatic
    fun setTargetColors(count: Int): SettingsState {
        var validCount = max(1, min(Constants.NUM_ROBOTS, count))
        if (Preferences.gameMode == Constants.GAME_MODE_MULTI_TARGET && validCount < 2) {
            validCount = 2
        }
        if (validCount < Preferences.robotCount) {
            Preferences.setRobotCount(validCount)
        }
        Preferences.setTargetColors(validCount)
        return currentState()
    }

    @JvmStatic
    fun setMinSolutionMoves(moves: Int): SettingsState {
        var validMoves = moves
        val minimumRequired = Preferences.robotCount + 1
        if (validMoves < minimumRequired) {
            validMoves = minimumRequired
        }
        if (validMoves > Preferences.maxSolutionMoves) {
            Preferences.setMaxSolutionMoves(validMoves)
        }
        Preferences.setMinSolutionMoves(validMoves)
        return currentState()
    }

    @JvmStatic
    fun setMaxSolutionMoves(moves: Int): SettingsState {
        var validMoves = moves
        if (validMoves <= 0) {
            validMoves = 99
        }
        if (validMoves < Preferences.minSolutionMoves) {
            validMoves = Preferences.minSolutionMoves
        }
        if (validMoves >= 40) {
            validMoves = 99
        }
        Preferences.setMaxSolutionMoves(validMoves)
        return currentState()
    }

    @JvmStatic
    fun setAllowMulticolorTarget(enabled: Boolean): SettingsState {
        Preferences.setAllowMulticolorTarget(enabled)
        return currentState()
    }

    @JvmStatic
    fun setHighContrastMode(enabled: Boolean): SettingsState {
        Preferences.setHighContrastMode(enabled)
        return currentState()
    }

    @JvmStatic
    fun setAppLanguage(code: String): SettingsState {
        val validCode = if (LANGUAGES.any { it.code == code }) code else Preferences.DEFAULT_APP_LANGUAGE
        Preferences.setAppLanguage(validCode)
        return currentState()
    }

    @JvmStatic
    fun setTalkbackLanguage(code: String): SettingsState {
        val validCode =
            if (TALKBACK_LANGUAGES.any { it.code == code }) code else Preferences.DEFAULT_TALKBACK_LANGUAGE
        Preferences.setTalkbackLanguage(validCode)
        return currentState()
    }
}
