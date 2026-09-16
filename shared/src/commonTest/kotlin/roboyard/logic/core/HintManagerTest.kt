package roboyard.logic.core

import driftingdroids.model.Board
import roboyard.logic.ui.StringProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for HintManager — verifies hint display logic matches Android.
 * Tests pre-hints, regular hints, color-coded backgrounds, and navigation.
 */
class HintManagerTest {

    /**
     * Simple test StringProvider that returns English strings.
     */
    private class TestStringProvider : StringProvider {
        private val strings = mapOf(
            "pre_hint_less_than_x" to "The A.I. found a solution in less than {0} moves",
            "pre_hint_less_than_1" to "You found a better solution than the A.I.!",
            "pre_hint_exact_solution" to "The A.I. found a solution in {0} moves",
            "pre_hint_involved_robots" to "Move the",
            "pre_hint_first_move" to "Move the {0} robot first",
            "pre_hint_ready" to "Ready to show step-by-step hints",
            "no_solution_found" to "No solution found",
            "all_hints_shown" to "All hints have been shown",
            "error_displaying_hint" to "Error displaying hint",
            "and" to "and",
            "color_red" to "Red",
            "color_green" to "Green",
            "color_blue" to "Blue",
            "color_yellow" to "Yellow",
            "color_silver" to "Silver",
            "color_pink" to "Pink",
            "color_brown" to "Brown",
            "color_orange" to "Orange",
            "color_white" to "White",
            "color_red_dative" to "the red",
            "color_green_dative" to "the green",
            "color_blue_dative" to "the blue",
            "color_yellow_dative" to "the yellow",
            "color_silver_dative" to "the silver",
            "direction_up" to "UP",
            "direction_down" to "DOWN",
            "direction_right" to "RIGHT",
            "direction_left" to "LEFT"
        )
        override fun getString(name: String): String? = strings[name]
        override fun getString(name: String, vararg formatArgs: Any): String? {
            val template = strings[name] ?: return null
            var result = template
            for (i in formatArgs.indices) {
                result = result.replace("{$i}", formatArgs[i].toString())
            }
            return result
        }
    }

    @Test
    fun test_hintManager_notInitialized_returnsNull() {
        val provider = TestStringProvider()
        val hintManager = HintManager(provider)
        assertFalse(hintManager.hasSolution())
        assertNull(hintManager.getHintForDisplay())
    }

    @Test
    fun test_hintManager_initialized_showsPreHint() {
        val provider = TestStringProvider()
        val hintManager = HintManager(provider)
        hintManager.initializeForTest(
            moves = listOf(Pair(0, Board.NORTH), Pair(1, Board.EAST)),
            isLevelGame = false, levelId = 0
        )

        assertTrue(hintManager.hasSolution())
        val hint = hintManager.getHintForDisplay()
        assertNotNull(hint)
        assertTrue(hint.isPreHint)
        // First pre-hint should be "less than X" or similar
        assertTrue(hint.text.contains("A.I.") || hint.text.contains("solution"))
        // Pre-hint should have default background (-1)
        assertEquals(-1, hint.robotColorForBackground)
    }

    @Test
    fun test_hintManager_regularHint_showsColorAndArrow() {
        val provider = TestStringProvider()
        val hintManager = HintManager(provider)
        hintManager.initializeForTest(
            moves = listOf(Pair(0, Board.NORTH), Pair(1, Board.EAST)),
            isLevelGame = false, levelId = 0
        )

        // Navigate past all pre-hints to first regular hint
        // numPreHints=3 (fixed in test), NUM_FIXED_PRE_HINTS=3, so 6 pre-hints
        repeat(6) {
            assertTrue(hintManager.hasNextHint(), "Should have hint at step ${it}")
            hintManager.nextHint()
        }

        // Now at first regular hint (step 6)
        val hint = hintManager.getHintForDisplay()
        assertNotNull(hint)
        assertFalse(hint.isPreHint)
        // Color 0 = COLOR_PINK, so should show "Pink" and "↑"
        assertTrue(hint.text.contains("Pink"), "Hint text should contain 'Pink': ${hint.text}")
        assertTrue(hint.text.contains("↑"), "Hint text should contain '↑': ${hint.text}")
        // Background should be pink (color index 0 = COLOR_PINK)
        assertEquals(0, hint.robotColorForBackground)
    }

