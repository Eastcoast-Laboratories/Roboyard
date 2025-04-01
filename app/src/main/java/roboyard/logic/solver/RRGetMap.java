package roboyard.pm.ia.ricochet;
import roboyard.logic.core.Constants;
import roboyard.logic.core.GridElement;
import roboyard.ui.activities.MainActivity;

import android.graphics.Color;

import driftingdroids.model.Board;
import roboyard.eclabs.*;
import timber.log.Timber;

import java.util.ArrayList;
import java.util.Arrays;
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

        // Generate and log the ASCII map for debugging
        generateAsciiMap(gridElements);
        
        // Create the board with the current dimensions
        Board board = Board.createBoardFreestyle(null, MainActivity.boardSizeX, MainActivity.boardSizeY, MainActivity.numRobots);
        board.removeGoals();

        // Color mappings for robots and targets
        Map<String, Integer> colors = new HashMap<>();
        Map<String, Integer> colors2 = new HashMap<>();

        // Robot colors for visual representation
        colors2.put("robot_red", Color.RED);
        colors2.put("robot_blue", Color.BLUE);
        colors2.put("robot_green", Color.GREEN);
        colors2.put("robot_yellow", Color.YELLOW);

        // Robot and target indices for game logic
        // Using Constants class values
        colors.put("robot_red", Constants.COLOR_RED);        // Constants.COLOR_RED
        colors.put("robot_green", Constants.COLOR_GREEN);    // Constants.COLOR_GREEN
        colors.put("robot_blue", Constants.COLOR_BLUE);      // Constants.COLOR_BLUE
        colors.put("robot_yellow", Constants.COLOR_YELLOW);  // Constants.COLOR_YELLOW

        colors.put("target_red", Constants.COLOR_RED);        // Constants.COLOR_RED
        colors.put("target_green", Constants.COLOR_GREEN);    // Constants.COLOR_GREEN
        colors.put("target_blue", Constants.COLOR_BLUE);      // Constants.COLOR_BLUE
        colors.put("target_yellow", Constants.COLOR_YELLOW);  // Constants.COLOR_YELLOW
        colors.put("target_multi", -1);

        int robotCounter = 0;
        boolean targetFound = false;

        // Process each grid element (walls, targets, robots)
        for (Object element : gridElements) {
            GridElement gridElement = (GridElement) element;
            int x = gridElement.getX();
            int y = gridElement.getY();
            
            // Calculate linear position (y * width + x)
            // This maps 2D coordinates to a unique 1D position
            int position = y * board.width + x;

            String type = gridElement.getType();
            
            // Handle horizontal walls
            if (type.equals("mh")) {
                board.setWall(position, "N", true);
            }
            // Handle vertical walls
            if (type.equals("mv")) {
                board.setWall(position, "W", true);
            }
            // Handle targets (both colored and multi-colored)
            if (type.equals("target_red") || type.equals("target_green") || 
                type.equals("target_blue") || type.equals("target_yellow") || 
                type.equals("target_multi")) {
                board.addGoal(position, colors.get(type), 1);
                targetFound = true;
                
                // Set this as the active goal
                board.setGoal(position);
                Timber.d("[SOLUTION_SOLVER] Setting goal at position %d for robot color %d", position, colors.get(type));
            }
            // Handle robots of different colors
            if (type.equals("robot_red") || type.equals("robot_green") || 
                type.equals("robot_blue") || type.equals("robot_yellow")) {
                // FIXED: Use the color index (0-3) from colors map instead of the RGB color value from colors2
                // Old code created piece with actual RGB color instead of index: new RRPiece(x, y, colors2.get(type), robotCounter)
                int colorIndex = colors.get(type);
                Timber.d("[HINT] Creating robot piece for %s with colorIndex=%d instead of RGB color %d", 
                        type, colorIndex, colors2.get(type));
                pieces[colorIndex] = new RRPiece(x, y, colorIndex, robotCounter);
                
                robotCounter++;
            }
        }

        // Set robot positions on the board
        // Must be exactly 4 robots as the solver expects this
        for(int i = 0; i < 4; i++) {
            int position = pieces[i].getY() * board.width + pieces[i].getX();
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
     * Generates an ASCII representation of the game board for debugging purposes
     * @param gridElements The grid elements to display in ASCII format
     */
    private static void generateAsciiMap(ArrayList<GridElement> gridElements) {
        // Create a double-width ASCII map to separate walls from robots/targets
        // For each grid cell (x,y), we'll use:
        // - asciiMap[x*2][y] for vertical walls
        // - asciiMap[x*2+1][y] for robots and targets
        String[][] asciiMap = new String[44][22]; // Double width to handle separate positions
        
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
                } else if (contents.contains("robot_blue")) {
                    asciiMap[x*2+1][y] = "b̅"; // b with overline
                } else if (contents.contains("robot_green")) {
                    asciiMap[x*2+1][y] = "g̅"; // g with overline
                } else if (contents.contains("target_")) {
                    // Target with horizontal wall
                    if (contents.contains("target_yellow")) {
                        asciiMap[x*2+1][y] = "Y̅"; // Y with overline
                    } else if (contents.contains("target_red")) {
                        asciiMap[x*2+1][y] = "R̅"; // R with overline
                    } else if (contents.contains("target_blue")) {
                        asciiMap[x*2+1][y] = "B̅"; // B with overline
                    } else if (contents.contains("target_green")) {
                        asciiMap[x*2+1][y] = "G̅"; // G with overline
                    } else if (contents.contains("target_multi")) {
                        asciiMap[x*2+1][y] = "M̅"; // M with overline
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
                } else if (contents.contains("target_multi")) {
                    asciiMap[x*2+1][y] = "M";
                }
            }
        }

        // Second pass: render the map row by row
        for (int y = 0; y < 22; y++) {
            StringBuilder sb = new StringBuilder();
            boolean hasContent = false;
            
            for (int x = 0; x < 22; x++) {
                // Add vertical wall position
                if (asciiMap[x*2][y] == null) {
                    sb.append(" ");
                } else {
                    sb.append(asciiMap[x*2][y]);
                    hasContent = true;
                }
                
                // Add robot/target or horizontal wall position
                if (asciiMap[x*2+1][y] == null) {
                    sb.append(" ");
                } else {
                    sb.append(asciiMap[x*2+1][y]);
                    hasContent = true;
                }
            }
            
            // Skip empty rows
            if (!hasContent) {
                break;
            }
            
            Timber.d("[SOLUTION_SOLVER] [Ascii map] " + (y<10?"0"+y:y) + ": " + sb);
        }
    }
}
