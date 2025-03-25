package roboyard.eclabs.util;

import java.util.ArrayList;
import java.util.List;

import roboyard.eclabs.GridElement;
import roboyard.pm.ia.GameSolution;
import roboyard.pm.ia.IGameMove;
import roboyard.pm.ia.ricochet.RRGameMove;
import roboyard.pm.ia.ricochet.RRPiece;
import timber.log.Timber;

/**
 * A utility class that handles the logic for translating solver solutions into actionable robot moves.
 * This class manages the conversion between solver-specific formats and game-specific formats,
 * as well as tracking and validating moves during solution execution.
 */
public class GameMoveManager {
    
    // Direction constants that match the game's direction values
    public static final int DIRECTION_NORTH = 0;
    public static final int DIRECTION_EAST = 1;
    public static final int DIRECTION_SOUTH = 2;
    public static final int DIRECTION_WEST = 3;
    
    private List<IGameMove> currentMoves = new ArrayList<>();
    private int currentMoveIndex = 0;
    private GameSolution currentSolution;
    
    // Event listener for move actions
    private MoveListener moveListener;
    
    /**
     * Interface for receiving move events
     */
    public interface MoveListener {
        /**
         * Called when a move should be executed
         * @param robotId The ID of the robot to move
         * @param direction The direction to move the robot
         */
        void onMoveExecuted(int robotId, int direction);
        
        /**
         * Called when all moves have been executed
         */
        void onAllMovesExecuted();
    }
    
    /**
     * Sets the listener for move events
     * @param listener The listener to receive events
     */
    public void setMoveListener(MoveListener listener) {
        this.moveListener = listener;
    }
    
    /**
     * Sets the current solution to manage
     * @param solution The solution to use
     */
    public void setSolution(GameSolution solution) {
        this.currentSolution = solution;
        this.currentMoves = solution != null ? solution.getMoves() : new ArrayList<>();
        this.currentMoveIndex = 0;
    }
    
    /**
     * Gets the current set of moves from the solution
     * @return The current set of moves
     */
    public List<IGameMove> getCurrentMoves() {
        return currentMoves;
    }
    
    /**
     * Gets the number of moves in the current solution
     * @return The number of moves in the current solution
     */
    public int getMoveCount() {
        return currentMoves != null ? currentMoves.size() : 0;
    }
    
    /**
     * Gets the current move index
     * @return The current move index
     */
    public int getCurrentMoveIndex() {
        return currentMoveIndex;
    }
    
    /**
     * Executes the next move in the solution
     * @return True if there was a next move to execute, false otherwise
     */
    public boolean executeNextMove() {
        if (currentMoves == null || currentMoveIndex >= currentMoves.size()) {
            if (moveListener != null) {
                moveListener.onAllMovesExecuted();
            }
            return false;
        }
        
        IGameMove move = currentMoves.get(currentMoveIndex++);
        if (move instanceof RRGameMove) {
            RRGameMove gameMove = (RRGameMove) move;
            int robotColor = gameMove.getColor();
            int direction = translateSolverDirectionToGameDirection(gameMove.getDirection());
            int robotId = getRobotIdFromColor(robotColor);
            
            if (moveListener != null) {
                moveListener.onMoveExecuted(robotId, direction);
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Translates a solver direction (UP=0, RIGHT=1, DOWN=2, LEFT=3) to a game direction
     * @param solverDirection The solver direction
     * @return The corresponding game direction
     */
    public int translateSolverDirectionToGameDirection(int solverDirection) {
        // The solver and game use the same direction constants
        return solverDirection;
    }
    
    /**
     * Gets a robot ID from its color
     * @param color The color of the robot
     * @return The ID of the robot
     */
    public int getRobotIdFromColor(int color) {
        // This mapping should match the game's robot ID assignments
        switch (color) {
            case 0: // RED
                return 0;
            case 1: // GREEN
                return 1;
            case 2: // BLUE
                return 2;
            case 3: // YELLOW
                return 3;
            default:
                Timber.e("Unknown robot color: %d", color);
                return 0;
        }
    }
    
    /**
     * Resets the move execution to the beginning of the solution
     */
    public void resetMoveExecution() {
        this.currentMoveIndex = 0;
    }
    
    /**
     * Finds robots and targets in the grid elements
     * @param gridElements The grid elements to search
     * @return A list of grid elements corresponding to robots and targets
     */
    public List<GridElement> findRobotsAndTargets(List<GridElement> gridElements) {
        List<GridElement> result = new ArrayList<>();
        
        for (GridElement element : gridElements) {
            String type = element.getType();
            if (type.startsWith("robot_") || type.startsWith("target_")) {
                result.add(element);
            }
        }
        
        return result;
    }
}
