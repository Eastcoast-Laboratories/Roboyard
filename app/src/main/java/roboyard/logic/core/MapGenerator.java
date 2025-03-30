package roboyard.eclabs;
import roboyard.ui.components.GridGameScreen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import timber.log.Timber;

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
    static Boolean generateNewMapEachTime = true; // option in settings

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
        int level = GridGameScreen.getLevel();
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
            allowMulticolorTarget = false;

            maxWallsInOneVerticalCol = 3;
            maxWallsInOneHorizontalRow = 3;
            wallsPerQuadrant = (int) (MainActivity.getBoardWidth()/3.3);

            loneWallsAllowed = true;
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
        GameLogic.setGenerateNewMapEachTime(generateNewMapEachTime);
        
        Timber.d("wallsPerQuadrant: " + wallsPerQuadrant + " Board size: " + MainActivity.boardSizeX + "x" + MainActivity.boardSizeY);
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

    public ArrayList<GridElement> addGameElementsToGameMap(ArrayList<GridElement> data ,int[][] horizontalWalls, int[][]verticalWalls){
        // Delegate to GameLogic
        return gameLogic.addGameElementsToGameMap(data, horizontalWalls, verticalWalls);
    }

    /**
     * generates a new map. The map is divided into four quadrants, like in the game ricochet robots. and walls are evenly distributed among all quadrants.
     * for each quadrant there are 2 walls placed at a right-angle to the each border.
     * @return Arraylist with all grid elements that belong to the map
     */
    public ArrayList<GridElement> getGeneratedGameMap() {
        // We'll check the latest value of generateNewMapEachTime from our static variable
        // which is already set by SettingsGameScreen when preferences are changed
        Timber.d("Using generateNewMapEachTime: %s", generateNewMapEachTime);
        
        ArrayList<GridElement> data = GridGameScreen.getMap();
        
        // Synchronize static settings with GameLogic
        GameLogic.setGenerateNewMapEachTime(generateNewMapEachTime);
        
        // Delegate the map generation to GameLogic
        data = gameLogic.generateGameMap(data);
        
        // Store the map for future use
        GridGameScreen.setMap(data);
        
        return data;
    }
}
