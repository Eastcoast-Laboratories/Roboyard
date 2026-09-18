package roboyard.logic.core

import roboyard.logic.ui.StringProvider

/**
 * Shared game utility functions used by both the Android app and ComposeApp.
 * Extracted to avoid duplication (DRY).
 */

/**
 * Format elapsed time as MM:SS.
 * @param elapsedTimeMs Time in milliseconds
 * @return Formatted string like "3:45"
 */
fun formatTime(elapsedTimeMs: Long): String {
    val seconds = (elapsedTimeMs / 1000) % 60
    val minutes = (elapsedTimeMs / 1000) / 60
    return String.format("%d:%02d", minutes, seconds)
}

/**
 * Format elapsed time with adaptive format.
 * Uses mm:ss for times under 100 minutes, hh:mm:ss for longer times.
 * @param elapsedTimeMs Time in milliseconds
 * @return Formatted string like "03:45" or "01:23:45"
 */
fun formatElapsedTime(elapsedTimeMs: Long): String {
    val totalSeconds = (elapsedTimeMs / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return if (minutes < 100) {
        String.format("%02d:%02d", minutes, seconds)
    } else {
        val hours = minutes / 60
        val mins = minutes % 60
        String.format("%02d:%02d:%02d", hours, mins, seconds)
    }
}

/**
 * Build a game win message based on performance.
 * @param moveCount Number of moves used
 * @param isLevelGame Whether this is a level game (vs random game)
 * @param optimalMoves Optimal number of moves, or null if not known
 * @param stars Stars earned (1-3)
 * @return Win message string
 */
fun buildGameWinMessage(
    moveCount: Int,
    isLevelGame: Boolean = false,
    optimalMoves: Int? = null,
    stars: Int = 0
): String {
    val baseMessage = if (isLevelGame) {
        "Level completed in $moveCount moves!"
    } else {
        "Game completed in $moveCount moves!"
    }

    if (optimalMoves != null) {
        if (moveCount == optimalMoves) {
            return "$baseMessage Perfect solution! Stars: $stars"
        } else {
            return "$baseMessage (Optimal: $optimalMoves moves) Stars: $stars"
        }
    }

    return "$baseMessage Stars: $stars"
}

/**
 * Build the optimal-solution suffix appended to the random-game completion
 * message when the player did NOT match the optimal move count.
 * Shared between Android GameFragment and Compose GameScreen so both apps
 * show identical messages.
 *
 * @param actualMoves Moves the player used
 * @param optimalMoves Moves in the solver's solution
 * @param stringProvider Localized string lookup, may be null (English fallback)
 * @return Localized message for the completion screen
 */
fun buildCompletionOptimalMessage(
    actualMoves: Int,
    optimalMoves: Int,
    stringProvider: StringProvider?
): String = when {
    optimalMoves < 1 ->
        (stringProvider?.getString("no_solution_found") ?: "No solution found") + "!"
    actualMoves < optimalMoves ->
        stringProvider?.getString("pre_hint_less_than_1")
            ?: "You found a better solution than the A.I.!"
    else ->
        stringProvider?.getString("pre_hint_less_than_x", actualMoves)
            ?: "The A.I. found a solution in less than $actualMoves moves"
}
