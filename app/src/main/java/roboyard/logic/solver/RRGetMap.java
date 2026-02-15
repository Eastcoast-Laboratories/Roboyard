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
import java.util.Set;

/**
 * Utility class for converting between Roboyard's game elements and DriftingDroids board format.
 * This class serves as the main conversion layer between the two systems, handling:
 * - Grid cell conversion
 * - Robot piece placement
 * - Wall and obstacle mapping
 * - target position translation
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
        // Find the board dimensions from the GridElements
        int maxX = 0;
        int maxY = 0;
        for (Object element : gridElements) {
            GridElement gridElement = (GridElement) element;
            maxX = Math.max(maxX, gridElement.getX());
            maxY = Math.max(maxY, gridElement.getY());
        }
        
        // Add 1 to get width/height from max coordinates
        int boardWidth = maxX + 1;
        int boardHeight = maxY + 1;
        
        // Log the actual board dimensions we're using
        Timber.d("[SOLUTION_SOLVER] createDDWorld: Using board dimensions " + boardWidth + "x" + boardHeight + " (MainActivity dimensions: " + MainActivity.boardSizeX + "x" + MainActivity.boardSizeY + ")");

        // Generate the ASCII map for debugging
        String asciiMap = generateAsciiMap(gridElements);
        Timber.d(asciiMap);
        // Create the board with dimensions from GridElements
        // IMPORTANT: Use boardWidth/boardHeight calculated from GridElements, not MainActivity dimensions
        // The GridElements may have coordinates up to boardWidth-1, so we need a board of that size
        // Using MainActivity dimensions caused walls at x=12 to wrap around to x=0 of the next row
        Board board = Board.createBoardFreestyle(null, boardWidth, boardHeight, MainActivity.numRobots);
        board.removeGoals();
        
        Timber.d("[SOLUTION_SOLVER] Board created with width=%d, height=%d", board.width, board.height);

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
        colors.put("target_silver", Constants.COLOR_SILVER);  // Constants.COLOR_SILVER
        // Explicitly setting multi-color target to use wildcard value (-1)
        // This tells the DriftingDroids solver that any robot can match this target
        colors.put("target_multi", Constants.COLOR_MULTI);

        int robotCounter = 0;
        boolean targetFound = false;
        
        // CRITICAL CHANGE: First process all non-wall elements (targets, robots) before walls
        // This ensures targets get priority over walls at the same position
        for (Object element : gridElements) {
            GridElement gridElement = (GridElement) element;
            String type = gridElement.getType();
            int x = gridElement.getX();
            int y = gridElement.getY();
            int position = y * board.width + x;
            
            // Skip walls - we'll handle them separately after targets
            if (type.equals("mh") || type.equals("mv")) {
                continue;
            }
            
            // Handle targets (both colored and multi-colored)
            if (type.equals("target_red") || type.equals("target_green") || 
                type.equals("target_blue") || type.equals("target_yellow") || 
                type.equals("target_silver") || type.equals("target_multi") ||
                type.equals("target_pink")) { // Added target_pink here
                
                int targetColor = colors.getOrDefault(type, Constants.COLOR_PINK);
                board.addGoal(position, targetColor, 1);
                targetFound = true;
                
                // Set this as the active target
                board.setGoal(position);
                Timber.d("[SOLUTION_SOLVER_TARGET] Setting goal at position %d (%d,%d) for robot color %d", 
                        position, x, y, targetColor);
            }else if (type.startsWith("target_")){
                Timber.w("[SOLUTION_SOLVER_TARGET] Unknown target type: %s", type);
            }
            
            // Handle robots of different colors
            if (type.equals("robot_red") || type.equals("robot_green") || 
                type.equals("robot_blue") || type.equals("robot_yellow") ||
                type.equals("robot_silver") || type.equals("robot_pink")) { // Added robot_pink
                
                // Get the color index from the GameLogic
                int colorIndex = colors.getOrDefault(type, robotCounter % Constants.NUM_ROBOTS);
                
                // Map color indices to valid piece array indices (0-3)
                // This is needed because the solver only supports 4 robots max
                int mappedIndex;
                if (colorIndex >= 0 && colorIndex < Constants.NUM_ROBOTS) {
                    // Standard robots (0-3) use their own indices
                    mappedIndex = colorIndex;
                } else {
                    // For extra robots (indices >= Constants.NUM_ROBOTS), map them to one of the standard robots
                    // This is a temporary solution - we log a warning to highlight the issue
                    // PINK = 0, GREEN = 1, BLUE = 2, YELLOW = 3, SILVER = 4
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

        // Now process walls - AFTER we've identified target positions
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
            int topPosition = 0 + x;
            if (!board.isWall(topPosition, Constants.NORTH)){
                board.setWall(topPosition, "N", true);
                Timber.w("[SOLUTION_SOLVER][WALLS] Adding missing top horizontal border wall at position (%d,0)", x);
                missingWallCountHorizontal++;
            }
            // Bottom border
            int bottomWallY = (board.height-1) * board.width;
            int bottomPosition = bottomWallY + x;
            if (!board.isWall(bottomPosition, Constants.SOUTH)){
                board.setWall(bottomPosition, "N", true);
                Timber.w("[SOLUTION_SOLVER][WALLS] Adding missing bottom horizontal border wall at position (%d,%d)", x, board.height-1);
                missingWallCountHorizontal++;
            }
        }
        
        for (int y = 0; y < board.height; y++) {
            // Left vertical border
            int verticalWallY = y * board.width;
            int leftPosition = 0 + verticalWallY;
            if (!board.isWall(leftPosition, Constants.WEST)){
                board.setWall(leftPosition, "W", true);
                Timber.w("[SOLUTION_SOLVER][WALLS] Adding missing left vertical border wall at position (0,%d)", y);
                missingWallCountVertical++;
            }
            // Right border
            int rightWallX = board.width - 1;
            int rightPosition = rightWallX + verticalWallY;
            if (!board.isWall(rightPosition, Constants.EAST)){
                board.setWall(rightPosition, "W", true);
                Timber.w("[SOLUTION_SOLVER][WALLS] Adding missing right vertical border wall at position (%d,%d)", board.width-1, y);
                missingWallCountVertical++;
            }
        }
        
        if (missingWallCountHorizontal > 0 || missingWallCountVertical > 0) {
            Timber.w("[SOLUTION_SOLVER][WALLS] Added %d missing outer walls to ensure solver stability, horizontal:%d, vertical:%d", missingWallCountHorizontal + missingWallCountVertical, missingWallCountHorizontal, missingWallCountVertical);
        }

        // Set robot positions on the board
        // Must be exactly 4 robots as the solver expects this
        for(int i = 0; i < Constants.NUM_ROBOTS; i++) {
            // Check if the piece exists at this position
            if (pieces[i] == null) {
                Timber.e("[ROBOT_MAPPING][ERRROR] Fatal: Missing robot at position %d. Creating a dummy robot at (0,0)", i);
                // Create a dummy piece at position (0,0) as a fallback
                // This ensures the solver can proceed but may not produce correct solutions
                pieces[i] = new RRPiece(0, 0, i, i);
            }
            
            int position = pieces[i].getY() * board.width + pieces[i].getX();
            Timber.d("[ROBOT_MAPPING] Setting robot %d at board position %d (x=%d, y=%d)", 
                    i, position, pieces[i].getX(), pieces[i].getY());
            board.setRobot(i, position, false);

            // Verify that setRobot succeeded
            if (!board.setRobot(i, position, false)) {
                Timber.e("[ROBOT_MAPPING][ERRROR] FATAL: Could not set robot %d at position %d (%d,%d). Position may be occupied or invalid.", 
                        i, position, pieces[i].getX(), pieces[i].getY());
            }
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
        
        // Determine actual board dimensions from grid elements (including border walls at +1)
        int maxX = 0;
        int maxY = 0;
        for (GridElement element : gridElements) {
            maxX = Math.max(maxX, element.getX());
            maxY = Math.max(maxY, element.getY());
        }
        int mapWidth = maxX + 1;
        int mapHeight = maxY + 1;
        
        // Create a double-width ASCII map to separate walls from robots/targets
        // For each grid cell (x,y), we'll use:
        // - asciiMap[x*2][y] for vertical walls
        // - asciiMap[x*2+1][y] for robots and targets
        String[][] asciiMap = new String[2 * mapWidth + 1][mapHeight];
        
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
                if (contents.contains("robot_")) {
                    // robots with horizontal wall - use combining overline
                    if (contents.contains("robot_yellow")) {
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
                    } else {
                        // Unknown robot
                        asciiMap[x*2+1][y] = "u̅"; // u with overline
                    }
                } else if (contents.contains("target_")) {
                    // targets with horizontal wall
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
                    } else {
                        // Unknown target
                        asciiMap[x*2+1][y] = "U̅"; // U with overline
                    }
                } else {
                    // Just a horizontal wall an overline
                    asciiMap[x*2+1][y] = "‾"; // UTF8 overline
                }
            } else {
                // No horizontal wall, just set the character based on element type
                if (contents.contains("robot_")) {
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
                    } else if (contents.contains("robot_silver")) {
                        asciiMap[x*2+1][y] = "s";
                    } else {
                        // Unknown robot
                        asciiMap[x*2+1][y] = "u";
                    }   
                } else if (contents.contains("target_")) {
                    if (contents.contains("target_yellow")) {
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
                    } else {
                        // Unknown target
                        asciiMap[x*2+1][y] = "U";
                    }
                }
            }
        }

        // Build the ASCII map as a string
        StringBuilder result = new StringBuilder("[ASCII_MAP] Board state:\n");
        
        // Add column headers (X coordinates)
        result.append("   "); // Space for row labels
        for (int x = 0; x < mapWidth; x++) {
            result.append(String.format("%2d", x));
        }
        result.append("\n");
        
        // Add each row with row header (Y coordinate)
        for (int y = 0; y < mapHeight; y++) {
            result.append(String.format("%2d ", y));
            
            for (int x = 0; x < mapWidth; x++) {
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

    /**
     * Parse an ASCII map (as generated by generateAsciiMap) back into GridElement list.
     * Supports robots (r,g,b,y,p,s), targets (R,G,B,Y,P,S,M), walls (|,‾),
     * and combined robot/target + horizontal wall characters (e.g. r̅, G̅).
     *
     * @param asciiMap The ASCII map string (with or without the "[ASCII_MAP]" prefix)
     * @return ArrayList of GridElement, or null if parsing failed
     */
    public static ArrayList<GridElement> parseAsciiMap(String asciiMap) {
        if (asciiMap == null || asciiMap.isEmpty()) {
            Timber.e("[ASCII_PARSE] Input is null or empty");
            return null;
        }

        ArrayList<GridElement> elements = new ArrayList<>();

        // Split into lines and find the data lines (skip header/prefix lines)
        String[] allLines = asciiMap.split("\n");
        java.util.List<String> dataLines = new java.util.ArrayList<>();
        boolean foundHeader = false;

        for (String line : allLines) {
            String trimmed = line.trim();
            // Skip empty lines and the "[ASCII_MAP]" prefix line
            if (trimmed.isEmpty() || trimmed.startsWith("[ASCII_MAP]")) continue;
            // Skip column header line (starts with spaces then digits)
            if (!foundHeader && trimmed.matches("^\\s*\\d.*")) {
                // Could be column header or data line - check if it starts with a number followed by space
                // Column header: "    0 1 2 3 ..."  Data line: " 0 |. . ..."
                // Column headers have many numbers in sequence
                String[] parts = trimmed.split("\\s+");
                boolean allNumbers = true;
                for (String p : parts) {
                    if (!p.isEmpty() && !p.matches("\\d+")) {
                        allNumbers = false;
                        break;
                    }
                }
                if (allNumbers && parts.length > 3) {
                    foundHeader = true;
                    continue; // Skip column header
                }
            }
            // Data lines start with a row number
            if (trimmed.matches("^\\d+\\s.*")) {
                dataLines.add(trimmed);
            }
        }

        if (dataLines.isEmpty()) {
            Timber.e("[ASCII_PARSE] No data lines found in ASCII map");
            return null;
        }

        Timber.d("[ASCII_PARSE] Found %d data lines", dataLines.size());

        // Map characters to element types
        // Lowercase = robots, Uppercase = targets
        java.util.Map<Character, String> robotMap = new java.util.HashMap<>();
        robotMap.put('r', "robot_red");
        robotMap.put('g', "robot_green");
        robotMap.put('b', "robot_blue");
        robotMap.put('y', "robot_yellow");
        robotMap.put('p', "robot_pink");
        robotMap.put('s', "robot_silver");

        java.util.Map<Character, String> targetMap = new java.util.HashMap<>();
        targetMap.put('R', "target_red");
        targetMap.put('G', "target_green");
        targetMap.put('B', "target_blue");
        targetMap.put('Y', "target_yellow");
        targetMap.put('P', "target_pink");
        targetMap.put('S', "target_silver");
        targetMap.put('M', "target_multi");

        for (String line : dataLines) {
            // Extract row number (first number in the line)
            int spaceIdx = line.indexOf(' ');
            if (spaceIdx < 0) continue;
            int y;
            try {
                y = Integer.parseInt(line.substring(0, spaceIdx).trim());
            } catch (NumberFormatException e) {
                continue;
            }

            // The rest of the line contains cell data
            // Format: for each cell x, two characters: vWall + cellContent
            // vWall = "|" or " ", cellContent = robot/target/wall/"."
            String cellData = line.substring(spaceIdx + 1);

            int x = 0;
            int i = 0;
            while (i < cellData.length()) {
                // First char: vertical wall indicator
                char vWallChar = cellData.charAt(i);
                i++;

                if (vWallChar == '|') {
                    elements.add(new GridElement(x, y, "mv"));
                }

                if (i >= cellData.length()) break;

                // Second char: cell content
                char cellChar = cellData.charAt(i);
                i++;

                // Check for combining overline (U+0305) following the character
                boolean hasOverline = false;
                if (i < cellData.length() && cellData.charAt(i) == '\u0305') {
                    hasOverline = true;
                    i++; // consume the combining character
                }

                if (hasOverline) {
                    // Character + overline = element + horizontal wall
                    elements.add(new GridElement(x, y, "mh"));
                }

                if (cellChar == '\u203E' || cellChar == '‾') {
                    // Standalone overline = horizontal wall only
                    elements.add(new GridElement(x, y, "mh"));
                } else if (robotMap.containsKey(cellChar)) {
                    elements.add(new GridElement(x, y, robotMap.get(cellChar)));
                    if (!hasOverline) {
                        // no extra action needed
                    }
                } else if (targetMap.containsKey(cellChar)) {
                    elements.add(new GridElement(x, y, targetMap.get(cellChar)));
                } else if (cellChar == '.' || cellChar == ' ') {
                    // Empty cell, nothing to add
                }
                // else: unknown character, skip

                x++;
            }
        }

        Timber.d("[ASCII_PARSE] Parsed %d elements from ASCII map", elements.size());
        return elements;
    }

}
