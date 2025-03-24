package roboyard.eclabs.util;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

import roboyard.eclabs.GamePiece;
import roboyard.pm.ia.GameSolution;
import roboyard.pm.ia.IGameMove;
import roboyard.pm.ia.ricochet.RRGameMove;
import timber.log.Timber;

/**
 * A utility class that handles the animation of solutions.
 * This class extracts the solution animation logic from the GridGameScreen
 * to make it reusable across different UI implementations.
 */
public class SolutionAnimator {
    private List<IGameMove> moves;
    private int currentMoveIndex = 0;
    private boolean isAnimating = false;
    private Handler handler;
    private int animationDelayMs = 500;
    private AnimationListener listener;
    
    /**
     * Interface for receiving solution animation events
     */
    public interface AnimationListener {
        /**
         * Called when a robot move should be executed
         * @param robotId The ID/color of the robot to move
         * @param direction The direction to move the robot (0-3)
         */
        void onRobotMove(int robotId, int direction);
        
        /**
         * Called when the animation is complete
         */
        void onAnimationComplete();
        
        /**
         * Called when the animation is stopped before completion
         */
        void onAnimationStopped();
    }
    
    /**
     * Creates a new SolutionAnimator instance.
     */
    public SolutionAnimator() {
        handler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Sets the listener for animation events
     * @param listener The listener to receive animation events
     */
    public void setAnimationListener(AnimationListener listener) {
        this.listener = listener;
    }
    
    /**
     * Sets the delay between animation steps
     * @param delayMs The delay in milliseconds
     */
    public void setAnimationDelay(int delayMs) {
        this.animationDelayMs = delayMs;
    }
    
    /**
     * Animates a solution by executing each move with a delay
     * @param solution The solution to animate
     */
    public void animateSolution(GameSolution solution) {
        if (solution == null) {
            Timber.e("SolutionAnimator: Cannot animate null solution");
            return;
        }
        
        // Copy the moves to avoid modifying the original solution
        moves = new ArrayList<>(solution.getMoves());
        currentMoveIndex = 0;
        
        if (moves.isEmpty()) {
            Timber.d("SolutionAnimator: Solution has no moves");
            if (listener != null) {
                listener.onAnimationComplete();
            }
            return;
        }
        
        Timber.d("SolutionAnimator: Starting animation with %d moves", moves.size());
        isAnimating = true;
        animateNextMove();
    }
    
    /**
     * Returns the next move to be executed.
     * This method is designed to work with existing legacy code from GridGameScreen.
     * @return The next move, or null if there are no more moves
     */
    public IGameMove getNextMove() {
        if (moves == null || currentMoveIndex >= moves.size()) {
            return null;
        }
        
        IGameMove move = moves.get(currentMoveIndex);
        currentMoveIndex++;
        
        // Signal completion if this was the last move
        if (currentMoveIndex >= moves.size() && listener != null) {
            handler.postDelayed(() -> {
                isAnimating = false;
                listener.onAnimationComplete();
            }, animationDelayMs);
        }
        
        return move;
    }
    
    /**
     * Returns the list of remaining moves.
     * This method helps the old game access moves directly.
     * @return The current list of remaining moves
     */
    public List<IGameMove> getRemainingMoves() {
        if (moves == null) {
            return new ArrayList<>();
        }
        
        if (currentMoveIndex >= moves.size()) {
            return new ArrayList<>();
        }
        
        return moves.subList(currentMoveIndex, moves.size());
    }
    
    /**
     * Gets the current move index
     * @return The index of the current move being animated
     */
    public int getCurrentMoveIndex() {
        return currentMoveIndex;
    }
    
    /**
     * Gets the total number of moves in the current solution
     * @return The total number of moves, or 0 if no solution is loaded
     */
    public int getTotalMoves() {
        return moves != null ? moves.size() : 0;
    }
    
    /**
     * Stops the current animation
     */
    public void stopAnimation() {
        if (isAnimating) {
            Timber.d("SolutionAnimator: Stopping animation");
            isAnimating = false;
            handler.removeCallbacksAndMessages(null);
            
            if (listener != null) {
                listener.onAnimationStopped();
            }
        }
    }
    
    /**
     * @return Whether the animator is currently animating a solution
     */
    public boolean isAnimating() {
        return isAnimating;
    }
    
    /**
     * Animates the next move in the solution
     */
    private void animateNextMove() {
        if (!isAnimating || currentMoveIndex >= moves.size()) {
            if (isAnimating) {
                isAnimating = false;
                Timber.d("SolutionAnimator: Animation complete");
                
                if (listener != null) {
                    listener.onAnimationComplete();
                }
            }
            return;
        }
        
        // Get the current move
        IGameMove move = moves.get(currentMoveIndex);
        currentMoveIndex++;
        
        try {
            // Extract robot ID/color and direction using reflection
            int robotId = -1;
            int direction = -1;
            
            if (move instanceof RRGameMove) {
                RRGameMove rrMove = (RRGameMove) move;
                robotId = rrMove.getColor();
                direction = translateIADirectionToGameDirection(rrMove.getDirection());
            } else {
                // Fallback to reflection for compatibility with other move types
                try {
                    if (move.getClass().getMethod("getColor") != null) {
                        robotId = (int) move.getClass().getMethod("getColor").invoke(move);
                    }
                    
                    if (move.getClass().getMethod("getDirection") != null) {
                        int rawDirection = (int) move.getClass().getMethod("getDirection").invoke(move);
                        direction = translateIADirectionToGameDirection(rawDirection);
                    }
                } catch (Exception e) {
                    Timber.w("SolutionAnimator: Could not extract move properties: %s", e.getMessage());
                }
            }
            
            // Execute the move
            if (robotId != -1 && direction != -1 && listener != null) {
                Timber.d("SolutionAnimator: Moving robot %d in direction %d", robotId, direction);
                listener.onRobotMove(robotId, direction);
            }
        } catch (Exception e) {
            Timber.e(e, "SolutionAnimator: Error executing move");
        }
        
        // Schedule the next move
        handler.postDelayed(this::animateNextMove, animationDelayMs);
    }
    
    /**
     * Translates the direction from the solver's representation to the game's representation
     * @param iaDirection The direction from the solver
     * @return The direction for the game (0-3)
     */
    public static int translateIADirectionToGameDirection(int iaDirection) {
        switch (iaDirection) {
            case 1: // Left
                return 0;
            case 2: // Down
                return 1;
            case 4: // Right
                return 2;
            case 8: // Up
                return 3;
            default:
                return -1;
        }
    }
}
