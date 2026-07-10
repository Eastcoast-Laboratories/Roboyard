package roboyard.logic.core


/**
 * Data class for storing level completion information.
 */
class LevelCompletionData(
    @JvmField val levelId: Int,
    @JvmField val moves: Int,
    @JvmField val timeMillis: Long,
    @JvmField val starCount: Int,
    @JvmField val difficulty: Int
) {

    private var completed: Boolean = false
    
    @JvmField var hintsShown: Int = 0
    @JvmField var timeNeeded: Long = 0
    @JvmField var movesNeeded: Int = 0
    @JvmField var robotsUsed: Int = 0
    @JvmField var squaresSurpassed: Int = 0
    @JvmField var optimalMoves: Int = 0
    
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
        this.starsInternal = stars.coerceIn(0, 3) // Allow up to 3 stars
    }

    override fun toString(): String {
        return "LevelCompletionData(levelId=$levelId, moves=$moves, timeMillis=$timeMillis, stars=${getStars()}, difficulty=$difficulty, isCompleted=$completed)"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
