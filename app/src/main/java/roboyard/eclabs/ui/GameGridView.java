package roboyard.eclabs.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.content.ContextCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.HashMap;

import roboyard.eclabs.GridElement;
import roboyard.eclabs.R;
import timber.log.Timber;

/**
 * Custom View that renders the game board grid and handles touch interactions.
 * Provides proper accessibility support for TalkBack.
 */
public class GameGridView extends View {
    private GameStateManager gameStateManager;
    private Paint cellPaint;
    private Paint robotPaint;
    private Paint targetPaint;
    private Paint textPaint;
    private Paint gridPaint;
    private float cellSize;
    
    // Grid dimensions
    private int gridWidth = 14;
    private int gridHeight = 14;
    
    // For accessibility - track the focused cell
    private int focusedX = -1;
    private int focusedY = -1;
    
    // Robot drawables for each color
    private Drawable redRobotRight, yellowRobotRight, blueRobotRight, greenRobotRight;
    private Drawable redRobotLeft, yellowRobotLeft, blueRobotLeft, greenRobotLeft;
    
    // Wall drawables
    private Drawable wallHorizontal;
    private Drawable wallVertical;
    
    // Target drawables for each color
    private Drawable targetRedDrawable;     // cr
    private Drawable targetGreenDrawable;   // cv
    private Drawable targetBlueDrawable;    // cb
    private Drawable targetYellowDrawable;  // cj
    private Drawable targetMultiDrawable;   // cm
    
    // Robot animation configuration
    private static final float SELECTED_ROBOT_SCALE = 1.5f; // 50% larger
    private static final float FOCUSED_ROBOT_SCALE = 1.3f; // 30% larger when focused but not selected
    private boolean enableRobotAnimation = true; // Can be toggled in settings
    private HashMap<GameElement, Float> robotScaleMap = new HashMap<>(); // Track current scale for each robot
    private GameElement focusedRobot = null; // Currently focused (hovered) robot
    private android.view.animation.DecelerateInterpolator easeInterpolator = new android.view.animation.DecelerateInterpolator(1.5f); // Ease function
    private android.os.Handler animationHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private long animationDuration = 300; // Animation duration in ms
    private Fragment fragment;

    /**
     * Constructor for programmatic creation
     */
    public GameGridView(Context context) {
        super(context);
        init(context);
    }
    
    /**
     * Constructor from XML layout
     */
    public GameGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    /**
     * Constructor from XML layout with style
     */
    public GameGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    /**
     * Initialize paint objects and accessibility support
     */
    private void init(Context context) {
        // Initialize paints
        cellPaint = new Paint();
        cellPaint.setStyle(Paint.Style.FILL);
        
        robotPaint = new Paint();
        robotPaint.setStyle(Paint.Style.FILL);
        
        targetPaint = new Paint();
        targetPaint.setStyle(Paint.Style.FILL);
        
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        gridPaint = new Paint();
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setColor(Color.rgb(40, 40, 70));
        
        // Load robot drawables
        redRobotRight = ContextCompat.getDrawable(context, R.drawable.robot_red_right);
        redRobotLeft = ContextCompat.getDrawable(context, R.drawable.robot_red_left);
        
        yellowRobotRight = ContextCompat.getDrawable(context, R.drawable.robot_yellow_right);
        yellowRobotLeft = ContextCompat.getDrawable(context, R.drawable.robot_yellow_left);
        
        blueRobotRight = ContextCompat.getDrawable(context, R.drawable.robot_blue_right);
        blueRobotLeft = ContextCompat.getDrawable(context, R.drawable.robot_blue_left);
        
        greenRobotRight = ContextCompat.getDrawable(context, R.drawable.robot_green_right);
        greenRobotLeft = ContextCompat.getDrawable(context, R.drawable.robot_green_left);
        
        // Load wall drawables
        wallHorizontal = ContextCompat.getDrawable(context, R.drawable.mh);
        wallVertical = ContextCompat.getDrawable(context, R.drawable.mv);
        
        // Load target drawables
        targetRedDrawable = ContextCompat.getDrawable(context, R.drawable.cr);
        targetGreenDrawable = ContextCompat.getDrawable(context, R.drawable.cv);
        targetBlueDrawable = ContextCompat.getDrawable(context, R.drawable.cb);
        targetYellowDrawable = ContextCompat.getDrawable(context, R.drawable.cj);
        targetMultiDrawable = ContextCompat.getDrawable(context, R.drawable.cm);
        
        // Set up accessibility support
        setFocusable(true);
        setClickable(true);
        
        // Configure accessibility delegate for TalkBack support
        ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                // Provide information about the grid for screen readers
                info.setClassName(GameGridView.class.getName());
                info.setContentDescription("Game board grid");
            }
            
