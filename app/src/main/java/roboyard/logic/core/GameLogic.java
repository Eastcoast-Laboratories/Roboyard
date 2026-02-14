package roboyard.logic.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import android.graphics.Color;
import driftingdroids.model.Solution;
import roboyard.pm.ia.GameSolution;
import timber.log.Timber;

import roboyard.logic.core.Constants;
import roboyard.logic.core.GridElement;
import roboyard.logic.core.WallStorage;

/**
 * A UI-agnostic class that contains the core game logic for map generation.
 * This class is designed to be used by both the classic UI and the modern UI.
 */
public class GameLogic {

    private final Random rand;

    // Difficulty level constants
    public static final int DIFFICULTY_BEGINNER = Constants.DIFFICULTY_BEGINNER;
    public static final int DIFFICULTY_ADVANCED = Constants.DIFFICULTY_ADVANCED;
    public static final int DIFFICULTY_INSANE = Constants.DIFFICULTY_INSANE;
    public static final int DIFFICULTY_IMPOSSIBLE = Constants.DIFFICULTY_IMPOSSIBLE;

    // position of the square in the middle of the game board
    private final int carrePosX; // horizontal position of the top wall of square, starting with 0
    private final int carrePosY; // vertical position of the left wall of the square

    private Boolean targetMustBeInCorner = true;
    private Boolean allowMulticolorTarget = true;
    private static Boolean generateNewMapEachTime = true; // option in settings

    // Wall configuration
    private int maxWallsInOneVerticalCol = 2;    // Maximum number of walls allowed in one vertical column
    private int maxWallsInOneHorizontalRow = 2;  // Maximum number of walls allowed in one horizontal row
    private int wallsPerQuadrant;                // Number of walls to place in each quadrant of the board

    private Boolean loneWallsAllowed = false; // walls that are not attached in a 90 deg. angle
    
    // Board dimensions
    private final int boardWidth;
    private final int boardHeight;
    
    // Current difficulty level
    private int currentLevel;
    
    // Configuration for multiple targets
    private int robotCount = 1; // Default to 1 robot per color
    private int targetColors = 1; // Anzahl der verschiedenen Zielfarben (1-4) (overridden by Preferences )
    
    // Configuration for the simplified board generation
    private boolean placeWallsInCorners = true;
    private boolean placeWallsOnEdges = true;
    private boolean placeWallsInMiddleSquare = false;
    private int minCornerWalls = 4;
    private int minEdgeWalls = 4;
    private int minTotalWalls = 10;
    private int maxTotalWalls = 20;
    
    /**
     * Constructor for GameLogic with specified board dimensions and difficulty level
     * 
     * @param boardWidth Width of the game board
     * @param boardHeight Height of the game board
     * @param difficultyLevel Difficulty level for the game
     */
    public GameLogic(int boardWidth, int boardHeight, int difficultyLevel) {
        this(boardWidth, boardHeight, difficultyLevel, new Random());
    }
    
    /**
     * Constructor that allows using the same random number generator
     */
    public GameLogic(int boardWidth, int boardHeight, int difficultyLevel, Random random) {
        this.rand = random;
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        this.currentLevel = difficultyLevel;
        
        // Initialize square position based on current board size
        carrePosX = (boardWidth/2)-1;
        carrePosY = (boardHeight/2)-1;

        // Calculate walls per quadrant based on board width
        // Ensure at least 1 wall per quadrant, but not more than board width / 4
        wallsPerQuadrant = Math.max(1, boardWidth/4);  // Default: quarter of board width
        Timber.d("[GAME LOGIC] Board size: %dx%d, walls per quadrant: %d", boardWidth, boardHeight, wallsPerQuadrant);

        // Apply difficulty settings
        applyDifficultySettings(difficultyLevel);
    }
    
    /**
     * Apply difficulty settings based on the level
     */
    private void applyDifficultySettings(int level) {
        // Default: targets must be in corners with two walls
        targetMustBeInCorner = true;
        
        // Store the current level for use in other methods
        currentLevel = level;
        
        // Use configurable preference for multicolor target, or fall back to difficulty-based setting
        allowMulticolorTarget = Preferences.allowMulticolorTarget;
        
        Timber.d("[DIFFICULTY] Setting difficulty level %d (BEGINNER=%d, ADVANCED=%d, INSANE=%d, IMPOSSIBLE=%d)", 
                level, DIFFICULTY_BEGINNER, DIFFICULTY_ADVANCED, DIFFICULTY_INSANE, DIFFICULTY_IMPOSSIBLE);
        
        if(level == DIFFICULTY_BEGINNER) { 
            // For beginner level - targets must be in corners
            targetMustBeInCorner = true;
            Timber.d("[DIFFICULTY] Using BEGINNER settings (targets in corners only)");
        } else if(level == DIFFICULTY_ADVANCED) {
            // For Advanced difficulty, targets can be in random positions
            targetMustBeInCorner = false;
            Timber.d("[DIFFICULTY] Using ADVANCED settings with mixed target placement");

            maxWallsInOneVerticalCol = 3;
            maxWallsInOneHorizontalRow = 3;
            wallsPerQuadrant = (int) (boardWidth/3.3);

            loneWallsAllowed = true;
        } else { 
            // Keep targetMustBeInCorner = true
            Timber.d("[DIFFICULTY] Using INSANE or IMPOSSIBLE settings");

            loneWallsAllowed = true;
            
            // For Insane and Impossible difficulties, targets can appear anywhere except the center
            targetMustBeInCorner = false;
            Timber.d("[DIFFICULTY] Using INSANE/IMPOSSIBLE settings, targets fully random");

            maxWallsInOneVerticalCol = 5;
            maxWallsInOneHorizontalRow = 5;
            wallsPerQuadrant = boardWidth/3;
        }
        
        if(level == DIFFICULTY_IMPOSSIBLE) {
            Timber.d("[DIFFICULTY] Using IMPOSSIBLE settings");
            wallsPerQuadrant = (int) (boardWidth/2.3);
        }
        
        if (boardWidth * boardHeight > 64) {
            // calculate maxWallsInOneVerticalCol and maxWallsInOneHorizontalRow based on board size
        }
        
        Timber.d("[DIFFICULTY] Final settings: targetMustBeInCorner=%b, allowMulticolorTarget=%b, maxWallsInOneVerticalCol=%d, maxWallsInOneHorizontalRow=%d, wallsPerQuadrant=%d, boardSize=%dx%d", 
                targetMustBeInCorner, allowMulticolorTarget, maxWallsInOneVerticalCol, maxWallsInOneHorizontalRow, wallsPerQuadrant, boardWidth, boardHeight);
    }
    
    /**
     * Get a random number between min and max (inclusive)
     * Adds safety checks to ensure min <= max
     */
    public int getRandom(int min, int max) {
        // Add safety check to prevent IllegalArgumentException
        if (min > max) {
            Timber.w("[GAME LOGIC] Invalid random range: min(%d) > max(%d). returning max(%d).", min, max, max);
            return max;
        }
        
        return rand.nextInt((max - min) + 1) + min;
    }
    
