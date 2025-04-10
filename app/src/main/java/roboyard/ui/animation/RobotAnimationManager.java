package roboyard.ui.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import roboyard.eclabs.ui.GameElement;
import roboyard.logic.core.GameLogic;
import roboyard.ui.components.GameGridView;
import roboyard.ui.components.GameStateManager;
import timber.log.Timber;

/**
 * Manages physics-based animations for robot movements
 */
public class RobotAnimationManager {
    
    private GameStateManager gameStateManager;
    private GameGridView gameGridView;
    
    // Track active animations by robot
    private final Map<GameElement, ValueAnimator> activeAnimations = new HashMap<>();
    
    // Queues for pending robot moves
    private final Map<GameElement, Queue<RobotMoveInfo>> robotMoveQueues = new HashMap<>();
    
    // Flags
    private final Map<GameElement, Boolean> isProcessingRobotAnimations = new HashMap<>();
    
    // Animation cancellation strategy enum
    public enum AnimationCancellationStrategy {
        JUMP_TO_END,
        STOP_AT_CURRENT
    }
    
    /**
     * Creates a new animation manager
     * @param gameStateManager The game state manager
     */
    public RobotAnimationManager(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
        Timber.d("[ANIM] RobotAnimationManager initialized");
    }
    
    /**
     * Set the game grid view for rendering
     * @param gameGridView The game grid view
     */
    public void setGameGridView(GameGridView gameGridView) {
        this.gameGridView = gameGridView;
        Timber.d("[ANIM] GameGridView set in RobotAnimationManager");
    }
    
    /**
     * Get the current GameGridView
     * @return The current GameGridView or null if not set
     */
    public GameGridView getGameGridView() {
        return gameGridView;
    }
    
    /**
     * Queue a robot move for animation
     * @param robot The robot to move
     * @param startX Starting X coordinate
     * @param startY Starting Y coordinate
     * @param endX Ending X coordinate
     * @param endY Ending Y coordinate
     * @param completionCallback Callback to run when animation is complete
     */
    public void queueRobotMove(GameElement robot, int startX, int startY, int endX, int endY, Runnable completionCallback) {
        Timber.d("[ANIM] Queueing robot move: %s (%d,%d) -> (%d,%d)", robot, startX, startY, endX, endY);
        
        // Validate the robot first
        if (robot == null) {
            Timber.e("[ANIM] Cannot queue move for null robot");
            if (completionCallback != null) {
                completionCallback.run();
            }
            return;
        }
        
        // Validate coordinates
        if (startX < 0 || startY < 0 || endX < 0 || endY < 0) {
            Timber.e("[ANIM] Invalid coordinates: (%d,%d) -> (%d,%d)", startX, startY, endX, endY);
            if (completionCallback != null) {
                completionCallback.run();
            }
            return;
        }

        // If the move is to the same position, skip animation
        if (startX == endX && startY == endY) {
            Timber.d("[ANIM] Skipping animation for move to same position");
            if (completionCallback != null) {
                completionCallback.run();
            }
            return;
        }
        
        // Initialize the queue for this robot if it doesn't exist
        if (!robotMoveQueues.containsKey(robot)) {
            robotMoveQueues.put(robot, new LinkedList<>());
            isProcessingRobotAnimations.put(robot, false);
        }
        
        // Create the move info object
        RobotMoveInfo moveInfo = new RobotMoveInfo(robot, startX, startY, endX, endY, completionCallback);
        
        // Add to queue
        robotMoveQueues.get(robot).add(moveInfo);
        
        // Start processing if not already processing
        if (!isProcessingRobotAnimations.getOrDefault(robot, false)) {
            // Start processing on the UI thread
            if (gameGridView != null) {
                gameGridView.post(() -> processRobotAnimationQueue(robot));
            } else {
                // Fallback if no view is set yet
                new Handler(Looper.getMainLooper()).post(() -> processRobotAnimationQueue(robot));
            }
        }
    }

