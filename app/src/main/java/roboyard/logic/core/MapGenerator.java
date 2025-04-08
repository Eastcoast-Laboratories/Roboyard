package roboyard.logic.core;

import roboyard.ui.activities.MainActivity;
import roboyard.ui.components.GridGameView;

import java.util.ArrayList;
import java.util.Random;

import timber.log.Timber;
import roboyard.logic.core.WallStorage;

/**
 * Created by Alain on 04/02/2015.
 * 
 * This class has been modified to use GameLogic internally while maintaining
 * backward compatibility with the original implementation.
 */
public class MapGenerator {

    private final Random rand;
    private final GameLogic gameLogic;

    // Difficulty level constants
    private static final int DIFFICULTY_BEGINNER = 0;
    private static final int DIFFICULTY_ADVANCED = 1;
    private static final int DIFFICULTY_INSANE = 2;
    private static final int DIFFICULTY_IMPOSSIBLE = 3;

    // position of the square in the middle of the game board
    int carrePosX; // horizontal position of the top wall of square, starting with 0
    int carrePosY; // vertical position of the left wall of the square

    Boolean targetMustBeInCorner = true; // TODO: only works together with generateNewMapEachTime==true (which is set only in Beginner Mode)
    Boolean allowMulticolorTarget = true;
    public static Boolean generateNewMapEachTime = true; // option in settings
    private int robotCount = 1; // Default to 1 robot per color
    private int targetColors = 4; // Default to 4 different target colors

    // Wall configuration
    int maxWallsInOneVerticalCol = 2;    // Maximum number of walls allowed in one vertical column
    int maxWallsInOneHorizontalRow = 2;  // Maximum number of walls allowed in one horizontal row
    int wallsPerQuadrant;                // Number of walls to place in each quadrant of the board

    Boolean loneWallsAllowed = false; // walls that are not attached in a 90 deg. angle

    public MapGenerator(){
        rand = new Random();

        // Initialize square position based on current board size
        carrePosX = (MainActivity.getBoardWidth()/2)-1;
        carrePosY = (MainActivity.getBoardHeight()/2)-1;

        // Calculate walls per quadrant based on board width
        wallsPerQuadrant = MainActivity.getBoardWidth()/4;  // Default: quarter of board width

        // Check difficulty level
        int level = GridGameView.getLevel();
        if(level == DIFFICULTY_BEGINNER){ // Difficulty Beginner
            // For beginner level
        } else {
            if(level == DIFFICULTY_ADVANCED){ // Advanced
                // nothing to do
            }
            
            if (generateNewMapEachTime) {
                // TODO: doesn't work if not generateNewMapEachTime because the position is not remembered above restarts with the same map
                // TODO: does not work with the roboyard in the middle, that is not moved to the new random position
                // random position of square in the middle
                // carrePosX=getRandom(3,MainActivity.getBoardWidth()-5);
                // carrePosY=getRandom(3,MainActivity.getBoardHeight()-5);
            }
            
            if(level == DIFFICULTY_INSANE){ // Insane
                allowMulticolorTarget = false;
                // target must be in corner is left to "true"
            }
            
            if(level == DIFFICULTY_IMPOSSIBLE){ // Impossible
                // Get a completely different wall layout then the target is harder to reach
                loneWallsAllowed = true;
                maxWallsInOneVerticalCol = 3;
                maxWallsInOneHorizontalRow = 3;
                targetMustBeInCorner = false;
            }
        }

        if(level == DIFFICULTY_INSANE || level == DIFFICULTY_IMPOSSIBLE) {
            targetMustBeInCorner = false;

            maxWallsInOneVerticalCol = 5;
            maxWallsInOneHorizontalRow = 5;
            wallsPerQuadrant = MainActivity.getBoardWidth()/3;
        }
        if(level == DIFFICULTY_IMPOSSIBLE) {
            wallsPerQuadrant = (int) (MainActivity.getBoardWidth()/2.3); // for debug, set to 1.3 with lots of walls
        }
        if (MainActivity.boardSizeX * MainActivity.boardSizeY > 64) {
            // calculate maxWallsInOneVerticalCol and maxWallsInOneHorizontalRow based on board size
        }
        
        // Initialize GameLogic with the same configuration
        gameLogic = new GameLogic(MainActivity.getBoardWidth(), MainActivity.getBoardHeight(), level);
        
        // Synchronize with GameLogic's static setting
        GameLogic.setgenerateNewMapEachTime(generateNewMapEachTime);
        
        Timber.d("wallsPerQuadrant: " + wallsPerQuadrant + " Board size: " + MainActivity.boardSizeX + "x" + MainActivity.boardSizeY);
    }
    
