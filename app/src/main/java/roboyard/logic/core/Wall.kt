package roboyard.logic.core;

/**
 * Represents a wall in the game board.
 * A wall is defined by its position (x,y) and type (horizontal or vertical).
 */
public class Wall {
    private final int x;
    private final int y;
    private final WallType type;
    
    /**
     * Creates a new wall at the specified position with the specified type.
     *
     * @param x The x-coordinate of the wall
     * @param y The y-coordinate of the wall
     * @param type The type of wall (horizontal or vertical)
     */
    public Wall(int x, int y, WallType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }
    
    /**
     * Gets the x-coordinate of the wall.
     *
     * @return The x-coordinate
     */
    public int getX() {
        return x;
    }
    
    /**
     * Gets the y-coordinate of the wall.
     *
     * @return The y-coordinate
     */
    public int getY() {
        return y;
    }
    
    /**
     * Gets the type of the wall.
     *
     * @return The wall type (horizontal or vertical)
     */
    public WallType getType() {
        return type;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Wall wall = (Wall) o;
        
        if (x != wall.x) return false;
        if (y != wall.y) return false;
        return type == wall.type;
    }
    
    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
    
    @Override
    public String toString() {
        return "Wall{" +
                "x=" + x +
                ", y=" + y +
                ", type=" + type +
                '}';
    }
}
