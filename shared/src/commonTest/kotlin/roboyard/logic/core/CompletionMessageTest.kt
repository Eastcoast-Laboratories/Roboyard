package roboyard.logic.core

import roboyard.logic.ui.StringProvider
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for buildCompletionOptimalMessage — the random-game completion suffix
 * shown when the player did not match the optimal move count.
 * Shared by Android GameFragment and Compose GameScreen.
 *
 * Regression: when the player beats the A.I. (actualMoves < optimalMoves,
 * e.g. 3 vs 5) the message must be pre_hint_less_than_1
 * ("You found a better solution than the A.I.!"), not the false
 * "less than 3 moves" text.
 *
 * Run: ./gradlew :shared:desktopTest --tests "roboyard.logic.core.CompletionMessageTest"
 */
class CompletionMessageTest {

    private class TestStringProvider(private val strings: Map<String, String>) : StringProvider {
        override fun getString(name: String): String? = strings[name]
    }

    private val en = TestStringProvider(
        mapOf(
            "no_solution_found" to "No solution found",
            "pre_hint_less_than_1" to "You found a better solution than the A.I.!",
            "pre_hint_less_than_x" to "The A.I. found a solution in less than {0} moves"
        )
    )

    private val de = TestStringProvider(
        mapOf(
            "no_solution_found" to "Keine Lösung gefunden",
            "pre_hint_less_than_1" to "Du hast eine bessere Lösung als die KI gefunden!",
            "pre_hint_less_than_x" to "Die KI hat eine Lösung mit weniger als {0} Zügen gefunden"
        )
    )

    @Test
    fun playerBetterThanAi_showsBetterSolutionMessage() {
        // Player: 3 moves, A.I.: 5 moves -> better-solution message
        assertEquals(
            "You found a better solution than the A.I.!",
            buildCompletionOptimalMessage(3, 5, en)
        )
    }

    @Test
    fun playerBetterThanAi_singleMove_showsBetterSolutionMessage() {
        // The old actualMoves==1 workaround case is covered by the same branch
        assertEquals(
            "You found a better solution than the A.I.!",
            buildCompletionOptimalMessage(1, 5, en)
        )
    }

    @Test
    fun playerBetterThanAi_german_showsLocalizedMessage() {
        assertEquals(
            "Du hast eine bessere Lösung als die KI gefunden!",
            buildCompletionOptimalMessage(3, 5, de)
        )
    }

    @Test
    fun playerWorseThanAi_showsLessThanXWithActualMoves() {
        // Player: 8 moves, A.I.: 5 moves -> "less than 8 moves"
        assertEquals(
            "The A.I. found a solution in less than 8 moves",
            buildCompletionOptimalMessage(8, 5, en)
        )
    }

    @Test
    fun noOptimalSolution_showsNoSolutionFound() {
        assertEquals(
            "Keine Lösung gefunden!",
            buildCompletionOptimalMessage(5, 0, de)
        )
    }

    @Test
    fun nullProvider_fallsBackToEnglish() {
        assertEquals(
            "You found a better solution than the A.I.!",
            buildCompletionOptimalMessage(2, 4, null)
        )
        assertEquals(
            "The A.I. found a solution in less than 9 moves",
            buildCompletionOptimalMessage(9, 4, null)
        )
    }
}
