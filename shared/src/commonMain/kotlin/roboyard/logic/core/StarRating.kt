package roboyard.logic.core

import roboyard.logic.managers.LevelCompletionManager

/**
 * Calculate star rating based on player performance (same as in main game)
 * 
 * Star allocation rules:
 * - 4 stars: Hyper-optimal solution (better than solver's optimal solution)
 * - 3 stars: Optimal solution (same as solver) with no hints
 * - 2 stars: One move more than optimal with no hints, OR optimal with one hint
 * - 1 star: Optimal solution with two hints, OR two moves more than optimal with no hints
 * - 0 stars: All other cases
 * 
 * @param playerMoves  Number of moves used by player
 * @param optimalMoves Optimal number of moves from solver
 * @param hintsUsed    Number of hints used
 * @return Number of stars earned (0-4)
 */
fun calculateStars(playerMoves: Int, optimalMoves: Int, hintsUsed: Int): Int {
    if (optimalMoves <= 0) {
        return 0 // No optimal solution available
    }

    // Calculate stars based on the rules
    if (playerMoves < optimalMoves) {
        // hyper-Optimal solution (better than solver's solution)
        return 4
    } else if (playerMoves == optimalMoves && hintsUsed == 0) {
        // Optimal solution (or better) and no hints
        return 3
    } else if ((playerMoves == optimalMoves + 1 && hintsUsed == 0) ||
        (playerMoves == optimalMoves && hintsUsed == 1)
    ) {
        // One move more than optimal with no hints OR optimal with one hint
        return 2
    } else if ((playerMoves == optimalMoves && hintsUsed == 2) ||
        (playerMoves == optimalMoves + 2 && hintsUsed == 0)
    ) {
        // Optimal with two hints OR two moves more than optimal with no hints
        return 1
    } else {
        // All other cases
        return 0
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
 * @param robotsUsed Number of robots used
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
