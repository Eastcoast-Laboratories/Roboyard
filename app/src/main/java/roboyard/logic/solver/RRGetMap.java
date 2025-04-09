package roboyard.pm.ia.ricochet;
import roboyard.logic.core.Constants;
import roboyard.logic.core.GameLogic;
import roboyard.logic.core.GridElement;
import roboyard.logic.core.Wall;
import roboyard.logic.core.WallModel;
import roboyard.logic.core.WallType;
import roboyard.ui.activities.MainActivity;

import android.graphics.Color;

import driftingdroids.model.Board;
import timber.log.Timber;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Utility class for converting between Roboyard's game elements and DriftingDroids board format.
 * This class serves as the main conversion layer between the two systems, handling:
 * - Grid cell conversion
 * - Robot piece placement
 * - Wall and obstacle mapping
 * - Goal position translation
 *
 * @see driftingdroids.model.Board
 * @see roboyard.eclabs.GridElement
 */
public class RRGetMap {

    /**
     * Create a virtual world in memory for the DD solver (IDDFS)
     * adds all game elements to that virtual world so the solver can solve it with the current data
     *
     * @param gridElements game elements
     * @param pieces
     * @return
     */
    public static Board createDDWorld(ArrayList<GridElement> gridElements, RRPiece[] pieces) {

        // Calculate actual board dimensions from grid elements
        int maxX = 0;
        int maxY = 0;
        
        for (GridElement element : gridElements) {
            if (element.getX() > maxX) maxX = element.getX();
            if (element.getY() > maxY) maxY = element.getY();
        }
        
        // Add 1 to get width/height from max coordinates
        int boardWidth = maxX + 1;
        int boardHeight = maxY + 1;
        
        // Log the actual board dimensions we're using
        Timber.d("[SOLUTION_SOLVER] createDDWorld: Using board dimensions " + boardWidth + "x" + boardHeight + " (MainActivity dimensions: " + MainActivity.boardSizeX + "x" + MainActivity.boardSizeY + ")");

        // Generate the ASCII map for debugging
        String asciiMap = generateAsciiMap(gridElements);
        Timber.d(asciiMap);
        // Create the board with the current dimensions
        Board board = Board.createBoardFreestyle(null, MainActivity.boardSizeX, MainActivity.boardSizeY, MainActivity.numRobots);
        board.removeGoals();

        // Color mappings for robots and targets
        Map<String, Integer> colors = new HashMap<>();

        // Robot and target indices for game logic
        // Using Constants class values
        colors.put("robot_red", Constants.COLOR_PINK);       // Constants.COLOR_PINK
        colors.put("robot_green", Constants.COLOR_GREEN);    // Constants.COLOR_GREEN
        colors.put("robot_blue", Constants.COLOR_BLUE);      // Constants.COLOR_BLUE
        colors.put("robot_yellow", Constants.COLOR_YELLOW);  // Constants.COLOR_YELLOW
        colors.put("robot_silver", Constants.COLOR_SILVER); 

        colors.put("target_red", Constants.COLOR_PINK);       // Constants.COLOR_PINK
        colors.put("target_green", Constants.COLOR_GREEN);    // Constants.COLOR_GREEN
        colors.put("target_blue", Constants.COLOR_BLUE);      // Constants.COLOR_BLUE
        colors.put("target_yellow", Constants.COLOR_YELLOW);  // Constants.COLOR_YELLOW
        colors.put("target_multi", -1);

        int robotCounter = 0;
        boolean targetFound = false;

        // Create a wall model from the grid elements
        WallModel wallModel = WallModel.fromGridElements(gridElements, boardWidth, boardHeight);
        
        // Process each wall in the model and set it on the board
        // to understand setWall: driftingdroids solver uses "N" and "W" to represent horizontal and vertical walls
        // e.g. walls[direction][x + y * this.width] = true;
        // direction is "N" for horizontal and "W" for vertical
        // the y position is stored in the same key as multiple of the width of the board
        for (Wall wall : wallModel.getWalls()) {
            int x = wall.getX();
            int y = wall.getY();
            int position = y * board.width + x;
            
            if (wall.getType() == WallType.HORIZONTAL) {
                board.setWall(position, "N", true);  // treated as "N" of the current field in driftingdroids solver
                
                Timber.d("[SOLUTION_SOLVER] Setting horizontal wall at position %d (x=%d, y=%d)", position, x, y);
            } else if (wall.getType() == WallType.VERTICAL) {
                board.setWall(position, "W", true);  // treated as "W" of the current field in driftingdroids solver
                
                Timber.d("[SOLUTION_SOLVER] Setting vertical wall at position %d (x=%d, y=%d)", position, x, y);
            }
        }
        
        // Force outer walls to be present - essential for the solver
        int missingWallCountHorizontal = 0;
        int missingWallCountVertical = 0;
        
        for (int x = 0; x < board.width; x++) {
            // Top border
            if (!board.isWall(0 + x, Constants.NORTH)){
                board.setWall(0 + x, "N", true);
                Timber.w("[SOLUTION_SOLVER][WALLS] Adding missing top horizontal border wall at position (%d,0)", x);
                missingWallCountHorizontal++;
            }
            // Bottom border
            int bottomWallY = (board.height-1) * board.width;
            if (!board.isWall(bottomWallY + x, Constants.SOUTH)){
                board.setWall(bottomWallY + x, "N", true);
                Timber.w("[SOLUTION_SOLVER][WALLS] Adding missing bottom horizontal border wall at position (%d,%d)", x, board.height-1);
                missingWallCountHorizontal++;
            }
        }
        
        for (int y = 0; y < board.height; y++) {
            // Left vertical border
            int verticalWallY = y * board.width;
            // board.setWall(0, y, Constants.WEST, false); // debug, delete a left vertical walls
            if (!board.isWall(0 + verticalWallY, Constants.WEST)){
                board.setWall(0 + verticalWallY, "W", true);
                Timber.w("[SOLUTION_SOLVER][WALLS] Adding missing left vertical border wall at position (0,%d)", y);
                missingWallCountVertical++;
            }
            // Right border
            int rightWallX = board.width - 1;
            if (!board.isWall(rightWallX + verticalWallY, Constants.EAST)){
                board.setWall(rightWallX + verticalWallY, "W", true);
                Timber.w("[SOLUTION_SOLVER][WALLS] Adding missing right vertical border wall at position (%d,%d)", board.width-1, y);
                missingWallCountVertical++;
            }
        }
        
        if (missingWallCountHorizontal > 0 || missingWallCountVertical > 0) {
            Timber.w("[SOLUTION_SOLVER][WALLS] Added %d missing outer walls to ensure solver stability, horizontal:%d, vertical:%d", missingWallCountHorizontal + missingWallCountVertical, missingWallCountHorizontal, missingWallCountVertical);
        }
        
        // Process each grid element (targets, robots)
        for (Object element : gridElements) {
            GridElement gridElement = (GridElement) element;
            int x = gridElement.getX();
            int y = gridElement.getY();
            
            // Calculate linear position (y * width + x)
            // This maps 2D coordinates to a unique 1D position
            int position = y * board.width + x;

            String type = gridElement.getType();
            
            // Skip walls as they're already handled by the wall model
            if (type.equals("mh") || type.equals("mv")) {
                continue;
            }
            
            // Handle targets (both colored and multi-colored)
            if (type.equals("target_red") || type.equals("target_green") || 
                type.equals("target_blue") || type.equals("target_yellow") || 
                type.equals("target_silver") || type.equals("target_multi")) {
                board.addGoal(position, colors.get(type), 1);
                targetFound = true;
                
                // Set this as the active goal
                board.setGoal(position);
                Timber.d("[SOLUTION_SOLVER] Setting goal at position %d for robot color %d", position, colors.get(type));
            }
            // Handle robots of different colors
            if (type.equals("robot_red") || type.equals("robot_green") || 
                type.equals("robot_blue") || type.equals("robot_yellow") ||
                type.equals("robot_silver")) {
                // Get the color index from the GameLogic
                int colorIndex = colors.get(type);
                
                // Map color indices to valid piece array indices (0-3)
                // This is needed because the solver only supports 4 robots max
                int mappedIndex;
                if (colorIndex >= 0 && colorIndex < Constants.NUM_ROBOTS) {
                    // Standard robots (0-3) use their own indices
                    mappedIndex = colorIndex;
                } else {
                    // For extra robots (indices >= 4), map them to one of the standard robots
                    // This is a temporary solution - we log a warning to highlight the issue
                    // PINK = 0, GREEN = 1, BLUE = 2, YELLOW = 3
                    mappedIndex = robotCounter % Constants.NUM_ROBOTS;
                    Timber.w("[COLOR_MAPPING] Mapped non-standard robot color %d (%s) to standard color index %d", 
                             colorIndex, GameLogic.getColorName(colorIndex, true), mappedIndex);
                }
                
                Timber.d("[HINT_SYSTEM] Creating robot piece for %s with colorIndex=%d (mapped to %d) with RGB color %d", 
                        type, colorIndex, mappedIndex, GameLogic.getColor(type));
                
                pieces[mappedIndex] = new RRPiece(x, y, colorIndex, robotCounter);
                robotCounter++;
            }
        }

        // Set robot positions on the board
        // Must be exactly 4 robots as the solver expects this
        for(int i = 0; i < Constants.NUM_ROBOTS; i++) {
            // Check if the piece exists at this position
            if (pieces[i] == null) {
                Timber.e("[ROBOT_MAPPING] Missing robot at position %d. Creating a dummy robot at (0,0)", i);
                // Create a dummy piece at position (0,0) as a fallback
                // This ensures the solver can proceed but may not produce correct solutions
                pieces[i] = new RRPiece(0, 0, i, i);
            }
            
            int position = pieces[i].getY() * board.width + pieces[i].getX();
            Timber.d("[ROBOT_MAPPING] Setting robot %d at board position %d (x=%d, y=%d)", 
                    i, position, pieces[i].getX(), pieces[i].getY());
            board.setRobot(i, position, false);
        }
        
        // If no target was found, throw an exception
        // This prevents the NullPointerException in Board.isSolution01()
        if (!targetFound) {
            throw new RuntimeException("[SOLUTION_SOLVER] No target found in level");
        }

        return board;
    }
    
