package roboyard.eclabs;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

import timber.log.Timber;

/**
 * Class representing a game history entry.
 * Contains metadata about a game that was automatically saved to history.
 */
public class GameHistoryEntry {
    private String mapPath;          // Path to the saved game file
    private String mapName;          // Name of the map
    private long timestamp;          // When the game was saved
    private int playDuration;        // In seconds
    private int movesMade;           // Number of moves made by player
    private int optimalMoves;        // Number of optimal moves (if available)
    private String boardSize;        // Board dimensions
    private String previewImagePath; // Path to a thumbnail image

    /**
     * Constructor for a new history entry
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
    }

    /**
     * Default constructor for JSON deserialization
     */
    public GameHistoryEntry() {
        // Default constructor for JSON deserialization
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

    /**
     * Format the timestamp as a readable date/time string
     */
    public String getFormattedDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    /**
     * Format the play duration as a readable string
     */
    public String getFormattedDuration() {
        if (playDuration < 60) {
            return playDuration + "s";
        } else if (playDuration < 3600) {
            int minutes = playDuration / 60;
            int seconds = playDuration % 60;
            return minutes + "m" + (seconds > 0 ? " " + seconds + "s" : "");
        } else {
            int hours = playDuration / 3600;
            int minutes = (playDuration % 3600) / 60;
            return hours + "h" + (minutes > 0 ? " " + minutes + "m" : "");
        }
    }

    /**
     * Get the history index from the map path
     * @return The history index
     */
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