    /**
     * Process the animation queue for a specific robot
     * @param robot The robot to process animations for
     */
    private void processRobotAnimationQueue(final GameElement robot) {
        Timber.d("[ANIM] Processing animation queue for robot %s", robot);
        
        // Set the processing flag immediately to prevent duplicate processing
        isProcessingRobotAnimations.put(robot, true);
        
        // Get the queue for this robot
        final Queue<RobotMoveInfo> robotQueue = robotMoveQueues.get(robot);
        if (robotQueue == null || robotQueue.isEmpty()) {
            Timber.d("[ANIM] No queued moves for robot %s, marking as not processing", robot);
            isProcessingRobotAnimations.put(robot, false);
            return;
        }
        
        // Get the next move
        final RobotMoveInfo moveInfo = robotQueue.poll();
        if (moveInfo == null) {
            Timber.d("[ANIM] Null move info for robot %s, marking as not processing", robot);
            isProcessingRobotAnimations.put(robot, false);
            return;
        }
        
        // Initialize animation position if not already set
        if (!robot.hasAnimationPosition()) {
            robot.setAnimationPosition(moveInfo.startX, moveInfo.startY);
        }
        
        // Calculate animation parameters
        final float distance = calculateDistance(moveInfo.startX, moveInfo.startY, moveInfo.endX, moveInfo.endY);
        final float animationDuration = calculateAnimationDuration(distance);
        
        Timber.d("[ANIM] Animating robot %s move: (%d,%d) -> (%d,%d), distance=%.2f, duration=%.2f", 
                robot, moveInfo.startX, moveInfo.startY, moveInfo.endX, moveInfo.endY, 
                distance, animationDuration);
        
        // Create and start the animation
        animateRobotMove(robot, moveInfo, animationDuration, () -> {
            // This runs when the animation completes
            if (moveInfo.completionCallback != null) {
                moveInfo.completionCallback.run();
            }
            
            // Process the next animation in queue on the UI thread
            if (gameGridView != null) {
                gameGridView.post(() -> processRobotAnimationQueue(robot));
            } else {
                new Handler(Looper.getMainLooper()).post(() -> processRobotAnimationQueue(robot));
            }
        });
    }

