package roboyard.logic.core;

import timber.log.Timber;

/**
 * Singleton class that provides a centralized representation of the game board grid.
 * This serves as the single source of truth for the board array used in collision detection
 * and spatial lookups.
 */
public class GameBoard {
    private static final String TAG = "GameBoard";
    private static GameBoard instance;
    
    // The board array represents the state of each cell
    // IMPORTANT: using boardWidth+1 and boardHeight+1 to account for walls at index boardWidth/Height
    private int[][] board;
    private int boardWidth;
    private int boardHeight;
    
    // Private constructor for singleton pattern
    private GameBoard() {
        // Default initialization with minimum size
        // Will be resized when needed
        resizeBoard(8, 8);
    }
    
    /**
     * Get the singleton instance
     */
    public static GameBoard getInstance() {
        if (instance == null) {
            instance = new GameBoard();
        }
        return instance;
    }
    
    /**
     * Get the board array
     * 
     * @return The 2D board array
     */
    public int[][] getBoard() {
        return board;
    }
    
    /**
     * Resize the board to the specified dimensions
     * 
     * @param width The new board width
     * @param height The new board height
     */
    public void resizeBoard(int width, int height) {
        // IMPORTANT: +1 to accommodate the walls at indices boardWidth/Height
        this.boardWidth = width;
        this.boardHeight = height;
        
        // Using boardWidth+1 and boardHeight+1 to account for walls at index boardWidth/Height
        board = new int[boardWidth+1][boardHeight+1];
        
        Timber.tag(TAG).d("Board resized to %dx%d (array size %dx%d)", 
                          width, height, boardWidth+1, boardHeight+1);
        
        // Initialize with empty cells
        clearBoard();
    }
    
    /**
     * Get the value of a cell on the board
     * 
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @return The cell value, or -1 if coordinates are out of bounds
     */
    public int getCellValue(int x, int y) {
        if (x >= 0 && x <= boardWidth && y >= 0 && y <= boardHeight) {
            return board[x][y];
        }
        
        // Out of bounds
        return -1;
    }
    
    /**
     * Set the value of a cell on the board
     * 
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @param value The new cell value
     * @return true if the cell was set, false if coordinates are out of bounds
     */
    public boolean setCellValue(int x, int y, int value) {
        if (x >= 0 && x <= boardWidth && y >= 0 && y <= boardHeight) {
            board[x][y] = value;
            return true;
        }
        
        // Out of bounds
        Timber.tag(TAG).w("Attempted to set cell value out of bounds: (%d,%d)", x, y);
        return false;
    }
    
    /**
     * Clear the board by setting all cells to empty
     */
    public void clearBoard() {
        for (int x = 0; x <= boardWidth; x++) {
            for (int y = 0; y <= boardHeight; y++) {
                board[x][y] = Constants.TYPE_EMPTY;
            }
        }
    }
    
    /**
     * Get the current board width
     */
    public int getBoardWidth() {
        return boardWidth;
    }
    
    /**
     * Get the current board height
     */
    public int getBoardHeight() {
        return boardHeight;
    }
}
