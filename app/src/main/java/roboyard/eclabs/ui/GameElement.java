package roboyard.eclabs.ui;

import java.io.Serializable;

import roboyard.logic.core.Constants;

/**
 * Represents a game element such as a robot or target.
 */
public class GameElement implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Element types
    public static final int TYPE_ROBOT = Constants.TYPE_ROBOT;
    public static final int TYPE_TARGET = Constants.TYPE_TARGET;
    public static final int TYPE_HORIZONTAL_WALL = Constants.TYPE_HORIZONTAL_WALL; // Horizontal wall between rows (mh)
    public static final int TYPE_VERTICAL_WALL = Constants.TYPE_VERTICAL_WALL;   // Vertical wall between columns (mv)
    
    // Element properties
    private final int type;
    private int x;
    private int y;
    private int color; // 0=red, 1=green, 2=blue, 3=yellow
    private boolean selected;
    
    /**
     * Create a new game element
     * @param type Element type
     * @param x Initial X position
     * @param y Initial Y position
     */
    public GameElement(int type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.color = 0;
        this.selected = false;
    }
    
    /**
     * Get the element type
     * @return Element type (TYPE_ROBOT or TYPE_TARGET)
     */
    public int getType() {
        return type;
    }
    
    /**
     * Get the X position
     * @return X position
     */
    public int getX() {
        return x;
    }
    
    /**
     * Set the X position
     * @param x New X position
     */
    public void setX(int x) {
        this.x = x;
    }
    
    /**
     * Get the Y position
     * @return Y position
     */
    public int getY() {
        return y;
    }
    
    /**
     * Set the Y position
     * @param y New Y position
     */
    public void setY(int y) {
        this.y = y;
    }
    
    /**
     * Get the element color
     * @return Color index (0=red, 1=green, 2=blue, 3=yellow)
     */
    public int getColor() {
        return color;
    }
    
    /**
     * Set the element color
     * @param color New color index
     */
    public void setColor(int color) {
        this.color = color;
    }
    
    /**
     * Check if the element is selected
     * @return True if selected
     */
    public boolean isSelected() {
        return selected;
    }
    
    /**
     * Set the selection state
     * @param selected New selection state
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isRobot() {
        return type == TYPE_ROBOT;
    }

}
