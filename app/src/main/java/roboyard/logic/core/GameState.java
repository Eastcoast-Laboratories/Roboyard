package roboyard.logic.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import roboyard.eclabs.ui.GameElement;
import roboyard.eclabs.ui.MinimapGenerator;
import roboyard.ui.components.GameStateManager;
import timber.log.Timber;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import roboyard.eclabs.util.MapIdGenerator;
import roboyard.ui.activities.MainActivity;

/**
 * Represents the state of a game, including the board, robots, targets, and game progress.
 * This class handles loading, saving, and manipulating the game state.
 */
public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String TAG = "GameState";
    
    // Board properties
    private final int width;
    private final int height;
    private final int[][] board; // 0=empty, 1=wall, 2=target
    private final int[][] targetColors; // Color index for targets, -1 if no target
    
    // Game elements (robots and targets)
    private final List<GameElement> gameElements;
    
    // Game information
    private int levelId;
    private String levelName;
    private long startTime;
    private int moveCount;
    private int squaresMoved;
    private int robotCount = 1; // Default to 1 robot per color
    private int targetColorsCount = 4; // Default to 4 different target colors
    private boolean completed = false;
    private int hintCount = 0; // Track the number of hints used in this game
    private String uniqueMapId = ""; // 5-letter unique ID for map identification
    
    // Tracking the last move for hint verification
    private GameElement lastMovedRobot = null;
    private Integer lastMoveDirection = null;
    
    // Transient properties (not serialized)
    private transient GameElement selectedRobot;
    private transient GameStateManager gameStateManager;
    
    // Store initial robot positions for reset functionality
    public Map<Integer, int[]> initialRobotPositions;

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
        this.levelName = "XXXXX";
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
            return Constants.TYPE_VERTICAL_WALL; // Treat out-of-bounds as vertical walls
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
     * Add a horizontal wall at the specified coordinates
     */
    public void addHorizontalWall(int x, int y) {
        GameElement wall = new GameElement(GameElement.TYPE_HORIZONTAL_WALL, x, y);
        gameElements.add(wall);
        setCellType(x, y, Constants.TYPE_HORIZONTAL_WALL);
    }
    
    /**
     * Add a vertical wall at the specified coordinates
     */
    public void addVerticalWall(int x, int y) {
        GameElement wall = new GameElement(GameElement.TYPE_VERTICAL_WALL, x, y);
        gameElements.add(wall);
        setCellType(x, y, Constants.TYPE_VERTICAL_WALL);
    }
    
    /**
     * Add a target at the specified coordinates
     */
    public void addTarget(int x, int y, int color) {
        GameElement target = new GameElement(GameElement.TYPE_TARGET, x, y);
        target.setColor(color); // Set the color on the target element
        gameElements.add(target);
        setCellType(x, y, Constants.TYPE_TARGET);
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
        // Use the areAllRobotsAtTargets method to check if all robots are on their targets
        if (areAllRobotsAtTargets()) {
            // Timber.d("[GOAL DEBUG] Game complete! All robots are on matching targets.");
            completed = true;
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Check if all robots are on targets of their matching color
     * @return True if all robots are on matching targets, false otherwise
     */
    public boolean areAllRobotsAtTargets() {
        // Get all robots in the game
        List<GameElement> robots = getRobots();
        int robotsAtTarget = 0;
        
        // For each robot, check if it's on a target of its matching color
        for (GameElement robot : robots) {
            if (isRobotAtTarget(robot)) {
                // Count robots that are on their targets
                robotsAtTarget++;
                Timber.d("[GOAL DEBUG] Robot %d is at target (%d,%d)", robot.getColor(), robot.getX(), robot.getY());
            } else {
                Timber.d("[GOAL DEBUG] Robot %d is NOT at target (%d,%d)", robot.getColor(), robot.getX(), robot.getY());
            }
        }
        
        // Game is complete when the number of robots at targets matches the robot count setting
        boolean allRobotsAtTargets = (robotsAtTarget >= robotCount);
        Timber.d("[GOAL DEBUG] %d/%d robots at targets (robot count: %d) -> Game complete: %b", 
                robotsAtTarget, robots.size(), robotCount, allRobotsAtTargets);
        
        return allRobotsAtTargets;
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
                if (getCellType(x, y) == Constants.TYPE_TARGET) {
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
                while (getCellType(rx, ry) != Constants.TYPE_EMPTY || getRobotAt(rx, ry) != null) {
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
        Timber.d("Attempting to load game from slot %d with filename: %s", slotId, Constants.SAVE_FILENAME_PREFIX + slotId + Constants.SAVE_FILENAME_EXTENSION);
        
        try {
            // Get save directory
            File savesDir = new File(context.getFilesDir(), Constants.SAVE_DIRECTORY);
            Timber.d("Save directory path: %s, exists: %b", savesDir.getAbsolutePath(), savesDir.exists());
            
            // Get save file
            String filename = Constants.SAVE_FILENAME_PREFIX + slotId + Constants.SAVE_FILENAME_EXTENSION;
            File saveFile = new File(savesDir, filename);
            Timber.d("Save file path: %s, exists: %b, size: %d bytes", saveFile.getAbsolutePath(), saveFile.exists(), saveFile.length());
            
            if (!saveFile.exists()) {
                Timber.e("Save file does not exist");
                return null;
            }
            
            // Read the file as text
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(saveFile)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            
            String saveData = content.toString();
            Timber.d("Read %d characters from save file", saveData.length());
            
            // Parse the save data
            return parseFromSaveData(saveData, context);
            
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "IOException while loading game from slot %d: %s", slotId, e.getMessage());
            return null;
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Unexpected error loading game from slot %d: %s", slotId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Parse a game state from save data
     * @param saveData The save data string
     * @param context The context
     * @return The parsed game state or null if parsing failed
     */
    public static GameState parseFromSaveData(String saveData, Context context) {
        try {
            String[] lines = saveData.split("\n");
            Timber.d("Parsing save data with %d lines", lines.length);
            
            // Default values
            int width = 8;
            int height = 8;
            String mapName = "Loaded Game";
            long timePlayed = 0;
            int moveCount = 0;
            
            boolean inRobotsSection = false;
            boolean inInitialPositionsSection = false;
            boolean boardDataStarted = false;
            
            // Parse metadata from the first line if it starts with #
            if (lines.length > 0 && lines[0].startsWith("#")) {
                String metadataLine = lines[0];
                String[] metadata = metadataLine.substring(1).split(";");
                
                for (String item : metadata) {
                    if (item.startsWith("MAPNAME:")) {
                        mapName = item.substring("MAPNAME:".length());
                    } else if (item.startsWith("TIME:")) {
                        timePlayed = Long.parseLong(item.substring("TIME:".length()));
                    } else if (item.startsWith("MOVES:")) {
                        moveCount = Integer.parseInt(item.substring("MOVES:".length()));
                    }
                }
            }
            
            // Look for WIDTH and HEIGHT in the file
            for (String line : lines) {
                if (line.startsWith("WIDTH:")) {
                    width = Integer.parseInt(line.substring("WIDTH:".length()).trim().replace(";", ""));
                    Timber.d("Found WIDTH: %d", width);
                } else if (line.startsWith("HEIGHT:")) {
                    height = Integer.parseInt(line.substring("HEIGHT:".length()).trim().replace(";", ""));
                    Timber.d("Found HEIGHT: %d", height);
                }
            }
            
            // Create the game state with the parsed dimensions
            GameState state = new GameState(width, height);
            state.setLevelName(mapName);
            state.setMoveCount(moveCount);
            state.startTime = System.currentTimeMillis() - timePlayed;
            
            // Parse board data
            int boardLine = 0;
            int wallsAdded = 0;
            int targetsAdded = 0;
            
            // Skip metadata and dimension lines
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                
                if (line.startsWith("WIDTH:") || line.startsWith("HEIGHT:")) {
                    continue;
                }
                
                // Check for section markers
                if (line.equals("ROBOTS:")) {
                    inRobotsSection = true;
                    inInitialPositionsSection = false;
                    boardDataStarted = false; // Exit board data mode
                    continue;
                } else if (line.equals("INITIAL_POSITIONS:")) {
                    inRobotsSection = false;
                    inInitialPositionsSection = true;
                    boardDataStarted = false; // Exit board data mode
                    continue;
                } else if (line.equals("WALLS:")) {
                    // New section for wall data
                    inRobotsSection = false;
                    inInitialPositionsSection = false;
                    boardDataStarted = false; // Exit board data mode
                    Timber.d("Entering WALLS section");
                    continue;
                }
                
                // Process WALLS section data
                if (!inRobotsSection && !inInitialPositionsSection && !boardDataStarted && line.contains(",") && 
                        (line.startsWith("H,") || line.startsWith("V,"))) {
                    // Format: type,x,y
                    String[] wallData = line.split(",");
                    if (wallData.length >= 3) {
                        String wallType = wallData[0];
                        int x = Integer.parseInt(wallData[1]);
                        int y = Integer.parseInt(wallData[2]);
                        
                        if ("H".equals(wallType)) {
                            state.addHorizontalWall(x, y);
                            wallsAdded++;
                            Timber.d("Added horizontal wall at (%d,%d) from WALLS section", x, y);
                        } else if ("V".equals(wallType)) {
                            state.addVerticalWall(x, y);
                            wallsAdded++;
                            // Timber.d("Added vertical wall at (%d,%d) from WALLS section", x, y);
                        }
                    }
                    continue;
                }
                
                // If we haven't started parsing board data yet, and this line contains commas, it's board data
                if (!boardDataStarted && line.contains(",")) {
                    boardDataStarted = true;
                    Timber.d("Started parsing board data at line %d", i);
                }
                
                if (boardDataStarted && boardLine < height) {
                    // Parse this line of board data
                    String[] cells = line.split(",");
                    Timber.d("Parsing board line %d with %d cells", boardLine, cells.length);
                    
                    for (int x = 0; x < Math.min(width, cells.length); x++) {
                        String cellData = cells[x];
                        
                        // Don't skip empty cells, they might be important for column 0
                        if (cellData.isEmpty()) {
                            Timber.d("Empty cell data at (%d,%d), treating as empty cell", x, boardLine);
                            continue;
                        }
                        
                        try {
                            if (cellData.contains(":")) {
                                // This is a target cell with color
                                String[] targetParts = cellData.split(":");
                                int cellType = Integer.parseInt(targetParts[0]);
                                int targetColor = Integer.parseInt(targetParts[1]);
                                
                                Timber.d("Found target cell at (%d,%d) with type %d and color %d", x, boardLine, cellType, targetColor);
                                
                                if (cellType == Constants.TYPE_TARGET) {
                                    state.addTarget(x, boardLine, targetColor);
                                    targetsAdded++;
                                    Timber.d("Added target at (%d,%d) with color %d", x, boardLine, targetColor);
                                } else {
                                    Timber.d("[LOAD/SAVE] unexpected target cellType: %d", cellType);
                                }
                            } else {
                                int cellType = Integer.parseInt(cellData);
                                // Timber.d("Found cell at (%d,%d) with type %d", x, boardLine, cellType);
                                
                                if (cellType == Constants.TYPE_HORIZONTAL_WALL) {
                                    state.addHorizontalWall(x, boardLine);
                                    wallsAdded++;
                                    // Timber.d("Added horizontal wall at (%d,%d)", x, boardLine);
                                } else if (cellType == Constants.TYPE_VERTICAL_WALL) {
                                    state.addVerticalWall(x, boardLine);
                                    wallsAdded++;
                                    // Timber.d("Added vertical wall at (%d,%d)", x, boardLine);
                                } else if (cellType == Constants.TYPE_EMPTY) {
                                    // Empty cell, nothing to do
                                } else if (cellType != Constants.TYPE_TARGET && cellType != Constants.TYPE_ROBOT) {
                                    // Only log unknown cell types that aren't targets or robots
                                    Timber.d("[LOAD/SAVE] unknown cellType: %d at (%d,%d)", cellType, x, boardLine);
                                }
                            }
                        } catch (NumberFormatException e) {
                            Timber.e("Error parsing cell data '%s' at (%d,%d): %s", cellData, x, boardLine, e.getMessage());
                        }
                    }
                    boardLine++;
                }
                
                // Process ROBOTS section data
                if (inRobotsSection && line.contains(",")) {
                    String[] robotData = line.split(",");
                    if (robotData.length >= 3) {
                        int x = Integer.parseInt(robotData[0]);
                        int y = Integer.parseInt(robotData[1]);
                        int color = Integer.parseInt(robotData[2]);
                        state.addRobot(x, y, color);
                        Timber.d("Added robot at (%d,%d) with color %d", x, y, color);
                    }
                    continue;
                }

                // Process INITIAL_POSITIONS section data
                if (inInitialPositionsSection && line.contains(",")) {
                    // Initialize the initialRobotPositions map if it doesn't exist
                    if (state.initialRobotPositions == null) {
                        state.initialRobotPositions = new HashMap<>();
                    }
                    
                    String[] positionData = line.split(",");
                    if (positionData.length >= 3) {
                        int x = Integer.parseInt(positionData[0]);
                        int y = Integer.parseInt(positionData[1]);
                        int color = Integer.parseInt(positionData[2]);
                        state.initialRobotPositions.put(color, new int[]{x, y});
                        Timber.d("Added initial position for robot color %d at (%d,%d)", color, x, y);
                    }
                    continue;
                }
                
                // Skip dimension lines and empty lines
                if (line.startsWith("WIDTH:") || line.startsWith("HEIGHT:") || line.trim().isEmpty()) {
                    continue;
                }
            }
            
            // If we have initial positions but no robots have been reset to them yet,
            // reset the robots to their initial positions
            if (state.initialRobotPositions != null && !state.initialRobotPositions.isEmpty()) {
                state.resetRobotPositions();
                Timber.d("Reset robots to their initial positions after loading");
            } else {
                // If we don't have initial positions saved in the file, store the current positions as initial
                state.storeInitialRobotPositions();
                Timber.d("No initial positions found in save file, storing current positions as initial");
            }
            
            Timber.d("Successfully parsed game state from save data: %d walls, %d targets", wallsAdded, targetsAdded);
            return state;
            
        } catch (Exception e) {
            Timber.e(e, "Error parsing save data: %s", e.getMessage());
            return null;
        }
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
//            Timber.d("[LEVEL LOADING] Generated ASCII map:\n%s", roboyard.pm.ia.ricochet.RRGetMap.generateAsciiMap(state.getGameElements()));

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
        saveData.append("MOVES:").append(moveCount).append(";");
        saveData.append("HINTS:").append(hintCount).append(";\n");
        
        // Add board dimensions
        saveData.append("WIDTH:").append(width).append(";\n");
        saveData.append("HEIGHT:").append(height).append(";\n");
        
        // Add board data
        for (int y = 0; y < height; y++) {
            // Serialize the cell contents including walls
            for (int x = 0; x < width; x++) {
                int cellType = board[y][x];
                
                // If the cell is a target, append the target color
                if (cellType == Constants.TYPE_TARGET) {
                    saveData.append(cellType).append(":").append(targetColors[y][x]);
                } else {
                    saveData.append(cellType);
                }
                
                saveData.append(",");
                // Timber.d("Serializing cell at (%d,%d) with type %d", x, y, cellType);
            }
            saveData.append("\n");
        }
        
        // Add dedicated WALLS section to ensure all walls are properly serialized
        saveData.append("WALLS:\n");
        // Save horizontal walls
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (hasHorizontalWall(x, y)) {
                    saveData.append("H,").append(x).append(",").append(y).append("\n");
                    // Timber.d("Serializing horizontal wall at (%d,%d)", x, y);
                }
            }
        }
        // Save vertical walls
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (hasVerticalWall(x, y)) {
                    saveData.append("V,").append(x).append(",").append(y).append("\n");
                    // Timber.d("Serializing vertical wall at (%d,%d)", x, y);
                }
            }
        }
        
        // Make sure initial robot positions are stored
        if (initialRobotPositions == null || initialRobotPositions.isEmpty()) {
            storeInitialRobotPositions();
            Timber.d("Initial robot positions were not stored, storing them now");
        }
        
        // Add initial robot positions as the ROBOTS section
        // This ensures that when a game is loaded, robots are placed at their initial positions
        saveData.append("ROBOTS:\n");
        for (Map.Entry<Integer, int[]> entry : initialRobotPositions.entrySet()) {
            int robotColor = entry.getKey();
            int[] position = entry.getValue();
            saveData.append(position[0]).append(",")
                   .append(position[1]).append(",")
                   .append(robotColor).append("\n");
            // Timber.d("Serializing initial position for robot color %d at (%d, %d)", robotColor, position[0], position[1]);
        }
        
        // Add current robot positions as INITIAL_POSITIONS section for reference
        // This maintains compatibility with the existing code
        saveData.append("INITIAL_POSITIONS:\n");
        for (Map.Entry<Integer, int[]> entry : initialRobotPositions.entrySet()) {
            int robotColor = entry.getKey();
            int[] position = entry.getValue();
            saveData.append(position[0]).append(",")
                   .append(position[1]).append(",")
                   .append(robotColor).append("\n");
            // Timber.d("Serializing initial position for robot color %d at (%d, %d)", robotColor, position[0], position[1]);
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
            Timber.e("[ROBOTS] resetRobotPositions: Cannot reset, initialRobotPositions is null or empty");
            return;
        }
        
        Timber.d("[ROBOTS] resetRobotPositions: Starting reset with %d stored initial positions", initialRobotPositions.size());
        
        // Get current robot elements
        List<GameElement> currentRobots = new ArrayList<>();
        for (GameElement element : gameElements) {
            if (element.getType() == GameElement.TYPE_ROBOT) {
                currentRobots.add(element);
            }
        }
        
        // Skip if no robots found
        if (currentRobots.isEmpty()) {
            Timber.e("[ROBOTS] resetRobotPositions: No robots found in current game state");
            return;
        }
        
        Timber.d("[ROBOTS] resetRobotPositions: Found %d robots to reset", currentRobots.size());
        
        // Reset each robot to its initial position
        for (GameElement robot : currentRobots) {
            int robotColor = robot.getColor();
            Timber.d("[ROBOTS] resetRobotPositions: Processing robot with color %d", robotColor);
            
            if (initialRobotPositions.containsKey(robotColor)) {
                int[] position = initialRobotPositions.get(robotColor);
                Timber.d("[ROBOTS] resetRobotPositions: Resetting robot color %d from (%d, %d) to (%d, %d)", 
                        robotColor, robot.getX(), robot.getY(), position[0], position[1]);
                robot.setX(position[0]);
                robot.setY(position[1]);
            } else {
                Timber.e("[ROBOTS] resetRobotPositions: No initial position found for robot color %d", robotColor);
            }
        }
        
        // Reset selected robot
        selectedRobot = null;
        
        // Reset move count
        moveCount = 0;
        
        // Reset completion flag
        completed = false;
        
        Timber.d("[ROBOTS] resetRobotPositions: Reset complete");
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
     * Get the last moved robot
     * @return The robot that was last moved, or null if no robot has been moved
     */
    public GameElement getLastMovedRobot() {
        return lastMovedRobot;
    }
    
    /**
     * Set the last moved robot
     * @param robot The robot that was just moved
     */
    public void setLastMovedRobot(GameElement robot) {
        this.lastMovedRobot = robot;
        Timber.d("[MOVE_TRACKING] Last moved robot set to color: %d", robot != null ? robot.getColor() : -1);
    }
    
    /**
     * Get the direction of the last move
     * @return The direction constant from ERRGameMove, or null if no move has been made
     */
    public Integer getLastMoveDirection() {
        return lastMoveDirection;
    }
    
    /**
     * Set the direction of the last move
     * @param direction The direction constant from ERRGameMove
     */
    public void setLastMoveDirection(Integer direction) {
        this.lastMoveDirection = direction;
        Timber.d("[MOVE_TRACKING] Last move direction set to: %d", direction != null ? direction : -1);
    }
    
    /**
     * Sets the number of robots per color to use for this game
     * @param count Number of robots (1-4)
     */
    public void setRobotCount(int count) {
        // Ensure count is within valid range
        this.robotCount = Math.max(1, Math.min(4, count));
        Timber.d("Robot count set to %d", this.robotCount);
    }
    
    /**
     * Gets the current robot count setting
     * @return Number of robots per color (1-4)
     */
    public int getRobotCount() {
        return robotCount;
    }
    
    /**
     * Sets the number of different target colors to use for this game
     * @param count Number of target colors (1-4)
     */
    public void setTargetColors(int count) {
        // Ensure count is within valid range
        this.targetColorsCount = Math.max(1, Math.min(4, count));
        Timber.d("Target colors count set to %d", this.targetColorsCount);
    }
    
    /**
     * Gets the current target colors count setting
     * @return Number of target colors (1-4)
     */
    public int getTargetColors() {
        return targetColorsCount;
    }
    
    /**
     * Converts difficulty integer to string for the original GridGameView class
     */
    private static String difficultyIntToString(int difficulty) {
        switch (difficulty) {
            case Constants.DIFFICULTY_BEGINNER: return "Beginner";
            case Constants.DIFFICULTY_ADVANCED: return "Intermediate";
            case Constants.DIFFICULTY_INSANE: return "Advanced";
            case Constants.DIFFICULTY_IMPOSSIBLE: return "Expert";
            default: return "Beginner";
        }
    }
    
    /**
     * Create a random game state
     */
    public static GameState createRandom() {
        // Set the global difficulty level first so difficulty is consistent
        String difficultyString = difficultyIntToString(Preferences.difficulty);
        
        // Log initial board size and requested size
        Timber.tag(TAG).d("[BOARD_SIZE_DEBUG] createRandom called with size: " + Preferences.boardSizeX + "x" + Preferences.boardSizeY);
        Timber.tag(TAG).d("[BOARD_SIZE_DEBUG] Current MainActivity.boardSize before setting: " +
                MainActivity.boardSizeX + "x" + MainActivity.boardSizeY);
        
        // Save current board dimensions and set them for game generation
        // Ensure board size is never zero to prevent ArrayIndexOutOfBoundsException
        int boardSizeX = Preferences.boardSizeWidth;
        int boardSizeY = Preferences.boardSizeHeight;
        
        // Safety check: ensure board dimensions are valid (at least 8x8)
        if (boardSizeX <= 0 || boardSizeY <= 0) {
            Timber.tag(TAG).e("[BOARD_SIZE_DEBUG] Invalid board dimensions: %dx%d, using default 16x16", boardSizeX, boardSizeY);
            boardSizeX = 16;
            boardSizeY = 16;
            
            // Also update the Preferences to fix the issue permanently
            Preferences.setBoardSize(boardSizeX, boardSizeY);
        }
        
        // Set the board size in MainActivity for compatibility with existing code
        MainActivity.boardSizeX = boardSizeX;
        MainActivity.boardSizeY = boardSizeY;
        
        // Log the board size being used for map generation
        Timber.tag(TAG).d("[BOARD_SIZE_DEBUG] Using board size: %dx%d", boardSizeX, boardSizeY);
        
        // Create new game state with specified dimensions
        GameState state = new GameState(boardSizeX, boardSizeY);

        // Use MapGenerator instead of directly using GameLogic to match the old canvas-based game
        Timber.tag(TAG).d("[BOARD_SIZE_DEBUG] Creating MapGenerator with dimensions: " +
                boardSizeX + "x" + boardSizeY);

        // Create MapGenerator instance
        MapGenerator mapGenerator = new MapGenerator();
        
        // Set the robot count and target colors from static Preferences
        state.robotCount = Preferences.robotCount;
        state.targetColorsCount = Preferences.targetColors;
        
        // Pass the values to the MapGenerator
        mapGenerator.setRobotCount(state.robotCount);
        mapGenerator.setTargetColors(state.targetColorsCount);
        
        Timber.tag(TAG).d("[PREFERENCES] Using robotCount=%d, targetColors=%d from static Preferences",
                state.robotCount, state.targetColorsCount);
        
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
                state.addTarget(x, y, Constants.COLOR_PINK);
                Timber.d("[COLOR_MAPPING] Added %s target at (%d,%d) with color ID %d (%s)",
                         type, x, y, Constants.COLOR_PINK, GameLogic.getColorName(Constants.COLOR_PINK, true));
            } else if (type.equals("target_green")) {
                state.addTarget(x, y, Constants.COLOR_GREEN);
                Timber.d("[COLOR_MAPPING] Added green target at (%d,%d) with color ID %d", x, y, Constants.COLOR_GREEN);
            } else if (type.equals("target_blue")) {
                state.addTarget(x, y, Constants.COLOR_BLUE);
                Timber.d("[COLOR_MAPPING] Added blue target at (%d,%d) with color ID %d", x, y, Constants.COLOR_BLUE);
            } else if (type.equals("target_yellow")) {
                state.addTarget(x, y, Constants.COLOR_YELLOW);
                Timber.d("[COLOR_MAPPING] Added yellow target at (%d,%d) with color ID %d", x, y, Constants.COLOR_YELLOW);
            } else if (type.equals("target_multi")) {
                // Multi-color target - we'll use pink as default
                state.addTarget(x, y, Constants.COLOR_PINK);
                Timber.d("[COLOR_MAPPING] Added multi-color target at (%d,%d) with color ID %d", x, y, Constants.COLOR_PINK);
            } else if (type.equals("robot_red")) {
                // Both pink and red map to COLOR_PINK (index 0) - pink is the actual game color, red is used in solver
                state.addRobot(x, y, Constants.COLOR_PINK);
                Timber.d("[COLOR_MAPPING] Added %s robot at (%d,%d) with color ID %d (%s)",
                         type, x, y, Constants.COLOR_PINK, GameLogic.getColorName(Constants.COLOR_PINK, true));
            } else if (type.equals("robot_green")) {
                state.addRobot(x, y, Constants.COLOR_GREEN);
                Timber.d("[COLOR_MAPPING] Added green robot at (%d,%d) with color ID %d", x, y, Constants.COLOR_GREEN);
            } else if (type.equals("robot_blue")) {
                state.addRobot(x, y, Constants.COLOR_BLUE);
                Timber.d("[COLOR_MAPPING] Added blue robot at (%d,%d) with color ID %d", x, y, Constants.COLOR_BLUE);
            } else if (type.equals("robot_yellow")) {
                state.addRobot(x, y, Constants.COLOR_YELLOW);
                Timber.d("[COLOR_MAPPING] Added yellow robot at (%d,%d) with color ID %d", x, y, Constants.COLOR_YELLOW);
            }
        }

        state.setLevelName("Random Game " + System.currentTimeMillis() % 1000);
        
        // Store initial robot positions for reset functionality
        state.storeInitialRobotPositions();
        Timber.d("[ROBOTS] Stored initial robot positions for new random game");
        
        // Generate a unique map ID for this random game
        ArrayList<GridElement> stateGridElements = state.getGridElements();
        String uniqueId = MapIdGenerator.generateUniqueId(stateGridElements);
        state.setUniqueMapId(uniqueId);
        state.setLevelName(uniqueId); // Use the unique ID as the level name
        
        Timber.d("GameState: Created random game with unique ID: %s", uniqueId);
        
        return state;
    }
    
    /**
     * Get the number of hints used in this game
     * @return The hint count
     */
    public int getHintCount() {
        return hintCount;
    }
    
    /**
     * Increment the hint count
     */
    public void incrementHintCount() {
        hintCount++;
    }
    
    /**
     * Get the unique map ID for this game
     * @return The 5-letter unique map ID
     */
    public String getUniqueMapId() {
        return uniqueMapId;
    }
    
    /**
     * Set the unique map ID for this game
     * @param uniqueMapId The 5-letter unique map ID
     */
    public void setUniqueMapId(String uniqueMapId) {
        this.uniqueMapId = uniqueMapId;
    }
    
    /**
     * Store initial robot positions for reset functionality
     */
    public void storeInitialRobotPositions() {
        // Initialize the map if it doesn't exist
        if (initialRobotPositions == null) {
            initialRobotPositions = new HashMap<>();
            Timber.d("[ROBOTS] storeInitialRobotPositions: Created new initialRobotPositions map");
        } else {
            Timber.d("[ROBOTS] storeInitialRobotPositions: Using existing initialRobotPositions map with %d entries", initialRobotPositions.size());
        }
        
        int robotCount = 0;
        // Loop through all game elements to find robots
        for (GameElement element : gameElements) {
            if (element.getType() == GameElement.TYPE_ROBOT) {
                // Store the robot's position by its color
                int[] position = new int[] { element.getX(), element.getY() };
                initialRobotPositions.put(element.getColor(), position);
                Timber.d("[ROBOTS] storeInitialRobotPositions: Stored robot color %d at position (%d, %d)", 
                        element.getColor(), position[0], position[1]);
                robotCount++;
            }
        }
        
        Timber.d("[ROBOTS] storeInitialRobotPositions: Stored positions for %d robots", robotCount);
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
                    if (hasVerticalWall(x + 1, currentY)) {
                        return false;
                    }
                }
            }
            // Moving west
            else if (currentX > nextX) {
                // Check for vertical walls between current position and target
                for (int x = nextX; x < currentX; x++) {
                    if (hasVerticalWall(x + 1, currentY)) {
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
                    if (hasHorizontalWall(currentX, y + 1)) {
                        return false;
                    }
                }
            }
            // Moving north
            else if (currentY > nextY) {
                // Check for horizontal walls between current position and target
                for (int y = nextY; y < currentY; y++) {
                    if (hasHorizontalWall(currentX, y + 1)) {
                        return false;
                    }
                }
            }
        }
        
        // If we've made it this far, the move is valid
        return true;
    }
}
