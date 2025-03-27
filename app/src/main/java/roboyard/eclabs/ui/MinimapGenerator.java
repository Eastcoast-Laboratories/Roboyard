package roboyard.eclabs.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

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
        emptyPaint.setColor(Color.rgb(20, 20, 40));
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
        
        // Fill background
        canvas.drawColor(Color.BLACK);
        
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
                    case 2: // Target
                        canvas.drawRect(left, top, right, bottom, targetPaint);
                        break;
                    default: // Empty
                        canvas.drawRect(left, top, right, bottom, emptyPaint);
                        break;
                }
            }
        }
        
        // Draw robots
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_ROBOT) {
                float centerX = offsetX + ((element.getX() + 0.5f) * cellSize);
                float centerY = offsetY + ((element.getY() + 0.5f) * cellSize);
                float radius = cellSize * 0.4f;
                
                // Set color based on robot color
                switch (element.getColor()) {
                    case 0: robotPaint.setColor(Color.RED); break;
                    case 1: robotPaint.setColor(Color.GREEN); break;
                    case 2: robotPaint.setColor(Color.BLUE); break;
                    case 3: robotPaint.setColor(Color.YELLOW); break;
                    default: robotPaint.setColor(Color.MAGENTA); break;
                }
                
                canvas.drawCircle(centerX, centerY, radius, robotPaint);
            }
        }
        
        return bitmap;
    }
    
    /**
     * Generate a minimap from raw map data (for integration with old code)
     * This specifically addresses the getMapData method mentioned in the memory
     * 
     * @param context Application context
     * @param mapData 2D array of map cells
     * @param width Width of the minimap in pixels
     * @param height Height of the minimap in pixels
     * @return Bitmap containing the minimap
     */
    public Bitmap generateMinimapFromData(Context context, int[][] mapData, int width, int height) {
        if (mapData == null || mapData.length == 0 || mapData[0].length == 0) {
            return null;
        }
        
        // Create bitmap and canvas
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // Fill background
        canvas.drawColor(Color.BLACK);
        
        int mapHeight = mapData.length;
        int mapWidth = mapData[0].length;
        
        // Calculate cell size
        float cellWidth = (float) width / mapWidth;
        float cellHeight = (float) height / mapHeight;
        float cellSize = Math.min(cellWidth, cellHeight);
        
        // Center the minimap
        float offsetX = (width - (cellSize * mapWidth)) / 2;
        float offsetY = (height - (cellSize * mapHeight)) / 2;
        
        // Draw cells
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                float left = offsetX + (x * cellSize);
                float top = offsetY + (y * cellSize);
                float right = left + cellSize;
                float bottom = top + cellSize;
                
                // Draw cell based on type from mapData
                int cellValue = mapData[y][x];
                if (cellValue == 1) { // Wall
                    canvas.drawRect(left, top, right, bottom, wallPaint);
                } else if (cellValue == 2) { // Target
                    canvas.drawRect(left, top, right, bottom, targetPaint);
                } else { // Empty or other
                    canvas.drawRect(left, top, right, bottom, emptyPaint);
                }
                
                // Robots would be handled separately in the GameState version
            }
        }
        
        return bitmap;
    }
    
    /**
     * Clear any cached resources
     */
    public void clearCache() {
        // Reset any cached resources
    }
}