    /**
     * Set whether to generate a new map each time
     */
    public static void setgenerateNewMapEachTime(boolean value) {
        generateNewMapEachTime = value;
    }
    
    /**
     * Removes game elements (robots and targets) from a map
     */
    public ArrayList<GridElement> removeGameElementsFromMap(ArrayList<GridElement> data) {
        String[] gameElementTypes = {
                "robot_green", "robot_yellow", "robot_red", "robot_blue", // robots
                "target_green", "target_yellow", "target_red", "target_blue", "target_multi" // targets (cible)
        };
        Iterator<GridElement> iterator = data.iterator();
        while (iterator.hasNext()) {
            GridElement e = iterator.next();
            if(Arrays.asList(gameElementTypes).contains(e.getType())){
                iterator.remove();
            }
        }
        return data;
    }

    /**
     * Convert wall arrays to a list of GridElements
     */
    public ArrayList<GridElement> translateArraysToMap(int[][] horizontalWalls, int[][] verticalWalls) {
        ArrayList<GridElement> data = new ArrayList<>();

        for(int x=0; x<=boardWidth; x++){
            for(int y=0; y <= boardHeight; y++)
            {
                if(horizontalWalls[x][y]== 1) {
                    data.add(new GridElement(x,y,"mh"));
                }
                if(verticalWalls[x][y]== 1) {
                    data.add(new GridElement(x,y,"mv"));
                }
            }
        }

        // Ensure all outer walls exist in the grid data
        data = ensureOuterWalls(data);
        Timber.d("[WALL STORAGE] translateArraysToMap - %d GridElements after ensuring outer walls", data.size());

        return data;
    }

    /**
     * Ensure all outer walls exist in the map data
     * This is critical for consistent wall behavior when walls are preserved
     */
    public ArrayList<GridElement> ensureOuterWalls(ArrayList<GridElement> data) {
        Timber.d("[WALL STORAGE] ensureOuterWalls called for board %dx%d", boardWidth, boardHeight);
        ArrayList<GridElement> newData = new ArrayList<>(data);
        // Check each outer wall position and add if missing
        boolean[] horizontalTopExists = new boolean[boardWidth];
        boolean[] horizontalBottomExists = new boolean[boardWidth];
        boolean[] verticalLeftExists = new boolean[boardHeight];
        boolean[] verticalRightExists = new boolean[boardHeight];

        // First pass: check which outer walls already exist
        for (GridElement element : data) {
            if (element.getType().equals("mh")) {
                // Horizontal walls
                if (element.getY() == 0) {
                    horizontalTopExists[element.getX()] = true;
                    Timber.d("[WALL STORAGE] Horizontal top wall found at (%d,0)", element.getX());
                } else if (element.getY() == boardHeight) {
                    horizontalBottomExists[element.getX()] = true;
                    Timber.d("[WALL STORAGE] Horizontal bottom wall found at (%d,%d)", element.getX(), boardHeight);
                }
            } else if (element.getType().equals("mv")) {
                // Vertical walls
                if (element.getX() == 0) {
                    verticalLeftExists[element.getY()] = true;
                    Timber.d("[WALL STORAGE] Vertical left wall found at (0,%d)", element.getY());
                } else if (element.getX() == boardWidth) {
                    verticalRightExists[element.getY()] = true;
                    Timber.d("[WALL STORAGE] Vertical right wall found at (%d,%d)", boardWidth, element.getY());
                }
            }
        }

        int missingWalls = 0;

        // Add missing top walls
        for (int x = 0; x < boardWidth; x++) {
            if (!horizontalTopExists[x]) {
                newData.add(new GridElement(x, 0, "mh"));
                Timber.d("[WALL STORAGE] missing top wall at (%d,0)", x);
                missingWalls++;
            }
        }

        // Add missing bottom walls
        for (int x = 0; x < boardWidth; x++) {
            if (!horizontalBottomExists[x]) {
                newData.add(new GridElement(x, boardHeight, "mh"));
                Timber.d("[WALL STORAGE] missing bottom wall at (%d,%d)", x, boardHeight);
                missingWalls++;
            }
        }

        // Add missing left walls
        for (int y = 0; y < boardHeight; y++) {
            if (!verticalLeftExists[y]) {
                newData.add(new GridElement(0, y, "mv"));
                Timber.d("[WALL STORAGE] missing left wall at (0,%d)", y);
                missingWalls++;
            }
        }

        // Add missing right walls
        for (int y = 0; y < boardHeight; y++) {
            if (!verticalRightExists[y]) {
                newData.add(new GridElement(boardWidth, y, "mv"));
                Timber.d("[WALL STORAGE] missing right wall at (%d,%d)", boardWidth, y);
                missingWalls++;
            }
        }

        Timber.d("[WALL STORAGE] %d missing outer walls", missingWalls);
        
        return newData;
        // return data; // send back the original data
        // return newData; this would send back the new data with the missing walls, but i'd rather just show the error messages and find out, why they are missing
    }

