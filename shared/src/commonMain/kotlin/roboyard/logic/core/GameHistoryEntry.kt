package roboyard.logic.core

import roboyard.logic.util.RLog

/**
 * Represents an entry in the game's move history.
 */
class GameHistoryEntry(
 val robotColor: Int,
 val fromX: Int,
 val fromY: Int,
 val toX: Int,
 val toY: Int,
 val moveNumber: Int
) {
    private val log = RLog.tag("GameHistoryEntry")


    // Additional fields required by Java code
 var mapName: String? = null
 var starsEarned: Int = 0
 var movesMade: Int = 0
 var completionCount: Int = 0

    fun isFirstCompletion(): Boolean = completionCount == 1
 var timestamp: Long = 0
 var lastCompletionTimestamp: Long = 0
 var difficulty: Int = 0
 var wallSignature: String? = null
 var positionSignature: String? = null
 var mapSignature: String? = null
 var maxHintUsed: Int = -1
 var lastSolvedWithoutHints: Long = 0
 var lastPerfectlySolvedWithoutHints: Long = 0
 var optimalMoves: Int = 0
 var bestTime: Int = 0
 var bestMoves: Int = 0
 var playDuration: Int = 0
 var boardSize: String? = null
 var previewImagePath: String? = null

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
        lastCompletionTimestamp = TODO("platform-specific time")
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
        lastSolvedWithoutHints = TODO("platform-specific time")
        if (optimal) lastPerfectlySolvedWithoutHints = TODO("platform-specific time")
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
                    log.e(e, "Failed to parse history index from map path: %s", path)
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
