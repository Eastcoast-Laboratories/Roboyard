package roboyard.eclabs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import driftingdroids.model.Solution;
import roboyard.eclabs.solver.ISolver;
import roboyard.eclabs.solver.SolverDD;
import roboyard.eclabs.solver.SolverStatus;
import roboyard.eclabs.util.SolutionMetrics;
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
        wallsPerQuadrant = boardWidth/4;  // Default: quarter of board width

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
        
        Timber.d("wallsPerQuadrant: " + wallsPerQuadrant + " Board size: " + boardWidth + "x" + boardHeight);
    }

    /**
     * Get a random number between min and max (inclusive)
     */
    public int getRandom(int min, int max) {
        return rand.nextInt((max - min) + 1) + min;
    }
    
    /**
     * Set whether to generate a new map each time
     */
    public static void setGenerateNewMapEachTime(boolean value) {
        generateNewMapEachTime = value;
    }
    
    /**
     * Get whether to generate a new map each time
     */
    public static boolean getGenerateNewMapEachTime() {
        return generateNewMapEachTime;
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
        int targetX;
        int targetY;
        Boolean tempTargetMustBeInCorner;

        tempTargetMustBeInCorner = targetMustBeInCorner;
        if(!targetMustBeInCorner && getRandom(0,1) != 1){
            // 50% probability that the target is in a corner
            tempTargetMustBeInCorner=true;
        }
        do{
            abandon = false;
            targetX = getRandom(0, boardWidth-1);
            targetY = getRandom(0, boardHeight-1);

            if(tempTargetMustBeInCorner && horizontalWalls[targetX][targetY] == 0 && horizontalWalls[targetX][targetY+1] == 0)
                abandon = true;
            if(tempTargetMustBeInCorner && verticalWalls[targetX][targetY] == 0 && verticalWalls[targetX+1][targetY] == 0)
                abandon = true;

            if((targetX == carrePosX && targetY == carrePosY)
                    || (targetX == carrePosX && targetY == carrePosY+1)
                    || (targetX == carrePosX+1 && targetY == carrePosY)
                    || (targetX == carrePosX+1 && targetY == carrePosY+1))
                abandon = true; // target was in square

        }while(abandon);

        String[] typesOfTargets = {"target_red", "target_blue", "target_yellow", "target_green", "target_multi"};

        if(allowMulticolorTarget) {
            data.add(new GridElement(targetX, targetY, typesOfTargets[getRandom(0,4)]));
        } else {
            data.add(new GridElement(targetX, targetY, typesOfTargets[getRandom(0,3)]));
        }

        String[] typesOfRobots = {"robot_red", "robot_blue", "robot_yellow", "robot_green"};

        ArrayList<GridElement> robotsTemp = new ArrayList<>();

        int cX;
        int cY;

        for(String currentRobotType : typesOfRobots)
        {
            do {
                abandon = false;
                cX = getRandom(0, boardWidth-1);
                cY = getRandom(0, boardHeight-1);

                for(GridElement robot:robotsTemp) {
                    if (robot.getX() == cX && robot.getY() == cY) {
                        abandon = true;
                        break;
                    }
                }

                if((cX == carrePosX && cY == carrePosY) || (cX == carrePosX && cY == carrePosY+1) || (cX == carrePosX+1 && cY == carrePosY) || (cX == carrePosX+1 && cY == carrePosY+1))
                    abandon = true; // robot was inside square
                if(cX == targetX && cY == targetY)
                    abandon = true; // robot was target

            }while(abandon);
            robotsTemp.add(new GridElement(cX, cY, currentRobotType));
        }
        data.addAll(robotsTemp);

        return data;
    }

    /**
     * Generate a new map with walls, robots, and targets
     */
    public ArrayList<GridElement> generateGameMap(ArrayList<GridElement> existingMap) {
        Timber.d("Using generateNewMapEachTime: %s", generateNewMapEachTime);
        
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
            for (int x = 0; x < boardWidth; x++)
                for (int y = 0; y < boardHeight; y++)
                    horizontalWalls[x][y] = verticalWalls[x][y] = 0;

            //Creation of the borders
            for (int x = 0; x < boardWidth; x++) {
                horizontalWalls[x][0] = 1;
                horizontalWalls[x][boardHeight] = 1;
            }
            for (int y = 0; y < boardHeight; y++) {
                verticalWalls[0][y] = 1;
                verticalWalls[boardWidth][y] = 1;
            }

            // right-angled Walls near the left border
            horizontalWalls[0][getRandom(2, 7)] = 1;
            do {
                temp = getRandom(boardHeight/2, boardHeight-1);
            }
            while (horizontalWalls[0][temp - 1] == 1 || horizontalWalls[0][temp] == 1 || horizontalWalls[0][temp + 1] == 1);
            horizontalWalls[0][temp] = 1;

            // right-angled Walls near the right border
            horizontalWalls[boardWidth-1][getRandom(2, 7)] = 1;
            do {
                temp = getRandom(boardHeight/2, boardHeight-1);
            }
            while (horizontalWalls[boardWidth-1][temp - 1] == 1 || horizontalWalls[boardWidth-1][temp] == 1 || horizontalWalls[boardWidth-1][temp + 1] == 1);
            horizontalWalls[boardWidth-1][temp] = 1;

            // right-angled Walls near the top border
            verticalWalls[getRandom(2, boardWidth/2 - 1)][0] = 1;
            do {
                temp = getRandom(boardWidth/2, boardWidth-1);
            }
            while (verticalWalls[temp - 1][0] == 1 || verticalWalls[temp][0] == 1 || verticalWalls[temp + 1][0] == 1);
            verticalWalls[temp][0] = 1;

            // right-angled Walls near the bottom border
            verticalWalls[getRandom(2, boardWidth/2 - 1)][boardHeight-1] = 1;
            do {
                temp = getRandom(8, boardWidth-1);
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
            for (int k = 0; k < wallsPerQuadrant * 4 + 1; k++) {
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
                    if (k < boardWidth/4) {
                        // top-left quadrant
                        tempX = getRandom(1, boardWidth/2 -1);
                        tempY = getRandom(1, boardHeight/2 -1);
                    } else if (k < 2*boardWidth/4) {
                        // top-right quadrant
                        tempX = getRandom(boardWidth/2, boardWidth-1);
                        tempY = getRandom(1, boardHeight/2 -1);
                    } else if (k < 3*boardWidth/4) {
                        // bottom-left quadrant
                        tempX = getRandom(1, boardWidth/2 -1);
                        tempY = getRandom(boardHeight/2, boardHeight-1);
                    } else if (k < boardWidth) {
                        // bottom-right quadrant
                        tempX = getRandom(boardWidth/2, boardWidth-1);
                        tempY = getRandom(boardHeight/2, boardHeight-1);
                    } else {
                        // bonus walls
                        tempX = getRandom(1, boardWidth-1);
                        tempY = getRandom(1, boardHeight-1);
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

                        for (int x = 1; x < boardWidth-1; x++) {
                            if (horizontalWalls[x][tempY] == 1)
                                countX++;
                        }

                        for (int y = 1; y < boardHeight-1; y++) {
                            if (horizontalWalls[tempX][y] == 1)
                                countY++;
                        }

                        if (tempY == carrePosY || tempY == carrePosY+2) {
                            countX -= 2;
                        }
                        if (countX >= maxWallsInOneHorizontalRow || countY >= maxWallsInOneVerticalCol) //If there are too many walls in the same row/column, we abandon
                            abandon = true;
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

                            for (int x = 1; x < boardWidth-1; x++) {
                                if (verticalWalls[x][tempYv] == 1)
                                    countX++;
                            }

                            for (int y = 1; y < boardHeight-1; y++) {
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
                Boolean skiponewall = false;
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

}