    @Test
    fun test_hintManager_secondHint_showsAbbreviatedHistory() {
        val provider = TestStringProvider()
        val hintManager = HintManager(provider)
        hintManager.initializeForTest(
            moves = listOf(Pair(0, Board.NORTH), Pair(1, Board.EAST)),
            isLevelGame = false, levelId = 0
        )

        // Navigate to second regular hint (past all 6 pre-hints + 1 regular hint)
        repeat(7) {
            assertTrue(hintManager.hasNextHint())
            hintManager.nextHint()
        }

        val hint = hintManager.getHintForDisplay()
        assertNotNull(hint)
        assertFalse(hint.isPreHint)
        // Color 0 = COLOR_PINK -> abbreviation "P", Color 1 = COLOR_GREEN -> "G"
        assertTrue(hint.text.contains("P"), "Hint should contain 'P' abbreviation: ${hint.text}")
        assertTrue(hint.text.contains("↑"), "Hint should contain '↑': ${hint.text}")
        assertTrue(hint.text.contains("Green"), "Hint should contain 'Green': ${hint.text}")
        assertTrue(hint.text.contains("→"), "Hint should contain '→': ${hint.text}")
    }

    @Test
    fun test_hintManager_resetStep_keepsSolution() {
        val provider = TestStringProvider()
        val hintManager = HintManager(provider)
        hintManager.initializeForTest(
            moves = listOf(Pair(0, Board.NORTH)),
            isLevelGame = false, levelId = 0
        )

        // Advance a few hints
        hintManager.nextHint()
        hintManager.nextHint()
        assertTrue(hintManager.getCurrentHintStep() > 0)

        // Reset step only
        hintManager.resetStep()
        assertEquals(0, hintManager.getCurrentHintStep())
        assertTrue(hintManager.hasSolution(), "Solution should still be set after resetStep")

        // Should still be able to get hints
        val hint = hintManager.getHintForDisplay()
        assertNotNull(hint)
    }

    @Test
    fun test_hintManager_reset_clearsSolution() {
        val provider = TestStringProvider()
        val hintManager = HintManager(provider)
        hintManager.initializeForTest(
            moves = listOf(Pair(0, Board.NORTH)),
            isLevelGame = false, levelId = 0
        )

        hintManager.reset()
        assertFalse(hintManager.hasSolution())
        assertNull(hintManager.getHintForDisplay())
    }

    @Test
    fun test_hintManager_levelRestrictions_levelAbove10() {
        val provider = TestStringProvider()
        val hintManager = HintManager(provider)
        hintManager.initializeForTest(
            moves = listOf(Pair(0, Board.NORTH)),
            isLevelGame = true, levelId = 15
        )

        assertTrue(hintManager.isHintsRestricted())
        assertEquals(0, hintManager.getMaxHintsAllowed())
    }

    @Test
    fun test_hintManager_levelRestrictions_level1to10() {
        val provider = TestStringProvider()
        val hintManager = HintManager(provider)
        hintManager.initializeForTest(
            moves = listOf(Pair(0, Board.NORTH)),
            isLevelGame = true, levelId = 5
        )

        assertFalse(hintManager.isHintsRestricted())
        assertEquals(4, hintManager.getMaxHintsAllowed())
    }

    @Test
    fun test_hintManager_preHintColors_moveFirstHint() {
        val provider = TestStringProvider()
        val hintManager = HintManager(provider)
        hintManager.initializeForTest(
            moves = listOf(
                Pair(2, Board.NORTH),  // Blue up (first move)
                Pair(0, Board.EAST)    // Pink right (color 0 = COLOR_PINK)
            ),
            isLevelGame = false, levelId = 0
        )

        // Navigate to "move first" pre-hint (last fixed pre-hint)
        // numPreHints=3, so step 5 (0-indexed) = numPreHints + 2 = 5
        repeat(5) {
            hintManager.nextHint()
        }

        val hint = hintManager.getHintForDisplay()
        assertNotNull(hint)
        assertTrue(hint.isPreHint)
        // "Move first" hint should use the first robot's color (Blue = 2)
        assertEquals(2, hint.robotColorForBackground,
            "Move-first pre-hint should use first robot's color (Blue=2)")
        // Should contain "the blue" (dative form)
        assertTrue(hint.text.contains("blue") || hint.text.contains("Blue"),
            "Move-first hint should mention blue robot: ${hint.text}")
    }
}
