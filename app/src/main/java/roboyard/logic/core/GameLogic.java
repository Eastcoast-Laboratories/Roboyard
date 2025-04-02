package roboyard.logic.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import driftingdroids.model.Solution;
import roboyard.pm.ia.GameSolution;
import timber.log.Timber;

/**
 * A UI-agnostic class that contains the core game logic for map generation.
 * This class is designed to be used by both the classic UI and the modern UI.
 */
public class GameLogic {

    private final Random rand;

    // Difficulty level constants
    public static final int DIFFICULTY_BEGINNER = 0;
    public static final int DIFFICULTY_ADVANCED = 1;
    public static final int DIFFICULTY_INSANE = 2;
    public static final int DIFFICULTY_IMPOSSIBLE = 3;

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
    private final int currentLevel;
    
    // Configuration for multiple targets
    private int robotCount = 1; // Default to 1 robot per color
    private int targetColors = 1; // Anzahl der verschiedenen Zielfarben (1-4) (overridden by Preferences )
    
    // Configuration for the simplified board generation
    private boolean placeWallsInCorners = true;
    private boolean placeWallsOnEdges = true;
    private boolean placeWallsInMiddleSquare = false;
    private boolean placeBorderPerpendicularWalls = true;
    private boolean placeWallsOnOuterBorders = true;
    private int minCornerWalls = 4;
    private int minEdgeWalls = 4;
    private int minBorderPerpendicularWalls = 4;
    private int minOuterBorderWalls = 4;
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
        if(level == DIFFICULTY_BEGINNER) { 
            // For beginner level - default settings
        } else {
            if(level == DIFFICULTY_ADVANCED) { 
                // nothing to do
            }
            
            allowMulticolorTarget = false;

            maxWallsInOneVerticalCol = 3;
            maxWallsInOneHorizontalRow = 3;
            wallsPerQuadrant = (int) (boardWidth/3.3);

            loneWallsAllowed = true;
        }

        if(level == DIFFICULTY_INSANE || level == DIFFICULTY_IMPOSSIBLE) {
            targetMustBeInCorner = false;

            maxWallsInOneVerticalCol = 5;
            maxWallsInOneHorizontalRow = 5;
            wallsPerQuadrant = boardWidth/3;
        }
        
        if(level == DIFFICULTY_IMPOSSIBLE) {
            wallsPerQuadrant = (int) (boardWidth/2.3);
        }
        
        if (boardWidth * boardHeight > 64) {
            // calculate maxWallsInOneVerticalCol and maxWallsInOneHorizontalRow based on board size
        }
        
        Timber.d("maxWallsInOneVerticalCol: " + maxWallsInOneVerticalCol + " maxWallsInOneHorizontalRow: " + maxWallsInOneHorizontalRow + " wallsPerQuadrant: " + wallsPerQuadrant + " Board size: " + boardWidth + "x" + boardHeight);
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
    public static void setGenerateNewMapEachTime(boolean value) {
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

        for(int x=0; x<=boardWidth; x++)
            for(int y=0; y <= boardHeight; y++)
            {
                if(horizontalWalls[x][y]== 1) {
                    // add all horizontal walls
                    data.add(new GridElement(x,y,"mh"));
                }
                if(verticalWalls[x][y]== 1) {
                    // add all vertical walls
                    data.add(new GridElement(x,y,"mv"));
                }
            }
        return data;
    }

