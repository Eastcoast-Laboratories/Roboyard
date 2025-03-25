package roboyard.eclabs.util;

/**
 * A class to hold metrics about a puzzle solution.
 * This provides information about whether a solution exists,
 * how many moves it requires, and how many different solutions exist.
 */
public class SolutionMetrics {
    /** Whether a solution exists for the puzzle */
    public boolean hasSolution = false;
    
    /** The number of moves in the best solution */
    public int moveCount = 0;
    
    /** The number of different solutions found */
    public int numberOfSolutions = 0;
    
    /**
     * Creates a new SolutionMetrics instance with default values
     */
    public SolutionMetrics() {
        // Initialize with default values
    }
    
    /**
     * Creates a new SolutionMetrics instance with the specified values
     * 
     * @param hasSolution Whether a solution exists
     * @param moveCount The number of moves in the best solution
     * @param numberOfSolutions The number of different solutions found
     */
    public SolutionMetrics(boolean hasSolution, int moveCount, int numberOfSolutions) {
        this.hasSolution = hasSolution;
        this.moveCount = moveCount;
        this.numberOfSolutions = numberOfSolutions;
    }
    
    /**
     * Gets the difficulty level based on the number of moves required
     * 
     * @return A string representing the difficulty level (Easy, Medium, Hard, Expert)
     */
    public String getDifficultyLevel() {
        if (!hasSolution) {
            return "Unsolvable";
        }
        
        if (moveCount <= 5) {
            return "Easy";
        } else if (moveCount <= 10) {
            return "Medium";
        } else if (moveCount <= 15) {
            return "Hard";
        } else {
            return "Expert";
        }
    }
}
