package roboyard.logic.core;


import timber.log.Timber;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a game history entry.
 * Contains metadata about a game that was automatically saved to history.
 * 
 * Enhanced for unique map tracking:
 * - Stores all completion timestamps (not just the last one)
 * - Separates wall layout from robot/target positions for achievement tracking
 * - Never deleted, only updated with new completions
 */
public class GameHistoryEntry {
    private String mapPath;          // Path to the saved game file
    private String mapName;          // Name of the map
    private long timestamp;          // When the game was first saved (first completion)
    private int playDuration;        // Best play duration in seconds
    private int movesMade;           // Best number of moves made by player
    private int optimalMoves;        // Number of optimal moves (if available)
    private String boardSize;        // Board dimensions
    private String previewImagePath; // Path to a thumbnail image
    private String originalMapPath;  // Reference to the original map path, if this is a history entry
    
    // New fields for unique map tracking
    private List<Long> completionTimestamps;  // All timestamps when this map was completed
    private int completionCount;              // How many times this map was completed
    private long lastCompletionTimestamp;     // When the map was most recently completed
    private int bestTime;                     // Fastest completion time in seconds
    private int bestMoves;                    // Fewest moves used to solve
    
    // Separate storage for wall layout (for achievements tracking same walls, different positions)
    private String wallSignature;             // Unique signature of wall layout only
    private String positionSignature;         // Unique signature of robot+target positions
    private String mapSignature;              // Full map signature (walls + positions) for exact match
    
    // Hint tracking for achievements - once hints are used, map is permanently marked
    // The critical hint is when robot colors are revealed (which robots to use)
    // -1 = no hints used, 0+ = max hint index viewed (0 = pre-hint with colors)
    private int maxHintUsed = -1;             // Highest hint index ever viewed for this map
    private boolean solvedWithoutHints;       // True if map was FIRST solved without using hints
    private boolean everUsedHints = false;    // True if hints were EVER used in ANY attempt (including later ones)

    // Timestamps for no-hints achievement tracking
    // These fields are only updated when solved without hints and never cleared by later hint usage
    private long lastSolvedWithoutHints = 0;          // Timestamp of the last completion without hints (0 = never)
    private long lastPerfectlySolvedWithoutHints = 0; // Timestamp of the last OPTIMAL completion without hints (0 = never)

    private String difficulty = "";           // Difficulty level when the game was created

    /**
     * Constructor for a new history entry
     */
    public GameHistoryEntry(String mapPath, String mapName, long timestamp, int playDuration, 
                           int movesMade, int optimalMoves, String boardSize, String previewImagePath) {
        this(mapPath, mapName, timestamp, playDuration, movesMade, optimalMoves, boardSize, previewImagePath, null);
    }

    /**
     * Constructor for a new history entry with original map path
     */
    public GameHistoryEntry(String mapPath, String mapName, long timestamp, int playDuration, 
                           int movesMade, int optimalMoves, String boardSize, String previewImagePath, String originalMapPath) {
        this.mapPath = mapPath;
        this.mapName = mapName;
        this.timestamp = timestamp;
        this.playDuration = playDuration;
        this.movesMade = movesMade;
        this.optimalMoves = optimalMoves;
        this.boardSize = boardSize;
        this.previewImagePath = previewImagePath;
        this.originalMapPath = originalMapPath;
        
        // Initialize new fields
        // completionCount starts at 0 - only incremented by recordCompletion() when goal is reached
        this.completionTimestamps = new ArrayList<>();
        this.completionCount = 0;
        this.lastCompletionTimestamp = 0;
        this.bestTime = 0;
        this.bestMoves = 0;
    }

    /**
     * Default constructor for JSON deserialization
     */
    public GameHistoryEntry() {
        // Default constructor for JSON deserialization
        this.completionTimestamps = new ArrayList<>();
    }
    
    /**
     * Record a new completion of this map
     * @param completionTime Time taken to complete in seconds
     * @param moves Number of moves used
     * @return true if this was a new best time or best moves
     */
    public boolean recordCompletion(int completionTime, int moves) {
        long now = System.currentTimeMillis();
        completionTimestamps.add(now);
        completionCount++;
        lastCompletionTimestamp = now;
        
        boolean newBest = false;
        if (completionTime < bestTime || bestTime == 0) {
            bestTime = completionTime;
            newBest = true;
        }
        if (moves < bestMoves || bestMoves == 0) {
            bestMoves = moves;
            newBest = true;
        }
        return newBest;
    }
    
    /**
     * Check if this is the first completion of this map
     * @return true if this map has only been completed once
     */
    public boolean isFirstCompletion() {
        return completionCount == 1;
    }

    // Getters and setters
    public String getMapPath() {
        return mapPath;
    }