    /**
     * Animate a single robot move with physics-based animation
     * @param robot The robot to animate
     * @param moveInfo Information about the move
     * @param duration Animation duration in milliseconds
     * @param completionCallback Callback to run when animation completes
     */
    private void animateRobotMove(final GameElement robot, final RobotMoveInfo moveInfo, 
                                 final float duration, final Runnable completionCallback) {
        // Make sure we have a view to animate on
        if (gameGridView == null) {
            Timber.e("[ANIM] Cannot animate without GameGridView");
            if (completionCallback != null) {
                completionCallback.run();
            }
            return;
        }
        
        // When starting a move, shrink to DEFAULT_ROBOT_SCALE
        if (gameGridView instanceof GameGridView) {
            ((GameGridView) gameGridView).animateRobotScale(robot, 
                    GameGridView.SELECTED_ROBOT_SCALE, GameGridView.DEFAULT_ROBOT_SCALE);
        }
        
        // Create a ValueAnimator to animate from 0 to 1 (progress)
        final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration((long) duration);
        
        // Apply custom interpolator for physics feel
        animator.setInterpolator(new DecelerateInterpolator(1.5f));
        
        // MEMORY OPTIMIZATION: Track frame timing for throttling
        final long[] lastFrameTimeNs = {System.nanoTime()};
        final long frameDelayMs = gameStateManager != null ? gameStateManager.getAnimationFrameDelay() : 16;
        final long frameDelayNs = frameDelayMs * 1000000; // Convert ms to ns
        
        Timber.d("[ANIM_FRAMERATE] Using frame delay of %d ms (%d ns)", frameDelayMs, frameDelayNs);
        
        // Update the animation position on each frame
        animator.addUpdateListener(animation -> {
            // MEMORY OPTIMIZATION: Only process frames at our desired framerate
            long currentTimeNs = System.nanoTime();
            if (currentTimeNs - lastFrameTimeNs[0] < frameDelayNs) {
                // Skip this frame update to reduce processing load
                return;
            }
            
            // Update last frame time for next throttle check
            lastFrameTimeNs[0] = currentTimeNs;
            
            float progress = (float) animation.getAnimatedValue();
            
            // Calculate current position
            float currentX = moveInfo.startX + (moveInfo.endX - moveInfo.startX) * progress;
            float currentY = moveInfo.startY + (moveInfo.endY - moveInfo.startY) * progress;
            
            if (GameLogic.hasDebugLogging()) {
                Timber.d("[ANIM] Robot %s animation progress: %.2f, position: (%.2f,%.2f)", robot, progress, currentX, currentY);
            }
            
            // Update the robot's animation position
            robot.setAnimationPosition(currentX, currentY);
            
            // Redraw the view
            gameGridView.invalidate();
        });
        
        // Handle animation completion
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                Timber.d("[ANIM] Animation complete for robot %s", robot);
                
                // Set final position
                robot.setAnimationPosition(moveInfo.endX, moveInfo.endY);
                
                // After movement, grow back to SELECTED_ROBOT_SCALE with a slight bounce effect
                if (gameGridView instanceof GameGridView) {
                    // First overshoot a bit larger (bounce effect)
                    ((GameGridView) gameGridView).animateRobotScaleWithCallback(
                            robot, 
                            GameGridView.DEFAULT_ROBOT_SCALE, 
                            GameGridView.SELECTED_ROBOT_SCALE * 1.1f,  // overshoot
                            150, // shorter duration for bounce
                            () -> {
                                // Then bounce back to normal selected scale
                                ((GameGridView) gameGridView).animateRobotScale(
                                        robot, 
                                        GameGridView.SELECTED_ROBOT_SCALE * 1.1f, 
                                        GameGridView.SELECTED_ROBOT_SCALE);
                            });
                }
                
                // Redraw one more time
                gameGridView.invalidate();
                
                // Call completion callback
                if (completionCallback != null) {
                    completionCallback.run();
                }
            }
        });
        
        // Start the animation
        animator.start();
    }
    
    /**
     * Calculate the distance between two points
     * @param x1 Starting X coordinate
     * @param y1 Starting Y coordinate
     * @param x2 Ending X coordinate
     * @param y2 Ending Y coordinate
     * @return Distance between the two points
     */
    private float calculateDistance(int x1, int y1, int x2, int y2) {
        return (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
    
    /**
     * Calculate the animation duration based on distance
     * @param distance Distance to animate
     * @return Animation duration in milliseconds
     */
    private float calculateAnimationDuration(float distance) {
        if (gameStateManager == null) {
            // Default duration if no gameStateManager is available
            Timber.d("[ANIM_SPEED] Using default animation duration (500ms) - no GameStateManager");
            return 500f;
        }
        
        // Calculate travel time based on physics parameters
        float accelTime = gameStateManager.getAccelerationDuration();
        float maxSpeed = gameStateManager.getMaxSpeed();
        float decelTime = gameStateManager.getDecelerationDuration();
        
        // Safeguard against division by zero
        if (maxSpeed <= 0) maxSpeed = 1f;
        
        // PERFORMANCE: Cap maximum speed to avoid ANR issues
        // With very high speeds, the animations can cause the UI to become unresponsive
        float cappedMaxSpeed = Math.min(maxSpeed, 2000f);
        if (maxSpeed != cappedMaxSpeed) {
            Timber.d("[ANIM_SPEED] Capping max speed from %.2f to %.2f to prevent ANR", maxSpeed, cappedMaxSpeed);
            maxSpeed = cappedMaxSpeed;
        }
        
        // PERFORMANCE: Ensure minimum duration to avoid UI thread overload
        // Too short durations can cause excessive frame calculations
        float minDuration = 100f;
        
        // Basic physics: time = distance/speed (adjusted by acceleration/deceleration phases)
        float duration = distance / maxSpeed * 1000f; // Convert to milliseconds
        
        // Add time for acceleration and deceleration phases
        duration += accelTime + decelTime;
        
        // Apply minimum duration safeguard
        if (duration < minDuration) {
            Timber.d("[ANIM_SPEED] Enforcing minimum animation duration: %.2fms (was %.2fms)", minDuration, duration);
            duration = minDuration;
        }
        
        Timber.d("[ANIM_SPEED] Calculated animation duration: %.2fms for distance: %.2f (accel: %.2f, maxSpeed: %.2f, decel: %.2f)", 
                duration, distance, accelTime, maxSpeed, decelTime);
        
        return duration;
    }
    
    /**
     * Cancel all active animations
     */
    public void cancelAllAnimations() {
        Timber.d("[ANIM] Canceling all animations");
        
        // Loop through all robots with active animations
        for (Map.Entry<GameElement, Boolean> entry : new HashMap<>(isProcessingRobotAnimations).entrySet()) {
            GameElement robot = entry.getKey();
            Boolean isProcessing = entry.getValue();
            
            if (isProcessing) {
                // Clear animation position
                robot.setAnimationPosition(robot.getX(), robot.getY());
                isProcessingRobotAnimations.put(robot, false);
            }
        }
        
        // Clear all queues
        robotMoveQueues.clear();
        
        // Request redraw
        if (gameGridView != null) {
            gameGridView.invalidate();
        }
    }
    
    /**
     * Update animation settings (for GameStateManager compatibility)
     */
    public void updateSettings(float accelerationDuration, float maxSpeed, float decelerationDuration, 
                             float overshootPercentage, float springBackDuration, 
                             AnimationCancellationStrategy strategy, long frameDelay) {
        // This is now a compatibility method - animation parameters are internally managed
        Timber.d("[ANIM_FRAMERATE] Animation settings updated: frameDelay=%d ms", frameDelay);
        
        // MEMORY OPTIMIZATION: Reduce object allocation during animations
        if (frameDelay > 16) {
            // Lower animations per second = less memory pressure
            // This is especially important for faster animations that create many object allocations
            int keyframes = (int) Math.max(5, 60 / (frameDelay / 16)); // Reduce keyframes based on framerate
            Timber.d("[ANIM_FRAMERATE] Using %d keyframes per animation instead of default", keyframes);
            
            try {
                // ValueAnimator.setFrameDelay() affects all animations globally,
                // but we found this isn't always reliable, so we also use frame throttling
                ValueAnimator.setFrameDelay(frameDelay);
            } catch (Exception e) {
                Timber.e(e, "[ANIM_FRAMERATE] Failed to set global frame delay");
            }
        }
    }
    
    /**
     * Information about a robot move
     */
    private static class RobotMoveInfo {
        final GameElement robot;
        final int startX, startY, endX, endY;
        final Runnable completionCallback;
        
        RobotMoveInfo(GameElement robot, int startX, int startY, int endX, int endY, Runnable completionCallback) {
            this.robot = robot;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.completionCallback = completionCallback;
        }
    }
}
