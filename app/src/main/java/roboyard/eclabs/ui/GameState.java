package roboyard.eclabs.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import timber.log.Timber;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import roboyard.eclabs.Constants;
import roboyard.eclabs.GridElement;
import roboyard.eclabs.GridGameScreen;
import roboyard.eclabs.MainActivity;
import roboyard.eclabs.MapGenerator;
import roboyard.eclabs.GameLogic;
import timber.log.Timber;

/**
 * Represents the state of a game, including the board, robots, targets, and game progress.
 * This class handles loading, saving, and manipulating the game state.
 */
public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String TAG = "GameState";
    
    // Element type constants
    public static final int ELEMENT_EMPTY = 0;
    public static final int ELEMENT_WALL = 1;
    public static final int ELEMENT_TARGET = 2;
    public static final int ELEMENT_ROBOT = 3;
    
    // Board properties
    private int width;
    private int height;
    private int[][] board; // 0=empty, 1=wall, 2=target
    private int[][] targetColors; // Color index for targets, -1 if no target
    
    // Game elements (robots and targets)
    private List<GameElement> gameElements;
    
    // Game information
    private int levelId;
    private String levelName;
    private long startTime;
    private int moveCount;
    private boolean completed = false;
    
    // Transient properties (not serialized)
    private transient GameElement selectedRobot;
    private transient int lastSquaresMoved; // Number of squares moved in the last successful robot move
    private transient GameStateManager gameStateManager;
    
    // Store initial robot positions for reset functionality
    private Map<Integer, int[]> initialRobotPositions;

    /**
     * Create a new game state with specified dimensions
     */
    public GameState(int width, int height) {
        this.width = width;
        this.height = height;
        this.board = new int[height][width];
        this.targetColors = new int[height][width];
        this.gameElements = new ArrayList<>();
        this.levelId = -1;
        this.levelName = "Random Game";
        this.startTime = System.currentTimeMillis();
        this.moveCount = 0;
        
        // Initialize target colors to -1 (no target)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                targetColors[y][x] = -1;
            }
        }
    }
    
    /**
     * Get board width
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Get board height
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Get cell type at the specified coordinates
     */
    public int getCellType(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return Constants.CELL_WALL; // Treat out-of-bounds as walls
        }
        return board[y][x];
    }
    
    /**
     * Set cell type at the specified coordinates
     */
    public void setCellType(int x, int y, int type) {
        if (x >= 0 && y >= 0 && x < width && y < height) {
            board[y][x] = type;
        }
    }
    
    /**
     * Get target color at the specified coordinates
     */
    public int getTargetColor(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return -1;
        }
        return targetColors[y][x];
    }
    
    /**
     * Set target color at the specified coordinates
     */
    public void setTargetColor(int x, int y, int color) {
        if (x >= 0 && y >= 0 && x < width && y < height) {
            targetColors[y][x] = color;
        }
    }
    
    /**
     * Add a wall at the specified coordinates
     */
    public void addWall(int x, int y) {
        setCellType(x, y, Constants.CELL_WALL);
    }
    
    /**
     * Add a horizontal wall at the specified coordinates
     */
    public void addHorizontalWall(int x, int y) {
        GameElement wall = new GameElement(GameElement.TYPE_HORIZONTAL_WALL, x, y);
        gameElements.add(wall);
    }
    
    /**
     * Add a vertical wall at the specified coordinates
     */
    public void addVerticalWall(int x, int y) {
        GameElement wall = new GameElement(GameElement.TYPE_VERTICAL_WALL, x, y);
        gameElements.add(wall);
    }
    
    /**
     * Add a target at the specified coordinates
     */
    public void addTarget(int x, int y, int color) {
        setCellType(x, y, Constants.CELL_TARGET);
        setTargetColor(x, y, color);
    }
    
    /**
     * Add a robot at the specified coordinates
     */
    public void addRobot(int x, int y, int color) {
        GameElement robot = new GameElement(GameElement.TYPE_ROBOT, x, y);
        robot.setColor(color);
        gameElements.add(robot);
    }
    
    /**
     * Get all game elements (robots and targets)
     */
    public List<GameElement> getGameElements() {
        return gameElements;
    }
    
    /**
     * Get the robot at the specified coordinates
     */
    public GameElement getRobotAt(int x, int y) {
        for (GameElement element : gameElements) {
            if (element.getType() == GameElement.TYPE_ROBOT && element.getX() == x && element.getY() == y) {
                return element;
            }
        }
        return null;
    }
    
    /**
     * Get the selected robot
     */
    public GameElement getSelectedRobot() {
        return selectedRobot;
    }
    
    /**
     * Set the selected robot
     */
    public void setSelectedRobot(GameElement robot) {
        // Deselect previous robot
        if (selectedRobot != null) {
            selectedRobot.setSelected(false);
        }
        
        // Select new robot
        if (robot != null) {
            robot.setSelected(true);
        }
        
        selectedRobot = robot;
    }
    
    /**
     * Move a robot to the specified coordinates, following game rules
     * Returns true if the move was successful
     */
    public boolean moveRobotTo(GameElement robot, int targetX, int targetY) {
        if (robot == null || robot.getType() != GameElement.TYPE_ROBOT) {
            return false;
        }
        
        int startX = robot.getX();
        int startY = robot.getY();
        
        // Check if the target position is in bounds
        if (targetX < 0 || targetX >= width || targetY < 0 || targetY >= height) {
            return false;
        }
        
        // Can only move in straight lines (horizontally or vertically)
        boolean isHorizontalMove = (startY == targetY);
        boolean isVerticalMove = (startX == targetX);
        if (!isHorizontalMove && !isVerticalMove) {
            return false;
        }
        
        // Calculate move direction
        int dx = 0;
        int dy = 0;
        if (isHorizontalMove) {
            dx = (targetX > startX) ? 1 : -1; // Moving right or left
        } else {
            dy = (targetY > startY) ? 1 : -1; // Moving down or up
        }
        
        // Determine the final position after collision checking
        int finalX = startX;
        int finalY = startY;
        
        // Move until hitting a wall or another robot
        int currentX = startX;
        int currentY = startY;
        boolean hitObstacle = false;
        
        // Track squares moved for this move
        int squaresMoved = 0;
        
        while (!hitObstacle) {
            // CRITICAL: First check for wall collision at current position before moving
            // This is needed to properly handle the wall being between cells
            if (dx > 0) { // Moving right/east - check for vertical wall at current position
                if (hasVerticalWall(currentX +1, currentY)) {
                    // Stop at current position, can't go past the wall
                    hitObstacle = true;
                    break;
                }
            } else if (dx < 0) { // Moving left/west - check for vertical wall at position to the left
                if (hasVerticalWall(currentX, currentY)) {
                    // Stop at current position, can't go past the wall
                    hitObstacle = true;
                    break;
                }
            } else if (dy > 0) { // Moving down/south - check for horizontal wall at current position
                if (hasHorizontalWall(currentX, currentY + 1)) {
                    // Stop at current position, can't go past the wall
                    hitObstacle = true;
                    break;
                }
            } else if (dy < 0) { // Moving up/north - check for horizontal wall at position above
                if (hasHorizontalWall(currentX, currentY)) {
                    // Stop at current position, can't go past the wall
                    hitObstacle = true;
                    break;
                }
            }
            
            // Now calculate next position
            int nextX = currentX + dx;
            int nextY = currentY + dy;
            
            // Check if we'd move out of bounds
            if (nextX < 0 || nextX >= width || nextY < 0 || nextY >= height) {
                hitObstacle = true;
                break;
            }
            
            // Check for robot collision at next position
            GameElement obstacleRobot = getRobotAt(nextX, nextY);
            if (obstacleRobot != null) {
                hitObstacle = true;
                break;
            }
            
            // Move to the next cell if no obstacles
            currentX = nextX;
            currentY = nextY;
            squaresMoved++; // Increment squares moved counter
        }
        
        // Update robot position to the final position
        finalX = currentX;
        finalY = currentY;
        
        // If the robot didn't move, return false
        if (finalX == startX && finalY == startY) {
            return false;
        }
        
        // Move the robot to the final position
        robot.setX(finalX);
        robot.setY(finalY);
        
        // Increment move count
        moveCount++;
        
        // Store the number of squares moved for this move
        this.lastSquaresMoved = squaresMoved;
        
        // Check if the game is completed
        if (checkCompletion()) {
            // Game is completed, let the GameStateManager handle the notification
            // This will be handled in GameStateManager.handleGridTouch method
        }
        
        return true;
    }
    
    /**
     * Get the number of squares moved in the last successful robot move
     * @return Number of squares moved
     */
    public int getLastSquaresMoved() {
        return lastSquaresMoved;
    }
    
    /**
     * Check if there is a vertical wall at the specified position
     * Vertical walls separate columns (they're placed between x and x+1)
     */
    private boolean hasVerticalWall(int x, int y) {
        // Check vertical walls from the game elements
        for (GameElement element : gameElements) {
            if (element.getType() == GameElement.TYPE_VERTICAL_WALL && 
                element.getX() == x && element.getY() == y) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if there is a horizontal wall at the specified position
     * Horizontal walls separate rows (they're placed between y and y+1)
     */
    private boolean hasHorizontalWall(int x, int y) {
        // Check horizontal walls from the game elements
        for (GameElement element : gameElements) {
            if (element.getType() == GameElement.TYPE_HORIZONTAL_WALL && 
                element.getX() == x && element.getY() == y) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if the game is complete (all robots on their target positions).
     * 
     * @return true if all robots are on their correct targets.
     */
    public boolean checkCompletion() {
        // Timber.d("[GOAL DEBUG] Checking game completion...");
        
        // Count how many robots we need to find on targets
        int robotCount = 0;
        int matchedRobots = 0;
        
        // First find all robots and check if they're on a target of matching color
        for (GameElement element : gameElements) {
            if (element.getType() == GameElement.TYPE_ROBOT) {
                robotCount++;
                
                int robotX = element.getX();
                int robotY = element.getY();
                int robotColor = element.getColor();
                
                // Timber.d("[GOAL DEBUG] Robot %d at (%d, %d)", robotColor, robotX, robotY);
                
                // Now check all targets to see if there's a matching target at this position
                for (GameElement targetElement : gameElements) {
                    if (targetElement.getType() == GameElement.TYPE_TARGET) {
                        int targetX = targetElement.getX();
                        int targetY = targetElement.getY();
                        int targetColor = targetElement.getColor();
                        
                        // Timber.d("[GOAL DEBUG] Target %d at (%d, %d)", targetColor, targetX, targetY);
                        
                        // Check if coordinates match and colors match
                        if (robotX == targetX && robotY == targetY && robotColor == targetColor) {
                            // Timber.d("[GOAL DEBUG] Robot matched with target at (%d, %d)!", robotX, robotY);
                            matchedRobots++;
                            break; // Found a match for this robot, stop checking targets
                        }
                    }
                }
            }
        }
        
        // Timber.d("[GOAL DEBUG] Found %d/%d robots on matching targets", matchedRobots, robotCount);
        
        // Game is complete if all robots are matched with targets
        if (matchedRobots > 0) {
            // Timber.d("[GOAL DEBUG] Game complete! All robots are on matching targets.");
            completed = true;
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Get the level ID
     */
    public int getLevelId() {
        return levelId;
    }
    
    /**
     * Set the level ID
     */
    public void setLevelId(int levelId) {
        this.levelId = levelId;
    }
    
    /**
     * Get the level name
     */
    public String getLevelName() {
        return levelName;
    }
    
    /**
     * Set the level name
     */
    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }
    
    /**
     * Get the start time
     */
    public long getStartTime() {
        return startTime;
    }
    
    /**
     * Get the move count
     */
    public int getMoveCount() {
        return moveCount;
    }
    
    /**
     * Set the move count
     */
    public void setMoveCount(int moveCount) {
        this.moveCount = moveCount;
    }
    
    /**
     * Check if the game is completed
     * @return true if all targets have robots of matching colors on them
     */
    public boolean isComplete() {
        return completed;
    }
    
    /**
     * Set the completed status
     * @param completed New completion status
     */
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    /**
     * Generate a minimap of the game state
     * This addresses the minimap issue mentioned in the memory
     */
    public Bitmap getMiniMap(Context context, int width, int height) {
        return MinimapGenerator.getInstance().generateMinimap(context, this, width, height);
    }
    
    /**
     * Get map data for minimap generation (used by GameButtonGotoSavedGame)
     * This specifically addresses the getMapData method mentioned in the memory
     */
    public int[][] getMapData() {
        return board;
    }
    
    /**
     * Get a list of GridElements representing the current board state
     * This is used by the solver to find a solution
     * @return List of GridElements
     */
    public ArrayList<GridElement> getGridElements() {
        ArrayList<GridElement> elements = new ArrayList<>();
        
        // Add wall cells
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Add horizontal walls (mh) - for each cell, check if there's a horizontal wall on its north side
                if (hasHorizontalWall(x, y)) {
                    elements.add(new GridElement(x, y, "mh"));
                }
                
                // Add vertical walls (mv) - for each cell, check if there's a vertical wall on its west side
                if (hasVerticalWall(x, y)) {
                    elements.add(new GridElement(x, y, "mv"));
                }
                
                // Add targets
                if (getCellType(x, y) == Constants.CELL_TARGET) {
                    String gridElementType;
                    int targetColor = getTargetColor(x, y);
                    
                    if (targetColor == 0) {
                        gridElementType = "target_red";
                    } else if (targetColor == 1) {
                        gridElementType = "target_green";
                    } else if (targetColor == 2) {
                        gridElementType = "target_blue";
                    } else if (targetColor == 3) {
                        gridElementType = "target_yellow";
                    } else {
                        // Fallback
                        gridElementType = "target_red";
                    }
                    
                    elements.add(new GridElement(x, y, gridElementType));
                }
            }
        }

        // Add right border walls (vertical walls on the right edge of the grid)
        for (int y = 0; y < height; y++) {
            // TODO: if not already, then add right border wall
            // elements.add(new GridElement(width - 1, y, "mv"));
        }
        
        // Add bottom border walls (horizontal walls on the bottom edge of the grid)
        for (int x = 0; x < width; x++) {
            // TODO: if not already, then add bottom border wall
            // elements.add(new GridElement(x, height - 1, "mh"));
        }

        // TODO: if not already, then add left and top border walls
        
        // Track which robot colors we've already added
        boolean[] robotColorsAdded = new boolean[4]; // For the 4 standard colors
        
        // Add robots from game elements
        for (GameElement element : gameElements) {
            if (element.getType() == GameElement.TYPE_ROBOT) {
                String gridElementType;
                int robotColor = element.getColor();
                
                if (robotColor == 0) {
                    gridElementType = "robot_red";
                } else if (robotColor == 1) {
                    gridElementType = "robot_green";
                } else if (robotColor == 2) {
                    gridElementType = "robot_blue";
                } else if (robotColor == 3) {
                    gridElementType = "robot_yellow";
                } else {
                    // Fallback
                    gridElementType = "robot_red";
                    robotColor = 0;
                }
                
                elements.add(new GridElement(element.getX(), element.getY(), gridElementType));
                robotColorsAdded[robotColor] = true;
            }
        }
        
        // The solver requires exactly 4 robots - add placeholder robots for any missing colors
        // These will be placed in the corners where they won't interfere with gameplay
        int[] cornerX = {1, width-2, 1, width-2};
        int[] cornerY = {1, 1, height-2, height-2};
        int cornerIndex = 0;
        
        for (int color = 0; color < 4; color++) {
            if (!robotColorsAdded[color]) {
                // Add a placeholder robot off-screen (the solver needs exactly 4 robots)
                String gridElementType;
                switch(color) {
                    case 0: gridElementType = "robot_red"; break;
                    case 1: gridElementType = "robot_green"; break;
                    case 2: gridElementType = "robot_blue"; break;
                    case 3: gridElementType = "robot_yellow"; break;
                    default: gridElementType = "robot_red"; break;
                }
                
                // Find an unoccupied corner to place the placeholder robot
                int rx = cornerX[cornerIndex];
                int ry = cornerY[cornerIndex];
                cornerIndex = (cornerIndex + 1) % 4;
                
                // Make sure the spot is empty
                while (getCellType(rx, ry) != Constants.CELL_EMPTY || getRobotAt(rx, ry) != null) {
                    rx = (rx + 1) % (width - 2) + 1; // Keep within bounds, avoiding edges
                    ry = (ry + 1) % (height - 2) + 1;
                }
                
                // Add the placeholder robot to GridElements but not to gameElements
                // This ensures the solver works but doesn't affect gameplay
                elements.add(new GridElement(rx, ry, gridElementType));
            }
        }
        
        return elements;
    }
    
    /**
     * Save the game state to a file
     */
    public boolean saveToFile(Context context, int slotId) {
        try {
            // Create saves directory if it doesn't exist
            File savesDir = new File(context.getFilesDir(), Constants.SAVE_DIRECTORY);
            if (!savesDir.exists()) {
                savesDir.mkdirs();
            }
            
            // Determine filename based on slot ID
            String filename;
            if (slotId == 0) {
                filename = Constants.AUTO_SAVE_FILENAME;
            } else {
                filename = Constants.SAVE_FILENAME_PREFIX + slotId + Constants.SAVE_FILENAME_EXTENSION;
            }
            
            // Create file
            File saveFile = new File(savesDir, filename);
            
            // Write object to file
            try (FileOutputStream fos = new FileOutputStream(saveFile);
                 ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(this);
                return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error saving game to slot " + slotId, e);
            return false;
        }
    }
    
    /**
     * Load a saved game from a file
     */
    public static GameState loadSavedGame(Context context, int slotId) {
        try {
            // Determine filename based on slot ID
            String filename;
            if (slotId == 0) {
                filename = Constants.AUTO_SAVE_FILENAME;
            } else {
                filename = Constants.SAVE_FILENAME_PREFIX + slotId + Constants.SAVE_FILENAME_EXTENSION;
            }
            
            Timber.d("Attempting to load game from slot %d with filename: %s", slotId, filename);
            
            // Get file
            File savesDir = new File(context.getFilesDir(), Constants.SAVE_DIRECTORY);
            File saveFile = new File(savesDir, filename);
            
            Timber.d("Save directory path: %s, exists: %b", savesDir.getAbsolutePath(), savesDir.exists());
            Timber.d("Save file path: %s, exists: %b, size: %d bytes", 
                    saveFile.getAbsolutePath(), 
                    saveFile.exists(), 
                    saveFile.exists() ? saveFile.length() : 0);
            
            // Check if file exists
            if (!saveFile.exists()) {
                Timber.e("Save file does not exist for slot %d", slotId);
                return null;
            }
            
            // Read object from file
            try (FileInputStream fis = new FileInputStream(saveFile);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                GameState state = (GameState) ois.readObject();
                return state;
            }
        } catch (IOException | ClassNotFoundException e) {
            Timber.tag(TAG).e(e, "Error loading game from slot %s", slotId);
            return null;
        }
    }
    
    /**
     * Create a random game state
     */
    public static GameState createRandom(int width, int height, int difficulty) {
        // Set the global difficulty level first so difficulty is consistent
        String difficultyString = difficultyIntToString(difficulty);
        GridGameScreen.setDifficulty(difficultyString);
        
        // Log initial board size and requested size
        Timber.tag(TAG).d("[BOARD_SIZE_DEBUG] createRandom called with size: " + width + "x" + height);
        Timber.tag(TAG).d("[BOARD_SIZE_DEBUG] Current MainActivity.boardSize before setting: " +
                MainActivity.boardSizeX + "x" + MainActivity.boardSizeY);
        
        // Save current board dimensions and set them for game generation
        MainActivity.boardSizeX = width;
        MainActivity.boardSizeY = height;
        
        // Ensure we're not limiting to a minimum of 14
        if (width < 14) {
            Timber.tag(TAG).d("[BOARD_SIZE_DEBUG] Note: Using board width smaller than 14: %s", width);
        }
        if (height < 14) {
            Timber.tag(TAG).d("[BOARD_SIZE_DEBUG] Note: Using board height smaller than 14: %s", height);
        }
        
        // Log the board size being used for map generation
        Timber.tag(TAG).d("[BOARD_SIZE_DEBUG] MainActivity.boardSize after setting: " +
                MainActivity.boardSizeX + "x" + MainActivity.boardSizeY);
        
        // Create new game state with specified dimensions
        GameState state = new GameState(width, height);
        
        try {
            // Use MapGenerator instead of directly using GameLogic to match the old canvas-based game
            Timber.tag(TAG).d("[BOARD_SIZE_DEBUG] Creating MapGenerator with dimensions: " +
                    width + "x" + height);
                  
            // Create MapGenerator instance
            MapGenerator mapGenerator = new MapGenerator();
            
            // Set difficulty in MapGenerator if needed
            // This is handled by the GridGameScreen.setDifficulty call above
            
            // Generate a new game map using the same method as the old canvas-based game
            ArrayList<GridElement> gridElements = mapGenerator.getGeneratedGameMap();

            Timber.tag(TAG).d("[BOARD_SIZE_DEBUG] MapGenerator generated " + gridElements.size() + " grid elements");
            
            // Process grid elements to create game state
            for (GridElement element : gridElements) {
                String type = element.getType();
                int x = element.getX();
                int y = element.getY();
                
                // Handle all wall types
                if (type.equals("mh")) {
                    // Add horizontal wall (between rows)
                    state.addHorizontalWall(x, y);
                } else if (type.equals("mv")) {
                    // Add vertical wall (between columns)
                    state.addVerticalWall(x, y);
                } else if (type.equals("target_red")) {
                    // Add target as a GameElement (TYPE_TARGET) and also mark the cell as a target
                    state.addTarget(x, y, 0);
                    GameElement target = new GameElement(GameElement.TYPE_TARGET, x, y);
                    target.setColor(0); // Red
                    state.getGameElements().add(target);
                } else if (type.equals("target_green")) {
                    state.addTarget(x, y, 1);
                    GameElement target = new GameElement(GameElement.TYPE_TARGET, x, y);
                    target.setColor(1); // Green
                    state.getGameElements().add(target);
                } else if (type.equals("target_blue")) {
                    state.addTarget(x, y, 2);
                    GameElement target = new GameElement(GameElement.TYPE_TARGET, x, y);
                    target.setColor(2); // Blue
                    state.getGameElements().add(target);
                } else if (type.equals("target_yellow")) {
                    state.addTarget(x, y, 3);
                    GameElement target = new GameElement(GameElement.TYPE_TARGET, x, y);
                    target.setColor(3); // Yellow
                    state.getGameElements().add(target);
                } else if (type.equals("target_multi")) {
                    // Multi-color target - we'll use red as default
                    state.addTarget(x, y, 0);
                    GameElement target = new GameElement(GameElement.TYPE_TARGET, x, y);
                    target.setColor(0); // Red (default for multi)
                    state.getGameElements().add(target);
                } else if (type.equals("robot_red")) {
                    state.addRobot(x, y, 0);
                } else if (type.equals("robot_green")) {
                    state.addRobot(x, y, 1);
                } else if (type.equals("robot_blue")) {
                    state.addRobot(x, y, 2);
                } else if (type.equals("robot_yellow")) {
                    state.addRobot(x, y, 3);
                }
            }
        } finally {
            // Keep the board size set to what was requested
            // Do not restore to previous values
        }
        
        state.setLevelName("Random Game " + System.currentTimeMillis() % 1000);
        return state;
    }
    
    /**
     * Load a level from assets
     */
    public static GameState loadLevel(Context context, int levelId) {
        Timber.d("Loading level %d from assets", levelId);
        
        try {
            // Construct the level file path
            String levelFilePath = "Maps/level_" + levelId + ".txt";
            
            // Read the level file content
            String levelContent = "";
            try (InputStream is = context.getAssets().open(levelFilePath);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                levelContent = sb.toString();
                Timber.d("Successfully read level file: %s", levelFilePath);
            }
            
            // Parse the level content
            GameState state = parseLevel(context, levelContent, levelId);
            state.setLevelId(levelId);
            state.setLevelName("Level " + levelId);
            
            // Initialize the solver with the grid elements
            Timber.d("Level %d loaded successfully with %d grid elements", 
                     levelId, state.getGridElements().size());
            
            return state;
            
        } catch (IOException e) {
            Timber.e(e, "Error loading level %d: %s", levelId, e.getMessage());
            // Don't fall back to random level - throw an exception instead
            throw new RuntimeException("Failed to load level " + levelId, e);
        }
    }
    
    /**
     * Parse level content from a level file
     */
    private static GameState parseLevel(Context context, String levelContent, int levelId) {
        // Default board size
        int width = 14;
        int height = 14;
        
        // Create a new game state
        GameState state = new GameState(width, height);
        
        // Track if we have at least one target
        boolean hasTarget = false;
        
        // Parse the level content line by line
        String[] lines = levelContent.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // Parse board dimensions
            if (line.startsWith("board:")) {
                String[] parts = line.substring(6, line.length() - 1).split(",");
                if (parts.length == 2) {
                    width = Integer.parseInt(parts[0]);
                    height = Integer.parseInt(parts[1]);
                    state = new GameState(width, height);
                    Timber.d("[BOARD_SIZE_DEBUG] Level %d has board size: %dx%d", levelId, width, height);
                }
                continue;
            }
            
            // Parse horizontal walls (mh)
            if (line.startsWith("mh")) {
                String[] parts = line.substring(2, line.length() - 1).split(",");
                if (parts.length == 2) {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    state.addHorizontalWall(x, y);
                }
                continue;
            }
            
            // Parse vertical walls (mv)
            if (line.startsWith("mv")) {
                String[] parts = line.substring(2, line.length() - 1).split(",");
                if (parts.length == 2) {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    state.addVerticalWall(x, y);
                }
                continue;
            }
            
            // Parse targets
            if (line.startsWith("target_")) {
                // Format: target_color8,11;
                // Extract color name (e.g., "yellow" from "target_yellow8,11;")
                String colorPart = line.substring(7, line.indexOf(";"));
                
                // Find the first digit to separate color from coordinates
                int digitPos = -1;
                for (int i = 0; i < colorPart.length(); i++) {
                    if (Character.isDigit(colorPart.charAt(i))) {
                        digitPos = i;
                        break;
                    }
                }
                
                if (digitPos != -1) {
                    String color = colorPart.substring(0, digitPos);
                    String coordsStr = colorPart.substring(digitPos);
                    String[] coords = coordsStr.split(",");
                    
                    if (coords.length == 2) {
                        try {
                            int x = Integer.parseInt(coords[0]);
                            int y = Integer.parseInt(coords[1]);
                            int colorId = -1;
                            
                            if (color.equals("red")) colorId = 0;
                            else if (color.equals("green")) colorId = 1;
                            else if (color.equals("blue")) colorId = 2;
                            else if (color.equals("yellow")) colorId = 3;
                            
                            if (colorId >= 0) {
                                state.addTarget(x, y, colorId);
                                GameElement target = new GameElement(GameElement.TYPE_TARGET, x, y);
                                target.setColor(colorId);
                                state.getGameElements().add(target);
                                hasTarget = true;
                                Timber.d("[LEVEL LOADING] Adding %s target at (%d,%d)", color, x, y);
                            }
                        } catch (NumberFormatException e) {
                            Timber.e(e, "[LEVEL LOADING] Error parsing target coordinates: %s", line);
                        }
                    }
                } else {
                    Timber.e("[LEVEL LOADING] Error parsing target line, no digits found: %s", line);
                }
                continue;
            }
            
            // Parse robots
            if (line.startsWith("robot_")) {
                // Format: robot_color6,1;
                // Extract color name (e.g., "red" from "robot_red6,1;")
                String colorPart = line.substring(6, line.indexOf(";"));
                
                // Find the first digit to separate color from coordinates
                int digitPos = -1;
                for (int i = 0; i < colorPart.length(); i++) {
                    if (Character.isDigit(colorPart.charAt(i))) {
                        digitPos = i;
                        break;
                    }
                }
                
                if (digitPos != -1) {
                    String color = colorPart.substring(0, digitPos);
                    String coordsStr = colorPart.substring(digitPos);
                    String[] coords = coordsStr.split(",");
                    
                    if (coords.length == 2) {
                        try {
                            int x = Integer.parseInt(coords[0]);
                            int y = Integer.parseInt(coords[1]);
                            int colorId = -1;
                            
                            if (color.equals("red")) colorId = 0;
                            else if (color.equals("green")) colorId = 1;
                            else if (color.equals("blue")) colorId = 2;
                            else if (color.equals("yellow")) colorId = 3;
                            
                            if (colorId >= 0) {
                                state.addRobot(x, y, colorId);
                                Timber.d("[LEVEL LOADING] Adding %s robot at (%d,%d)", color, x, y);
                            }
                        } catch (NumberFormatException e) {
                            Timber.e(e, "[LEVEL LOADING] Error parsing robot coordinates: %s", line);
                        }
                    }
                } else {
                    Timber.e("[LEVEL LOADING] Error parsing robot line, no digits found: %s", line);
                }
                continue;
            }
        }
        
        // If no target was found, throw an exception
        // This prevents the NullPointerException in the solver
        if (!hasTarget && !state.getGameElements().isEmpty()) {
            Timber.e("[LEVEL LOADING] No target found in level");
            throw new IllegalStateException("Level has no target, cannot create a valid game state");
        }
        
        // Store initial robot positions for reset functionality
        state.storeInitialRobotPositions();
        
        return state;
    }
    
    /**
     * Serialize the game state to a string representation for saving to a file
     * @return String representation of the game state
     */
    public String serialize() {
        StringBuilder saveData = new StringBuilder();
        
        // Add metadata as a comment line
        // Format: #MAPNAME:name;TIME:seconds;MOVES:count;
        saveData.append("#MAPNAME:").append(levelName).append(";");
        saveData.append("TIME:").append(System.currentTimeMillis() - startTime).append(";");
        saveData.append("MOVES:").append(moveCount).append(";\n");
        
        // Add board dimensions
        saveData.append("WIDTH:").append(width).append(";\n");
        saveData.append("HEIGHT:").append(height).append(";\n");
        
        // Add board data
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int cellType = board[y][x];
                saveData.append(cellType);
                
                // If it's a target, add the color
                if (cellType == Constants.CELL_TARGET) {
                    saveData.append(":").append(targetColors[y][x]);
                }
                
                saveData.append(",");
            }
            saveData.append("\n");
        }
        
        // Add robots
        saveData.append("ROBOTS:\n");
        for (GameElement element : gameElements) {
            if (element.getType() == GameElement.TYPE_ROBOT) {
                saveData.append(element.getX()).append(",")
                       .append(element.getY()).append(",")
                       .append(element.getColor()).append("\n");
            }
        }
        
        return saveData.toString();
    }
    
    /**
     * Reset robot positions to their starting positions
     * This keeps the same map but moves robots back to where they started
     */
    public void resetRobotPositions() {
        // Store initial robot positions if not already stored
        if (initialRobotPositions == null || initialRobotPositions.isEmpty()) {
            // Can't reset if we don't have initial positions
            return;
        }
        
        // Get current robot elements
        List<GameElement> currentRobots = new ArrayList<>();
        for (GameElement element : gameElements) {
            if (element.getType() == GameElement.TYPE_ROBOT) {
                currentRobots.add(element);
            }
        }
        
        // Skip if no robots found
        if (currentRobots.isEmpty()) {
            return;
        }
        
        // Reset each robot to its initial position
        for (GameElement robot : currentRobots) {
            int robotColor = robot.getColor();
            if (initialRobotPositions.containsKey(robotColor)) {
                int[] position = initialRobotPositions.get(robotColor);
                robot.setX(position[0]);
                robot.setY(position[1]);
            }
        }
        
        // Reset selected robot
        selectedRobot = null;
        
        // Reset move count
        moveCount = 0;
        
        // Reset completion flag
        completed = false;
    }

    /**
     * Store initial robot positions for reset functionality
     */
    public void storeInitialRobotPositions() {
        // Initialize the map if it doesn't exist
        if (initialRobotPositions == null) {
            initialRobotPositions = new HashMap<>();
        }
        
        // Loop through all game elements to find robots
        for (GameElement element : gameElements) {
            if (element.getType() == GameElement.TYPE_ROBOT) {
                // Store the robot's position by its color
                int[] position = new int[] { element.getX(), element.getY() };
                initialRobotPositions.put(element.getColor(), position);
            }
        }
    }
    
    /**
     * Set the GameStateManager reference
     * @param manager The GameStateManager to use
     */
    public void setGameStateManager(GameStateManager manager) {
        this.gameStateManager = manager;
    }

    public boolean canRobotMoveTo(GameElement robot, int nextX, int nextY) {
        // Check if the target position is within the board boundaries
        if (nextX < 0 || nextX >= width || nextY < 0 || nextY >= height) {
            return false;
        }
        
        // Check if there's already another robot at the target position
        GameElement otherRobot = getRobotAt(nextX, nextY);
        if (otherRobot != null && otherRobot != robot) {
            return false;
        }
        
        // Check current robot position
        int currentX = robot.getX();
        int currentY = robot.getY();
        
        // Check for walls in the movement path
        // Moving horizontally
        if (currentY == nextY) {
            // Moving east
            if (currentX < nextX) {
                // Check for vertical walls between current position and target
                for (int x = currentX; x < nextX; x++) {
                    if (hasVerticalWall(x, currentY)) {
                        return false;
                    }
                }
            }
            // Moving west
            else if (currentX > nextX) {
                // Check for vertical walls between current position and target
                for (int x = nextX; x < currentX; x++) {
                    if (hasVerticalWall(x, currentY)) {
                        return false;
                    }
                }
            }
        }
        // Moving vertically
        else if (currentX == nextX) {
            // Moving south
            if (currentY < nextY) {
                // Check for horizontal walls between current position and target
                for (int y = currentY; y < nextY; y++) {
                    if (hasHorizontalWall(currentX, y)) {
                        return false;
                    }
                }
            }
            // Moving north
            else if (currentY > nextY) {
                // Check for horizontal walls between current position and target
                for (int y = nextY; y < currentY; y++) {
                    if (hasHorizontalWall(currentX, y)) {
                        return false;
                    }
                }
            }
        }
        
        // If we've made it this far, the move is valid
        return true;
    }

    /**
     * Calculate how far a robot can move in each direction
     * @param robotId Robot ID to check movement for
     * @return Map with directions ("north", "south", "east", "west") as keys and distance as values
     */
    public Map<String, Integer> calculatePossibleMoves(int robotId) {
        Map<String, Integer> moves = new HashMap<>();
        GameElement robot = findRobotById(robotId);
        if (robot == null) {
            return moves;
        }
        
        int x = robot.getX();
        int y = robot.getY();
        
        // Calculate distance in each direction
        // North (up)
        int northDist = 0;
        for (int i = y - 1; i >= 0; i--) {
            if (canRobotMoveTo(robot, x, i)) {
                northDist++;
            } else {
                break;
            }
        }
        moves.put("north", northDist);
        
        // South (down)
        int southDist = 0;
        for (int i = y + 1; i < height; i++) {
            if (canRobotMoveTo(robot, x, i)) {
                southDist++;
            } else {
                break;
            }
        }
        moves.put("south", southDist);
        
        // East (right)
        int eastDist = 0;
        for (int i = x + 1; i < width; i++) {
            if (canRobotMoveTo(robot, i, y)) {
                eastDist++;
            } else {
                break;
            }
        }
        moves.put("east", eastDist);
        
        // West (left)
        int westDist = 0;
        for (int i = x - 1; i >= 0; i--) {
            if (canRobotMoveTo(robot, i, y)) {
                westDist++;
            } else {
                break;
            }
        }
        moves.put("west", westDist);
        
        return moves;
    }

    /**
     * Find a robot by its ID
     * @param robotId The robot ID to find
     * @return The robot game element or null if not found
     */
    public GameElement findRobotById(int robotId) {
        // First try to find by exact color match
        for (GameElement element : gameElements) {
            if (element.getType() == GameElement.TYPE_ROBOT && element.getColor() == robotId) {
                return element;
            }
        }
        
        // If no exact match, try finding robots by RGB color constants
        // Common Android color constants
        if (robotId == -16777216) { // Color.BLACK
            return findRobotByColor(0); // Assuming BLACK is represented as 0 in our system
        } else if (robotId == -16711936) { // Color.GREEN
            return findRobotByColor(2); // Assuming GREEN is represented as 2 in our system
        } else if (robotId == -256) { // Color.BLUE
            return findRobotByColor(1); // Assuming BLUE is represented as 1 in our system
        } else if (robotId == -65536) { // Color.RED
            return findRobotByColor(3); // Assuming RED is represented as 3 in our system
        } else if (robotId == -16711681) { // Color.CYAN
            return findRobotByColor(4); // Assuming CYAN is represented as 4 in our system
        } else if (robotId == -16776961) { // Color.YELLOW
            return findRobotByColor(5); // Assuming YELLOW is represented as 5 in our system
        }
        
        // If still not found, log detailed information
        boolean hasRobots = false;
        StringBuilder robotInfo = new StringBuilder("Available robots: ");
        for (GameElement element : gameElements) {
            if (element.getType() == GameElement.TYPE_ROBOT) {
                hasRobots = true;
                robotInfo.append("[ID: ").append(element.getColor()).append(" at ").append(element.getX()).append(",").append(element.getY()).append("] ");
            }
        }
        
        if (hasRobots) {
            Timber.d("findRobotById: Could not find robot with ID %d. %s", robotId, robotInfo.toString());
        } else {
            Timber.d("findRobotById: No robots found in the game state!");
        }
        
        return null;
    }
    
    /**
     * Helper method to find a robot by color index
     * @param colorIndex The color index to search for
     * @return The robot game element or null if not found
     */
    private GameElement findRobotByColor(int colorIndex) {
        for (GameElement element : gameElements) {
            if (element.getType() == GameElement.TYPE_ROBOT && element.getColor() == colorIndex) {
                return element;
            }
        }
        return null;
    }

    /**
     * Check if a robot has reached its target
     * @param robot The robot to check
     * @return True if the robot is at its matching target, false otherwise
     */
    public boolean isRobotAtTarget(GameElement robot) {
        if (robot == null || robot.getType() != GameElement.TYPE_ROBOT) {
            return false;
        }
        
        int robotX = robot.getX();
        int robotY = robot.getY();
        int robotColor = robot.getColor();
        
        // Find matching target
        for (GameElement element : gameElements) {
            if (element.getType() == GameElement.TYPE_TARGET && 
                element.getColor() == robotColor &&
                element.getX() == robotX &&
                element.getY() == robotY) {
                return true;
            }
        }
        
        return false;
    }

    public List<GameElement> getRobots() {
        List<GameElement> robots = new ArrayList<>();
        for (GameElement element : gameElements) {
            if (element.getType() == GameElement.TYPE_ROBOT) {
                robots.add(element);
            }
        }
        return robots;
    }

    public GameElement getTarget() {
        for (GameElement element : gameElements) {
            if (element.getType() == GameElement.TYPE_TARGET) {
                return element;
            }
        }
        return null;
    }
    
    /**
     * Converts difficulty integer to string for the original GridGameScreen class
     */
    private static String difficultyIntToString(int difficulty) {
        switch (difficulty) {
            case 0: return "Beginner";
            case 1: return "Advanced";
            case 2: return "Insane";
            case 3: return "Impossible";
            default: return "Beginner";
        }
    }
}
