package roboyard.logic.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import roboyard.eclabs.ui.GameElement;
import roboyard.logic.core.Constants;
import timber.log.Timber;

/**
 * Model class that represents all walls on a game board.
 * This class serves as a single source of truth for wall data.
 */
public class WallModel {
    private final List<Wall> walls = new ArrayList<>();
    private final int boardWidth;
    private final int boardHeight;
    
    /**
     * Creates a new wall model with the specified board dimensions.
     *
     * @param boardWidth The width of the game board
     * @param boardHeight The height of the game board
     */
    public WallModel(int boardWidth, int boardHeight) {
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
    }
    
    /**
     * Adds a wall to the model.
     *
     * @param x The x-coordinate of the wall
     * @param y The y-coordinate of the wall
     * @param type The type of wall (horizontal or vertical)
     */
    public void addWall(int x, int y, WallType type) {
        Wall wall = new Wall(x, y, type);
        if (!walls.contains(wall)) {
            walls.add(wall);
        }
    }
    
    /**
     * Gets all walls in the model.
     *
     * @return An unmodifiable list of all walls
     */
    public List<Wall> getWalls() {
        return Collections.unmodifiableList(walls);
    }
    
    /**
     * Gets the width of the board.
     *
     * @return The board width
     */
    public int getBoardWidth() {
        return boardWidth;
    }
    
    /**
     * Gets the height of the board.
     *
     * @return The board height
     */
    public int getBoardHeight() {
        return boardHeight;
    }
    
    /**
     * Checks if there is a wall at the specified position with the specified type.
     *
     * @param x The x-coordinate to check
     * @param y The y-coordinate to check
     * @param type The wall type to check for
     * @return true if a wall exists at the position with the specified type, false otherwise
     */
    public boolean hasWallAt(int x, int y, WallType type) {
        return walls.contains(new Wall(x, y, type));
    }
    
    /**
     * Creates a WallModel from a list of GameElement objects.
     *
     * @param elements The list of game elements
     * @param width The width of the game board
     * @param height The height of the game board
     * @return A new WallModel containing all walls from the game elements
     */
    public static WallModel fromGameElements(List<GameElement> elements, int width, int height) {
        WallModel model = new WallModel(width, height);
        
        for (GameElement element : elements) {
            if (element.getType() == Constants.TYPE_HORIZONTAL_WALL) {
                model.addWall(element.getX(), element.getY(), WallType.HORIZONTAL);
                Timber.d("Added horizontal wall at (%d,%d)", element.getX(), element.getY());
            } else if (element.getType() == Constants.TYPE_VERTICAL_WALL) {
                model.addWall(element.getX(), element.getY(), WallType.VERTICAL);
                Timber.d("Added vertical wall at (%d,%d)", element.getX(), element.getY());
            }
        }
        
        return model;
    }
    
    /**
     * Creates a WallModel from a list of GridElement objects.
     * This is used for compatibility with the older grid element system.
     *
     * @param elements The list of grid elements
     * @param width The width of the game board
     * @param height The height of the game board
     * @return A new WallModel containing all walls from the grid elements
     */
    public static WallModel fromGridElements(List<GridElement> elements, int width, int height) {
        WallModel model = new WallModel(width, height);
        
        for (GridElement element : elements) {
            String type = element.getType();
            if ("mh".equals(type)) {
                model.addWall(element.getX(), element.getY(), WallType.HORIZONTAL);
                Timber.d("Added horizontal wall at (%d,%d)", element.getX(), element.getY());
            } else if ("mv".equals(type)) {
                model.addWall(element.getX(), element.getY(), WallType.VERTICAL);
                Timber.d("Added vertical wall at (%d,%d)", element.getX(), element.getY());
            }
        }
        
        return model;
    }
}
