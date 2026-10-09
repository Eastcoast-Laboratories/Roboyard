package roboyard.logic.core

import kotlin.jvm.JvmField
import roboyard.logic.json.JsonObject


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

    /**
     * Serializes with the same field names/order Gson used, including the
     * private backing fields `completed` and `starsInternal`, so stored data
     * stays compatible with files written by the Android app.
     */
    fun toJsonObject(): JsonObject {
        val o = JsonObject()
        o.addProperty("levelId", levelId)
        o.addProperty("moves", moves)
        o.addProperty("timeMillis", timeMillis)
        o.addProperty("starCount", starCount)
        o.addProperty("difficulty", difficulty)
        o.addProperty("completed", completed)
        o.addProperty("hintsShown", hintsShown)
        o.addProperty("timeNeeded", timeNeeded)
        o.addProperty("movesNeeded", movesNeeded)
        o.addProperty("robotsUsed", robotsUsed)
        o.addProperty("squaresSurpassed", squaresSurpassed)
        o.addProperty("optimalMoves", optimalMoves)
        o.addProperty("starsInternal", starsInternal)
        return o
    }

    companion object {
        private const val serialVersionUID = 1L

        fun fromJsonObject(o: JsonObject): LevelCompletionData {
            fun num(k: String, default: Long = 0L): Long =
                o.get(k)?.takeIf { it.isJsonPrimitive }?.asLong ?: default
            val d = LevelCompletionData(
                num("levelId").toInt(),
                num("moves").toInt(),
                num("timeMillis"),
                num("starCount").toInt(),
                num("difficulty").toInt()
            )
            d.completed = o.get("completed")?.asBoolean ?: false
            d.hintsShown = num("hintsShown").toInt()
            d.timeNeeded = num("timeNeeded")
            d.movesNeeded = num("movesNeeded").toInt()
            d.robotsUsed = num("robotsUsed").toInt()
            d.squaresSurpassed = num("squaresSurpassed").toInt()
            d.optimalMoves = num("optimalMoves").toInt()
            d.starsInternal = num("starsInternal").toInt()
            return d
        }
    }
}
