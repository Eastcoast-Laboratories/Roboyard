package roboyard.logic.core

import java.io.Serializable
import timber.log.Timber

/**
 * Represents an entry in the game's move history.
 */
class GameHistoryEntry(
    @JvmField val robotColor: Int,
    @JvmField val fromX: Int,
    @JvmField val fromY: Int,
    @JvmField val toX: Int,
    @JvmField val toY: Int,
    @JvmField val moveNumber: Int
) : Serializable {

    // Additional fields required by Java code
    @JvmField var mapName: String? = null
    @JvmField var starsEarned: Int = 0
    @JvmField var movesMade: Int = 0
    @JvmField var completionCount: Int = 0

    fun getCompletionCount(): Int = completionCount
    @JvmField var timestamp: Long = 0
    @JvmField var lastCompletionTimestamp: Long = 0
    @JvmField var difficulty: Int = 0
    @JvmField var wallSignature: String? = null
    @JvmField var positionSignature: String? = null
    @JvmField var mapSignature: String? = null
    @JvmField var maxHintUsed: Int = -1
    @JvmField var lastSolvedWithoutHints: Long = 0
    @JvmField var lastPerfectlySolvedWithoutHints: Long = 0
    @JvmField var optimalMoves: Int = 0
    @JvmField var bestTime: Int = 0
    @JvmField var bestMoves: Int = 0
    @JvmField var playDuration: Int = 0
    @JvmField var boardSize: String? = null
    @JvmField var previewImagePath: String? = null

    private var everUsedHints: Boolean = false
    private var solvedWithoutHints: Boolean = true
    private var completionTimestamps: MutableList<Long> = mutableListOf()
    private var completionStars: MutableList<Int> = mutableListOf()
    private var completionMoves: MutableList<Int> = mutableListOf()
    private var historyIndex: Int = 0
    private var mapPath: String? = null

    // Default constructor for SyncManager/DebugSettingsFragment
    constructor() : this(0, 0, 0, 0, 0, 0)

    // Constructor for GameStateManager.saveToHistory() - matches original Java signature
    constructor(
        mapPath: String?,
        mapName: String?,
        timestamp: Long,
        playDuration: Int,
        movesMade: Int,
        optimalMoves: Int,
        boardSize: String?,
        previewImagePath: String?
    ) : this(0, 0, 0, 0, 0, 0) {
        this.mapPath = mapPath
        this.mapName = mapName
        this.timestamp = timestamp
        this.playDuration = playDuration
        this.movesMade = movesMade
        this.optimalMoves = optimalMoves
        this.boardSize = boardSize
        this.previewImagePath = previewImagePath
    }

    fun recordCompletion(time: Int, moves: Int, stars: Int): Boolean {
        completionCount++
        lastCompletionTimestamp = System.currentTimeMillis()
        playDuration += time
        var newRecord = false
        if (movesMade == 0 || moves < movesMade) {
            movesMade = moves
            newRecord = true
        }
        if (stars > starsEarned) {
            starsEarned = stars
        }
        if (bestMoves == 0 || moves < bestMoves) {
            bestMoves = moves
            newRecord = true
        }
        if (bestTime == 0 || time < bestTime) {
            bestTime = time
            newRecord = true
        }
        completionTimestamps.add(lastCompletionTimestamp)
        completionStars.add(stars)
        completionMoves.add(moves)
        return newRecord
    }

    fun getMapPath(): String = mapPath ?: mapName ?: ""
    fun setMapPath(path: String?) { this.mapPath = path }
    fun hasUsedHints(): Boolean = everUsedHints
    fun recordHintUsed(hint: Int) {
        if (hint > maxHintUsed) maxHintUsed = hint
        everUsedHints = true
    }
    fun recordSolvedWithoutHints(optimal: Boolean) {
        solvedWithoutHints = true
        lastSolvedWithoutHints = System.currentTimeMillis()
        if (optimal) lastPerfectlySolvedWithoutHints = System.currentTimeMillis()
    }
    fun markEverUsedHints() { everUsedHints = true }
    fun isEverUsedHints(): Boolean = everUsedHints
    fun setEverUsedHints(value: Boolean) { everUsedHints = value }
    fun setSolvedWithoutHints(value: Boolean) { solvedWithoutHints = value }
    fun isSolvedWithoutHints(): Boolean = solvedWithoutHints
    fun qualifiesForNoHintsAchievement(): Boolean = solvedWithoutHints
    fun qualifiesForPerfectNoHintsAchievement(): Boolean = solvedWithoutHints && (movesMade <= optimalMoves)

    fun getCompletionTimestamps(): List<Long> = completionTimestamps
    fun setCompletionTimestamps(list: List<Long>) { completionTimestamps = list.toMutableList() }
    
    fun getCompletionStars(): List<Int> = completionStars
    fun setCompletionStars(list: List<Int>) { completionStars = list.toMutableList() }
    
    fun getCompletionMoves(): List<Int> = completionMoves
    fun setCompletionMoves(list: List<Int>) { completionMoves = list.toMutableList() }

    fun getHistoryIndex(): Int {
        // e.g. getMapPath() = history_1.txt -> extract the number
        val path = mapPath
        if (path != null) {
            val parts = path.split("_")
            if (parts.size > 1) {
                try {
                    return parts[1].split(".")[0].toInt()
                } catch (e: NumberFormatException) {
                    Timber.e(e, "Failed to parse history index from map path: %s", path)
                }
            }
        }
        return 0
    }
    fun setHistoryIndex(index: Int) { historyIndex = index }

    companion object {
        private const val serialVersionUID = 1L
    }
}
