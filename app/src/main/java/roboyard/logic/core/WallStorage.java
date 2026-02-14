package roboyard.logic.core;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Stores and manages wall configurations for preserving walls across game changes.
 * This class allows the game to maintain the same wall layout when resetting robots
 * or starting a new game, based on user preferences.
 */
public class WallStorage {
    private static final String TAG = "WallStorage";
    private static final String PREFS_NAME = "WallStoragePrefs";
    private static final String KEY_WALLS_PREFIX = "walls_";
    
    // Singleton instance
    private static WallStorage instance;
    
    // Stored wall elements
    private ArrayList<GridElement> storedWalls = new ArrayList<>();
    
    // Current board size
    private int currentBoardWidth = 0;
    private int currentBoardHeight = 0;
    
    // Private constructor for singleton pattern
    private WallStorage() {
        // Private constructor to enforce singleton pattern
    }
    
    /**
     * Get the singleton instance of WallStorage
     * @return The WallStorage instance
     */
    public static synchronized WallStorage getInstance() {
        if (instance == null) {
            instance = new WallStorage();
        }
        return instance;
    }
    
    /**
     * Store wall elements from a list of grid elements
     * @param elements List of grid elements containing walls and other elements
     */
    public void storeWalls(List<GridElement> elements) {
        storedWalls.clear();
        
        if (elements == null || elements.isEmpty()) {
            Timber.tag(TAG).d("No elements to store");
            return;
        }
        
        // Extract only wall elements (horizontal and vertical walls)
        for (GridElement element : elements) {
            String type = element.getType();
            if ("mh".equals(type) || "mv".equals(type)) {
                storedWalls.add(element);
            }
        }
        
        // Update current board size
        updateCurrentBoardSize();
        
        Timber.tag(TAG).d("[WALL STORAGE] Stored %d wall elements for board size %dx%d", 
            storedWalls.size(), currentBoardWidth, currentBoardHeight);
        
        // Save to persistent storage
        saveWallsToDisk();
    }
    
    /**
     * Update the current board size from Preferences
     */
    public void updateCurrentBoardSize() {
        int newWidth = Preferences.boardSizeWidth;
        int newHeight = Preferences.boardSizeHeight;
        
        // If board size has changed, clear the in-memory walls
        if (currentBoardWidth != newWidth || currentBoardHeight != newHeight) {
            Timber.tag(TAG).d("[WALL STORAGE] Board size changed from %dx%d to %dx%d, clearing in-memory walls", 
                currentBoardWidth, currentBoardHeight, newWidth, newHeight);
            storedWalls.clear();
        }
        
        currentBoardWidth = newWidth;
        currentBoardHeight = newHeight;
    }
    
    /**
     * Save walls to disk for persistence across app restarts
     */
    private void saveWallsToDisk() {
        Context context = Preferences.getContext();
        if (context == null) {
            Timber.tag(TAG).e("Cannot save walls: context is null");
            return;
        }
        
        updateCurrentBoardSize();
        String key = getWallsKey(currentBoardWidth, currentBoardHeight);
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Convert walls to a string representation
        String wallsData = gridElementsToString(storedWalls);
        
        editor.putString(key, wallsData);
        editor.apply();
        
        Timber.tag(TAG).d("[WALL STORAGE] Saved %d walls to disk for board size %dx%d: String: %s", 
            storedWalls.size(), currentBoardWidth, currentBoardHeight, wallsData);
    }
    
    /**
     * Convert GridElements to a string for storage
     */
    private String gridElementsToString(ArrayList<GridElement> elements) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        
        // Count walls by type and position for debugging
        int topWalls = 0;
        int bottomWalls = 0;
        int leftWalls = 0;
        int rightWalls = 0;
        int otherWalls = 0;
        int horizontalWalls = 0;
        int verticalWalls = 0;
        
        for (GridElement element : elements) {
            // Only store walls (mh, mv)
            if (element.getType().equals("mh") || element.getType().equals("mv")) {
                if (count > 0) {
                    sb.append(";");
                }
                sb.append(element.getType()).append(",").append(element.getX()).append(",").append(element.getY());
                count++;
                
                // Count wall types for debugging
                if (element.getType().equals("mh")) {
                    horizontalWalls++;
                    if (element.getY() == 0) {
                        topWalls++;
                    } else if (element.getY() == currentBoardHeight) {
                        bottomWalls++;
                    } else {
                        otherWalls++;
                    }
                } else if (element.getType().equals("mv")) {
                    verticalWalls++;
                    if (element.getX() == 0) {
                        leftWalls++;
                    } else if (element.getX() == currentBoardWidth) {
                        rightWalls++;
                    } else {
                        otherWalls++;
                    }
                }
            }
        }
        