    /**
     * Add game elements (robots and targets) to a map
     */
    public ArrayList<GridElement> addGameElementsToGameMap(ArrayList<GridElement> data, int[][] horizontalWalls, int[][] verticalWalls) {
        Timber.d("[TARGET PLACEMENT] INITIAL CHECK: currentLevel=%d, targetMustBeInCorner=%b (DIFF_INSANE=%d, DIFF_IMPOSSIBLE=%d)", 
                currentLevel, targetMustBeInCorner, DIFFICULTY_INSANE, DIFFICULTY_IMPOSSIBLE);
        
        boolean abandon;
        
        Timber.d("[TARGET PLACEMENT] Starting target placement with difficulty=%d, targetMustBeInCorner=%b", 
                currentLevel, targetMustBeInCorner);
        
        // Use our color management methods to generate target and robot type strings
        String[] typesOfTargets;
        if (allowMulticolorTarget) {
            // Include multi-color target if allowed
            typesOfTargets = new String[Constants.NUM_ROBOTS + 1]; // standard targets + multi-colored target
            for (int i = 0; i < Constants.NUM_ROBOTS; i++) {
                typesOfTargets[i] = getObjectType(i, false); // false = target
            }
            typesOfTargets[Constants.NUM_ROBOTS] = "target_multi"; // Add multi-target at the last index
            Timber.d("[TARGET_MULTI] Multi-color target INCLUDED in available targets");
        } else {
            // Exclude multi-color target if not allowed
            typesOfTargets = new String[Constants.NUM_ROBOTS]; // standard targets only
            for (int i = 0; i < Constants.NUM_ROBOTS; i++) {
                typesOfTargets[i] = getObjectType(i, false); // false = target
            }
            Timber.d("[TARGET_MULTI] Multi-color target EXCLUDED from available targets");
        }
        
        String[] typesOfRobots = new String[Constants.NUM_ROBOTS]; 
        for (int i = 0; i < Constants.NUM_ROBOTS; i++) {
            typesOfRobots[i] = getObjectType(i, true); // true = robot
        }
        // workaround for backward compatibility
        typesOfRobots[0] = "robot_red";
        typesOfTargets[0] = "target_red";

        // debug: set target to pink (red)
        // typesOfTargets = new String[1]; typesOfTargets[0] = "target_red";

        // Store all positions of game elements to avoid overlapping
        ArrayList<GridElement> allElements = new ArrayList<>();
        
        // Create targets based on targetCount and targetColors settings
        // We'll create targets for each color (or multi-color) up to the targetColors limit
        int maxTargetTypes = typesOfTargets.length; // Use the actual array length which already accounts for allowMulticolorTarget
        int targetTypesCount = Math.min(targetColors, maxTargetTypes); // Limit to targetColors
        targetTypesCount = Math.max(1, targetTypesCount); // Ensure at least one target is always created
        
        Timber.d("[TARGET GENERATION] targetColors=%d, maxTargetTypes=%d, targetTypesCount=%d", 
                targetColors, maxTargetTypes, targetTypesCount);
        
        // Create an array of indices to use for target types, and shuffle it to randomize which colors are used
        int[] targetTypeIndices = new int[typesOfTargets.length];
        for (int i = 0; i < typesOfTargets.length; i++) {
            targetTypeIndices[i] = i;
        }
        
        // Shuffle the array to randomize which colors are used when targetColors < NUM_ROBOTS
        shuffleIntArray(targetTypeIndices);
        
        // Only use the first targetTypesCount elements from the shuffled array
        Timber.d("[TARGET] Will use %d different target types out of %d possible types", targetTypesCount, maxTargetTypes);
        
        // If horizontalWalls and verticalWalls are null, create empty arrays
        if (horizontalWalls == null || verticalWalls == null) {
            Timber.d("[WALL STORAGE] Creating empty wall arrays for target placement");
            horizontalWalls = new int[boardWidth+1][boardHeight+1];
            verticalWalls = new int[boardWidth+1][boardHeight+1];
            
            // Extract wall information from data if available
            if (data != null) {
                for (GridElement element : data) {
                    String type = element.getType();
                    int x = element.getX();
                    int y = element.getY();
                    
                    if ("mh".equals(type) && x < boardWidth && y < boardHeight) {
                        horizontalWalls[x][y] = 1;
                    } else if ("mv".equals(type) && x < boardWidth && y < boardHeight) {
                        verticalWalls[x][y] = 1;
                    }
                }
            }
        }
        
        for (int i = 0; i < targetTypesCount; i++) {
            int targetType = targetTypeIndices[i];
            
            // For each target type, create exactly one target
            int targetX, targetY;
            Boolean useCornerPlacement = targetMustBeInCorner;
            
            // For ADVANCED difficulty with targetMustBeInCorner=false, we use a 50% probability
            // For INSANE/IMPOSSIBLE difficulty with targetMustBeInCorner=false, always random placement
            if (!targetMustBeInCorner && currentLevel == DIFFICULTY_ADVANCED) {
                // For Advanced difficulty, use 50% probability for corner placement
                int randomChoice = getRandom(0, 1);
                Timber.d("[TARGET PLACEMENT] DECISION at LINE 385: randomChoice=%d for 50%% probability", randomChoice);
                if (randomChoice == 0) {
                    useCornerPlacement = true;
                    Timber.d("[TARGET PLACEMENT] Target %d will use corner placement (50%% probability)", i);
                } else {
                    Timber.d("[TARGET PLACEMENT] Target %d will use random placement (50%% probability)", i);
                }
            } else {
                Timber.d("[TARGET PLACEMENT] TARGET=%d MODE=%s FINAL_CHECK: mustBeInCorner=%b, useCornerPlacement=%b, difficulty=%d", 
                        i, useCornerPlacement ? "corner-only" : "fully-random", 
                        targetMustBeInCorner, useCornerPlacement, currentLevel);
            }
            
            do {
                abandon = false;
                targetX = getRandom(0, boardWidth - 1);
                targetY = getRandom(0, boardHeight - 1);
                
                Timber.d("[TARGET PLACEMENT] Generate position at LINE 384: position=(%d,%d), useCornerPlacement=%b", 
                        targetX, targetY, useCornerPlacement);
                
                // Check corner walls if required
                if (useCornerPlacement) {
                    // For a corner, we need at least one horizontal wall AND at least one vertical wall
                    boolean hasHorizontalWall = (horizontalWalls[targetX][targetY] == 1 || horizontalWalls[targetX][targetY + 1] == 1);
                    boolean hasVerticalWall = (verticalWalls[targetX][targetY] == 1 || verticalWalls[targetX + 1][targetY] == 1);
                    
                    Timber.d("[TARGET PLACEMENT] CORNER CHECK at LINE 422: position=(%d,%d), hasHWall=%b, hasVWall=%b", 
                            targetX, targetY, hasHorizontalWall, hasVerticalWall);
                    
                    // Debug wall values directly
                    Timber.d("[TARGET PLACEMENT] WALL VALUES: h1=%d, h2=%d, v1=%d, v2=%d", 
                            horizontalWalls[targetX][targetY],
                            horizontalWalls[targetX][targetY + 1],
                            verticalWalls[targetX][targetY],
                            verticalWalls[targetX + 1][targetY]);
                    
                    // We need both a horizontal and vertical wall to form a corner
                    if (!hasHorizontalWall || !hasVerticalWall) {
                        abandon = true;
                        Timber.d("[TARGET PLACEMENT] Position (%d,%d) abandoned - not in corner (h=%b, v=%b), LINE 395", 
                                targetX, targetY, hasHorizontalWall, hasVerticalWall);
                    } else {
                        Timber.d("[TARGET PLACEMENT] Position (%d,%d) is a valid corner (h=%b, v=%b)", 
                                targetX, targetY, hasHorizontalWall, hasVerticalWall);
                    }
                } else {
                    // If we're NOT using corner placement, let's verify that corners are actually being allowed
                    Timber.d("[TARGET PLACEMENT] Using random placement at LINE 436 - position=(%d,%d)", targetX, targetY);
                }
                
                // Check if in the center square - always avoid the center square regardless of difficulty
                if ((targetX == carrePosX && targetY == carrePosY)
                        || (targetX == carrePosX && targetY == carrePosY + 1)
                        || (targetX == carrePosX + 1 && targetY == carrePosY)
                        || (targetX == carrePosX + 1 && targetY == carrePosY + 1)) {
                    abandon = true;
                    Timber.d("[TARGET PLACEMENT] Position (%d,%d) abandoned - in center square", targetX, targetY);
                }
                
                // Check if position is already occupied by another element
                for (GridElement element : allElements) {
                    if (element.getX() == targetX && element.getY() == targetY) {
                        abandon = true;
                        Timber.d("[TARGET PLACEMENT] Position (%d,%d) abandoned - already occupied", targetX, targetY);
                        break;
                    }
                }
                
            } while (abandon);
            
            // Create and add the target
            GridElement newTarget = new GridElement(targetX, targetY, typesOfTargets[targetType]);
            data.add(newTarget);
            allElements.add(newTarget);
            
            Timber.d("[TARGET PLACEMENT] PLACEMENT_COMPLETE: target=%d at position=(%d,%d) of type=%s", 
                    i, targetX, targetY, typesOfTargets[targetType]);
        }
        
        // Create robots
        for (String currentRobotType : typesOfRobots) {
            int cX, cY;
            
            do {
                abandon = false;
                cX = getRandom(0, boardWidth - 1);
                cY = getRandom(0, boardHeight - 1);
                
                // Check if position is already occupied
                for (GridElement element : allElements) {
                    if (element.getX() == cX && element.getY() == cY) {
                        abandon = true;
                        break;
                    }
                }
                
                // Check if in the center square - always avoid the center square regardless of difficulty
                if ((cX == carrePosX && cY == carrePosY) 
                        || (cX == carrePosX && cY == carrePosY + 1) 
                        || (cX == carrePosX + 1 && cY == carrePosY) 
                        || (cX == carrePosX + 1 && cY == carrePosY + 1))
                    abandon = true; // robot was inside square
                
            } while (abandon);
            
            // Create and add the robot
            GridElement newRobot = new GridElement(cX, cY, currentRobotType);
            data.add(newRobot);
            allElements.add(newRobot);
            
            Timber.d("Added robot %s at position %d,%d", currentRobotType, cX, cY);
        }
        
        return data;
    }

