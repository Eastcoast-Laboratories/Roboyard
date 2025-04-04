package roboyard.eclabs.util;


import roboyard.logic.core.Constants;

/**
 * A class representing a single robot move in the game.
 * This provides a standardized format for representing moves
 * across different parts of the application.
 */
public class GameMove {
    // Robot identifiers
    public static final int ROBOT_PINK = Constants.COLOR_PINK;
    public static final int ROBOT_GREEN = Constants.COLOR_GREEN;
    public static final int ROBOT_BLUE = Constants.COLOR_BLUE;
    public static final int ROBOT_YELLOW = Constants.COLOR_YELLOW;
    
    // Move directions
    public static final int DIRECTION_NORTH = Constants.NORTH;
    public static final int DIRECTION_EAST = Constants.EAST;
    public static final int DIRECTION_SOUTH = Constants.SOUTH;
    public static final int DIRECTION_WEST = Constants.WEST;
    
    private final int robotId;
    private final int direction;
    private int startX;
    private int startY;
    private int endX;
    private int endY;
    private int distance;
    
    /**
     * Creates a new GameMove instance
     * 
     * @param robotId The ID of the robot making the move
     * @param direction The direction of the move
     * @param startX The starting X coordinate
     * @param startY The starting Y coordinate
     * @param endX The ending X coordinate
     * @param endY The ending Y coordinate
     */
    public GameMove(int robotId, int direction, int startX, int startY, int endX, int endY) {
        this.robotId = robotId;
        this.direction = direction;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        
        // Calculate distance based on start and end positions
        switch (direction) {
            case DIRECTION_NORTH:
            case DIRECTION_SOUTH:
                this.distance = Math.abs(endY - startY);
                break;
            case DIRECTION_EAST:
            case DIRECTION_WEST:
                this.distance = Math.abs(endX - startX);
                break;
            default:
                this.distance = 0;
        }
    }
    
    /**
     * Creates a new GameMove instance with just robot ID and direction
     * 
     * @param robotId The ID of the robot making the move
     * @param direction The direction of the move
     */
    public GameMove(int robotId, int direction) {
        this.robotId = robotId;
        this.direction = direction;
        this.startX = -1;
        this.startY = -1;
        this.endX = -1;
        this.endY = -1;
        this.distance = 0;
    }
    
    /**
     * @return The ID of the robot making the move
     */
    public int getRobotId() {
        return robotId;
    }
    
    /**
     * @return The direction of the move
     */
    public int getDirection() {
        return direction;
    }
    
    /**
     * @return The starting X coordinate
     */
    public int getStartX() {
        return startX;
    }
    
    /**
     * @return The starting Y coordinate
     */
    public int getStartY() {
        return startY;
    }
    
    /**
     * @return The ending X coordinate
     */
    public int getEndX() {
        return endX;
    }
    
    /**
     * @return The ending Y coordinate
     */
    public int getEndY() {
        return endY;
    }
    
    /**
     * @return The distance traveled in this move
     */
    public int getDistance() {
        return distance;
    }
    
    /**
     * Sets the end position of this move
     * 
     * @param endX The ending X coordinate
     * @param endY The ending Y coordinate
     */
    public void setEndPosition(int endX, int endY) {
        this.endX = endX;
        this.endY = endY;
        
        // Update distance based on new end position
        switch (direction) {
            case DIRECTION_NORTH:
            case DIRECTION_SOUTH:
                this.distance = Math.abs(endY - startY);
                break;
            case DIRECTION_EAST:
            case DIRECTION_WEST:
                this.distance = Math.abs(endX - startX);
                break;
        }
    }
    
    /**
     * Sets the start position of this move
     * 
     * @param startX The starting X coordinate
     * @param startY The starting Y coordinate
     */
    public void setStartPosition(int startX, int startY) {
        this.startX = startX;
        this.startY = startY;
        
        // Update distance based on new start position
        if (endX != -1 && endY != -1) {
            switch (direction) {
                case DIRECTION_NORTH:
                case DIRECTION_SOUTH:
                    this.distance = Math.abs(endY - startY);
                    break;
                case DIRECTION_EAST:
                case DIRECTION_WEST:
                    this.distance = Math.abs(endX - startX);
                    break;
            }
        }
    }
    
    /**
     * Creates a string representation of this move
     * 
     * @return A string representation of this move
     */
    @Override
    public String toString() {
        String robotName = "";
        switch (robotId) {
            case ROBOT_PINK:
                robotName = "Red";
                break;
            case ROBOT_GREEN:
                robotName = "Green";
                break;
            case ROBOT_BLUE:
                robotName = "Blue";
                break;
            case ROBOT_YELLOW:
                robotName = "Yellow";
                break;
        }
        
        String directionName = "";
        switch (direction) {
            case DIRECTION_NORTH:
                directionName = "North";
                break;
            case DIRECTION_EAST:
                directionName = "East";
                break;
            case DIRECTION_SOUTH:
                directionName = "South";
                break;
            case DIRECTION_WEST:
                directionName = "West";
                break;
        }
        
        return robotName + " robot moves " + directionName + " by " + distance + " spaces";
    }
}
