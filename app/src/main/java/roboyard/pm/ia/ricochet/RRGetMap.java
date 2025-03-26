package roboyard.pm.ia.ricochet;

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
        colors.put("robot_red", 0);
        colors.put("robot_blue", 1);
        colors.put("robot_green", 2);
        colors.put("robot_yellow", 3);

        colors.put("target_red", 0);
        colors.put("target_blue", 1);
        colors.put("target_green", 2);
        colors.put("target_yellow", 3);
        colors.put("target_multi", -1);

        int robotCounter = 0;

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
            }
            // Handle robots of different colors
            if (type.equals("robot_red") || type.equals("robot_green") || 
                type.equals("robot_blue") || type.equals("robot_yellow")) {
                pieces[colors.get(type)] = new RRPiece(x, y, colors2.get(type), robotCounter);
                robotCounter++;
            }
        }

        // Set robot positions on the board
        // Must be exactly 4 robots as the solver expects this
        for(int i = 0; i < 4; i++) {
            int position = pieces[i].getY() * board.width + pieces[i].getX();
            board.setRobot(i, position, false);
        }

        return board;
    }
    
    /**
     * Generates an ASCII representation of the game board for debugging purposes
     * @param gridElements The grid elements to display in ASCII format
     */
    private static void generateAsciiMap(ArrayList<GridElement> gridElements) {
        String[][] asciiMap = new String[22][22];
        
        // First pass: place all elements on the ASCII map
        for (GridElement element : gridElements) {
            // Special handling for walls to better represent their position in ASCII
            if (element.getType().equals("mh") || element.getType().equals("mv")) {
                if (element.getType().equals("mh")) {
                    // Horizontal walls connect cells horizontally - place the wall character 
                    // between the cells in the ASCII map representation
                    asciiMap[element.getX()][element.getY()] = "-";
                } else { // mv - vertical wall
                    // Vertical walls connect cells vertically
                    asciiMap[element.getX()][element.getY()] = "|";
                }
            } else {
                // For other elements (robots, targets) just place them directly
                asciiMap[element.getX()][element.getY()] = element.toChar();
            }
        }

        // Second pass: render the map row by row
        for (int y = 0; y < 22; y++) {
            // glue all elements in asciiMap[i] into a string
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < 22; x++) {
                if (asciiMap[x][y] == null) {
                    asciiMap[x][y] = " ";
                }
                sb.append(asciiMap[x][y]);
            }
            // stop, if sb is a string with just spaces
            if (sb.toString().trim().isEmpty()) {
                break;
            }
            Timber.d("[SOLUTION_SOLVER] [Ascii map] " + (y<10?"0"+y:y) + ": " + sb);
        }
    }
}
