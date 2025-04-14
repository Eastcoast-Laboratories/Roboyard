package roboyard.eclabs;

import android.graphics.Bitmap;

/**
 * Class to store information about a game history entry
 */
public class GameHistoryEntry {
    private String mapPath;          // Path to the saved game file
    private long timestamp;          // When the game was saved
    private int playDuration;        // In seconds
    private int movesMade;           // Number of moves made
    private String boardSize;        // Board dimensions
    private String previewImagePath; // Path to a thumbnail image
    private String mapName;          // Name of the map
    private int optimalMoves;        // Optimal number of moves (if available)
    private int historyIndex;        // Index in the history list
    private int difficulty;          // Difficulty level (1-30)
    
    // Default constructor needed for JSON deserialization
    public GameHistoryEntry() {
    }
    
    /**
     * Full constructor
     */
    public GameHistoryEntry(String mapPath, long timestamp, int playDuration, int movesMade, 
                             String boardSize, String previewImagePath) {
        this.mapPath = mapPath;
        this.timestamp = timestamp;
        this.playDuration = playDuration;
        this.movesMade = movesMade;
        this.boardSize = boardSize;
        this.previewImagePath = previewImagePath;
        parseHistoryIndexFromPath();
    }
    
    /**
     * Constructor with map name and optimal moves
     */
    public GameHistoryEntry(String mapPath, String mapName, long timestamp, int playDuration, 
                             int movesMade, int optimalMoves, String boardSize, String previewImagePath) {
        this.mapPath = mapPath;
        this.mapName = mapName;
        this.timestamp = timestamp;
        this.playDuration = playDuration;
        this.movesMade = movesMade;
        this.optimalMoves = optimalMoves;
        this.boardSize = boardSize;
        this.previewImagePath = previewImagePath;
        parseHistoryIndexFromPath();
    }
    
    /**
     * Constructor with difficulty
     */
    public GameHistoryEntry(String mapPath, String mapName, long timestamp, int playDuration, 
                             int movesMade, int optimalMoves, int difficulty, String boardSize, String previewImagePath) {
        this.mapPath = mapPath;
        this.mapName = mapName;
        this.timestamp = timestamp;
        this.playDuration = playDuration;
        this.movesMade = movesMade;
        this.optimalMoves = optimalMoves;
        this.difficulty = difficulty;
        this.boardSize = boardSize;
        this.previewImagePath = previewImagePath;
        parseHistoryIndexFromPath();
    }
    
    /**
     * Parse the history index from the map path
     */
    private void parseHistoryIndexFromPath() {
        try {
            if (mapPath != null && mapPath.startsWith("history/history_") && mapPath.endsWith(".txt")) {
                String indexStr = mapPath.substring("history/history_".length(), 
                                                mapPath.length() - ".txt".length());
                this.historyIndex = Integer.parseInt(indexStr);
            }
        } catch (Exception e) {
            // Default to 0 if parsing fails
            this.historyIndex = 0;
        }
    }
    
    // Getters and setters
    public String getMapPath() {
        return mapPath;
    }
    
    public void setMapPath(String mapPath) {
        this.mapPath = mapPath;
        parseHistoryIndexFromPath();
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
    
    public String getMapName() {
        return mapName;
    }
    
    public void setMapName(String mapName) {
        this.mapName = mapName;
    }
    
    public int getOptimalMoves() {
        return optimalMoves;
    }
    
    public void setOptimalMoves(int optimalMoves) {
        this.optimalMoves = optimalMoves;
    }
    
    public int getHistoryIndex() {
        return historyIndex;
    }
    
    public void setHistoryIndex(int historyIndex) {
        this.historyIndex = historyIndex;
    }
    
    public int getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }
}
