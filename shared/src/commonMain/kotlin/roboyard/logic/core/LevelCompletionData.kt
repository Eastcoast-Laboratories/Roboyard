package roboyard.logic.core


/**
 * Data class for storing level completion information.
 */
class LevelCompletionData(
 val levelId: Int,
 val moves: Int,
 val timeMillis: Long,
 val starCount: Int,
 val difficulty: Int
) {

    private var completed: Boolean = false
    
    var hintsShown: Int = 0
    var timeNeeded: Long = 0
    var movesNeeded: Int = 0
    var robotsUsed: Int = 0
    var squaresSurpassed: Int = 0
    var optimalMoves: Int = 0
    
    private var starsInternal: Int = 0

    // Secondary constructor for int-only calls
    constructor(levelId: Int) : this(levelId, 0, 0, 0, 0)

    fun isCompleted(): Boolean = completed

    fun getStars(): Int = if (starsInternal != 0) starsInternal else starCount

    fun getCompletionStars(): Int = getStars()

    fun setCompleted(completed: Boolean) {
        this.completed = completed
    }

    fun setStars(stars: Int) {
        this.starsInternal = stars.coerceIn(0, 4) // Allow up to 4 stars (hyper-optimal)
    }

    override fun toString(): String {
        return "LevelCompletionData(levelId=$levelId, moves=$moves, timeMillis=$timeMillis, stars=${getStars()}, difficulty=$difficulty, isCompleted=$completed)"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
