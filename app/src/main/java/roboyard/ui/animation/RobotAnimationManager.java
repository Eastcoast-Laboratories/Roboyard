package roboyard.ui.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import roboyard.eclabs.ui.GameElement;
import roboyard.logic.core.GameState;
import roboyard.ui.components.GameGridView;
import roboyard.ui.components.GameStateManager;
import timber.log.Timber;

/**
 * Manages physics-based animations for robot movements with acceleration,
 * max speed, deceleration, and spring-back overshoot effects.
 * Includes queue management for sequential movements and interruption handling.
 */
public class RobotAnimationManager {
    // Animation configuration parameters
    private float accelerationDuration = 150f; // ms
    private float maxSpeed = 1000f; // pixels per second
    private float decelerationDuration = 200f; // ms
    private float overshootPercentage = 0.15f; // 15% overshoot
    private float springBackDuration = 200f; // ms
    private AnimationCancellationStrategy cancellationStrategy = AnimationCancellationStrategy.JUMP_TO_END;
    
    // Reference to game components
    private final GameStateManager gameStateManager;
    private GameGridView gameGridView;
    
    // Animation state tracking
    private final Map<GameElement, Queue<MoveCommand>> robotMoveQueues;
    private final Map<GameElement, ValueAnimator> activeAnimations = new HashMap<>();
    private boolean isProcessingResetOrBack = false;
    
    /**
     * Cancellation strategies for handling animation interruptions
     */
    public enum AnimationCancellationStrategy {
        COMPLETE_CURRENT, // Finish current animation then discard queue
        JUMP_TO_END,      // Jump to end position of current animation then discard queue
        IMMEDIATE_CANCEL  // Cancel all animations immediately (might look jarring)
    }
    
    /**
     * Represents a queued robot movement command
     */
    private static class MoveCommand {
        final int startX, startY, endX, endY;
        final Runnable completionCallback;
        
