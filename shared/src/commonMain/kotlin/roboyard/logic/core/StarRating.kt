package roboyard.logic.core

import roboyard.logic.managers.LevelCompletionManager

/**
 * Calculate star rating based on player performance
 *
 * Star allocation rules (from HOW-TO-PLAY.md):
 * - 3 stars: Complete the level in optimal moves
 * - 2 stars: Complete the level within optimal moves + 1
 * - 1 star: Complete the level (any number of moves)
 *
 * @param playerMoves  Number of moves used by player
 * @param optimalMoves Optimal number of moves from solver
 * @param hintsUsed    Number of hints used (not used in current logic)
 * @return Number of stars earned (1-3)
 */
fun calculateStars(playerMoves: Int, optimalMoves: Int, hintsUsed: Int): Int {
    if (optimalMoves <= 0) {
        return 1 // No optimal solution available, but level completed
    }

    // Calculate stars based on the rules
    if (playerMoves == optimalMoves) {
        // Optimal solution
        return 3
    } else if (playerMoves <= optimalMoves + 1) {
        // Within optimal moves + 1
        return 2
    } else {
        // Any number of moves (level completed)
        return 1
    }
}

/**
 * Calculate the star rating shown for a history entry (display only — the
 * level-completion rating above stays untouched).
 *
 * Star allocation rules:
 * - Optimal solutions up to 10 moves:
 *   - 3 stars: optimal moves or better
 *   - 2 stars: one move over optimal
 *   - 1 star: two moves over optimal
 *   - 0 stars: more than two moves over optimal
 * - Optimal solutions above 10 moves (linear scaling):
 *   - 3 stars: optimal moves or better
 *   - 2 stars: at most 10% over optimal
 *   - 1 star: at most 20% over optimal
 *   - 0 stars: more than 20% over optimal
 *
 * @param playerMoves  Number of moves used by player
 * @param optimalMoves Optimal number of moves from solver
 * @return Number of stars earned (0-3)
 */
fun calculateHistoryStars(playerMoves: Int, optimalMoves: Int): Int {
    if (optimalMoves <= 0 || playerMoves <= 0) {
        return 0
    }
    if (playerMoves <= optimalMoves) {
        return 3
    }
    return if (optimalMoves <= Constants.HISTORY_SMALL_LEVEL_OPTIMAL) {
        when {
            playerMoves <= optimalMoves + 1 -> 2
            playerMoves <= optimalMoves + 2 -> 1
            else -> 0
        }
    } else {
        val ratio = playerMoves.toDouble() / optimalMoves
        when {
            ratio <= 1.10 -> 2
            ratio <= 1.20 -> 1
            else -> 0
        }
    }
}

/**
 * Save level completion data (DRY - same as in main game)
 * 
 * @param levelCompletionManager LevelCompletionManager instance
 * @param levelId Level ID
 * @param moveCount Number of moves used
 * @param hintsUsed Number of hints used
 * @param optimalMoves Optimal number of moves
 * @param stars Stars earned
 * @param squaresMoved Number of squares moved
 * @param elapsedTime Time elapsed in milliseconds
 * @param robotsUsed Number of robots used (calculated as robotsUsed.size in main game)
 */
fun saveLevelCompletion(
    levelCompletionManager: LevelCompletionManager,
    levelId: Int,
    moveCount: Int,
    hintsUsed: Int,
    optimalMoves: Int,
    stars: Int,
    squaresMoved: Int = 0,
    elapsedTime: Long = 0,
    robotsUsed: Int = 4 // Default to 4 robots (same as in main game)
) {
    val levelData = levelCompletionManager.getLevelCompletionData(levelId)
    if (levelData != null) {
        levelData.setCompleted(true)
        levelData.movesNeeded = moveCount
        levelData.hintsShown = hintsUsed
        levelData.optimalMoves = optimalMoves
        levelData.squaresSurpassed = squaresMoved
        levelData.timeNeeded = elapsedTime
        levelData.robotsUsed = robotsUsed
        
        // For beginner levels (1-10), always earn at least 1 star (same as in main game)
        val finalStars = if (stars < 1 && levelId <= Constants.MIN_STAR_GUARANTEE_LEVEL) {
            1
        } else {
            stars
        }
        
        levelData.setStars(finalStars)
        levelCompletionManager.saveLevelCompletionData(levelData)
    }
}
