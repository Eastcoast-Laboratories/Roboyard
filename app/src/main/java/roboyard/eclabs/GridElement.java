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

    /* returns a character representation of the grid element, used for debugging:
        * - 'h' for horizontal walls
        * - 'v' for vertical walls
        * - 'r' for red robot
        * - 'g' for green robot
        * - 'b' for blue robot
        * - 'y' for yellow robot
        * - 'R' for red target
        * - 'G' for green target
        * - 'B' for blue target
        * - 'Y' for yellow target
        * - 'M' for multi-colored target
        * - ' ' for empty space
        * @return The character representation of the grid element.
     */
    public String toChar() {
        switch (this.type) {
            case "mh":
                return "|";
            case "mv":
                return "-";
            case "robot_red":
                return "r";
            case "robot_green":
                return "g";
            case "robot_blue":
                return "b";
            case "robot_yellow":
                return "y";
            case "target_red":
                return "R";
            case "target_green":
                return "G";
            case "target_blue":
                return "B";
            case "target_yellow":
                return "Y";
            case "target_multi":
                return "M";
            case "":
                return " ";
            default:
                return this.type;
        }
    }
}
