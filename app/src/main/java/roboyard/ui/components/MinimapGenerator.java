package roboyard.ui.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import roboyard.logic.core.GameState;
import roboyard.logic.core.GameElement;

/**
 * Utility class to generate minimap thumbnails of game boards.
 * This addresses the minimap display issue mentioned in the memory.
 */
public class MinimapGenerator {
    
    // Singleton instance
    private static MinimapGenerator instance;
    
    // Paint objects for different map elements
    private final Paint wallPaint;
    private final Paint emptyPaint;
    private final Paint targetPaint;
    private final Paint robotPaint;
    
    /**
     * Get the singleton instance of MinimapGenerator
     */
    public static synchronized MinimapGenerator getInstance() {
        if (instance == null) {
            instance = new MinimapGenerator();
        }
        return instance;
    }
    
    /**
     * Private constructor to enforce singleton pattern
     */
    private MinimapGenerator() {
        // Initialize paint objects
        wallPaint = new Paint();
        wallPaint.setColor(Color.DKGRAY);
        wallPaint.setStyle(Paint.Style.FILL);
        
        emptyPaint = new Paint();
        emptyPaint.setColor(Color.rgb(200, 240, 200));
        emptyPaint.setStyle(Paint.Style.FILL);
        
        targetPaint = new Paint();
        targetPaint.setColor(Color.rgb(40, 40, 80));
        targetPaint.setStyle(Paint.Style.FILL);
        
        robotPaint = new Paint();
        robotPaint.setStyle(Paint.Style.FILL);
    }
    
