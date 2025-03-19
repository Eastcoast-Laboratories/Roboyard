package roboyard.eclabs;

/**
 * Represents an element in a grid.
 */
public class GridElement {

    /** The x-coordinate of the grid element. */
    private int x;

    /** The y-coordinate of the grid element. */
    private int y;

    /** The type of the grid element. */
    private String type;

    /**
     * Constructs a grid element with the specified coordinates and type.
     *
     * @param x The x-coordinate of the grid element.
     * @param y The y-coordinate of the grid element.
     * @param objectType The type of the grid element.
     */
    public GridElement(int x, int y, String objectType) {
        this.setX(x);
        this.setY(y);
        this.setType(objectType);
    }

    /**
     * Gets the x-coordinate of the grid element.
     *
     * @return The x-coordinate of the grid element.
     */
    public int getX() {
        return this.x;
    }

    /**
     * Sets the x-coordinate of the grid element.
     *
     * @param x The x-coordinate to set.
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Gets the y-coordinate of the grid element.
     *
     * @return The y-coordinate of the grid element.
     */
    public int getY() {
        return this.y;
    }

    /**
     * Sets the y-coordinate of the grid element.
     *
     * @param y The y-coordinate to set.
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Gets the type of the grid element.
     *
     * @return The type of the grid element.
     */
    public String getType() {
        return this.type;
    }

    /**
     * Sets the type of the grid element.
     *
     * @param objectType The type to set.
     */
    public void setType(String objectType) {
        this.type = objectType;
    }
}
