package roboyard.eclabs.ui;

import java.io.Serializable;

/**
 * Stores completion data for a level, including statistics about the player's performance.
 */
public class LevelCompletionData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final int levelId;
    private boolean completed;
    private int hintsShown;
    private long timeNeeded; // in milliseconds
    private int movesNeeded;
    private int optimalMoves;
    private int robotsUsed;
    private int squaresSurpassed;
    private int stars; // Number of stars earned (0-3)
    
    public LevelCompletionData(int levelId) {
        this.levelId = levelId;
        this.completed = false;
        this.hintsShown = 0;
        this.timeNeeded = 0;
        this.movesNeeded = 0;
        this.optimalMoves = 0;
        this.robotsUsed = 0;
        this.squaresSurpassed = 0;
        this.stars = 0;
    }
    
    public int getLevelId() {
        return levelId;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    public int getHintsShown() {
        return hintsShown;
    }
    
    public void setHintsShown(int hintsShown) {
        this.hintsShown = hintsShown;
    }
    
    public long getTimeNeeded() {
        return timeNeeded;
    }
    
    public void setTimeNeeded(long timeNeeded) {
        this.timeNeeded = timeNeeded;
    }
    
    public int getMovesNeeded() {
        return movesNeeded;
    }
    
    public void setMovesNeeded(int movesNeeded) {
        this.movesNeeded = movesNeeded;
    }
    
    public int getOptimalMoves() {
        return optimalMoves;
    }
    
    public void setOptimalMoves(int optimalMoves) {
        this.optimalMoves = optimalMoves;
    }
    
    public int getRobotsUsed() {
        return robotsUsed;
    }
    
    public void setRobotsUsed(int robotsUsed) {
        this.robotsUsed = robotsUsed;
    }
    
    public int getSquaresSurpassed() {
        return squaresSurpassed;
    }
    
    public void setSquaresSurpassed(int squaresSurpassed) {
        this.squaresSurpassed = squaresSurpassed;
    }
    
    public int getStars() {
        return stars;
    }
    
    public void setStars(int stars) {
        this.stars = Math.max(0, Math.min(3, stars)); // Ensure stars are between 0-3
    }
    
    @Override
    public String toString() {
        return "LevelCompletionData{" +
                "levelId=" + levelId +
                ", completed=" + completed +
                ", hintsShown=" + hintsShown +
                ", timeNeeded=" + timeNeeded +
                ", movesNeeded=" + movesNeeded +
                ", optimalMoves=" + optimalMoves +
                ", robotsUsed=" + robotsUsed +
                ", squaresSurpassed=" + squaresSurpassed +
                ", stars=" + stars +
                '}';
    }
}