    public void setMapPath(String mapPath) {
        this.mapPath = mapPath;
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getPlayDuration() {
        return playDuration;
    }

    public void setPlayDuration(int playDuration) {
        this.playDuration = playDuration;
    }

    public int getMovesMade() {
        return movesMade;
    }

    public void setMovesMade(int movesMade) {
        this.movesMade = movesMade;
    }

    public int getOptimalMoves() {
        return optimalMoves;
    }

    public void setOptimalMoves(int optimalMoves) {
        this.optimalMoves = optimalMoves;
    }

    public String getBoardSize() {
        return boardSize;
    }

    public void setBoardSize(String boardSize) {
        this.boardSize = boardSize;
    }

    public String getPreviewImagePath() {
        return previewImagePath;
    }

    public void setPreviewImagePath(String previewImagePath) {
        this.previewImagePath = previewImagePath;
    }
    
    // New getters and setters for unique map tracking
    
    public List<Long> getCompletionTimestamps() {
        return completionTimestamps;
    }
    
    public void setCompletionTimestamps(List<Long> completionTimestamps) {
        this.completionTimestamps = completionTimestamps != null ? completionTimestamps : new ArrayList<>();
    }
    
    public int getCompletionCount() {
        return completionCount;
    }
    
    public void setCompletionCount(int completionCount) {
        this.completionCount = completionCount;
    }
    
    public long getLastCompletionTimestamp() {
        return lastCompletionTimestamp;
    }
    
    public void setLastCompletionTimestamp(long lastCompletionTimestamp) {
        this.lastCompletionTimestamp = lastCompletionTimestamp;
    }
    
    public int getBestTime() {
        return bestTime;
    }
    
    public void setBestTime(int bestTime) {
        this.bestTime = bestTime;
    }
    
    public int getBestMoves() {
        return bestMoves;
    }
    
    public void setBestMoves(int bestMoves) {
        this.bestMoves = bestMoves;
    }
    
    public String getWallSignature() {
        return wallSignature;
    }
    
    public void setWallSignature(String wallSignature) {
        this.wallSignature = wallSignature;
    }
    
    public String getPositionSignature() {
        return positionSignature;
    }
    
    public void setPositionSignature(String positionSignature) {
        this.positionSignature = positionSignature;
    }
    
    public String getMapSignature() {
        return mapSignature;
    }
    
    public void setMapSignature(String mapSignature) {
        this.mapSignature = mapSignature;
    }
    
    // Hint tracking getters and setters
    
    public int getMaxHintUsed() {
        return maxHintUsed;
    }
    
    public void setMaxHintUsed(int maxHintUsed) {
        this.maxHintUsed = maxHintUsed;
    }
    
    public boolean isSolvedWithoutHints() {
        return solvedWithoutHints;
    }
    
    public void setSolvedWithoutHints(boolean solvedWithoutHints) {
        this.solvedWithoutHints = solvedWithoutHints;
    }
    
    /**
     * Record that a hint was viewed during this game session.
     * Once a hint is used, the map is permanently marked.
     * The critical hint (index 0) reveals which robot colors to use.
     * @param hintIndex The hint index that was viewed (0 = pre-hint with colors)
     */
    public void recordHintUsed(int hintIndex) {
        if (hintIndex > maxHintUsed) {
            maxHintUsed = hintIndex;
        }
        // Permanently mark that hints were ever used in any attempt
        everUsedHints = true;
    }
    
    /**
     * Check if hints were ever used on this map.
     * @return true if any hint was ever viewed
     */
    public boolean hasUsedHints() {
        return maxHintUsed >= 0;
    }
    
    /**
     * Check if this map qualifies for "no hints" achievements.
     * A map qualifies if it was EVER solved without hints (lastSolvedWithoutHints > 0).
     * Later hint usage in subsequent attempts does NOT revoke this qualification.
     * @return true if map qualifies for no-hints achievements
     */
    public boolean qualifiesForNoHintsAchievement() {
        return lastSolvedWithoutHints > 0;
    }

    /**
     * Check if this map qualifies for "perfect no hints" achievements.
     * A map qualifies if it was EVER solved optimally without hints.
     * Later hint usage in subsequent attempts does NOT revoke this qualification.
     * @return true if map qualifies for perfect no-hints achievements
     */
    public boolean qualifiesForPerfectNoHintsAchievement() {
        return lastPerfectlySolvedWithoutHints > 0;
    }

    public boolean isEverUsedHints() {
        return everUsedHints;
    }

    public void setEverUsedHints(boolean everUsedHints) {
        this.everUsedHints = everUsedHints;
    }

    /**
     * Mark that hints were used in any attempt of this map.
     * Once set to true, this is permanent - it tracks lifetime hint usage.
     */
    public void markEverUsedHints() {
        this.everUsedHints = true;
    }

    /**
     * Get the history index from the map path
     * @return The history index
     */
    public long getLastSolvedWithoutHints() { return lastSolvedWithoutHints; }
    public void setLastSolvedWithoutHints(long ts) { this.lastSolvedWithoutHints = ts; }

    public long getLastPerfectlySolvedWithoutHints() { return lastPerfectlySolvedWithoutHints; }
    public void setLastPerfectlySolvedWithoutHints(long ts) { this.lastPerfectlySolvedWithoutHints = ts; }

    /**
     * Record a no-hints completion. Updates lastSolvedWithoutHints.
     * If also optimal, updates lastPerfectlySolvedWithoutHints.
     *
     * IMPORTANT: If hints were EVER used in ANY prior attempt (everUsedHints=true),
     * this method does nothing. Once a map is disqualified (hints used), it stays disqualified
     * permanently - even if later solved without hints.
     */
    public void recordSolvedWithoutHints(boolean isOptimal) {
        if (everUsedHints) {
            return; // permanently disqualified - cannot restore no-hints status
        }
        long now = System.currentTimeMillis();
        this.lastSolvedWithoutHints = now;
        this.solvedWithoutHints = true;
        if (isOptimal) {
            this.lastPerfectlySolvedWithoutHints = now;
        }
    }

    public String getDifficulty() { return difficulty != null ? difficulty : ""; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty != null ? difficulty : ""; }

    public int getHistoryIndex() {
        // e.g. getMapPath() = history_1.txt
        // extract the number
        String[] parts = mapPath.split("_");
        if (parts.length > 1) {
            try {
                return Integer.parseInt(parts[1].split("\\.")[0]);
            } catch (NumberFormatException e) {
                Timber.e(e, "Failed to parse history index from map path: %s", mapPath);
            }
        }
        return 0;
    }
}
