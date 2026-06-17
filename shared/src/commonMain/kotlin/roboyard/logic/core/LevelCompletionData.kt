package roboyard.logic.core


/**
 * Data class for storing level completion information.
 */
class LevelCompletionData(
 val levelId: Int,
 val moves: Int,
 val timeMillis: Long,
 val stars: Int,
 val difficulty: Int
) {

 var isCompleted: Boolean = false
        private set
 var hintsShown: Int = 0
 var timeNeeded: Long = 0
 var movesNeeded: Int = 0
 var robotsUsed: Int = 0
 var squaresSurpassed: Int = 0
 var optimalMoves: Int = 0
 var starCount: Int = 0
        private set

    // Secondary constructor for int-only calls
    constructor(levelId: Int) : this(levelId, 0, 0, 0, 0)

    fun setCompleted(completed: Boolean) {
        this.isCompleted = completed
    }

    fun setStars(stars: Int) {
        this.starCount = stars.coerceIn(0, 3)
    }

    override fun toString(): String {
        return "LevelCompletionData(levelId=$levelId, moves=$moves, timeMillis=$timeMillis, stars=$starCount, difficulty=$difficulty, isCompleted=$isCompleted)"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
