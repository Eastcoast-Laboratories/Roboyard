package roboyard.logic.core


/**
 * Data class for storing level completion information.
 */
class LevelCompletionData(
    @JvmField val levelId: Int,
    @JvmField val moves: Int,
    @JvmField val timeMillis: Long,
    @JvmField val stars: Int,
    @JvmField val difficulty: Int
) {

    @JvmField var isCompleted: Boolean = false
    @JvmField var hintsShown: Int = 0
    @JvmField var timeNeeded: Long = 0
    @JvmField var movesNeeded: Int = 0
    @JvmField var robotsUsed: Int = 0
    @JvmField var squaresSurpassed: Int = 0
    @JvmField var optimalMoves: Int = 0
    @JvmField var starCount: Int = 0

    // Secondary constructor for int-only calls
    constructor(levelId: Int) : this(levelId, 0, 0, 0, 0)

    fun setCompleted(completed: Boolean) {
        this.isCompleted = completed
    }
    
    fun isCompleted(): Boolean = isCompleted

    fun setStars(stars: Int) {
        this.starCount = stars
    }
    
    fun getStars(): Int = starCount

    companion object {
        private const val serialVersionUID = 1L
    }
}