            @Override
            public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
                super.onInitializeAccessibilityEvent(host, event);
                event.setClassName(GameGridView.class.getName());
            }
        });
    }
    
    /**
     * Set the fragment that contains this view
     * @param fragment Owner fragment
     */
    public void setFragment(Fragment fragment) {
        this.fragment = fragment;
        
        // If gameStateManager already exists, update observation
        if (gameStateManager != null && fragment != null) {
            setupRobotAnimationObservers();
        }
    }
    
    /**
     * Set the game state manager
     * @param manager Game state manager
     */
    public void setGameStateManager(GameStateManager manager) {
        this.gameStateManager = manager;
        
        // If fragment is available, set up animations
        if (fragment != null) {
            setupRobotAnimationObservers();
        }
        
        // Update width and height based on state
        if (gameStateManager != null && gameStateManager.getCurrentState().getValue() != null) {
            GameState state = gameStateManager.getCurrentState().getValue();
            gridWidth = state.getWidth();
            gridHeight = state.getHeight();
        }
        invalidate();
    }
    
    /**
     * Set up robot animation observers
     */
    private void setupRobotAnimationObservers() {
        if (fragment != null && gameStateManager != null) {
            gameStateManager.getCurrentState().observe(fragment.getViewLifecycleOwner(), state -> {
                if (state != null) {
                    GameElement selectedRobot = state.getSelectedRobot();
                    
                    // Update robot scales based on selection
                    for (GameElement element : state.getGameElements()) {
                        if (element.getType() == GameElement.TYPE_ROBOT) {
                            if (element == selectedRobot) {
                                // Robot selected - animate growth
                                animateRobotScale(element, 1.0f, SELECTED_ROBOT_SCALE);
                            } else if (robotScaleMap.containsKey(element) && robotScaleMap.get(element) > 1.0f) {
                                // Robot deselected - animate shrinking
                                animateRobotScale(element, robotScaleMap.get(element), 1.0f);
                            }
                        }
                    }
                }
            });
        }
    }
    
    /**
     * Animate robot scaling with easing function
     * @param robot The robot to animate
     * @param fromScale Starting scale
     * @param toScale Target scale
     */
    private void animateRobotScale(GameElement robot, float fromScale, float toScale) {
        if (robot == null || !enableRobotAnimation) return;
        
        // Initialize scale if not present
        if (!robotScaleMap.containsKey(robot)) {
            robotScaleMap.put(robot, fromScale);
        }
        
        // Create value animator with easing
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(fromScale, toScale);
        animator.setDuration(animationDuration);
        animator.setInterpolator(easeInterpolator); // Apply ease function
        animator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            robotScaleMap.put(robot, scale);
            invalidate(); // Redraw view
        });
        animator.start();
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        
        // Calculate cell size to fit the grid
        float cellWidth = (float) width / gridWidth;
        float cellHeight = (float) height / gridHeight;
        cellSize = Math.min(cellWidth, cellHeight);
        
        // Adjust view size to maintain aspect ratio
        int newWidth = (int) (cellSize * gridWidth);
        int newHeight = (int) (cellSize * gridHeight);
        setMeasuredDimension(newWidth, newHeight);
    }
    
    /**
     * Determine if a wall at the given position is horizontal or vertical
     * @param state Current game state
     * @param x Wall X position
     * @param y Wall Y position
     * @return true if horizontal wall, false if vertical wall
     */
    private boolean isHorizontalWall(GameState state, int x, int y) {
        // Check cells to the left and right
        boolean hasLeftWall = (x > 0) && (state.getCellType(x-1, y) == 1);
        boolean hasRightWall = (x < gridWidth-1) && (state.getCellType(x+1, y) == 1);
        
        // If has walls on either side, it's likely horizontal
        if (hasLeftWall || hasRightWall) {
            return true;
        }
        
        // Otherwise check top and bottom
        boolean hasTopWall = (y > 0) && (state.getCellType(x, y-1) == 1);
        boolean hasBottomWall = (y < gridHeight-1) && (state.getCellType(x, y+1) == 1);
        
        // If has vertical neighbors, it's likely vertical
        if (hasTopWall || hasBottomWall) {
            return false;
        }
        
        // Default to horizontal for isolated walls
        return true;
    }
    
    /**
     * Get the appropriate target drawable based on target color
     * @param targetElement The target game element
     * @return The drawable to use for this target
     */
    private Drawable getTargetDrawable(GameElement targetElement) {
        if (targetElement == null) {
            return targetMultiDrawable; // Default to multi-colored target
        }
        
        switch (targetElement.getColor()) {
            case 0: return targetRedDrawable;
            case 1: return targetGreenDrawable;
            case 2: return targetBlueDrawable;
            case 3: return targetYellowDrawable;
            default: return targetMultiDrawable;
        }
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (gameStateManager == null || gameStateManager.getCurrentState().getValue() == null) {
            return;
        }
        
        GameState state = gameStateManager.getCurrentState().getValue();
        gridWidth = state.getWidth();
        gridHeight = state.getHeight();
        
        // Draw grid cells - board background
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                float left = x * cellSize;
                float top = y * cellSize;
                float right = left + cellSize;
                float bottom = top + cellSize;
                
                // Draw empty cell background
                cellPaint.setColor(Color.rgb(30, 30, 60));
                canvas.drawRect(left, top, right, bottom, cellPaint);
                
                // Draw grid lines
                gridPaint.setColor(Color.rgb(40, 40, 70));
                canvas.drawRect(left, top, right, bottom, gridPaint);
                
                // Highlight focused cell for accessibility
                if (x == focusedX && y == focusedY) {
                    cellPaint.setColor(Color.rgb(80, 80, 120));
                    canvas.drawRect(left, top, right, bottom, cellPaint);
                }
            }
        }
        
        // Draw targets
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_TARGET) {
                float left = element.getX() * cellSize;
                float top = element.getY() * cellSize;
                float right = left + cellSize;
                float bottom = top + cellSize;
                
                Drawable targetDrawable = getTargetDrawable(element);
                if (targetDrawable != null) {
                    // Draw target using drawable
                    targetDrawable.setBounds((int)left, (int)top, (int)right, (int)bottom);
                    targetDrawable.draw(canvas);
                } else {
                    // Fallback to color
                    cellPaint.setColor(Color.rgb(60, 60, 90));
                    canvas.drawRect(left, top, right, bottom, cellPaint);
                }
            }
        }
        
        // Draw walls between cells, not on cells
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_HORIZONTAL_WALL) {
                // Horizontal walls are drawn between rows (separating cells vertically)
                int x = element.getX();
                int y = element.getY();
                
                float left = x * cellSize;
                float top = y * cellSize; // Top of the lower cell
                float right = left + cellSize;
                float wallThickness = cellSize / 8; // Thinner walls look better
                
                // Draw horizontal wall - between y and y+1
                cellPaint.setColor(Color.DKGRAY);
                canvas.drawRect(left, top - wallThickness/2, right, top + wallThickness/2, cellPaint);
                
                if (wallHorizontal != null) {
                    wallHorizontal.setBounds((int)left, (int)(top - wallThickness/2), 
                                             (int)right, (int)(top + wallThickness/2));
                    wallHorizontal.draw(canvas);
                }
            } 
            else if (element.getType() == GameElement.TYPE_VERTICAL_WALL) {
                // Vertical walls are drawn between columns (separating cells horizontally)
                int x = element.getX();
                int y = element.getY();
                
                float left = x * cellSize; // Left of the right cell
                float top = y * cellSize;
                float bottom = top + cellSize;
                float wallThickness = cellSize / 8;
                
                // Draw vertical wall - between x and x+1
                cellPaint.setColor(Color.DKGRAY);
                canvas.drawRect(left - wallThickness/2, top, left + wallThickness/2, bottom, cellPaint);
                
                if (wallVertical != null) {
                    wallVertical.setBounds((int)(left - wallThickness/2), (int)top, 
                                           (int)(left + wallThickness/2), (int)bottom);
                    wallVertical.draw(canvas);
                }
            }
        }
        
        // Draw robots (on top of walls and targets)
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_ROBOT) {
                float left = element.getX() * cellSize;
                float top = element.getY() * cellSize;
                float right = left + cellSize;
                float bottom = top + cellSize;
                
                // Select appropriate robot drawable based on color
                Drawable robotDrawable = null;
                
                switch (element.getColor()) {
                    case 0: // RED
                        robotDrawable = redRobotRight;
                        break;
                    case 1: // GREEN
                        robotDrawable = greenRobotRight;
                        break;
                    case 2: // BLUE
                        robotDrawable = blueRobotRight;
                        break;
                    case 3: // YELLOW
                        robotDrawable = yellowRobotRight;
                        break;
                }
                
                // Get current scale for this robot (default to 1.0 if not set)
                float robotScale = robotScaleMap.containsKey(element) ? 
                        robotScaleMap.get(element) : 1.0f;
                
                // Highlight and scale selected or focused robot
                boolean isSelected = element.isSelected() || (state.getSelectedRobot() == element);
                boolean isFocused = (element == focusedRobot);
                
                if (isSelected || isFocused) {
                    // Create a highlight rectangle around the robot
                    Paint highlightPaint = new Paint();
                    highlightPaint.setColor(isSelected ? Color.WHITE : Color.LTGRAY);
                    highlightPaint.setStyle(Paint.Style.STROKE);
                    highlightPaint.setStrokeWidth(isSelected ? 4 : 2);
                    canvas.drawRect(left, top, right, bottom, highlightPaint);
                    
                    // If robot isn't already being animated to proper scale, start animation
                    if (enableRobotAnimation) {
                        float targetScale = isSelected ? SELECTED_ROBOT_SCALE : FOCUSED_ROBOT_SCALE;
                        if (robotScale < targetScale) {
                            animateRobotScale(element, robotScale, targetScale);
                        }
                    }
                } else if (robotScale > 1.0f && enableRobotAnimation) {
                    // Robot not selected or focused but still enlarged - animate back to normal
                    animateRobotScale(element, robotScale, 1.0f);
                }
                
                // Calculate the center point of the robot
                float centerX = left + cellSize / 2;
                float centerY = top + cellSize / 2;
                
                // Save canvas state before scaling
                canvas.save();
                
                // Apply current scale from the center of the robot
                canvas.scale(robotScale, robotScale, centerX, centerY);
                
                // Calculate the scaled bounds
                float scaledSize = cellSize / robotScale;
                float scaledLeft = centerX - scaledSize / 2;
                float scaledTop = centerY - scaledSize / 2;
                float scaledRight = centerX + scaledSize / 2;
                float scaledBottom = centerY + scaledSize / 2;
                
                // Draw the robot using the drawable
                if (robotDrawable != null) {
                    robotDrawable.setBounds((int)scaledLeft, (int)scaledTop, 
                            (int)scaledRight, (int)scaledBottom);
                    robotDrawable.draw(canvas);
                } else {
                    // Fallback to colored circle
                    float radius = scaledSize / 2.5f;
                    switch (element.getColor()) {
                        case 0: robotPaint.setColor(Color.RED); break;
                        case 1: robotPaint.setColor(Color.GREEN); break;
                        case 2: robotPaint.setColor(Color.BLUE); break;
                        case 3: robotPaint.setColor(Color.YELLOW); break;
                    }
                    canvas.drawCircle(centerX, centerY, radius, robotPaint);
                }
                
                // Restore canvas to original state
                canvas.restore();
            }
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameStateManager == null) {
            return false;
        }
        
        float x = event.getX();
        float y = event.getY();
        
        // Convert to grid coordinates
        int gridX = (int) (x / cellSize);
        int gridY = (int) (y / cellSize);
        
        // Ensure coordinates are within bounds
        if (gridX < 0 || gridY < 0 || gridX >= gridWidth || gridY >= gridHeight) {
            return false;
        }
        
        // Capture action for accessibility announcements
        int action = event.getAction();
        GameState state = gameStateManager.getCurrentState().getValue();
        
        // Handle accessibility announcements for selection and movement
        if (action == MotionEvent.ACTION_DOWN && state != null) {
            GameElement robot = state.getRobotAt(gridX, gridY);
            if (robot != null) {
                announceForAccessibility("Selected " + getRobotDescription(robot));
            }
        } else if (action == MotionEvent.ACTION_UP && state != null) {
            GameElement selectedRobot = state.getSelectedRobot();
            if (selectedRobot != null) {
                int oldX = selectedRobot.getX();
                int oldY = selectedRobot.getY();
                
                // Let GameStateManager handle the touch which will update counters properly
                gameStateManager.handleGridTouch(gridX, gridY, action);
                
                // Check if robot moved by comparing old and new positions
                Timber.d("[GOAL DEBUG] Selected robot moved from " + oldX + ", " + oldY + " to " + selectedRobot.getX() + ", " + selectedRobot.getY());
                if (oldX != selectedRobot.getX() || oldY != selectedRobot.getY()) {
                    if (state.checkCompletion()) {
                        Timber.d("[GOAL DEBUG] Goal reached! Game complete in " + gameStateManager.getMoveCount().getValue() + " moves and " + gameStateManager.getSquaresMoved().getValue() + " squares moved");
                        
                        // Critical fix: Tell the GameStateManager the game is complete
                        gameStateManager.setGameComplete(true);
                        
                        announceForAccessibility("Goal reached! Game complete in " + 
                            gameStateManager.getMoveCount().getValue() + " moves and " +
                            gameStateManager.getSquaresMoved().getValue() + " squares moved");
                    } else {
                        Timber.d("[GOAL DEBUG] Robot moved");
                        announceForAccessibility(getRobotDescription(selectedRobot) + " moved");
                    }
                }
            } else {
                // No robot selected, just handle the touch
                gameStateManager.handleGridTouch(gridX, gridY, action);
            }
        } else {
            // Other actions, just handle the touch
            gameStateManager.handleGridTouch(gridX, gridY, action);
        }
        
        // Redraw the view
        invalidate();
        
        // Consume the event
        return true;
    }
    
    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        // For TalkBack exploration
        float x = event.getX();
        float y = event.getY();
        
        // Convert to grid coordinates
        int gridX = (int) (x / cellSize);
        int gridY = (int) (y / cellSize);
        
        // Ensure coordinates are within bounds
        if (gridX < 0 || gridY < 0 || gridX >= gridWidth || gridY >= gridHeight) {
            // Clear focused robot when moving out of bounds
            if (focusedRobot != null) {
                focusedRobot = null;
                invalidate();
            }
            return super.dispatchHoverEvent(event);
        }
        
        if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER ||
            event.getAction() == MotionEvent.ACTION_HOVER_MOVE) {
            
            // Update focused cell if changed
            if (focusedX != gridX || focusedY != gridY) {
                focusedX = gridX;
                focusedY = gridY;
                
                // Check for robot at this position
                GameState state = gameStateManager.getCurrentState().getValue();
                if (state != null) {
                    GameElement previousFocusedRobot = focusedRobot;
                    focusedRobot = state.getRobotAt(gridX, gridY);
                    
                    // Animate the newly focused robot to grow
                    if (focusedRobot != null && enableRobotAnimation) {
                        // Only animate if this isn't the selected robot (which is already scaled larger)
                        if (state.getSelectedRobot() != focusedRobot) {
                            float currentScale = robotScaleMap.containsKey(focusedRobot) ? 
                                    robotScaleMap.get(focusedRobot) : 1.0f;
                            if (currentScale < FOCUSED_ROBOT_SCALE) {
                                animateRobotScale(focusedRobot, currentScale, FOCUSED_ROBOT_SCALE);
                            }
                        }
                    }
                    
                    // Shrink the previously focused robot if it's not the new focus or selected
                    if (previousFocusedRobot != null && previousFocusedRobot != focusedRobot && 
                            state.getSelectedRobot() != previousFocusedRobot && enableRobotAnimation) {
                        float currentScale = robotScaleMap.containsKey(previousFocusedRobot) ? 
                                robotScaleMap.get(previousFocusedRobot) : 1.0f;
                        if (currentScale > 1.0f) {
                            animateRobotScale(previousFocusedRobot, currentScale, 1.0f);
                        }
                    }
                    
                    invalidate();
                    
                    // Announce what's at this position
                    if (focusedRobot != null) {
                        announceForAccessibility(getRobotDescription(focusedRobot));
                    }
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
            // When hover exits, shrink any focused robot that isn't selected
            if (focusedRobot != null) {
                GameState state = gameStateManager.getCurrentState().getValue();
                if (state != null && state.getSelectedRobot() != focusedRobot && enableRobotAnimation) {
                    float currentScale = robotScaleMap.containsKey(focusedRobot) ? 
                            robotScaleMap.get(focusedRobot) : 1.0f;
                    if (currentScale > 1.0f) {
                        animateRobotScale(focusedRobot, currentScale, 1.0f);
                    }
                }
                focusedRobot = null;
                invalidate();
            }
        }
        
        return super.dispatchHoverEvent(event);
    }
    
    /**
     * Get a description of a robot for accessibility announcements
     */
    private String getRobotDescription(GameElement robot) {
        if (robot == null) {
            return "Unknown robot";
        }
        
        String color = "Unknown";
        switch (robot.getColor()) {
            case 0: color = "Red"; break;
            case 1: color = "Green"; break;
            case 2: color = "Blue"; break;
            case 3: color = "Yellow"; break;
        }
        
        return color + " robot at position " + robot.getX() + ", " + robot.getY();
    }
    
    /**
     * Set grid elements directly from an ArrayList of GridElements
     * This is used by ModernGameFragment to update the grid view
     * @param gridElements List of grid elements to display
     */
    public void setGridElements(ArrayList<GridElement> gridElements) {
        if (gameStateManager == null || gameStateManager.getCurrentState().getValue() == null) {
            return;
        }
        
        GameState state = gameStateManager.getCurrentState().getValue();
        
        // Update the grid dimensions
        gridWidth = state.getWidth();
        gridHeight = state.getHeight();
        
        // Force redraw
        invalidate();
    }
}