    /**
     * Sets the number of robots per color for map generation
     * @param count Number of robots per color (1-4)
     */
    public void setRobotCount(int count) {
        this.robotCount = Math.max(1, Math.min(4, count));
        
        // Pass the robot count to the GameLogic if it exists
        if (gameLogic != null) {
            gameLogic.setRobotCount(this.robotCount);
        }
        
        Timber.d("MapGenerator robot count set to %d", this.robotCount);
    }
    
    /**
     * Gets the current robot count setting
     * @return Number of robots per color (1-4)
     */
    public int getRobotCount() {
        return robotCount;
    }

    /**
     * Sets the number of different target colors for map generation
     * @param count Number of different target colors (1-4)
     */
    public void setTargetColors(int count) {
        this.targetColors = Math.max(1, Math.min(4, count));
        
        // Pass the target colors to the GameLogic if it exists
        if (gameLogic != null) {
            gameLogic.setTargetColors(this.targetColors);
        }
        
        Timber.d("MapGenerator target colors set to %d", this.targetColors);
    }
    
    /**
     * Gets the current target colors setting
     * @return Number of different target colors (1-4)
     */
    public int getTargetColors() {
        return targetColors;
    }

    /**
     * gets the value of generateNewMapEachTime
     */
    public static boolean getgenerateNewMapEachTime() {
        return generateNewMapEachTime;
    }

    public ArrayList<GridElement> removeGameElementsFromMap(ArrayList<GridElement> data) {
        // Delegate to GameLogic
        return gameLogic.removeGameElementsFromMap(data);
    }

    public ArrayList<GridElement> translateArraysToMap(int[][] horizontalWalls, int[][] verticalWalls) {
        // Delegate to GameLogic
        return gameLogic.translateArraysToMap(horizontalWalls, verticalWalls);
    }

    public int getRandom(int min, int max) {
        // Delegate to GameLogic
        return gameLogic.getRandom(min, max);
    }

    /**
     * generates a new map. The map is divided into four quadrants, like in the game ricochet robots. and walls are evenly distributed among all quadrants.
     * for each quadrant there are 2 walls placed at a right-angle to the each border.
     * @return Arraylist with all grid elements that belong to the map
     */
    public ArrayList<GridElement> getGeneratedGameMap() {
        // We'll check the latest value of generateNewMapEachTime from our static variable
        // which is already set by SettingsGameScreen when preferences are changed
        Timber.d("[WALL STORAGE] class default value for generateNewMapEachTime: %s", generateNewMapEachTime);
        
        ArrayList<GridElement> data = GridGameView.getMap();
        
        // Synchronize static settings with GameLogic
        GameLogic.setgenerateNewMapEachTime(generateNewMapEachTime);
        
        // Check if we should preserve walls
        WallStorage wallStorage = WallStorage.getInstance();
        // Update board size to ensure we're using the right storage
        wallStorage.updateCurrentBoardSize();
        
        boolean preserveWalls = !Preferences.generateNewMapEachTime && wallStorage.hasStoredWalls();
        Timber.d("[WALL STORAGE] MapGenerator: generateNewMapEachTime: %s, Preserving walls: %s, hasStoredWalls: %s", 
                Preferences.generateNewMapEachTime, preserveWalls, wallStorage.hasStoredWalls());
        
        if (preserveWalls) {
            Timber.d("[WALL STORAGE] Preserving walls from stored configuration");
            // Remove game elements (robots and targets) but keep walls
            if (data != null && !data.isEmpty()) {
                data = removeGameElementsFromMap(data);
                
                // Apply stored walls to the map
                data = wallStorage.applyWallsToElements(data);
            } else {
                // If no existing map, use the stored walls
                data = new ArrayList<>(wallStorage.getStoredWalls());
            }
            
            // IMPORTANT: Ensure all outer walls exist before adding game elements
            // This ensures consistent wall behavior even with preserved walls
            data = gameLogic.ensureOuterWalls(data);
            Timber.d("[WALL STORAGE] MapGenerator - Verified outer walls in preserved walls: %d elements", data.size());
            
            // Delegate to GameLogic to add robots and targets
            data = gameLogic.addGameElementsToGameMap(data, null, null);
        } else {
            // Generate a completely new map
            data = gameLogic.generateGameMap(data);
            
            // IMPORTANT: Explicitly ensure all outer walls exist before storing
            data = gameLogic.ensureOuterWalls(data);
            Timber.d("[WALL STORAGE] MapGenerator - Verified outer walls in new map: %d elements", data.size());
            
            // Store the walls for future use if we're not generating new maps each time
            if (!Preferences.generateNewMapEachTime) {
                wallStorage.storeWalls(data);
                Timber.d("[WALL STORAGE] Stored walls for future use");
            }
        }
        
        // Store the map for future use
        GridGameView.setMap(data);
        
        return data;
    }
}
