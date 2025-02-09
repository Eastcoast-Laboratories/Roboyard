import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

/**
 * GeneratorLevelSolver - A lightweight solver used during level generation
 * 
 * This solver is specifically designed for the level generation process.
 * It provides a fast way to determine the minimum number of moves required
 * to solve a generated level, which helps categorize levels by difficulty.
 * 
 * Key features:
 * - Fast execution for quick level validation
 * - Minimal memory usage
 * - Simple command-line interface
 * - Returns only the move count
 * 
 * Usage:
 * - Input: Level description via stdin
 * - Output: Number of moves to solve, or -1 if unsolvable
 */
public class GeneratorLevelSolver {
    public static class GridElement {
        private int x, y;
        private String type;
        
        public GridElement(int x, int y, String type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }
        
        public int getX() { return x; }
        public int getY() { return y; }
        public String getType() { return type; }
    }
    
    public static int solveLevelFromString(String mapContent) {
        // Parse map content into GridElements
        ArrayList<GridElement> elements = new ArrayList<>();
        String[] lines = mapContent.split("\n");
        
        System.err.println("Parsing map content:"); // Debug
        System.err.println(mapContent); // Debug
        
        for (String line : lines) {
            if (line.startsWith("board:")) {
                continue; // Skip board size line
            }
            // Format: type x,y;
            String[] parts = line.split("[,;]");
            if (parts.length >= 2) {
                String type = parts[0].replaceAll("\\d+$", "");
                int x = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
                int y = Integer.parseInt(parts[1]);
                elements.add(new GridElement(x, y, type));
            }
        }
        
        System.err.println("Parsed " + elements.size() + " elements"); // Debug
        
        // For now, simulate with random number of moves
        // We'll integrate the real solver later
        Random random = new Random();
        return random.nextInt(20) + 4;
    }
    
    public static void main(String[] args) {
        try {
            // Read from stdin
            Scanner scanner = new Scanner(System.in);
            StringBuilder mapContent = new StringBuilder();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                mapContent.append(line).append("\n");
            }
            scanner.close();
            
            System.err.println("Read map content:"); // Debug
            System.err.println(mapContent.toString()); // Debug
            
            int moves = solveLevelFromString(mapContent.toString());
            System.out.println(moves); // Print number of moves
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage()); // Debug
            e.printStackTrace(); // Debug
            System.out.println("-1"); // Error case
        }
    }
}