        Timber.d("[WALL STORAGE] Wall count by position: top=%d, bottom=%d, left=%d, right=%d, other=%d, horizontal=%d, vertical=%d", 
                topWalls, bottomWalls, leftWalls, rightWalls, otherWalls, horizontalWalls, verticalWalls);
        
        return sb.toString();
    }
    
    /**
     * Load walls from disk for the current board size
     */
    public void loadStoredWalls() {
        Context context = Preferences.getContext();
        if (context == null) {
            Timber.tag(TAG).e("Cannot load walls: context is null");
            return;
        }
        
        updateCurrentBoardSize();
        String key = getWallsKey(currentBoardWidth, currentBoardHeight);
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String wallsData = prefs.getString(key, "");
        
        storedWalls.clear();
        
        if (wallsData.isEmpty()) {
            Timber.tag(TAG).d("No saved walls found for board size %dx%d", 
                currentBoardWidth, currentBoardHeight);
            return;
        }
        
        // Parse the string representation back to GridElements
        String[] wallEntries = wallsData.split(";");
        for (String entry : wallEntries) {
            if (entry.isEmpty()) continue;
            
            String[] parts = entry.split(",");
            if (parts.length != 3) continue;
            
            try {
                String type = parts[0];
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                
                GridElement wall = new GridElement(x, y, type);
                storedWalls.add(wall);
            } catch (NumberFormatException e) {
                Timber.tag(TAG).e("Error parsing wall data: %s", e.getMessage());
            }
        }
        
        Timber.tag(TAG).d("Loaded %d walls from disk for board size %dx%d", 
            storedWalls.size(), currentBoardWidth, currentBoardHeight);
    }
    
    /**
     * Get the key for storing walls based on board size
     */
    private String getWallsKey(int width, int height) {
        return KEY_WALLS_PREFIX + width + "x" + height;
    }
    
    /**
     * Clear stored walls for a specific board size
     * @param width Board width
     * @param height Board height
     */
    public void clearStoredWallsForBoardSize(int width, int height) {
        Context context = Preferences.getContext();
        if (context == null) {
            Timber.tag(TAG).e("Cannot clear walls: context is null");
            return;
        }
        
        String key = getWallsKey(width, height);
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(key);
        editor.apply();
        
        // If this is the current board size, also clear the in-memory walls
        if (width == currentBoardWidth && height == currentBoardHeight) {
            storedWalls.clear();
        }
        
        Timber.tag(TAG).d("Cleared stored walls for board size %dx%d", width, height);
    }
    
    /**
     * Check if there are stored walls available
     * @return true if walls are stored, false otherwise
     */
    public boolean hasStoredWalls() {
        // If in-memory walls are empty, try loading from disk
        if (storedWalls.isEmpty()) {
            loadStoredWalls();
        }
        
        return !storedWalls.isEmpty();
    }
    
    /**
     * Get the stored wall elements
     * @return List of stored wall elements
     */
    public ArrayList<GridElement> getStoredWalls() {
        // If in-memory walls are empty, try loading from disk
        if (storedWalls.isEmpty()) {
            loadStoredWalls();
        }
        
        return new ArrayList<>(storedWalls);
    }
    
    
    /**
     * Apply stored walls to a list of grid elements
     * @param elements Original grid elements
     * @return Updated grid elements with walls applied
     */
    public ArrayList<GridElement> applyWallsToElements(ArrayList<GridElement> elements) {
        // If no stored walls, return original elements
        if (storedWalls.isEmpty()) {
            // Try to load from disk
            loadStoredWalls();
            
            // If still empty after loading, return original elements
            if (storedWalls.isEmpty()) {
                Timber.tag(TAG).d("No stored walls to apply");
                return elements;
            }
        }
        
        // Ensure the stored walls match the current board size
        boolean wallsMatchBoardSize = true;
        for (GridElement wall : storedWalls) {
            if (wall.getX() >= currentBoardWidth || wall.getY() >= currentBoardHeight) {
                wallsMatchBoardSize = false;
                Timber.tag(TAG).w("[WALL STORAGE] Stored wall at (%d,%d) is outside current board size %dx%d", 
                    wall.getX(), wall.getY(), currentBoardWidth, currentBoardHeight);
                break;
            }
        }
        
        // If walls don't match board size, clear them and return original elements
        if (!wallsMatchBoardSize) {
            Timber.tag(TAG).d("[WALL STORAGE] Stored walls don't match current board size, clearing and generating new map");
            storedWalls.clear();
            clearStoredWallsForBoardSize(currentBoardWidth, currentBoardHeight);
            return elements;
        }
        
        // Create a copy of the original elements
        ArrayList<GridElement> result = new ArrayList<>(elements);
        
        // Add stored walls
        result.addAll(storedWalls);
        Timber.tag(TAG).d("Applied %d stored walls to grid elements", storedWalls.size());
        
        return result;
    }
}