    private void shuffleIntArray(int[] array) {
        // Use Fisher-Yates algorithm to shuffle the array
        for (int i = array.length - 1; i > 0; i--) {
            int index = rand.nextInt(i + 1);
            // Simple swap
            int temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }


    /**
     * Generate a new map with walls, robots, and targets
     */
    public ArrayList<GridElement> generateGameMap(ArrayList<GridElement> existingMap) {
        Timber.d("[WALLS] Using generateNewMapEachTime: %s", Preferences.generateNewMapEachTime);
        
        // Check if we should preserve walls from the existing map
        WallStorage wallStorage = WallStorage.getInstance();
        boolean preserveWalls = !Preferences.generateNewMapEachTime && wallStorage.hasStoredWalls();
        Timber.d("[WALL STORAGE] GameLogic: generateNewMapEachTime: %s, Preserving walls: %s, hasStoredWalls: %s", Preferences.generateNewMapEachTime, preserveWalls, wallStorage.hasStoredWalls());
        // If this is the first time generating a map or we're not preserving walls, generate everything new
        if(existingMap == null || existingMap.isEmpty() || Preferences.generateNewMapEachTime){
            Timber.d("[WALLS] Generating completely new map");
            
            // Generate a new map based on board size
            if (boardWidth <= 8 || boardHeight <= 8) {
                // For small boards, use the simplified generation algorithm
                existingMap = generateSimpleGameMap3(null);
            } else {
                // For larger boards, use the standard generation algorithm
                existingMap = generateStandardGameMap();
            }
            
            // Store the walls for future use if we're not generating new maps each time
            if (!Preferences.generateNewMapEachTime) {
                wallStorage.storeWalls(existingMap);
                Timber.d("[WALLS][WALL STORAGE] Stored walls for future use");
            }
            
            return existingMap;
        } else {
            // We have an existing map and should preserve walls
            ArrayList<GridElement> data;
            
            if (preserveWalls) {
                Timber.d("[WALLS][WALL STORAGE] Preserving walls from stored configuration");
                // Remove game elements (robots and targets) but keep walls
                data = removeGameElementsFromMap(existingMap);
                
                // Apply stored walls to the map
                data = wallStorage.applyWallsToElements(data);
            } else {
                // Remove all game elements including walls
                data = new ArrayList<>();
                
                // Generate a new map based on board size
                if (boardWidth <= 8 || boardHeight <= 8) {
                    // For small boards, use the simplified generation algorithm
                    data = generateSimpleGameMap3(null);
                } else {
                    // For larger boards, use the standard generation algorithm
                    data = generateStandardGameMap();
                }
                
                // Store the walls for future use
                if (!Preferences.generateNewMapEachTime) {
                    wallStorage.storeWalls(data);
                    Timber.d("[WALLS][WALL STORAGE] Stored new walls for future use");
                }
            }
            
            // Add game elements (robots and targets) to the map
            data = addGameElementsToGameMap(data, null, null);
            
            return data;
        }
    }
    
    /**
     * Generate a standard game map for normal-sized boards
     * This is extracted from the original generateGameMap method
     */
    private ArrayList<GridElement> generateStandardGameMap() {
        int[][] horizontalWalls = new int[boardWidth+1][boardHeight+1];
        int[][] verticalWalls = new int[boardWidth+1][boardHeight+1];

        int temp = 0;
        int countX = 0;
        int countY = 0;

        boolean restart;
        // Add a counter to limit the number of restarts
        int restartCount = 0;
        int maxRestarts = 50; // Maximum number of times we'll restart before relaxing constraints
        int restartRelaxThreshold = 20; // Number of restarts before we start relaxing constraints

        // Original difficulty constraints
        int originalMaxWallsInOneHorizontalRow = maxWallsInOneHorizontalRow;
        int originalMaxWallsInOneVerticalCol = maxWallsInOneVerticalCol;
        int tolerance = 0;
        do {
            restart = false;
            
            // If we've had to restart multiple times, gradually relax the constraints
            if (restartCount > restartRelaxThreshold) {
                // this happens on small maps like 12x12
                // Add 1 to the allowed walls per row/column for each restart beyond 20
                tolerance = (restartCount - restartRelaxThreshold) / 5;
                maxWallsInOneHorizontalRow = originalMaxWallsInOneHorizontalRow + tolerance;
                maxWallsInOneVerticalCol = originalMaxWallsInOneVerticalCol + tolerance;
                Timber.d("Relaxing wall constraints after %d restarts: h=%d, v=%d", 
                       restartCount, maxWallsInOneHorizontalRow, maxWallsInOneVerticalCol);
            }

            //We initialize with no walls
            for (int x = 0; x < boardWidth+1; x++)
                for (int y = 0; y < boardHeight+1; y++)
                    horizontalWalls[x][y] = verticalWalls[x][y] = 0;

            //Creation of the borders
            for (int x = 0; x < boardWidth; x++) {
                horizontalWalls[x][0] = 1;  // Top border
                horizontalWalls[x][boardHeight] = 1;  // Bottom border (important: use boardHeight to place at edge)
            }
            for (int y = 0; y < boardHeight; y++) {
                verticalWalls[0][y] = 1;  // Left border
                verticalWalls[boardWidth][y] = 1;  // Right border (important: use boardWidth to place at edge)
            }

            // right-angled Walls near the left border
            horizontalWalls[0][getRandom(2, 7)] = 1;
            do {
                temp = getRandom(boardHeight/2, boardHeight-2);  // Adjust to ensure walls are visible
            }
            while (horizontalWalls[0][temp - 1] == 1 || horizontalWalls[0][temp] == 1 || horizontalWalls[0][temp + 1] == 1);
            horizontalWalls[0][temp] = 1;

            // right-angled Walls near the right border
            horizontalWalls[boardWidth-1][getRandom(2, 7)] = 1;  // Position one cell to the left of border
            do {
                temp = getRandom(boardHeight/2, boardHeight-2);  // Adjust to ensure walls are visible
            }
            while (horizontalWalls[boardWidth-1][temp - 1] == 1 || horizontalWalls[boardWidth-1][temp] == 1 || horizontalWalls[boardWidth-1][temp + 1] == 1);
            horizontalWalls[boardWidth-1][temp] = 1;

            // right-angled Walls near the top border
            verticalWalls[getRandom(2, boardWidth/2 - 1)][0] = 1;
            do {
                temp = getRandom(boardWidth/2, boardWidth-2);  // Adjust to ensure walls are visible
            }
            while (verticalWalls[temp - 1][0] == 1 || verticalWalls[temp][0] == 1 || verticalWalls[temp + 1][0] == 1);
            verticalWalls[temp][0] = 1;

            // right-angled Walls near the bottom border
            verticalWalls[getRandom(2, boardWidth/2 - 1)][boardHeight-1] = 1;  // Position one cell above the border
            do {
                temp = getRandom(8, boardWidth-2);  // Adjust to ensure walls are visible
            }
            while (verticalWalls[temp - 1][boardHeight-1] == 1 || verticalWalls[temp][boardHeight-1] == 1 || verticalWalls[temp + 1][boardHeight-1] == 1);
            verticalWalls[temp][boardHeight-1] = 1;

            //Drawing the middle square (carré)
            horizontalWalls[carrePosX][carrePosY] = horizontalWalls[carrePosX + 1][carrePosY] = 1;
            horizontalWalls[carrePosX][carrePosY+2] = horizontalWalls[carrePosX + 1][carrePosY+2] = 1;
            verticalWalls[carrePosX][carrePosY] = verticalWalls[carrePosX][carrePosY + 1] = 1;
            verticalWalls[carrePosX+2][carrePosY] = verticalWalls[carrePosX+2][carrePosY + 1] = 1;

            // Loop to place walls in each quadrant of the board
            // The board is divided into 4 quadrants, and we try to place an equal number of walls in each
            // Each wall consists of two parts placed at right angles to form an L-shape
            for (int k = 0; k < wallsPerQuadrant * 4 + boardWidth / 2; k++) {
                boolean abandon = false;
                int tempX;
                int tempY;
                int tempXv = 0;
                int tempYv = 0;

                long compteLoop1 = 0;
                do {
                    compteLoop1++;
                    abandon = false;

                    //Choice of random coordinates in each quadrant of the game board
                    if (k < wallsPerQuadrant) {
                        // top-left quadrant
                        tempX = getRandom(1, boardWidth/2 - 1);
                        tempY = getRandom(1, boardHeight/2 - 1);
                    } else if (k < wallsPerQuadrant * 2) {
                        // top-right quadrant
                        tempX = getRandom(boardWidth/2, boardWidth-2); // Use boardWidth-2 to stay visible
                        tempY = getRandom(1, boardHeight/2 - 1);
                    } else if (k < wallsPerQuadrant * 3) {
                        // bottom-left quadrant
                        tempX = getRandom(1, boardWidth/2 -1);
                        tempY = getRandom(boardHeight/2, boardHeight-2); // Use boardHeight-2 to stay visible
                    } else if (k < wallsPerQuadrant * 4) {
                        // bottom-right quadrant
                        tempX = getRandom(boardWidth/2, boardWidth-2); // Use boardWidth-2 to stay visible
                        tempY = getRandom(boardHeight/2, boardHeight-2); // Use boardHeight-2 to stay visible
                    } else {
                        // bonus walls
                        tempX = getRandom(1, boardWidth-2); // Use boardWidth-2 to stay visible
                        tempY = getRandom(1, boardHeight-2); // Use boardHeight-2 to stay visible
                    }

                    if (horizontalWalls[tempX][tempY] == 1 // already chosen
                        || horizontalWalls[tempX - 1][tempY] == 1 // left
                        || horizontalWalls[tempX + 1][tempY] == 1 // right
                        || horizontalWalls[tempX][tempY - 1] == 1 // directly above
                        || horizontalWalls[tempX][tempY + 1] == 1 // directly below
                        ) abandon = true;

                    if (verticalWalls[tempX][tempY] == 1 // already chosen
                        || verticalWalls[tempX + 1][tempY] == 1 // left
                        || verticalWalls[tempX][tempY - 1] == 1 // above
                        || verticalWalls[tempX + 1][tempY - 1] == 1 // diagonal right-above
                        ) abandon = true;

                    if (!abandon) {
                        //We count the number of walls in the same row/column
                        countX = countY = 0;

                        for (int x = 1; x < boardWidth; x++) {
                            if (horizontalWalls[x][tempY] == 1)
                                countX++;
                        }

                        for (int y = 1; y < boardHeight; y++) {
                            if (horizontalWalls[tempX][y] == 1)
                                countY++;
                        }

                        if (tempY == carrePosY || tempY == carrePosY+2) {
                            countX -= 2;
                        }
                        if (countX >= maxWallsInOneHorizontalRow || countY >= maxWallsInOneVerticalCol){
                            // Timber.d("[GAME LOGIC] There are too many walls in the same row/column, we abandon");
                            abandon = true;
                        }
                    }

                    if (!abandon) {
                        //Choice of the 2nd wall of the corner being drawn
                        tempXv = tempX + getRandom(0, 1);
                        tempYv = tempY - getRandom(0, 1);

                        //We check that it does not fall on or near existing walls
                        if (verticalWalls[tempXv][tempYv] == 1 || verticalWalls[tempXv - 1][tempYv] == 1 || verticalWalls[tempXv + 1][tempYv] == 1)
                            abandon = true;
                        if (verticalWalls[tempXv][tempYv - 1] == 1 || verticalWalls[tempXv][tempYv + 1] == 1)
                            abandon = true;

                        if (horizontalWalls[tempXv][tempYv] == 1 || horizontalWalls[tempXv - 1][tempYv] == 1)
                            abandon = true;

                        if (horizontalWalls[tempXv][tempYv - 1] == 1 || horizontalWalls[tempXv - 1][tempYv - 1] == 1)
                            abandon = true;

                        if (verticalWalls[tempXv - 1][tempYv - 1] == 1 || verticalWalls[tempXv - 1][tempYv + 1] == 1)
                            abandon = true;

                        if (verticalWalls[tempXv + 1][tempYv + 1] == 1 || verticalWalls[tempXv + 1][tempYv - 1] == 1)
                            abandon = true;

                        if (!abandon) {
                            //We count the number of walls in the same row/column
                            countX = countY = 0;

                            for (int x = 1; x < boardWidth; x++) {
                                if (verticalWalls[x][tempYv] == 1)
                                    countX++;
                            }

                            for (int y = 1; y < boardHeight; y++) {
                                if (verticalWalls[tempXv][y] == 1)
                                    countY++;
                            }

                            if (tempXv == carrePosX || tempXv == carrePosX+2) {
                                countY -= 2;
                            }
                            if (countX >= maxWallsInOneHorizontalRow || countY >= maxWallsInOneVerticalCol) //If there are too many walls in the same row/column, we abandon
                                abandon = true;
                        }
                    }

                    if (compteLoop1 > 1000) {
                        Timber.d("Wall creation restarted, too many loops (%d), tolerance: %d", restartCount, tolerance);
                        restart = true;
                    }

                } while (abandon && !restart);
                boolean skiponewall = false;
                if(loneWallsAllowed && getRandom(0, 4) == 1) {
                    skiponewall = true;
                }
                if(skiponewall){
                    if(getRandom(0, 1) == 1){
                        horizontalWalls[tempX][tempY] = 1;
                    }else{
                        verticalWalls[tempXv][tempYv] = 1;
                    }
                }else{
                    horizontalWalls[tempX][tempY] = 1;
                    verticalWalls[tempXv][tempYv] = 1;
                }
            }
        }while(restart && restartCount++ < maxRestarts);

        // Convert wall arrays to grid elements
        ArrayList<GridElement> data = translateArraysToMap(horizontalWalls, verticalWalls);
        
        // Add game elements (robots and targets)
        data = addGameElementsToGameMap(data, horizontalWalls, verticalWalls);

        return data;
    }

    
    
    /**
     * Generate a simplified game map for small boards (8x8) (version 3)
     */
    private ArrayList<GridElement> generateSimpleGameMap3(ArrayList<GridElement> existingMap) {
        // Create a simple wall pattern for small boards
        int[][] horizontalWalls = new int[boardWidth+1][boardHeight+1];
        int[][] verticalWalls = new int[boardWidth+1][boardHeight+1];

        // Clear the existing map data to start fresh
        ArrayList<GridElement> data = new ArrayList<>();
        if (existingMap != null) {
            data.addAll(removeGameElementsFromMap(existingMap));
        }

        // IMPORTANT: Always explicitly set border walls - don't rely on automatic addition
        // This ensures consistent behavior especially with saved walls
        Timber.d("[WALL STORAGE] Explicitly setting ALL outer border walls for board %dx%d", boardWidth, boardHeight);
        
        // Set horizontal border walls (top and bottom)
        for (int x = 0; x < boardWidth; x++) {
            horizontalWalls[x][0] = 1; // Top border
            horizontalWalls[x][boardHeight] = 1; // Bottom border
            Timber.d("[WALL STORAGE] Setting horizontal border walls at (%d,0) and (%d,%d)", x, x, boardHeight);
        }
        
        // Set vertical border walls (left and right)
        for (int y = 0; y < boardHeight; y++) {
            verticalWalls[0][y] = 1; // Left border
            verticalWalls[boardWidth][y] = 1; // Right border
            Timber.d("[WALL STORAGE] Setting vertical border walls at (0,%d) and (%d,%d)", y, boardWidth, y);
        }

        // Create a center square similar to the original game but simpler
        int centerX = boardWidth / 2 - 1;
        int centerY = boardHeight / 2 - 1;
        horizontalWalls[centerX][centerY] = horizontalWalls[centerX + 1][centerY] = 1;
        horizontalWalls[centerX][centerY+2] = horizontalWalls[centerX + 1][centerY+2] = 1;
        verticalWalls[centerX][centerY] = verticalWalls[centerX][centerY + 1] = 1;
        verticalWalls[centerX+2][centerY] = verticalWalls[centerX+2][centerY + 1] = 1;

        // CRITICAL: Add four right-angled walls at the outer borders - one on each side
        // These form the characteristic right angles with the border walls

        // Calculate random positions at least 2 squares from corners
        // Top edge (row 0) - vertical wall
        verticalWalls[getRandom(2, boardWidth-2)][0] = 1;
        // Right edge (col boardWidth-1) - horizontal wall
        horizontalWalls[boardWidth-1][getRandom(2, boardHeight-2)] = 1;
        // Bottom edge (row boardHeight-1) - vertical wall
        verticalWalls[getRandom(2, boardWidth-2)][boardHeight-1] = 1;
        // Left edge (col 0) - horizontal wall
        horizontalWalls[0][getRandom(2, boardHeight-2)] = 1;
        
        // Determine the number of additional walls based on difficulty (10-20 walls total)
        // We already placed 8 walls for the center square, so add between 2-12 more
        int baseWallCount = 4; // Base number of additional walls
        int difficultyBonus = currentLevel * 3; // Difficulty adds 0, 3, 6, or 9 walls
        int randomBonus = getRandom(0, 3); // Add 0-3 random walls regardless of difficulty
        
        int additionalWalls = baseWallCount + difficultyBonus + randomBonus;
        int wallsToPlace = Math.min(additionalWalls, maxTotalWalls - 8);
        wallsToPlace = Math.max(wallsToPlace, minTotalWalls - 8); // Ensure minimum walls
        
        Timber.d("[GAME LOGIC] Adding %d additional walls (total: %d) for difficulty level %d", 
                wallsToPlace, wallsToPlace + 8, currentLevel);

        // 1. First place corner walls (where walls touch at corners)
        int cornerWallsPlaced = 0;
        if (placeWallsInCorners) {
            // Define corner wall pairs (horizontal wall, connecting vertical wall)
            int[][][] cornerPositions = {
                // Top-left corner walls
                {{1, 1}, {1, 1}}, {{2, 1}, {2, 1}},
                // Top-right corner walls
                {{5, 1}, {6, 1}}, {{6, 1}, {6, 1}},
                // Bottom-left corner walls
                {{1, 6}, {1, 5}}, {{2, 6}, {2, 5}},
                // Bottom-right corner walls
                {{5, 6}, {6, 5}}, {{6, 6}, {6, 5}}
            };
            
            // Shuffle corner positions
            shuffle3DArray(cornerPositions);
            
            // Place corner walls until we meet the minimum
            for (int i = 0; i < cornerPositions.length && cornerWallsPlaced < minCornerWalls; i++) {
                int[] hPos = cornerPositions[i][0];
                int[] vPos = cornerPositions[i][1];
                
                // Only place if positions are empty
                if (horizontalWalls[hPos[0]][hPos[1]] == 0 && verticalWalls[vPos[0]][vPos[1]] == 0) {
                    horizontalWalls[hPos[0]][hPos[1]] = 1;
                    verticalWalls[vPos[0]][vPos[1]] = 1;
                    cornerWallsPlaced += 2; // We placed two walls
                    wallsToPlace -= 2;
                    Timber.d("[GAME LOGIC] Placed corner walls at H(%d,%d) and V(%d,%d)", 
                            hPos[0], hPos[1], vPos[0], vPos[1]);
                }
            }
        }
        
        // 2. Next place edge walls
        int edgeWallsPlaced = 0;
        if (placeWallsOnEdges && wallsToPlace > 0) {
            // Define edge wall positions (alternating horizontal and vertical)
            int[][] edgePositions = {
                // Top edge (horizontal walls)
                {1, 0}, {2, 0}, {3, 0}, {4, 0}, {5, 0}, {6, 0},
                // Bottom edge (horizontal walls)
                {1, boardHeight-1}, {2, boardHeight-1}, {3, boardHeight-1}, 
                {4, boardHeight-1}, {5, boardHeight-1}, {6, boardHeight-1},
                // Left edge (vertical walls)
                {0, 1}, {0, 2}, {0, 3}, {0, 4}, {0, 5}, {0, 6},
                // Right edge (vertical walls)
                {boardWidth-1, 1}, {boardWidth-1, 2}, {boardWidth-1, 3}, 
                {boardWidth-1, 4}, {boardWidth-1, 5}, {boardWidth-1, 6}
            };
            
            boolean[] isVertical = {
                false, false, false, false, false, false, 
                false, false, false, false, false, false,
                true, true, true, true, true, true,
                true, true, true, true, true, true
            };
            
            // Shuffle edge positions (keeping track of which are vertical)
            shuffleArrayWithFlags(edgePositions, isVertical);
            
            // Place edge walls until we meet the minimum
            for (int i = 0; i < edgePositions.length && edgeWallsPlaced < minEdgeWalls && wallsToPlace > 0; i++) {
                int[] pos = edgePositions[i];
                boolean vertical = isVertical[i];
                
                // Ensure the positions are within bounds
                if (pos[0] >= 0 && pos[0] < boardWidth+1 && pos[1] >= 0 && pos[1] < boardHeight+1) {
                    // Only place if position is empty
                    if ((vertical && verticalWalls[pos[0]][pos[1]] == 0) || 
                        (!vertical && horizontalWalls[pos[0]][pos[1]] == 0)) {
                            
                        if (vertical) {
                            verticalWalls[pos[0]][pos[1]] = 1;
                        } else {
                            horizontalWalls[pos[0]][pos[1]] = 1;
                        }
                        
                        edgeWallsPlaced++;
                        wallsToPlace--;
                        Timber.d("[GAME LOGIC] Placed %s edge wall at (%d,%d)", 
                                vertical ? "vertical" : "horizontal", pos[0], pos[1]);
                    }
                }
            }
        }
        
        // 3. Finally, place any remaining walls in non-corner, non-edge, non-center positions
        if (wallsToPlace > 0) {
            // Define potential wall positions that won't block the game
            // Avoid the center square (carré) which is at positions (3,3), (3,4), (4,3), (4,4)
            int[][] potentialHorizontalWalls = {
                {1, 2}, {2, 2}, {5, 2}, {6, 2},  // Top area positions
                {1, 5}, {2, 5}, {5, 5}, {6, 5},  // Bottom area positions
                {2, 1}, {2, 6}, {5, 1}, {5, 6},  // Side area positions
                {3, 1}, {4, 1}, {3, 6}, {4, 6},  // Mid-edge positions
                {1, 3}, {1, 4}, {6, 3}, {6, 4},  // More side positions
                {2, 3}, {2, 4}, {5, 3}, {5, 4}   // Near center positions
            };
            
            int[][] potentialVerticalWalls = {
                {1, 2}, {1, 5}, {6, 2}, {6, 5},  // Corner area positions
                {2, 1}, {2, 6}, {5, 1}, {5, 6},  // Edge-adjacent positions
                {1, 3}, {1, 4}, {6, 3}, {6, 4},  // More side positions
                {2, 2}, {2, 5}, {5, 2}, {5, 5},  // Internal corner positions
                {2, 3}, {2, 4}, {5, 3}, {5, 4}   // Near center positions
            };

            // Shuffle both arrays
            shuffleArray(potentialHorizontalWalls);
            shuffleArray(potentialVerticalWalls);
            
            // Place walls alternating between horizontal and vertical
            int additionalWallsPlaced = 0;
            int maxAttempts = potentialHorizontalWalls.length + potentialVerticalWalls.length;
            
            for (int i = 0; i < maxAttempts && additionalWallsPlaced < wallsToPlace; i++) {
                if (i % 2 == 0 && i/2 < potentialHorizontalWalls.length) {
                    // Place horizontal wall
                    int x = potentialHorizontalWalls[i/2][0];
                    int y = potentialHorizontalWalls[i/2][1];
                    
                    // Skip if in center square and we don't want walls there
                    if (!placeWallsInMiddleSquare && isCenterSquare(x, y)) {
                        continue;
                    }
                    
                    // Ensure the indices are valid for our array size
                    if (x >= 0 && x < boardWidth && y >= 0 && y < boardHeight) {
                        // Only place if position is empty
                        if (horizontalWalls[x][y] == 0) {
                            horizontalWalls[x][y] = 1;
                            additionalWallsPlaced++;
                            Timber.d("[GAME LOGIC] Placed horizontal wall at %d,%d", x, y);
                        }
                    }
                } else if (i % 2 == 1 && (i-1)/2 < potentialVerticalWalls.length) {
                    // Place vertical wall
                    int x = potentialVerticalWalls[(i-1)/2][0];
                    int y = potentialVerticalWalls[(i-1)/2][1];
                    
                    // Skip if in center square and we don't want walls there
                    if (!placeWallsInMiddleSquare && isCenterSquare(x, y)) {
                        continue;
                    }
                    
                    // Ensure the indices are valid for our array size
                    if (x >= 0 && x < boardWidth && y >= 0 && y < boardHeight) {
                        // Only place if position is empty
                        if (verticalWalls[x][y] == 0) {
                            verticalWalls[x][y] = 1;
                            additionalWallsPlaced++;
                            Timber.d("[GAME LOGIC] Placed vertical wall at %d,%d", x, y);
                        }
                    }
                }
            }
            
            Timber.d("[GAME LOGIC] Placed %d additional walls beyond corners and edges", additionalWallsPlaced);
        }

        // Add game elements (robots and targets)
        ArrayList<GridElement> result = translateArraysToMap(horizontalWalls, verticalWalls);
        result = addGameElementsToGameMap(result, horizontalWalls, verticalWalls);

        return result;
    }
    
    /**
     * Check if a position is within the center square (carré)
     */
    private boolean isCenterSquare(int x, int y) {
        int centerX = boardWidth / 2 - 1;
        int centerY = boardHeight / 2 - 1;
        return (x >= centerX && x <= centerX + 2 && y >= centerY && y <= centerY + 2);
    }
    
    /**
     * Shuffle an array using Fisher-Yates algorithm
     */
    private void shuffleArray(int[][] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = rand.nextInt(i + 1);
            // Simple swap
            int[] temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }
    
    /**
     * Shuffle an array along with a corresponding array of flags
     * Both arrays must be the same length
     */
    private void shuffleArrayWithFlags(int[][] array, boolean[] flags) {
        if (array.length != flags.length) {
            Timber.e("[GAME LOGIC] Cannot shuffle arrays of different lengths");
            return;
        }
        
        for (int i = array.length - 1; i > 0; i--) {
            int index = rand.nextInt(i + 1);
            // Swap the position arrays
            int[] tempArray = array[index];
            array[index] = array[i];
            array[i] = tempArray;
            
            // Swap the corresponding flags
            boolean tempFlag = flags[index];
            flags[index] = flags[i];
            flags[i] = tempFlag;
        }
    }
    
    /**
     * Shuffle a 3D array using Fisher-Yates algorithm
     */
    private void shuffle3DArray(int[][][] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = rand.nextInt(i + 1);
            // Simple swap
            int[][] temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }
    

    /**
     * Set the number of robots per color to be generated on the map
     * @param count Number of robots (1-4)
     */
    public void setRobotCount(int count) {
        // Ensure count is between 1 and 4
        this.robotCount = Math.max(1, Math.min(Constants.NUM_ROBOTS, count));
        Timber.d("Robot count set to %d", this.robotCount);
    }

    /**
     * Get the current robot count setting
     * @return Number of robots per color (1-4)
     */
    public int getRobotCount() {
        return robotCount;
    }

    /**
     * Set the number of different target colors to be generated on the map
     * @param count Number of different target colors (1-4)
     */
    public void setTargetColors(int count) {
        // Ensure count is between 1 and 4
        this.targetColors = Math.max(1, Math.min(Constants.NUM_ROBOTS, count));
        Timber.d("Target colors set to %d", this.targetColors);
    }

    /**
     * Get the current target colors setting
     * @return Number of different target colors (1-4)
     */
    public int getTargetColors() {
        return targetColors;
    }

    /**
     * Convert a color ID to its corresponding name
     * @param colorId The color ID from Constants
     * @param capitalize Whether to capitalize the first letter of the color name
     * @return The color name as a string
     */
    public static String getColorName(int colorId, boolean capitalize) {
        String name;
        switch (colorId) {
            case Constants.COLOR_PINK: name = "pink"; break;
            case Constants.COLOR_GREEN: name = "green"; break;
            case Constants.COLOR_BLUE: name = "blue"; break;
            case Constants.COLOR_YELLOW: name = "yellow"; break;
            case Constants.COLOR_SILVER: name = "silver"; break;
            case Constants.COLOR_RED: name = "red"; break;
            case Constants.COLOR_BROWN: name = "brown"; break;
            case Constants.COLOR_ORANGE: name = "orange"; break;
            case Constants.COLOR_WHITE: name = "white"; break;
            case Constants.COLOR_MULTI: name = "multi"; break;
            default: 
                Timber.w("[COLOR] Unknown color ID: %d", colorId);
                throw new IllegalArgumentException("Unknown color ID: " + colorId);
        }
        
        return capitalize ? capitalizeFirstLetter(name) : name;
    }
    
    /**
     * Helper method to capitalize the first letter of a string
     */
    private static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
    
    /**
     * Converts a color name to its corresponding ID
     * @param colorName The color name (case insensitive)
     * @return The color ID from Constants
     */
    public static int getColorId(String colorName) {
        if (colorName == null) {
            throw new IllegalArgumentException("Color name cannot be null");
        }
        
        switch (colorName.toLowerCase()) {
            case "pink": return Constants.COLOR_PINK;
            case "green": return Constants.COLOR_GREEN;
            case "blue": return Constants.COLOR_BLUE;
            case "yellow": return Constants.COLOR_YELLOW;
            case "silver": return Constants.COLOR_SILVER;
            case "red": return Constants.COLOR_RED;
            case "brown": return Constants.COLOR_BROWN;
            case "orange": return Constants.COLOR_ORANGE;
            case "white": return Constants.COLOR_WHITE;
            case "multi": return Constants.COLOR_MULTI;
            default:
                Timber.w("[COLOR] Unknown color name: %s", colorName);
                throw new IllegalArgumentException("Unknown color name: " + colorName);
        }
    }
    
    /**
     * Get the object type string ("robot_X" or "target_X") from color ID
     * @param colorId The color ID from Constants
     * @param isRobot Whether this is a robot (true) or target (false)
     * @return The object type string
     */
    public static String getObjectType(int colorId, boolean isRobot) {
        String prefix = isRobot ? "robot_" : "target_";
        return prefix + getColorName(colorId, false);
    }
    
    /**
     * Extract the color ID from an object type string (e.g., "robot_blue" or "target_pink")
     * @param objectType The object type string
     * @return The color ID from Constants
     */
    public static int getColorIdFromObjectType(String objectType) {
        if (objectType == null || objectType.isEmpty() ||
            (!objectType.startsWith("robot_") && !objectType.startsWith("target_"))) {
            throw new IllegalArgumentException("Invalid object type: " + objectType);
        }
        
        String colorName = objectType.substring(objectType.indexOf("_") + 1);
        return getColorId(colorName);
    }

    
    /**
     * Get the RGB color value for an object type
     * @param objectType The object type string like "robot_blue" or "target_pink"
     * @return The RGB color value from Constants.colors_rgb
     */
    public static int getColor(String objectType) {
        int colorId = getColorIdFromObjectType(objectType);
        // Special case for multi-colored targets
        if (colorId == Constants.COLOR_MULTI) {
            return Color.WHITE; // Default color for multi-target
        }
        if (colorId >= 0 && colorId < Constants.colors_rgb.length) {
            return Constants.colors_rgb[colorId];
        }
        Timber.w("[COLOR] Invalid color ID: %d from objectType: %s", colorId, objectType);
        throw new IllegalArgumentException("getColor: Invalid color ID: " + colorId + " from objectType: " + objectType);
    }

    /**
     * Check if any targets exist in the gridElements list
     * @param gridElements The list of grid elements to check
     * @return true if at least one target is found, false otherwise
     */
    public static boolean hasTargets(ArrayList<GridElement> gridElements) {
        if (gridElements == null) {
            Timber.e("[TARGET CHECK] gridElements is null!");
            return false;
        }
        
        Timber.d("[TARGET CHECK] Checking %d grid elements for targets", gridElements.size());
        int targetCount = 0;
        for (GridElement element : gridElements) {
            String type = element.getType();
            if (type != null && type.startsWith("target_")) {
                targetCount++;
                Timber.d("[TARGET CHECK] Found target of type %s at position (%d,%d)", 
                         type, element.getX(), element.getY());
            }
        }
        
        Timber.d("[TARGET CHECK] Found %d targets", targetCount);
        return targetCount > 0;
    }

    /**
     * Check if debug logging is enabled
     * @return true if debug logging is enabled
     */
    public static boolean hasDebugLogging() {
        // For now, always return false to minimize log output
        // Can be changed to a configurable setting later
        return false;
    }

}
