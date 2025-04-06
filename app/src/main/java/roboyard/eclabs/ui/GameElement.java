package roboyard.eclabs.ui;

import java.io.Serializable;

import roboyard.logic.core.Constants;
import timber.log.Timber;

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
    
    // Color constants
    public static final int COLOR_RED = 0;
    public static final int COLOR_GREEN = 1;
    public static final int COLOR_BLUE = 2;
    public static final int COLOR_YELLOW = 3;
    
    // Element properties
    private final int type;
    private int x;
    private int y;
    private int color; // 0=red, 1=green, 2=blue, 3=yellow
    private boolean selected;
    private int directionX = 1; // Default direction (1=right, -1=left)
    
    // Transient properties for animation (not serialized)
    private transient float animationX;
    private transient float animationY;
    private transient boolean hasAnimationPosition;
    private transient boolean animationPositionSet;
    
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
        this.hasAnimationPosition = false;
        this.animationPositionSet = false;
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
    
    /**
     * Check if this element has an animation position set
     * @return true if animation position is set
     */
    public boolean hasAnimationPosition() {
        // Only return true if animationX and animationY are explicitly set
        return animationPositionSet;
    }
    
    /**
     * Set the animation position for this element
     * @param x X position in grid coordinates
     * @param y Y position in grid coordinates
     */
    public void setAnimationPosition(float x, float y) {
        // Log the position change for debugging
        Timber.d("[ANIM] Set animation position for robot %d: (%.2f,%.2f)", getColor(), x, y);
        
        // Validate inputs to avoid setting invalid positions
        if (Float.isNaN(x) || Float.isNaN(y) || Float.isInfinite(x) || Float.isInfinite(y)) {
            Timber.e("[ANIM] Attempted to set invalid animation position: (%.2f,%.2f)", x, y);
            return;
        }
        
        this.animationX = x;
        this.animationY = y;
        this.hasAnimationPosition = true;
        this.animationPositionSet = true;
    }
    
    /**
     * Clear the animation position
     */
    public void clearAnimationPosition() {
        animationPositionSet = false;
    }
    
    /**
     * Get the animation X position in pixels
     * @return Animation X position
     */
    public float getAnimationX() {
        return animationX;
    }
    
    /**
     * Get the animation Y position in pixels
     * @return Animation Y position
     */
    public float getAnimationY() {
        return animationY;
    }
    
    /**
     * Get the horizontal direction the robot is facing
     * @return 1 for right, -1 for left
     */
    public int getDirectionX() {
        return directionX;
    }
    
    /**
     * Set the horizontal direction the robot is facing
     * @param direction 1 for right, -1 for left
     */
    public void setDirectionX(int direction) {
        if (direction != 0) {
            this.directionX = direction > 0 ? 1 : -1;
        }
    }
}