        MoveCommand(int startX, int startY, int endX, int endY, Runnable completionCallback) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.completionCallback = completionCallback;
        }
    }
    
    /**
     * Parameters for configuring the physics-based animation
     */
    static class AnimationParameters {
        // Configurable settings
        float accelerationDuration;
        float maxSpeed;
        float decelerationDuration;
        float overshootPercentage;
        float springBackDuration;
        
        // Calculated values
        float totalDistance;
        float cruiseDistance;
        float cruiseDuration;
        float totalDuration;
        float cellSize; // Size of a game cell in pixels
    }
    
    /**
     * Creates a new RobotAnimationManager
     * @param gameStateManager Reference to the game state manager
     */
    public RobotAnimationManager(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
        this.robotMoveQueues = new HashMap<>();
        Timber.d("[ANIM] RobotAnimationManager initialized");
    }
    
    /**
     * Sets the GameGridView for rendering animations
     * @param gameGridView The game grid view
     */
    public void setGameGridView(GameGridView gameGridView) {
        Timber.d("[ANIM] Setting GameGridView in RobotAnimationManager: %s", 
                gameGridView != null ? "valid" : "null");
        this.gameGridView = gameGridView;
    }
    
    /**
     * Updates animation settings
     */
    public void updateSettings(float accelerationDuration, float maxSpeed, 
                             float decelerationDuration, float overshootPercentage, 
                             float springBackDuration, AnimationCancellationStrategy strategy) {
        this.accelerationDuration = accelerationDuration;
        this.maxSpeed = maxSpeed;
        this.decelerationDuration = decelerationDuration;
        this.overshootPercentage = overshootPercentage;
        this.springBackDuration = springBackDuration;
        this.cancellationStrategy = strategy;
    }
    
    /**
     * Queue a robot movement for execution
     * @param robot The robot to move
     * @param startX Starting X position
     * @param startY Starting Y position
     * @param endX Ending X position
     * @param endY Ending Y position
     * @param completionCallback Callback to run when movement completes
     */
    public void queueRobotMove(GameElement robot, int startX, int startY, 
                              int endX, int endY, Runnable completionCallback) {
        Timber.d("[ANIM] Robot movement QUEUED: (%d,%d) to (%d,%d)", startX, startY, endX, endY);
        
        if (isProcessingResetOrBack || gameGridView == null) {
            Timber.d("[ANIM] Movement skipped: reset=%b, gridView=%s", 
                   isProcessingResetOrBack, gameGridView != null ? "ok" : "null");
            if (completionCallback != null) {
                // Still execute callback even if we can't animate
                completionCallback.run();
            }
            return;
        }
        
        // Create a queue for this robot if it doesn't exist
        if (!robotMoveQueues.containsKey(robot)) {
            robotMoveQueues.put(robot, new LinkedList<>());
        }
        
        // Add movement to queue
        robotMoveQueues.get(robot).add(new MoveCommand(startX, startY, endX, endY, completionCallback));
        
        // Start processing the queue if this robot isn't currently animating
        if (!activeAnimations.containsKey(robot)) {
            processNextMove(robot);
        }
    }
    
    /**
     * Process the next queued movement for a robot
     * @param robot The robot to process
     */
    private void processNextMove(GameElement robot) {
        if (!robotMoveQueues.containsKey(robot) || robotMoveQueues.get(robot).isEmpty()) {
            return; // No more moves to process
        }
        
        MoveCommand nextMove = robotMoveQueues.get(robot).poll();
        Timber.d("[ANIM] Robot movement STARTED: (%d,%d) to (%d,%d)", 
                 nextMove.startX, nextMove.startY, nextMove.endX, nextMove.endY);
        
        animateRobotMove(robot, nextMove.startX, nextMove.startY, 
                nextMove.endX, nextMove.endY, () -> {
            // When animation completes, run the completion callback
            if (nextMove.completionCallback != null) {
                Timber.d("[ANIM] Robot movement COMPLETED: (%d,%d) to (%d,%d)", 
                        nextMove.startX, nextMove.startY, nextMove.endX, nextMove.endY);
                nextMove.completionCallback.run();
            }
            
            // Process the next move in the queue
            processNextMove(robot);
        });
    }
    
    /**
     * Animate a single robot move with physics-based motion
     */
    private void animateRobotMove(GameElement robot, int startX, int startY, 
                                 int endX, int endY, Runnable onComplete) {
        // Calculate animation parameters based on distance
        AnimationParameters params = calculateParameters(startX, startY, endX, endY);
        
        // Create a temporary visual position for the robot during animation
        float animStartX = startX * params.cellSize;
        float animStartY = startY * params.cellSize;
        
        // Account for game grid view offset
        if (gameGridView != null) {
            int gridWidth = gameGridView.getWidth() / (int)params.cellSize;
            int gridHeight = gameGridView.getHeight() / (int)params.cellSize;
            
            // Calculate offset from grid edges
            float offsetX = (gameGridView.getWidth() - (gridWidth * params.cellSize)) / 2;
            float offsetY = (gameGridView.getHeight() - (gridHeight * params.cellSize)) / 2;
            
            animStartX += offsetX;
            animStartY += offsetY;
            
            // Set animation end coordinates with offset
            float animEndX = endX * params.cellSize + offsetX;
            float animEndY = endY * params.cellSize + offsetY;
            
            Timber.d("[ANIM] Full animation path: (%.2f,%.2f) -> (%.2f,%.2f)", 
                    animStartX, animStartY, animEndX, animEndY);
        }
        
        // CRITICAL: Start with the correct animation position
        robot.setAnimationPosition(animStartX, animStartY);
        
        // Create path with potential overshoot
        Path animationPath = createAnimationPath(startX, startY, endX, endY, params);
        
        // Create the animator to follow the path
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration((long)params.totalDuration);
        
        // Fixed 1 second duration for testing
        animator.setDuration(1000);
        Timber.d("[ANIM] Created animator with duration: 1000ms");
        
        // Use a physics-based interpolator
        animator.setInterpolator(new PhysicsInterpolator(params));
        
        animator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            
            // Calculate current position on path including overshoot/spring-back
            PathMeasure pathMeasure = new PathMeasure(animationPath, false);
            float[] pos = new float[2];
            pathMeasure.getPosTan(pathMeasure.getLength() * fraction, pos, null);
            
            // Update visual position only (logical position remains unchanged until complete)
            robot.setAnimationPosition(pos[0], pos[1]);
            
            // Trigger redraw
            if (gameGridView != null) {
                gameGridView.invalidate();  // Force refresh of view
                Timber.d("[ANIM] Animation progress: %.0f%%", fraction * 100);
            }
        });
        
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Animation complete - update actual robot position
                robot.clearAnimationPosition();
                activeAnimations.remove(robot);
                
                // Trigger redraw
                if (gameGridView != null) {
                    gameGridView.invalidate();
                }
                
                // Execute completion callback
                if (onComplete != null) {
                    new Handler(Looper.getMainLooper()).post(onComplete);
                }
            }
        });
        
        activeAnimations.put(robot, animator);
        animator.start();
    }
    
    /**
     * Calculate animation parameters based on the movement distance
     */
    private AnimationParameters calculateParameters(int startX, int startY, int endX, int endY) {
        AnimationParameters params = new AnimationParameters();
        
        // Copy current settings
        params.accelerationDuration = this.accelerationDuration;
        params.maxSpeed = this.maxSpeed;
        params.decelerationDuration = this.decelerationDuration;
        params.overshootPercentage = this.overshootPercentage;
        params.springBackDuration = this.springBackDuration;
        
        // Get cell size from game grid view
        params.cellSize = gameGridView != null ? gameGridView.getCellSize() : 100f;
        
        // Calculate physical distance in pixels
        float dx = (endX - startX) * params.cellSize;
        float dy = (endY - startY) * params.cellSize;
        params.totalDistance = (float) Math.sqrt(dx * dx + dy * dy);
        
        // Calculate time needed at max speed
        float accDistance = 0.5f * params.maxSpeed * (params.accelerationDuration / 1000f);
        float decDistance = 0.5f * params.maxSpeed * (params.decelerationDuration / 1000f);
        
        // If distance is too short for full acceleration and deceleration
        if (accDistance + decDistance > params.totalDistance) {
            // Scale down acceleration and deceleration phases
            float scaleFactor = params.totalDistance / (accDistance + decDistance);
            params.accelerationDuration *= scaleFactor;
            params.decelerationDuration *= scaleFactor;
            params.cruiseDistance = 0;
            params.cruiseDuration = 0;
        } else {
            // Calculate cruise phase
            params.cruiseDistance = params.totalDistance - accDistance - decDistance;
            params.cruiseDuration = (params.cruiseDistance / params.maxSpeed) * 1000f;
        }
        
        // Total animation duration including overshoot/spring-back
        params.totalDuration = params.accelerationDuration + params.cruiseDuration + 
                              params.decelerationDuration + params.springBackDuration;
        
        return params;
    }
    
    /**
     * Create a path for the robot to follow with potential overshoot
     */
    private Path createAnimationPath(int startX, int startY, int endX, int endY, AnimationParameters params) {
        Path path = new Path();
        
        // Convert grid coordinates to pixel coordinates
        float startXPx = startX * params.cellSize;
        float startYPx = startY * params.cellSize;
        float endXPx = endX * params.cellSize;
        float endYPx = endY * params.cellSize;
        
        // Calculate direction vector
        float dx = endXPx - startXPx;
        float dy = endYPx - startYPx;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        // Normalize direction vector
        if (distance > 0) {
            dx /= distance;
            dy /= distance;
        }
        
        // Calculate overshoot point
        float overshootDistance = distance * params.overshootPercentage;
        float overshootX = endXPx + dx * overshootDistance;
        float overshootY = endYPx + dy * overshootDistance;
        
        // Start at beginning
        path.moveTo(startXPx, startYPx);
        
        // Straight line to destination
        path.lineTo(endXPx, endYPx);
        
        // Add overshoot and spring-back
        if (params.overshootPercentage > 0) {
            // Overshoot
            path.lineTo(overshootX, overshootY);
            
            // Spring back to destination
            path.lineTo(endXPx, endYPx);
        }
        
        return path;
    }
    
    /**
     * Cancel an animation for a specific robot
     */
    public void cancelAnimation(GameElement robot) {
        if (!activeAnimations.containsKey(robot)) {
            return;
        }
        
        ValueAnimator animator = activeAnimations.get(robot);
        
        switch (cancellationStrategy) {
            case COMPLETE_CURRENT:
                // Let current animation complete but clear the queue
                if (robotMoveQueues.containsKey(robot)) {
                    robotMoveQueues.get(robot).clear();
                }
                break;
                
            case JUMP_TO_END:
                // Jump to end position
                animator.end(); // This calls onAnimationEnd which updates the position
                break;
                
            case IMMEDIATE_CANCEL:
                // Cancel immediately
                animator.cancel();
                // Clear animation position
                robot.clearAnimationPosition();
                // Remove from active animations
                activeAnimations.remove(robot);
                break;
        }
    }
    
    /**
     * Immediately cancel all animations and clear all queued movements
     * Used when resetting the game or navigating back
     */
    public void cancelAllAnimations() {
        isProcessingResetOrBack = true;
        
        // Cancel all active animations according to strategy
        for (Map.Entry<GameElement, ValueAnimator> entry : new HashMap<>(activeAnimations).entrySet()) {
            cancelAnimation(entry.getKey());
        }
        
        // Clear all queued movements
        robotMoveQueues.clear();
        
        // Reset all robots to remove any animation positions
        GameState state = gameStateManager.getCurrentState().getValue();
        if (state != null) {
            for (GameElement element : state.getGameElements()) {
                if (element.getType() == GameElement.TYPE_ROBOT) {
                    element.clearAnimationPosition();
                }
            }
        }
        
        // Trigger redraw
        if (gameGridView != null) {
            gameGridView.invalidate();
        }
        
        isProcessingResetOrBack = false;
    }
    
    /**
     * Handle robot selection changes while animations are active
     */
    public void handleRobotSelectionChange(GameElement newSelectedRobot) {
        if (newSelectedRobot == null) return;
        
        // Continue any animations for the newly selected robot
        // but cancel animations for other robots
        for (GameElement robot : new ArrayList<>(activeAnimations.keySet())) {
            if (robot != newSelectedRobot) {
                cancelAnimation(robot);
            }
        }
    }
    
    /**
     * Check if any animations are currently running
     */
    public boolean hasActiveAnimations() {
        return !activeAnimations.isEmpty();
    }
    
    /**
     * Check if any animations are queued
     */
    public boolean hasQueuedAnimations() {
        for (Queue<MoveCommand> queue : robotMoveQueues.values()) {
            if (!queue.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
