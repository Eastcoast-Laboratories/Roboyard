package roboyard.eclabs;

import android.os.Handler;
import android.os.Looper;

import java.util.List;

import roboyard.eclabs.util.BrailleSpinner;
import roboyard.eclabs.util.SolutionAnimator;
import roboyard.eclabs.util.SolutionDisplayManager;
import roboyard.pm.ia.GameSolution;
import roboyard.pm.ia.IGameMove;
import timber.log.Timber;

/**
 * Implementation of SolutionDisplayManager for the original canvas-based game.
 * This adapter connects the GridGameScreen with our reusable solution components
 * without requiring changes to the original code.
 */
public class CanvasSolutionDisplayManager implements SolutionDisplayManager {

    private final GridGameScreen gridGameScreen;
    private BrailleSpinner spinner;
    private SolutionAnimator animator;
    private SolutionDisplayListener listener;
    private boolean isAnimating = false;
    private boolean spinnerVisible = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * Creates a new solution display manager for the canvas game
     * @param gridGameScreen The game screen to manage solutions for
     */
    public CanvasSolutionDisplayManager(GridGameScreen gridGameScreen) {
        this.gridGameScreen = gridGameScreen;
    }

    @Override
    public void displaySolution(GameSolution solution) {
        if (solution == null || solution.getMoves().isEmpty()) {
            Timber.w("Cannot display null or empty solution");
            return;
        }

        isAnimating = true;
        
        // Create animator if needed
        if (animator == null) {
            animator = new SolutionAnimator();
            animator.setAnimationListener(new SolutionAnimator.AnimationListener() {
                @Override
                public void onRobotMove(int robotId, int direction) {
                    // The old game handles moves itself through doMovesInMemory
                    if (listener != null) {
                        listener.onSolutionAnimationStep(robotId, direction, 
                            animator.getCurrentMoveIndex(), solution.getMoves().size());
                    }
                }

                @Override
                public void onAnimationComplete() {
                    isAnimating = false;
                    if (listener != null) {
                        listener.onSolutionAnimationComplete();
                    }
                }

                @Override
                public void onAnimationStopped() {
                    isAnimating = false;
                    if (listener != null) {
                        listener.onSolutionAnimationStopped();
                    }
                }
            });
        }

        // Instead of using our animator, let the game handle it using its showSolution method
        // This wrapper integrates with the existing code without modifying it
        if (listener != null) {
            listener.onSolutionAnimationStart();
        }
        
        // Use reflection to access the private showSolution method
        try {
            java.lang.reflect.Method showSolutionMethod = 
                GridGameScreen.class.getDeclaredMethod("showSolution", GameSolution.class);
            showSolutionMethod.setAccessible(true);
            showSolutionMethod.invoke(gridGameScreen, solution);
        } catch (Exception e) {
            Timber.e(e, "Error calling showSolution via reflection");
        }
    }

    @Override
    public void stopSolutionAnimation() {
        if (isAnimating) {
            isAnimating = false;
            
            // Reset game state
            try {
                java.lang.reflect.Method resetMethod = 
                    GridGameScreen.class.getDeclaredMethod("loadMap");
                resetMethod.setAccessible(true);
                resetMethod.invoke(gridGameScreen);
                
                if (listener != null) {
                    listener.onSolutionAnimationStopped();
                }
            } catch (Exception e) {
                Timber.e(e, "Error resetting game state");
            }
        }
    }

    @Override
    public boolean isAnimating() {
        return isAnimating;
    }

    @Override
    public void showSpinner(boolean show) {
        if (show == spinnerVisible) {
            return;
        }
        
        spinnerVisible = show;
        
        if (show) {
            if (spinner == null) {
                spinner = new BrailleSpinner();
                spinner.setSpinnerListener(spinnerChar -> {
                    try {
                        // Set a field in GridGameScreen to display the spinner
                        java.lang.reflect.Field requestToastField = 
                            GridGameScreen.class.getDeclaredField("requestToast");
                        requestToastField.setAccessible(true);
                        requestToastField.set(null, spinnerChar + " Calculating solution...");
                    } catch (Exception e) {
                        Timber.e(e, "Error setting spinner text");
                    }
                });
            }
            spinner.start();
        } else if (spinner != null) {
            spinner.stop();
            
            try {
                // Clear the toast text
                java.lang.reflect.Field requestToastField = 
                    GridGameScreen.class.getDeclaredField("requestToast");
                requestToastField.setAccessible(true);
                requestToastField.set(null, "");
            } catch (Exception e) {
                Timber.e(e, "Error clearing spinner text");
            }
        }
    }

    @Override
    public boolean isSpinnerVisible() {
        return spinnerVisible;
    }

    @Override
    public void setListener(SolutionDisplayListener listener) {
        this.listener = listener;
    }
    
    /**
     * Gets access to moves from the animator for the legacy code
     * @return The list of remaining moves, or null if not animating
     */
    public List<IGameMove> getRemainingMoves() {
        if (animator != null && isAnimating) {
            return animator.getRemainingMoves();
        }
        return null;
    }
    
    /**
     * Gets the next move from the animator for the legacy code
     * @return The next move, or null if not animating or no more moves
     */
    public IGameMove getNextMove() {
        if (animator != null && isAnimating) {
            return animator.getNextMove();
        }
        return null;
    }
}