    /**
     * Generate a minimap bitmap from a game state
     * 
     * @param context Application context
     * @param state Game state to render
     * @param width Width of the minimap in pixels
     * @param height Height of the minimap in pixels
     * @return Bitmap containing the minimap
     */
    public Bitmap generateMinimap(Context context, GameState state, int width, int height) {
        if (state == null) {
            return null;
        }
        
        // Create bitmap and canvas
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // Fill background with light green
        canvas.drawColor(Color.rgb(200, 240, 200));
        
        // Calculate cell size
        float cellWidth = (float) width / state.getWidth();
        float cellHeight = (float) height / state.getHeight();
        float cellSize = Math.min(cellWidth, cellHeight);
        
        // Center the minimap
        float offsetX = (width - (cellSize * state.getWidth())) / 2;
        float offsetY = (height - (cellSize * state.getHeight())) / 2;
        
        // Draw cells
        for (int y = 0; y < state.getHeight(); y++) {
            for (int x = 0; x < state.getWidth(); x++) {
                float left = offsetX + (x * cellSize);
                float top = offsetY + (y * cellSize);
                float right = left + cellSize;
                float bottom = top + cellSize;
                
                // Draw cell based on type
                int cellType = state.getCellType(x, y);
                switch (cellType) {
                    case 1: // Wall
                        canvas.drawRect(left, top, right, bottom, wallPaint);
                        break;
                    default: // Empty or target (targets drawn separately as X)
                        canvas.drawRect(left, top, right, bottom, emptyPaint);
                        break;
                }
            }
        }
        
        // Draw grid lines thin)
        Paint gridPaint = new Paint();
        gridPaint.setColor(Color.rgb(140, 164, 140));
        gridPaint.setStrokeWidth(1.0f);
        gridPaint.setAntiAlias(true);
        
        // Draw vertical grid lines (including middle)
        for (int x = 0; x <= state.getWidth(); x++) {
            float lineX = offsetX + (x * cellSize);
            canvas.drawLine(lineX, offsetY, lineX, offsetY + (cellSize * state.getHeight()), gridPaint);
        }
        
        // Draw horizontal grid lines (including middle)
        for (int y = 0; y <= state.getHeight(); y++) {
            float lineY = offsetY + (y * cellSize);
            canvas.drawLine(offsetX, lineY, offsetX + (cellSize * state.getWidth()), lineY, gridPaint);
        }
        
        // Draw center carree (2x2 dark green square in the middle)
        int centerX = (state.getWidth() / 2) - 1;
        int centerY = (state.getHeight() / 2) - 1;
        Paint carreePaint = new Paint();
        carreePaint.setColor(Color.rgb(0, 100, 0));
        carreePaint.setStyle(Paint.Style.FILL);
        
        float carreeLeft = offsetX + (centerX * cellSize);
        float carreeTop = offsetY + (centerY * cellSize);
        float carreeRight = carreeLeft + (2 * cellSize);
        float carreeBottom = carreeTop + (2 * cellSize);
        canvas.drawRect(carreeLeft, carreeTop, carreeRight, carreeBottom, carreePaint);
        
        // Draw targets as X marks
        Paint targetXPaint = new Paint();
        targetXPaint.setStyle(Paint.Style.STROKE);
        targetXPaint.setStrokeWidth(Math.max(1.5f, cellSize * 0.15f));
        targetXPaint.setAntiAlias(true);
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_TARGET) {
                float left = offsetX + (element.getX() * cellSize);
                float top = offsetY + (element.getY() * cellSize);
                float right = left + cellSize;
                float bottom = top + cellSize;
                float pad = cellSize * 0.2f;
                
                // Set target X color based on target color
                switch (element.getColor()) {
                    case 0: targetXPaint.setColor(Color.rgb(255, 100, 150)); break; // Pink
                    case 1: targetXPaint.setColor(Color.rgb(0, 180, 0)); break;     // Green
                    case 2: targetXPaint.setColor(Color.rgb(50, 50, 255)); break;   // Blue
                    case 3: targetXPaint.setColor(Color.rgb(200, 200, 0)); break;   // Yellow
                    default: targetXPaint.setColor(Color.MAGENTA); break;
                }
                
                // Draw X
                canvas.drawLine(left + pad, top + pad, right - pad, bottom - pad, targetXPaint);
                canvas.drawLine(right - pad, top + pad, left + pad, bottom - pad, targetXPaint);
            }
        }
        
        // Draw walls as thick dark green lines
        Paint wallLinePaint = new Paint();
        wallLinePaint.setColor(Color.rgb(104, 131, 54));
        wallLinePaint.setStrokeWidth(Math.max(2.0f, cellSize * 0.2f));
        wallLinePaint.setAntiAlias(true);
        
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_HORIZONTAL_WALL) {
                // Horizontal wall: draw line above the cell
                float wallX1 = offsetX + (element.getX() * cellSize);
                float wallY = offsetY + (element.getY() * cellSize);
                float wallX2 = wallX1 + cellSize;
                canvas.drawLine(wallX1, wallY, wallX2, wallY, wallLinePaint);
            } else if (element.getType() == GameElement.TYPE_VERTICAL_WALL) {
                // Vertical wall: draw line to the left of the cell
                float wallX = offsetX + (element.getX() * cellSize);
                float wallY1 = offsetY + (element.getY() * cellSize);
                float wallY2 = wallY1 + cellSize;
                canvas.drawLine(wallX, wallY1, wallX, wallY2, wallLinePaint);
            }
        }
        
        // Draw robots
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_ROBOT) {
                float centerX_robot = offsetX + ((element.getX() + 0.5f) * cellSize);
                float centerY_robot = offsetY + ((element.getY() + 0.5f) * cellSize);
                float radius = cellSize * 0.4f;
                
                // Set color based on robot color
                switch (element.getColor()) {
                    case 0: robotPaint.setColor(Color.rgb(255, 105, 180)); break; // Pink
                    case 1: robotPaint.setColor(Color.rgb(0, 100, 0)); break;     // Dark green
                    case 2: robotPaint.setColor(Color.BLUE); break;
                    case 3: robotPaint.setColor(Color.YELLOW); break;
                    default: robotPaint.setColor(Color.MAGENTA); break;
                }
                
                canvas.drawCircle(centerX_robot, centerY_robot, radius, robotPaint);
            }
        }
        
        return bitmap;
    }
    

}