    /**
     * Generate an ASCII representation of the game board from the provided grid elements
     * @param gridElements List of grid elements to represent
     * @return ASCII string representation of the board
     */
    public static String generateAsciiMap(ArrayList<GridElement> gridElements) {
        if (gridElements == null || gridElements.isEmpty()) {
            return "[ASCII_MAP] Empty or null grid elements provided.";
        }
        
        // Create a double-width ASCII map to separate walls from robots/targets
        // For each grid cell (x,y), we'll use:
        // - asciiMap[x*2][y] for vertical walls
        // - asciiMap[x*2+1][y] for robots and targets
        String[][] asciiMap = new String[2*Constants.MAX_BOARD_SIZE+1][Constants.MAX_BOARD_SIZE]; // Double width to handle separate positions
        
        // Track cells that have both a robot and a horizontal wall
        Map<String, String> cellContents = new HashMap<>();
        
        // First pass: collect all elements by position to handle overlaps
        for (GridElement element : gridElements) {
            int x = element.getX();
            int y = element.getY();
            String key = x + "," + y;
            
            if (element.getType().equals("mh")) {
                // Remember this position has a horizontal wall
                String prevContent = cellContents.getOrDefault(key, "");
                cellContents.put(key, prevContent + "mh,");
            } else if (element.getType().equals("mv")) {
                // Vertical walls go in their own position
                asciiMap[x*2][y] = "|";
            } else {
                // Remember this position has a robot or target
                String prevContent = cellContents.getOrDefault(key, "");
                cellContents.put(key, prevContent + element.getType() + ",");
            }
        }
        
        // Process collected cell contents to handle overlaps
        for (Map.Entry<String, String> entry : cellContents.entrySet()) {
            String[] coords = entry.getKey().split(",");
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            String contents = entry.getValue();
            
            boolean hasHorizontalWall = contents.contains("mh,");
            
            // Handle special cases for robot + horizontal wall combinations
            if (hasHorizontalWall) {
                if (contents.contains("robot_yellow")) {
                    // Yellow robot with horizontal wall - use combining overline
                    asciiMap[x*2+1][y] = "y̅"; // y with overline
                } else if (contents.contains("robot_red")) {
                    asciiMap[x*2+1][y] = "r̅"; // r with overline
                } else if (contents.contains("robot_pink")) {
                    asciiMap[x*2+1][y] = "p̅"; // p with overline
                } else if (contents.contains("robot_blue")) {
                    asciiMap[x*2+1][y] = "b̅"; // b with overline
                } else if (contents.contains("robot_green")) {
                    asciiMap[x*2+1][y] = "g̅"; // g with overline
                } else if (contents.contains("robot_silver")) {
                    asciiMap[x*2+1][y] = "s̅"; // s with overline
                } else if (contents.contains("target_")) {
                    // Target with horizontal wall
                    if (contents.contains("target_yellow")) {
                        asciiMap[x*2+1][y] = "Y̅"; // Y with overline
                    } else if (contents.contains("target_red")) {
                        asciiMap[x*2+1][y] = "R̅"; // R with overline
                    } else if (contents.contains("target_pink")) {
                        asciiMap[x*2+1][y] = "P̅"; // P with overline
                    } else if (contents.contains("target_blue")) {
                        asciiMap[x*2+1][y] = "B̅"; // B with overline
                    } else if (contents.contains("target_green")) {
                        asciiMap[x*2+1][y] = "G̅"; // G with overline
                    } else if (contents.contains("target_multi")) {
                        asciiMap[x*2+1][y] = "M̅"; // M with overline
                    } else if (contents.contains("target_silver")) {
                        asciiMap[x*2+1][y] = "S̅"; // S with overline
                    }
                } else {
                    // Just a horizontal wall an overline
                    asciiMap[x*2+1][y] = "‾"; // UTF8 overline
                }
            } else {
                // No horizontal wall, just set the character based on element type
                if (contents.contains("robot_yellow")) {
                    asciiMap[x*2+1][y] = "y";
                } else if (contents.contains("robot_red")) {
                    asciiMap[x*2+1][y] = "r";
                } else if (contents.contains("robot_pink")) {
                    asciiMap[x*2+1][y] = "p";
                } else if (contents.contains("robot_blue")) {
                    asciiMap[x*2+1][y] = "b";
                } else if (contents.contains("robot_green")) {
                    asciiMap[x*2+1][y] = "g";
                } else if (contents.contains("target_yellow")) {
                    asciiMap[x*2+1][y] = "Y";
                } else if (contents.contains("target_red")) {
                    asciiMap[x*2+1][y] = "R";
                } else if (contents.contains("target_blue")) {
                    asciiMap[x*2+1][y] = "B";
                } else if (contents.contains("target_green")) {
                    asciiMap[x*2+1][y] = "G";
                } else if (contents.contains("target_silver")) {
                    asciiMap[x*2+1][y] = "S";
                } else if (contents.contains("target_multi")) {
                    asciiMap[x*2+1][y] = "M";
                }
            }
        }

        // Build the ASCII map as a string
        StringBuilder result = new StringBuilder("[ASCII_MAP] Board state:\n");
        
        // Find the actual dimensions of the board from the grid elements
        int maxX = 0;
        int maxY = 0;
        for (GridElement element : gridElements) {
            maxX = Math.max(maxX, element.getX());
            maxY = Math.max(maxY, element.getY());
        }
        
        // Add column headers (X coordinates)
        result.append("   "); // Space for row labels
        for (int x = 0; x <= maxX; x++) {
            result.append(String.format("%2d", x));
        }
        result.append("\n");
        
        // Add each row with row header (Y coordinate)
        for (int y = 0; y <= maxY; y++) {
            result.append(String.format("%2d ", y));
            
            for (int x = 0; x <= maxX; x++) {
                // Add vertical wall or space
                String vWall = (asciiMap[x*2][y] != null) ? asciiMap[x*2][y] : " ";
                result.append(vWall);
                
                // Add cell content or space
                String cell = (asciiMap[x*2+1][y] != null) ? asciiMap[x*2+1][y] : ".";
                result.append(cell);
            }
            result.append("\n");
        }
        
        return result.toString();
    }
    

}
