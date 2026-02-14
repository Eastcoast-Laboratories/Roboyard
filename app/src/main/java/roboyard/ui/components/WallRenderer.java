package roboyard.ui.components;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import roboyard.logic.core.Wall;
import roboyard.logic.core.WallModel;
import roboyard.logic.core.WallType;
import timber.log.Timber;

/**
 * Renderer class responsible for drawing walls on the game board.
 * This class handles the visual representation of walls with proper scaling.
 */
public class WallRenderer {
    private final WallModel model;
    private final float cellSize;
    private final Drawable horizontalWallDrawable;
    private final Drawable verticalWallDrawable;
    
    // Factors for wall thickness and offset, moved from GameGridView
    private static final float WALL_THICKNESS_FACTOR = 0.6f;
    private static final float WALL_OFFSET_FACTOR = 0.24f;
    
    /**
     * Creates a new wall renderer with the specified model and drawables.
     *
     * @param model The wall model to render
     * @param cellSize The size of each cell in pixels
     * @param horizontalWall The drawable for horizontal walls
     * @param verticalWall The drawable for vertical walls
     */
    public WallRenderer(WallModel model, float cellSize, 
                      Drawable horizontalWall, Drawable verticalWall) {
        this.model = model;
        this.cellSize = cellSize;
        this.horizontalWallDrawable = horizontalWall;
        this.verticalWallDrawable = verticalWall;
    }
    
    /**
     * Draws all walls on the canvas.
     *
     * @param canvas The canvas to draw on
     * @param offsetX The x offset for drawing
     * @param offsetY The y offset for drawing
     */
    public void drawWalls(Canvas canvas, float offsetX, float offsetY) {
        for (Wall wall : model.getWalls()) {
            if (wall.getType() == WallType.HORIZONTAL) {
                drawHorizontalWall(canvas, wall, offsetX, offsetY);
            } else {
                drawVerticalWall(canvas, wall, offsetX, offsetY);
            }
        }
    }
    
    /**
     * Draws a horizontal wall on the canvas.
     *
     * @param canvas The canvas to draw on
     * @param wall The wall to draw
     * @param offsetX The x offset for drawing
     * @param offsetY The y offset for drawing
     */
    private void drawHorizontalWall(Canvas canvas, Wall wall, float offsetX, float offsetY) {
        if (horizontalWallDrawable == null) {
            Timber.d("Horizontal wall drawable not available");
            return;
        }
        
        int x = wall.getX();
        int y = wall.getY();
        
        // Calculate wall dimensions with proper scaling
        float offset = cellSize * WALL_OFFSET_FACTOR;
        float left = offsetX + (x * cellSize - offset);
        float top = offsetY + (y * cellSize);
        float right = offsetX + ((x + 1) * cellSize + offset);
        float wallThickness = cellSize * WALL_THICKNESS_FACTOR;
        
        // Handle the border case for bottom border correctly
        if (y == model.getBoardHeight()) {
            // Bottom border wall at edge of board
            top = offsetY + (model.getBoardHeight() * cellSize);
        }
        
        // Ensure the wall doesn't extend beyond the board boundaries
        if (right > offsetX + (model.getBoardWidth() * cellSize)) {
            right = offsetX + (model.getBoardWidth() * cellSize);
        }
        
        // Draw the wall
        horizontalWallDrawable.setBounds(
            (int)left, 
            (int)(top - wallThickness/2), 
            (int)right, 
            (int)(top + wallThickness/2)
        );
        horizontalWallDrawable.draw(canvas);
    }
    
    /**
     * Draws a vertical wall on the canvas.
     *
     * @param canvas The canvas to draw on
     * @param wall The wall to draw
     * @param offsetX The x offset for drawing
     * @param offsetY The y offset for drawing
     */
    private void drawVerticalWall(Canvas canvas, Wall wall, float offsetX, float offsetY) {
        if (verticalWallDrawable == null) {
            Timber.d("Vertical wall drawable not available");
            return;
        }
        
        int x = wall.getX();
        int y = wall.getY();
        
        // Calculate wall dimensions with proper scaling
        float offset = cellSize * WALL_OFFSET_FACTOR;
        float left = offsetX + (x * cellSize);
        float top = offsetY + (y * cellSize - offset);
        float bottom = offsetY + ((y + 1) * cellSize + offset);
        float wallThickness = cellSize * WALL_THICKNESS_FACTOR;
        
        // Handle the border case for right border correctly
        if (x == model.getBoardWidth()) {
            // Right border wall at edge of board
            left = offsetX + (model.getBoardWidth() * cellSize);
        }
        
        // Ensure the wall doesn't extend beyond the board boundaries
        if (bottom > offsetY + (model.getBoardHeight() * cellSize)) {
            bottom = offsetY + (model.getBoardHeight() * cellSize);
        }
        
        // Draw the wall
        verticalWallDrawable.setBounds(
            (int)(left - wallThickness/2), 
            (int)top, 
            (int)(left + wallThickness/2), 
            (int)bottom
        );
        verticalWallDrawable.draw(canvas);
    }
}
