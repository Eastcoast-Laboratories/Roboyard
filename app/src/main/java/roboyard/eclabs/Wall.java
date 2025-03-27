package roboyard.eclabs;

import android.graphics.drawable.Drawable;

/**
 * Wall game object for drawing walls on top of other elements
 */
public class Wall extends AbstractGameObject {
    private final String type; // "mh" or "mv" for horizontal/vertical
    private final int x;
    private final int y;
    private float gridSpace;
    private int xGrid, yGrid;
    private Drawable drawable;
    
    // Wall configuration - matches GameGridView
    private static final float WALL_THICKNESS_FACTOR = 0.375f; // 3x thicker walls
    
    /**
     * Create a new wall
     * @param type Wall type ("mh" for horizontal, "mv" for vertical)
     * @param x X position in grid
     * @param y Y position in grid
     */
    public Wall(String type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }
    
    /**
     * Set the grid dimensions for this wall
     * @param xGrid X offset of the grid
     * @param yGrid Y offset of the grid
     * @param gridSpace Size of each grid cell
     */
    public void setGridDimensions(int xGrid, int yGrid, float gridSpace) {
        this.xGrid = xGrid;
        this.yGrid = yGrid;
        this.gridSpace = gridSpace;
    }
    
    /**
     * Set the drawable for this wall
     * @param drawable The drawable to use for rendering
     */
    public void setDrawable(Drawable drawable) {
        this.drawable = drawable;
    }

    /**
     * Get the x coordinate of this wall
     * @return x coordinate
     */
    public int getX() {
        return x;
    }
    
    /**
     * Get the y coordinate of this wall
     * @return y coordinate
     */
    public int getY() {
        return y;
    }
    
    /**
     * Get the wall type ("mh" for horizontal, "mv" for vertical)
     * @return The wall type string
     */
    public String getType() {
        return type;
    }
    
    @Override
    public void create() {
        // Nothing to do here
    }
    
    @Override
    public void load(RenderManager renderManager) {
        // Nothing to do here, drawable is set externally
    }
    
    @Override
    public void draw(RenderManager renderManager) {
        if (drawable == null) return;
        
        // Calculate wall dimensions based on grid size
        int pixel = Math.max(1, (int)(gridSpace / 45)); // ensure minimum thickness of 1 pixel
        int stretchWall = 12 * pixel; // stretch all walls
        int offsetWall = -2 * pixel;
        int wallThickness = (int)(gridSpace * WALL_THICKNESS_FACTOR); // 3x thicker
        
        // Calculate bounds based on wall type
        int left, top, right, bottom;
        
        if (type.equals("mh")) { // horizontal wall
            left = xGrid + (int)(x * gridSpace - stretchWall);
            top = yGrid + (int)(y * gridSpace - stretchWall + offsetWall);
            right = xGrid + (int)((x + 1) * gridSpace + stretchWall);
            bottom = yGrid + (int)(y * gridSpace + wallThickness + offsetWall);
        } else if (type.equals("mv")) { // vertical wall
            left = xGrid + (int)(x * gridSpace - stretchWall + offsetWall);
            top = yGrid + (int)(y * gridSpace - stretchWall);
            right = xGrid + (int)(x * gridSpace + wallThickness + offsetWall);
            bottom = yGrid + (int)((y + 1) * gridSpace + stretchWall);
        } else {
            return; // Unknown wall type
        }
        
        // Draw the wall using the renderManager's drawDrawable method
        renderManager.drawDrawable(left, top, right, bottom, drawable);
    }
    
    @Override
    public void update(GameManager gameManager) {
        // Nothing to do here
    }
    
    @Override
    public void destroy() {
        // Nothing to do here
    }
}