    /**
     * Add game elements (robots and targets) to a map
     */
    public ArrayList<GridElement> addGameElementsToGameMap(ArrayList<GridElement> data, int[][] horizontalWalls, int[][] verticalWalls) {
        boolean abandon;
        String[] typesOfTargets = {"target_red", "target_blue", "target_yellow", "target_green", "target_multi"};
        String[] typesOfRobots = {"robot_red", "robot_blue", "robot_yellow", "robot_green"};

        // Store all positions of game elements to avoid overlapping
        ArrayList<GridElement> allElements = new ArrayList<>();
        
        // Create targets based on targetCount and targetColors settings
        // We'll create targets for each color (or multi-color) up to the targetColors limit
        int maxTargetTypes = allowMulticolorTarget ? 5 : 4;
        int targetTypesCount = Math.min(targetColors, maxTargetTypes); // Limit to targetColors
        
        Timber.d("Creating targets with targetCount=%d and targetColors=%d", robotCount, targetTypesCount);
        
        // Create an array of indices to use for target types, and shuffle it to randomize which colors are used
        int[] targetTypeIndices = new int[maxTargetTypes];
        for (int i = 0; i < maxTargetTypes; i++) {
            targetTypeIndices[i] = i;
        }
        
        // Shuffle the array to randomize which colors are used when targetColors < 4
        shuffleIntArray(targetTypeIndices);
        
        // Only use the first targetTypesCount elements from the shuffled array
        Timber.d("[TARGET] Will use %d different target types out of %d possible types", targetTypesCount, maxTargetTypes);
        for (int i = 0; i < targetTypesCount; i++) {
            int targetType = targetTypeIndices[i];
            
            // For each target type, create targetCount targets
            for (int count = 0; count < robotCount; count++) {
                int targetX, targetY;
                Boolean tempTargetMustBeInCorner = targetMustBeInCorner;
                
                // 50% probability that the target is in a corner if targetMustBeInCorner is false
                if (!targetMustBeInCorner && getRandom(0, 1) != 1) {
                    tempTargetMustBeInCorner = true;
                }
                
                do {
                    abandon = false;
                    targetX = getRandom(0, boardWidth - 1);
                    targetY = getRandom(0, boardHeight - 1);
                    
                    // Check corner walls if required
                    if (tempTargetMustBeInCorner && horizontalWalls[targetX][targetY] == 0 && horizontalWalls[targetX][targetY + 1] == 0)
                        abandon = true;
                    if (tempTargetMustBeInCorner && verticalWalls[targetX][targetY] == 0 && verticalWalls[targetX + 1][targetY] == 0)
                        abandon = true;
                    
                    // Check if in the center square
                    if ((targetX == carrePosX && targetY == carrePosY)
                            || (targetX == carrePosX && targetY == carrePosY + 1)
                            || (targetX == carrePosX + 1 && targetY == carrePosY)
                            || (targetX == carrePosX + 1 && targetY == carrePosY + 1))
                        abandon = true; // target was in square
                    
                    // Check if position is already occupied by another element
                    for (GridElement element : allElements) {
                        if (element.getX() == targetX && element.getY() == targetY) {
                            abandon = true;
                            break;
                        }
                    }
                    
                } while (abandon);
                
                // Create and add the target
                GridElement newTarget = new GridElement(targetX, targetY, typesOfTargets[targetType]);
                data.add(newTarget);
                allElements.add(newTarget);
                
                Timber.d("Added target %s at position %d,%d", typesOfTargets[targetType], targetX, targetY);
            }
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
                
                // Check if in the center square
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
        Timber.d("Using generateNewMapEachTime: %s", generateNewMapEachTime);
        
        // For small boards (8x8), use a simplified approach with predefined wall patterns
        if (boardWidth <= 8 && boardHeight <= 8) {
            Timber.d("[GAME LOGIC] Using simplified wall generation for small board (%dx%d)", boardWidth, boardHeight);
            return generateSimpleGameMap(existingMap);
        }
        
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

        ArrayList<GridElement> data;
        
        if(existingMap == null || existingMap.isEmpty() || generateNewMapEachTime){
            data = translateArraysToMap(horizontalWalls, verticalWalls);
        } else {
            data = removeGameElementsFromMap(existingMap);
        }

        data = addGameElementsToGameMap(data, horizontalWalls, verticalWalls);

        return data;
    }

    /**
     * Generate a simplified game map for small boards (8x8)
     */
    private ArrayList<GridElement> generateSimpleGameMap(ArrayList<GridElement> existingMap) {
        // Create a simple wall pattern for small boards
        int[][] horizontalWalls = new int[boardWidth+1][boardHeight+1];
        int[][] verticalWalls = new int[boardWidth+1][boardHeight+1];

        // Clear the existing map data to start fresh
        ArrayList<GridElement> data = new ArrayList<>();
        if (existingMap != null) {
            data.addAll(removeGameElementsFromMap(existingMap));
        }

        // Create borders (these are automatically added by the game but we'll define them anyway)
        // Important: In an 8x8 board, valid indices are 0-7, with the borders at -1, 0, boardWidth, and boardHeight
        for (int x = 0; x < boardWidth+1; x++) {
            horizontalWalls[x][0] = 1;
            horizontalWalls[x][boardHeight] = 1;
        }
        for (int y = 0; y < boardHeight+1; y++) {
            verticalWalls[0][y] = 1;
            verticalWalls[boardWidth][y] = 1;
        }
        
        // Create a center square similar to the original game but simpler
        int centerX = boardWidth / 2 - 1;
        int centerY = boardHeight / 2 - 1;
        horizontalWalls[centerX][centerY] = horizontalWalls[centerX + 1][centerY] = 1;
        horizontalWalls[centerX][centerY+2] = horizontalWalls[centerX + 1][centerY+2] = 1;
        verticalWalls[centerX][centerY] = verticalWalls[centerX][centerY + 1] = 1;
        verticalWalls[centerX+2][centerY] = verticalWalls[centerX+2][centerY + 1] = 1;

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
        
        // Add walls directly on the outer borders (top and left)
        int outerBorderWallsPlaced = 0;
        if (placeWallsOnOuterBorders) {
            // Create arrays to hold wall positions for outer borders
            int[][] outerWallPositions = new int[16][2];
            boolean[] isVerticalWall = new boolean[16];
            
            // Top border positions (horizontal walls at y=0)
            // For horizontal walls at the top border: indices are x=0-6, y=0
            outerWallPositions[0] = new int[]{0, 0}; isVerticalWall[0] = false;
            outerWallPositions[1] = new int[]{1, 0}; isVerticalWall[1] = false;
            outerWallPositions[2] = new int[]{2, 0}; isVerticalWall[2] = false;
            outerWallPositions[3] = new int[]{3, 0}; isVerticalWall[3] = false;
            outerWallPositions[4] = new int[]{4, 0}; isVerticalWall[4] = false;
            outerWallPositions[5] = new int[]{5, 0}; isVerticalWall[5] = false;
            
            // Left border positions (vertical walls at x=0)
            // For vertical walls at the left border: indices are x=0, y=0-6
            outerWallPositions[6] = new int[]{0, 0}; isVerticalWall[6] = true;
            outerWallPositions[7] = new int[]{0, 1}; isVerticalWall[7] = true;
            outerWallPositions[8] = new int[]{0, 2}; isVerticalWall[8] = true;
            outerWallPositions[9] = new int[]{0, 3}; isVerticalWall[9] = true;
            outerWallPositions[10] = new int[]{0, 4}; isVerticalWall[10] = true;
            outerWallPositions[11] = new int[]{0, 5}; isVerticalWall[11] = true;
            
            // Some additional bottom and right outer walls for variety
            outerWallPositions[12] = new int[]{0, boardHeight}; isVerticalWall[12] = false;
            outerWallPositions[13] = new int[]{4, boardHeight}; isVerticalWall[13] = false;
            outerWallPositions[14] = new int[]{boardWidth, 0}; isVerticalWall[14] = true;
            outerWallPositions[15] = new int[]{boardWidth, 4}; isVerticalWall[15] = true;
            
            // Shuffle the wall positions
            shuffleArrayWithFlags(outerWallPositions, isVerticalWall);
            
            // Place outer border walls
            for (int i = 0; i < outerWallPositions.length && outerBorderWallsPlaced < minOuterBorderWalls && wallsToPlace > 0; i++) {
                int[] pos = outerWallPositions[i];
                boolean isVertical = isVerticalWall[i];
                
                // Determine the actual position to place the wall
                int x = pos[0];
                int y = pos[1];
                
                // Directly place walls - no need for extra boundary checks as these are the outer walls
                if (!isVertical) { // Horizontal wall
                    horizontalWalls[x][y] = 1;
                    Timber.d("[GAME LOGIC] Placed horizontal wall on outer border at (%d,%d)", x, y);
                } else { // Vertical wall
                    verticalWalls[x][y] = 1;
                    Timber.d("[GAME LOGIC] Placed vertical wall on outer border at (%d,%d)", x, y);
                }
                
                outerBorderWallsPlaced++;
                wallsToPlace--;
            }
            
            Timber.d("[GAME LOGIC] Placed %d walls directly on outer borders", outerBorderWallsPlaced);
        }
        
        // Add perpendicular walls extending inward from the border walls
        int borderPerpendicularWallsPlaced = 0;
        if (placeBorderPerpendicularWalls) {
            // Create arrays to hold wall positions
            int[][] wallPositions = new int[16][2];
            boolean[] isVerticalWall = new boolean[16];
            
            // Top border with vertical walls extending down
            wallPositions[0] = new int[]{1, 0}; isVerticalWall[0] = true;
            wallPositions[1] = new int[]{2, 0}; isVerticalWall[1] = true;
            wallPositions[2] = new int[]{5, 0}; isVerticalWall[2] = true;
            wallPositions[3] = new int[]{6, 0}; isVerticalWall[3] = true;
            
            // Bottom border with vertical walls extending up
            wallPositions[4] = new int[]{1, boardHeight}; isVerticalWall[4] = true;
            wallPositions[5] = new int[]{2, boardHeight}; isVerticalWall[5] = true;
            wallPositions[6] = new int[]{5, boardHeight}; isVerticalWall[6] = true;
            wallPositions[7] = new int[]{6, boardHeight}; isVerticalWall[7] = true;
            
            // Left border with horizontal walls extending right
            wallPositions[8] = new int[]{0, 1}; isVerticalWall[8] = false;
            wallPositions[9] = new int[]{0, 2}; isVerticalWall[9] = false;
            wallPositions[10] = new int[]{0, 5}; isVerticalWall[10] = false;
            wallPositions[11] = new int[]{0, 6}; isVerticalWall[11] = false;
            
            // Right border with horizontal walls extending left
            wallPositions[12] = new int[]{boardWidth, 1}; isVerticalWall[12] = false;
            wallPositions[13] = new int[]{boardWidth, 2}; isVerticalWall[13] = false;
            wallPositions[14] = new int[]{boardWidth, 5}; isVerticalWall[14] = false;
            wallPositions[15] = new int[]{boardWidth, 6}; isVerticalWall[15] = false;
            
            // Shuffle the wall positions (keeping track of wall type)
            shuffleArrayWithFlags(wallPositions, isVerticalWall);
            
            // Place perpendicular walls at the borders until we meet the minimum
            for (int i = 0; i < wallPositions.length && borderPerpendicularWallsPlaced < minBorderPerpendicularWalls && wallsToPlace > 0; i++) {
                int[] pos = wallPositions[i];
                boolean vertical = isVerticalWall[i];
                
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
                        
                        borderPerpendicularWallsPlaced++;
                        wallsToPlace--;
                        Timber.d("[GAME LOGIC] Placed %s wall perpendicular to border at (%d,%d)", 
                                vertical ? "vertical" : "horizontal", pos[0], pos[1]);
                    }
                }
            }
            
            Timber.d("[GAME LOGIC] Placed %d perpendicular walls at borders", borderPerpendicularWallsPlaced);
        }
        
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
                {1, boardHeight}, {2, boardHeight}, {3, boardHeight}, 
                {4, boardHeight}, {5, boardHeight}, {6, boardHeight},
                // Left edge (vertical walls)
                {0, 1}, {0, 2}, {0, 3}, {0, 4}, {0, 5}, {0, 6},
                // Right edge (vertical walls)
                {boardWidth, 1}, {boardWidth, 2}, {boardWidth, 3}, 
                {boardWidth, 4}, {boardWidth, 5}, {boardWidth, 6}
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
     * Generate a simplified game map for small boards (8x8) (version 2)
     */
    private ArrayList<GridElement> generateSimpleGameMap2(ArrayList<GridElement> existingMap) {
        // Create a simple wall pattern for small boards
        int[][] horizontalWalls = new int[boardWidth+1][boardHeight+1];
        int[][] verticalWalls = new int[boardWidth+1][boardHeight+1];

        // Clear the existing map data to start fresh
        ArrayList<GridElement> data = new ArrayList<>();
        if (existingMap != null) {
            data.addAll(removeGameElementsFromMap(existingMap));
        }

        // Create borders (these are automatically added by the game but we'll define them anyway)
        // Important: In an 8x8 board, valid indices are 0-7, with the borders at -1, 0, boardWidth, and boardHeight
        for (int x = 0; x < boardWidth; x++) {
            horizontalWalls[x][0] = 1;
            horizontalWalls[x][boardHeight] = 1;
        }
        for (int y = 0; y < boardHeight; y++) {
            verticalWalls[0][y] = 1;
            verticalWalls[boardWidth][y] = 1;
        }

        // Create a center square similar to the original game but simpler
        int centerX = boardWidth / 2 - 1;
        int centerY = boardHeight / 2 - 1;
        horizontalWalls[centerX][centerY] = horizontalWalls[centerX + 1][centerY] = 1;
        horizontalWalls[centerX][centerY+2] = horizontalWalls[centerX + 1][centerY+2] = 1;
        verticalWalls[centerX][centerY] = verticalWalls[centerX][centerY + 1] = 1;
        verticalWalls[centerX+2][centerY] = verticalWalls[centerX+2][centerY + 1] = 1;

        // Determine the number of additional walls based on difficulty (10-20 walls total)
        // We already placed 8 walls for the center square, so add between 2-12 more
        int minAdditionalWalls = 2; // Minimum additional walls
        int baseWallCount = 4; // Base number of additional walls
        int difficultyBonus = currentLevel * 2; // Difficulty adds 0, 2, 4, or 6 walls
        int randomBonus = getRandom(0, 4); // Add 0-4 random walls regardless of difficulty
        
        int additionalWalls = baseWallCount + difficultyBonus + randomBonus;
        int minWalls = 10; // Minimum total walls
        int maxWalls = 20; // Maximum total walls
        
        // Ensure we have at least minAdditionalWalls beyond the center square
        additionalWalls = Math.max(minAdditionalWalls, additionalWalls);
        
        // Ensure we meet the minimum total wall count
        if (8 + additionalWalls < minWalls) {
            additionalWalls = minWalls - 8;
        }
        
        // Cap at maximum total walls
        int wallsToPlace = Math.min(additionalWalls, maxWalls - 8);
        
        Timber.d("[GAME LOGIC] Adding %d additional walls (total: %d) for difficulty level %d", 
                wallsToPlace, wallsToPlace + 8, currentLevel);

        // Place walls in semi-random positions
        // Define potential wall positions that won't block the game
        // For an 8x8 board, valid indices are 0-7, with the borders at -1, 0, boardWidth, and boardHeight
        int[][] potentialHorizontalWalls = {
            {1, 2}, {2, 2}, {5, 2}, {6, 2},  // Top row positions
            {1, 5}, {2, 5}, {5, 5}, {6, 5},  // Bottom row positions
            {2, 1}, {2, 6}, {5, 1}, {5, 6},  // Side positions
            {3, 1}, {4, 1}, {3, 6}, {4, 6},  // More side positions
            {1, 3}, {1, 4}, {6, 3}, {6, 4},  // More vertical sides
            {0, 2}, {0, 5}, {7, 2}, {7, 5},  // Edge positions
            {2, 3}, {2, 4}, {5, 3}, {5, 4}   // Interior positions
        };
        
        int[][] potentialVerticalWalls = {
            {1, 2}, {1, 5}, {6, 2}, {6, 5},  // Corner approaches
            {2, 1}, {2, 6}, {5, 1}, {5, 6},  // Edge positions
            {3, 3}, {4, 4}, {3, 4}, {4, 3},  // Scattered positions (not blocking center)
            {1, 3}, {1, 4}, {6, 3}, {6, 4},  // More side positions
            {3, 0}, {4, 0}, {3, 7}, {4, 7},  // More edge positions
            {2, 3}, {2, 4}, {5, 3}, {5, 4},  // Interior positions
            {0, 3}, {0, 4}, {7, 3}, {7, 4}   // Far edge positions
        };

        // Shuffle the arrays to randomize which walls get placed first
        shuffleArray(potentialHorizontalWalls);
        shuffleArray(potentialVerticalWalls);
        
        // Place walls alternating between horizontal and vertical
        int wallsPlaced = 0;
        int maxAttempts = potentialHorizontalWalls.length + potentialVerticalWalls.length;
        
        for (int i = 0; i < maxAttempts && wallsPlaced < wallsToPlace; i++) {
            if (i % 2 == 0 && i/2 < potentialHorizontalWalls.length) {
                // Place horizontal wall
                int x = potentialHorizontalWalls[i/2][0];
                int y = potentialHorizontalWalls[i/2][1];
                
                // Ensure the indices are valid for our array size
                if (x >= 0 && x < boardWidth && y >= 0 && y < boardHeight) {
                    // Only place if position is empty
                    if (horizontalWalls[x][y] == 0) {
                        horizontalWalls[x][y] = 1;
                        wallsPlaced++;
                        Timber.d("[GAME LOGIC] Placed horizontal wall at %d,%d", x, y);
                    }
                }
            } else if (i % 2 == 1 && (i-1)/2 < potentialVerticalWalls.length) {
                // Place vertical wall
                int x = potentialVerticalWalls[(i-1)/2][0];
                int y = potentialVerticalWalls[(i-1)/2][1];
                
                // Ensure the indices are valid for our array size
                if (x >= 0 && x < boardWidth && y >= 0 && y < boardHeight) {
                    // Only place if position is empty
                    if (verticalWalls[x][y] == 0) {
                        verticalWalls[x][y] = 1;
                        wallsPlaced++;
                        Timber.d("[GAME LOGIC] Placed vertical wall at %d,%d", x, y);
                    }
                }
            }
        }

        // Add game elements (robots and targets)
        ArrayList<GridElement> result = translateArraysToMap(horizontalWalls, verticalWalls);
        result = addGameElementsToGameMap(result, horizontalWalls, verticalWalls);

        return result;
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

        // Create borders (these are automatically added by the game but we'll define them anyway)
        // Important: In an 8x8 board, valid indices are 0-7, with the borders at -1, 0, boardWidth, and boardHeight
        for (int x = 0; x < boardWidth; x++) {
            horizontalWalls[x][0] = 1;
            horizontalWalls[x][boardHeight] = 1;
        }
        for (int y = 0; y < boardHeight; y++) {
            verticalWalls[0][y] = 1;
            verticalWalls[boardWidth][y] = 1;
        }

        // Create a center square similar to the original game but simpler
        int centerX = boardWidth / 2 - 1;
        int centerY = boardHeight / 2 - 1;
        horizontalWalls[centerX][centerY] = horizontalWalls[centerX + 1][centerY] = 1;
        horizontalWalls[centerX][centerY+2] = horizontalWalls[centerX + 1][centerY+2] = 1;
        verticalWalls[centerX][centerY] = verticalWalls[centerX][centerY + 1] = 1;
        verticalWalls[centerX+2][centerY] = verticalWalls[centerX+2][centerY + 1] = 1;

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
     * Shuffle an array of objects with their associated data
     */
    private void shuffleArrayWithObject(Object[][] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = rand.nextInt(i + 1);
            // Simple swap
            Object[] temp = array[index];
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
        this.robotCount = Math.max(1, Math.min(4, count));
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
        this.targetColors = Math.max(1, Math.min(4, count));
        Timber.d("Target colors set to %d", this.targetColors);
    }

    /**
     * Get the current target colors setting
     * @return Number of different target colors (1-4)
     */
    public int getTargetColors() {
        return targetColors;
    }
}
