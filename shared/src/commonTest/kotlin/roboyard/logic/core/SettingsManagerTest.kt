package roboyard.logic.core

import roboyard.logic.storage.PlatformStorage
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsManagerTest {

    private class InMemoryStorage : PlatformStorage {
        private val strings = mutableMapOf<String, String>()
        private val ints = mutableMapOf<String, Int>()
        private val longs = mutableMapOf<String, Long>()
        private val booleans = mutableMapOf<String, Boolean>()
        private val files = mutableMapOf<String, String>()

        override fun getString(key: String, defaultValue: String?): String? = strings[key] ?: defaultValue
        override fun putString(key: String, value: String) { strings[key] = value }
        override fun getInt(key: String, defaultValue: Int): Int = ints[key] ?: defaultValue
        override fun putInt(key: String, value: Int) { ints[key] = value }
        override fun getLong(key: String, defaultValue: Long): Long = longs[key] ?: defaultValue
        override fun putLong(key: String, value: Long) { longs[key] = value }
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = booleans[key] ?: defaultValue
        override fun putBoolean(key: String, value: Boolean) { booleans[key] = value }
        override fun remove(key: String) {
            strings.remove(key); ints.remove(key); longs.remove(key); booleans.remove(key)
        }
        override fun clear() {
            strings.clear(); ints.clear(); longs.clear(); booleans.clear(); files.clear()
        }
        override fun readFile(fileName: String): String = files[fileName] ?: ""
        override fun writeFile(fileName: String, content: String): Boolean {
            files[fileName] = content
            return true
        }
        override fun fileExists(fileName: String): Boolean = files.containsKey(fileName)
        override fun deleteFile(fileName: String): Boolean = files.remove(fileName) != null
        override fun getFilePath(fileName: String): String = fileName
        override fun hasSavedGames(): Boolean = false
        override fun readBitmap(fileName: String): Any? = null
        override fun writeBitmap(fileName: String, bitmap: Any?): Boolean = true
    }

    @BeforeTest
    fun setup() {
        Preferences.initialize(InMemoryStorage())
    }

    @Test
    fun test_currentState_mirrorsDefaults() {
        val state = SettingsManager.currentState()
        assertEquals(Preferences.DEFAULT_ROBOT_COUNT, state.robotCount)
        assertEquals(Preferences.DEFAULT_TARGET_COLORS, state.targetColors)
        assertEquals(Preferences.DEFAULT_SOUND_ENABLED, state.soundEnabled)
        assertEquals(Preferences.DEFAULT_DIFFICULTY, state.difficulty)
        assertEquals(Preferences.DEFAULT_BOARD_SIZE_WIDTH, state.boardSizeWidth)
        assertEquals(Preferences.DEFAULT_BOARD_SIZE_HEIGHT, state.boardSizeHeight)
        assertEquals(Preferences.DEFAULT_GENERATE_NEW_MAP, state.generateNewMapEachTime)
        assertEquals(Preferences.DEFAULT_ACCESSIBILITY_MODE, state.accessibilityMode)
        assertEquals(Preferences.DEFAULT_APP_LANGUAGE, state.appLanguage)
        assertEquals(Preferences.DEFAULT_TALKBACK_LANGUAGE, state.talkbackLanguage)
        assertEquals(Preferences.DEFAULT_GAME_MODE, state.gameMode)
        assertEquals(Preferences.DEFAULT_FULLSCREEN_ENABLED, state.fullscreenEnabled)
        assertEquals(Preferences.DEFAULT_MIN_SOLUTION_MOVES, state.minSolutionMoves)
        assertEquals(Preferences.DEFAULT_MAX_SOLUTION_MOVES, state.maxSolutionMoves)
        assertEquals(Preferences.DEFAULT_ALLOW_MULTICOLOR_TARGET, state.allowMulticolorTarget)
        assertEquals(Preferences.DEFAULT_HIGH_CONTRAST_MODE, state.highContrastMode)
        assertEquals(Preferences.DEFAULT_BACKGROUND_SOUND_VOLUME, state.backgroundSoundVolume)
        assertEquals(Preferences.DEFAULT_LIVE_MOVE_COUNTER_ENABLED, state.liveMoveCounterEnabled)
        assertEquals(Preferences.DEFAULT_HINT_AUTO_MOVE_ENABLED, state.hintAutoMoveEnabled)
        assertEquals(Preferences.DEFAULT_HINT_AUTO_MOVE_MODE, state.hintAutoMoveMode)
        assertEquals(Preferences.DEFAULT_SOUND_EFFECTS_VOLUME, state.soundEffectsVolume)
    }

    @Test
    fun test_setDifficulty_appliesPresets() {
        var state = SettingsManager.setDifficulty(Constants.DIFFICULTY_BEGINNER)
        assertEquals(Constants.DIFFICULTY_BEGINNER, state.difficulty)
        assertEquals(4, state.minSolutionMoves)
        assertEquals(6, state.maxSolutionMoves)
        assertTrue(state.allowMulticolorTarget)
        assertTrue(state.generateNewMapEachTime)

        state = SettingsManager.setDifficulty(Constants.DIFFICULTY_ADVANCED)
        assertEquals(Constants.DIFFICULTY_ADVANCED, state.difficulty)
        assertEquals(6, state.minSolutionMoves)
        assertEquals(10, state.maxSolutionMoves)
        assertFalse(state.allowMulticolorTarget)
        assertTrue(state.generateNewMapEachTime)

        state = SettingsManager.setDifficulty(Constants.DIFFICULTY_INSANE)
        assertEquals(Constants.DIFFICULTY_INSANE, state.difficulty)
        assertEquals(10, state.minSolutionMoves)
        assertEquals(99, state.maxSolutionMoves)
        assertFalse(state.allowMulticolorTarget)
        assertFalse(state.generateNewMapEachTime)
    }

    @Test
    fun test_setDifficulty_impossible_preservesGenerateNewMap() {
        SettingsManager.setGenerateNewMapEachTime(false)
        var state = SettingsManager.setDifficulty(Constants.DIFFICULTY_IMPOSSIBLE)
        assertEquals(17, state.minSolutionMoves)
        assertEquals(99, state.maxSolutionMoves)
        assertFalse(state.allowMulticolorTarget)
        assertFalse(state.generateNewMapEachTime)

        SettingsManager.setGenerateNewMapEachTime(true)
        SettingsManager.setDifficulty(Constants.DIFFICULTY_BEGINNER)
        SettingsManager.setGenerateNewMapEachTime(false)
        state = SettingsManager.setDifficulty(Constants.DIFFICULTY_IMPOSSIBLE)
        assertFalse(state.generateNewMapEachTime)
    }

    @Test
    fun test_setDifficulty_beginner_capsOversizedBoardOnlyOnTransition() {
        SettingsManager.setBoardSize(16, 16)
        var state = SettingsManager.setDifficulty(Constants.DIFFICULTY_BEGINNER)
        assertEquals(16, state.boardSizeWidth)
        assertEquals(16, state.boardSizeHeight)

        SettingsManager.setDifficulty(Constants.DIFFICULTY_ADVANCED)
        state = SettingsManager.setDifficulty(Constants.DIFFICULTY_BEGINNER)
        assertEquals(12, state.boardSizeWidth)
        assertEquals(14, state.boardSizeHeight)

        SettingsManager.setDifficulty(Constants.DIFFICULTY_ADVANCED)
        SettingsManager.setBoardSize(10, 10)
        state = SettingsManager.setDifficulty(Constants.DIFFICULTY_BEGINNER)
        assertEquals(10, state.boardSizeWidth)
        assertEquals(10, state.boardSizeHeight)
    }

    @Test
    fun test_setGameMode_coupledRobotAndTargets() {
        SettingsManager.setTargetColors(4)
        SettingsManager.setRobotCount(3)
        var state = SettingsManager.setGameMode(Constants.GAME_MODE_STANDARD)
        assertEquals(Constants.GAME_MODE_STANDARD, state.gameMode)
        assertEquals(1, state.robotCount)
        assertEquals(1, state.targetColors)

        state = SettingsManager.setGameMode(Constants.GAME_MODE_MULTI_TARGET)
        assertEquals(Constants.GAME_MODE_MULTI_TARGET, state.gameMode)
        assertEquals(2, state.targetColors)
        assertEquals(1, state.robotCount)

        Preferences.setRobotCount(4)
        Preferences.setTargetColors(2)
        state = SettingsManager.setGameMode(Constants.GAME_MODE_MULTI_TARGET)
        assertEquals(2, state.targetColors)
        assertEquals(2, state.robotCount)
    }

    @Test
    fun test_robotAndTargetCoupling() {
        SettingsManager.setTargetColors(2)
        var state = SettingsManager.setRobotCount(4)
        assertEquals(2, state.robotCount)

        SettingsManager.setTargetColors(4)
        SettingsManager.setRobotCount(3)
        state = SettingsManager.setTargetColors(2)
        assertEquals(2, state.targetColors)
        assertEquals(2, state.robotCount)

        SettingsManager.setGameMode(Constants.GAME_MODE_MULTI_TARGET)
        state = SettingsManager.setTargetColors(1)
        assertEquals(2, state.targetColors)
    }

    @Test
    fun test_minMaxMoves() {
        var state = SettingsManager.setMinSolutionMoves(1)
        assertEquals(2, state.minSolutionMoves)

        state = SettingsManager.setMinSolutionMoves(8)
        assertEquals(8, state.minSolutionMoves)
        assertEquals(8, state.maxSolutionMoves)

        state = SettingsManager.setMaxSolutionMoves(2)
        assertEquals(8, state.maxSolutionMoves)

        state = SettingsManager.setMaxSolutionMoves(45)
        assertEquals(99, state.maxSolutionMoves)

        state = SettingsManager.setMaxSolutionMoves(0)
        assertEquals(99, state.maxSolutionMoves)
    }

    @Test
    fun test_accessibilityMode_setsAccessibleDefaults() {
        SettingsManager.setBoardSize(16, 16)
        val state = SettingsManager.setAccessibilityMode(true)
        assertTrue(state.accessibilityMode)
        assertEquals(8, state.boardSizeWidth)
        assertEquals(8, state.boardSizeHeight)
        assertFalse(state.generateNewMapEachTime)
    }

    @Test
    fun test_clampingAndLanguageValidation() {
        assertEquals(100, SettingsManager.setBackgroundSoundVolume(150).backgroundSoundVolume)
        assertEquals(0, SettingsManager.setBackgroundSoundVolume(-5).backgroundSoundVolume)
        assertEquals(100, SettingsManager.setSoundEffectsVolume(999).soundEffectsVolume)
        assertEquals(0, SettingsManager.setSoundEffectsVolume(-1).soundEffectsVolume)

        assertEquals(Preferences.HINT_AUTO_MOVE_SEMI_AUTO, SettingsManager.setHintAutoMoveMode(5).hintAutoMoveMode)
        assertEquals(Preferences.HINT_AUTO_MOVE_MANUAL, SettingsManager.setHintAutoMoveMode(-1).hintAutoMoveMode)

        assertEquals("de", SettingsManager.setAppLanguage("de").appLanguage)
        assertEquals("fr", SettingsManager.setTalkbackLanguage("fr").talkbackLanguage)

        assertEquals(Preferences.DEFAULT_APP_LANGUAGE, SettingsManager.setAppLanguage("xx").appLanguage)
        assertEquals(Preferences.DEFAULT_TALKBACK_LANGUAGE, SettingsManager.setTalkbackLanguage("xx").talkbackLanguage)
    }

    @Test
    fun test_validBoardSizes() {
        val landscape = SettingsManager.validBoardSizes(1.0f, isLandscape = true)
        assertEquals(SettingsManager.ALL_BOARD_SIZES.size, landscape.size)

        val portrait = SettingsManager.validBoardSizes(1.5f, isLandscape = false)
        assertEquals(
            listOf(BoardSizeOption(8, 7), BoardSizeOption(10, 8), BoardSizeOption(16, 14)),
            portrait
        )

        val tallPortrait = SettingsManager.validBoardSizes(2.0f, isLandscape = false)
        assertEquals(SettingsManager.ALL_BOARD_SIZES.size - 3, tallPortrait.size)
        assertFalse(tallPortrait.any { it.width == 8 && it.height == 12 })
        assertFalse(tallPortrait.any { it.width == 10 && it.height == 14 })
        assertFalse(tallPortrait.any { it.width == 12 && it.height == 18 })

        val mid = SettingsManager.validBoardSizes(1.75f, isLandscape = false)
        assertTrue(mid.size > portrait.size)
        assertTrue(mid.size <= SettingsManager.ALL_BOARD_SIZES.size)
        assertFalse(mid.any { it.height.toFloat() / it.width * 1.3f > 1.5f })
    }
}
