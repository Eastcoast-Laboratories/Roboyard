package roboyard.eclabs.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import roboyard.eclabs.Constants;
import roboyard.eclabs.GridElement;
import roboyard.eclabs.GridGameScreen;
import roboyard.eclabs.MainActivity;
import roboyard.eclabs.MapGenerator;

/**
 * Represents the state of a game, including the board, robots, targets, and game progress.
 * This class handles loading, saving, and manipulating the game state.
 */
public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String TAG = "GameState";
    
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
        
        // Determine movement direction
        int dirX = 0;
        int dirY = 0;
        
        if (targetX > startX) dirX = 1;
        else if (targetX < startX) dirX = -1;
        
        if (targetY > startY) dirY = 1;
        else if (targetY < startY) dirY = -1;
        
        // Can only move in one direction at a time
        if (dirX != 0 && dirY != 0) {
            return false;
        }
        
        // Can't stay in place
        if (dirX == 0 && dirY == 0) {
            return false;
        }
        
        // Move in the direction until hitting an obstacle
        int newX = startX;
        int newY = startY;
        
        while (true) {
            int nextX = newX + dirX;
            int nextY = newY + dirY;
            
            // Check if out of bounds or hit a wall
            if (getCellType(nextX, nextY) == Constants.CELL_WALL) {
                break;
            }
            
            // Check if another robot is in the way
            if (getRobotAt(nextX, nextY) != null) {
                break;
            }
            
            // Move to the next position
            newX = nextX;
            newY = nextY;
        }
        
        // If we didn't move, return false
        if (newX == startX && newY == startY) {
            return false;
        }
        
        // Move the robot
        robot.setX(newX);
        robot.setY(newY);
        
        return true;
    }
    
    /**
     * Check if the game is complete (all robots on matching targets)
     */
    public boolean checkCompletion() {
        for (GameElement element : gameElements) {
            if (element.getType() == GameElement.TYPE_ROBOT) {
                int x = element.getX();
                int y = element.getY();
                int color = element.getColor();
                
                // Check if robot is on a matching target
                if (getCellType(x, y) != Constants.CELL_TARGET || getTargetColor(x, y) != color) {
                    return false;
                }
            }
        }
        
        // Mark the game as completed
        completed = true;
        return true;
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
        
        // Add walls
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (getCellType(x, y) == Constants.CELL_WALL) {
                    elements.add(new GridElement(x, y, "wall"));
                } else if (getCellType(x, y) == Constants.CELL_TARGET) {
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
            
            // Get file
            File savesDir = new File(context.getFilesDir(), Constants.SAVE_DIRECTORY);
            File saveFile = new File(savesDir, filename);
            
            // Check if file exists
            if (!saveFile.exists()) {
                return null;
            }
            
            // Read object from file
            try (FileInputStream fis = new FileInputStream(saveFile);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                GameState state = (GameState) ois.readObject();
                return state;
            }
        } catch (IOException | ClassNotFoundException e) {
            Log.e(TAG, "Error loading game from slot " + slotId, e);
            return null;
        }
    }
    
    /**
     * Create a random game state
     */
    public static GameState createRandom(int width, int height, int difficulty) {
        // Set the global difficulty level first so MapGenerator knows which difficulty to use
        GridGameScreen.setDifficulty(difficultyIntToString(difficulty));
        
        // Temporarily save the current board size
        int oldWidth = MainActivity.boardSizeX;
        int oldHeight = MainActivity.boardSizeY;
        
        // Set board dimensions for MapGenerator
        MainActivity.boardSizeX = width;
        MainActivity.boardSizeY = height;
        
        // Create new game state
        GameState state = new GameState(width, height);
        
        try {
            // Use the original MapGenerator to generate map elements
            MapGenerator generator = new MapGenerator();
            ArrayList<GridElement> gridElements = generator.getGeneratedGameMap();
            
            // Process grid elements to create game state
            for (GridElement element : gridElements) {
                String type = element.getType();
                int x = element.getX();
                int y = element.getY();
                
                // Handle all wall types
                if (type.equals("mh") || type.equals("mv")) {
                    // Add horizontal and vertical walls from the MapGenerator
                    state.addWall(x, y);
                } else if (type.equals("target_red")) {
                    state.addTarget(x, y, 0);
                } else if (type.equals("target_green")) {
                    state.addTarget(x, y, 1);
                } else if (type.equals("target_blue")) {
                    state.addTarget(x, y, 2);
                } else if (type.equals("target_yellow")) {
                    state.addTarget(x, y, 3);
                } else if (type.equals("target_multi")) {
                    // Multi-color target - we'll use red as default
                    state.addTarget(x, y, 0);
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
            // Restore original board size
            MainActivity.boardSizeX = oldWidth;
            MainActivity.boardSizeY = oldHeight;
        }
        
        state.setLevelName("Random Game " + System.currentTimeMillis() % 1000);
        return state;
    }
    
    /**
     * Load a level from assets
     */
    public static GameState loadLevel(Context context, int levelId) {
        // This would load level data from assets or a database
        // For now, just create a random level with fixed seed
        // TODO: Implement proper level loading from assets
        
        GameState state = createRandom(14, 14, 1); // Medium board, medium difficulty
        state.setLevelId(levelId);
        state.setLevelName("Level " + levelId);
        return state;
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
}
