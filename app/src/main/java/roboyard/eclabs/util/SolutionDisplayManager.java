package roboyard.eclabs.util;

import java.util.List;

import roboyard.pm.ia.GameSolution;

/**
 * Interface for standardizing solution display across different UI implementations.
 * This interface defines the methods that any solution display manager must implement
 * to properly handle the animation and display of game solutions.
 */
public interface SolutionDisplayManager {
    
    /**
     * Displays the solution by animating robot movements.
     * @param solution The solution to display
     */
    void displaySolution(GameSolution solution);
    
    /**
     * Stops the current solution animation.
     */
    void stopSolutionAnimation();
    
    /**
     * @return Whether a solution animation is currently in progress
     */
    boolean isAnimating();
    
    /**
     * Shows or hides a spinner to indicate solution calculation is in progress.
     * @param show True to show the spinner, false to hide it
     */
    void showSpinner(boolean show);
    
    /**
     * @return Whether the spinner is currently visible
     */
    boolean isSpinnerVisible();
    
    /**
     * Sets a listener for solution display events.
     * @param listener The listener to receive events
     */
    void setListener(SolutionDisplayListener listener);
    
    /**
     * Interface for receiving solution display events.
     */
    interface SolutionDisplayListener {
        /**
         * Called when the solution animation starts.
         */
        void onSolutionAnimationStart();
        
        /**
         * Called when a robot is moved during the solution animation.
         * @param robotId The ID/color of the robot that moved
         * @param direction The direction the robot moved in
         * @param moveIndex The index of the current move in the solution
         * @param totalMoves The total number of moves in the solution
         */
        void onSolutionAnimationStep(int robotId, int direction, int moveIndex, int totalMoves);
        
        /**
         * Called when the solution animation is completed.
         */
        void onSolutionAnimationComplete();
        
        /**
         * Called when the solution animation is manually stopped or aborted.
         */
        void onSolutionAnimationStopped();
    }
}
