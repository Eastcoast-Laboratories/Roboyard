package roboyard.eclabs.ui;

import java.io.Serializable;

import roboyard.logic.core.GameState;

/**
 * Represents a move in the game, including the robot being moved and its start/end positions.
 * Used for recording moves, providing hints, and implementing undo functionality.
 */
public class GameMove implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Direction constants
    public static final int UP = 0;
    public static final int RIGHT = 1;
    public static final int DOWN = 2;
    public static final int LEFT = 3;
    
    // Robot info
    private final int robotId;
    private final int robotColor;
    
    // Start position
    private final int fromX;
    private final int fromY;
    
    // End position
    private final int toX;
    private final int toY;
    
    // Direction and distance (for solver hints)
    private int direction = -1;
    private int distance = 0;
    
    /**
     * Create a new game move
     * 
     * @param robotId ID of the robot being moved
     * @param robotColor Color of the robot being moved
     * @param fromX Starting X position
     * @param fromY Starting Y position
     * @param toX Ending X position
     * @param toY Ending Y position
     */
    public GameMove(int robotId, int robotColor, int fromX, int fromY, int toX, int toY) {
        this.robotId = robotId;
        this.robotColor = robotColor;
        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
        
        // Calculate direction and distance
        if (fromX == toX) {
            // Vertical movement
            if (fromY > toY) {
                this.direction = UP;
                this.distance = fromY - toY;
            } else {
                this.direction = DOWN;
                this.distance = toY - fromY;
            }
        } else {
            // Horizontal movement
            if (fromX > toX) {
                this.direction = LEFT;
                this.distance = fromX - toX;
            } else {
                this.direction = RIGHT;
                this.distance = toX - fromX;
            }
        }
    }
    
    /**
     * Create a new game move from a direction and distance (used by solver)
     * 
     * @param robotId ID of the robot being moved
     * @param direction Direction of movement (UP, RIGHT, DOWN, LEFT)
     * @param distance Distance to move
     */
    public GameMove(int robotId, int direction, int distance) {
        this.robotId = robotId;
        this.robotColor = -1; // Unknown color, to be set by the game state manager
        this.direction = direction;
        this.distance = distance;
        
        // Set dummy positions, they'll be updated when the move is applied
        this.fromX = -1;
        this.fromY = -1;
        this.toX = -1;
        this.toY = -1;
    }
    
    /**
     * Get the robot ID
     * @return Robot ID
     */
    public int getRobotId() {
        return robotId;
    }
    
    /**
     * Get the robot color
     * @return Robot color
     */
    public int getRobotColor() {
        return robotColor;
    }
    
    /**
     * Get the robot color name
     * @return Color name (Red, Green, Blue, Yellow, or Unknown)
     */
    public String getRobotColorName() {
        switch (robotColor) {
            case 0: return "Red";
            case 1: return "Green";
            case 2: return "Blue";
            case 3: return "Yellow";
            default: return "Unknown";
        }
    }
    
    /**
     * Get the starting X position
     * @return Starting X position
     */
    public int getFromX() {
        return fromX;
    }
    
    /**
     * Get the starting Y position
     * @return Starting Y position
     */
    public int getFromY() {
        return fromY;
    }
    
    /**
     * Get the ending X position
     * @return Ending X position
     */
    public int getToX() {
        return toX;
    }
    
    /**
     * Get the ending Y position
     * @return Ending Y position
     */
    public int getToY() {
        return toY;
    }
    
    /**
     * Get the direction of the move
     * @return Direction constant (UP, RIGHT, DOWN, LEFT)
     */
    public int getDirection() {
        return direction;
    }
    
    /**
     * Get the distance of the move
     * @return Number of cells moved
     */
    public int getDistance() {
        return distance;
    }
    
    /**
     * Get a human-readable description of the move
     * @return String description of the move
     */
    @Override
    public String toString() {
        String colorName = getRobotColorName();
        String directionName = "Unknown";
        
        switch (direction) {
            case UP: directionName = "Up"; break;
            case RIGHT: directionName = "Right"; break;
            case DOWN: directionName = "Down"; break;
            case LEFT: directionName = "Left"; break;
        }
        
        return colorName + " robot moves " + directionName + " by " + distance + " space(s)";
    }
    
    /**
     * Check if this move is a target move (robot reaches a target)
     * @param state The game state to check against
     * @return True if this move reaches a target
     */
    public boolean isTargetMove(GameState state) {
        // A target move is one where the robot ends on a target of the same color
        if (state == null) return false;
        
        // Get the cell type at the destination
        int cellType = state.getCellType(toX, toY);
        
        // Check if there's a target at the destination matching the robot's color
        return cellType == 2 && state.getTargetColor(toX, toY) == robotColor;
    }
}
