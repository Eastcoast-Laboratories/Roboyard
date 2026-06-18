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
