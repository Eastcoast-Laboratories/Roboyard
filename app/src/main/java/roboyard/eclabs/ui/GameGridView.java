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
import android.view.accessibility.AccessibilityManager;

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
    private static final float FOCUSED_ROBOT_SCALE = 1.2f; // 20% larger when focused but not selected
    private boolean enableRobotAnimation = true; // Can be toggled in settings
    private HashMap<GameElement, Float> robotScaleMap = new HashMap<>(); // Track current scale for each robot
    private GameElement focusedRobot = null; // Currently focused (hovered) robot
    private android.view.animation.DecelerateInterpolator easeInterpolator = new android.view.animation.DecelerateInterpolator(1.5f); // Ease function
    private android.os.Handler animationHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private long animationDuration = 300; // Animation duration in ms
    private Fragment fragment;
    
    // For hover events
    private int lastHoverX = -1;
    private int lastHoverY = -1;
    private int highlightedX = -1;
    private int highlightedY = -1;

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
                
                // Highlight hovered cell
                if (x == highlightedX && y == highlightedY) {
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
                // Draw the robot using the drawable
                drawRobotWithGraphics(canvas, element, state);
            }
        }
    }
    
    /**
     * Draw a robot on the canvas using its original graphics
     * @param canvas Canvas to draw on
     * @param robot Robot to draw
     * @param state Current game state
     */
    private void drawRobotWithGraphics(Canvas canvas, GameElement robot, GameState state) {
        int x = robot.getX();
        int y = robot.getY();
        
        float left = x * cellSize;
        float top = y * cellSize;
        float right = left + cellSize;
        float bottom = top + cellSize;
        
        // Select appropriate robot drawable based on color
        Drawable robotDrawable = null;
        
        switch (robot.getColor()) {
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
        
        // Get the current scale from robotScaleMap, or use default scale based on selection
        float scale = 1.0f;
        if (robotScaleMap.containsKey(robot)) {
            // Use the animated scale if available
            scale = robotScaleMap.get(robot);
        } else {
            // Otherwise use default scale based on selection state
            boolean isSelected = (state.getSelectedRobot() == robot);
            if (isSelected) {
                scale = SELECTED_ROBOT_SCALE;
                // Initialize the scale in the map for future animations
                robotScaleMap.put(robot, scale);
            }
        }
        
        if (robotDrawable != null) {
            // Calculate center point
            float centerX = left + cellSize / 2;
            float centerY = top + cellSize / 2;
            
            // Calculate scaled dimensions
            float scaledWidth = cellSize * scale;
            float scaledHeight = cellSize * scale;
            
            // Calculate new bounds with center preserved
            float scaledLeft = centerX - scaledWidth / 2;
            float scaledTop = centerY - scaledHeight / 2;
            float scaledRight = centerX + scaledWidth / 2;
            float scaledBottom = centerY + scaledHeight / 2;
            
            robotDrawable.setBounds(
                (int) scaledLeft,
                (int) scaledTop,
                (int) scaledRight,
                (int) scaledBottom
            );
            robotDrawable.draw(canvas);
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
                // Use the rich description that includes goal information
                announceForAccessibility("Selected " + getRobotDescription(robot));
            } else {
                // For empty spaces, announce what's at this position
                announceForAccessibility(getPositionDescription(gridX, gridY));
            }
        } else if (action == MotionEvent.ACTION_UP && state != null) {
            GameElement selectedRobot = state.getSelectedRobot();
            if (selectedRobot != null) {
                int robotX = selectedRobot.getX();
                int robotY = selectedRobot.getY();
                
                // Store original position for change detection
                int oldX = robotX;
                int oldY = robotY;
                
                // If the click is not on the same row or column as the robot,
                // determine which direction (row or column) is closest
                if (robotX != gridX && robotY != gridY) {
                    // Calculate horizontal and vertical distances from robot to clicked point
                    int horizontalDistance = Math.abs(gridX - robotX);
                    int verticalDistance = Math.abs(gridY - robotY);
                    
                    if (horizontalDistance <= verticalDistance) {
                        // Horizontal movement is closer/preferred - keep robot's Y but use clicked X
                        gridY = robotY;
                    } else {
                        // Vertical movement is closer/preferred - keep robot's X but use clicked Y
                        gridX = robotX;
                    }
                    
                    Timber.d("Adjusted movement to nearest direction: (%d,%d)", gridX, gridY);
                }
                
                // Let GameStateManager handle the touch which will update counters properly
                gameStateManager.handleGridTouch(gridX, gridY, action);
                
                // Check if robot moved by comparing old and new positions
                Timber.d("[GOAL DEBUG] Selected robot moved from " + oldX + ", " + oldY + " to " + selectedRobot.getX() + ", " + selectedRobot.getY());
                if (oldX != selectedRobot.getX() || oldY != selectedRobot.getY()) {
                    // Shrink the robot back to normal size when it starts moving
                    if (robotScaleMap.containsKey(selectedRobot) && robotScaleMap.get(selectedRobot) > 1.0f) {
                        animateRobotScale(selectedRobot, robotScaleMap.get(selectedRobot), 1.0f);
                    }
                    
                    // Calculate if robot hit a wall or another robot
                    boolean hitWall = false;
                    boolean hitRobot = false;
                    int dx = selectedRobot.getX() - oldX;
                    int dy = selectedRobot.getY() - oldY;
                    
                    // Determine if we hit something
                    if (dx != 0) {
                        // Moving horizontally
                        int nextX = selectedRobot.getX() + (dx > 0 ? 1 : -1);
                        if (nextX >= 0 && nextX < state.getWidth()) {
                            GameElement robotAtPosition = state.getRobotAt(nextX, selectedRobot.getY());
                            if (robotAtPosition != null) {
                                hitRobot = true;
                            } else if (!state.canRobotMoveTo(selectedRobot, nextX, selectedRobot.getY())) {
                                hitWall = true;
                            }
                        }
                    } else if (dy != 0) {
                        // Moving vertically
                        int nextY = selectedRobot.getY() + (dy > 0 ? 1 : -1);
                        if (nextY >= 0 && nextY < state.getHeight()) {
                            GameElement robotAtPosition = state.getRobotAt(selectedRobot.getX(), nextY);
                            if (robotAtPosition != null) {
                                hitRobot = true;
                            } else if (!state.canRobotMoveTo(selectedRobot, selectedRobot.getX(), nextY)) {
                                hitWall = true;
                            }
                        }
                    }
                    
                    // Play the appropriate sound effect based on what happened
                    Timber.d("[SOUND] Playing sound from GameGridView: " + (hitRobot ? "hit_robot" : hitWall ? "hit_wall" : "move"));
                    if (fragment instanceof ModernGameFragment) {
                        if (hitRobot) {
                            ((ModernGameFragment) fragment).playSound("hit_robot");
                        } else if (hitWall) {
                            ((ModernGameFragment) fragment).playSound("hit_wall");
                        } else {
                            ((ModernGameFragment) fragment).playSound("move");
                        }
                        
                        // Also announce possible moves after movement
                        ((ModernGameFragment) fragment).announcePossibleMoves(selectedRobot);
                    }
                    
                    if (state.checkCompletion()) {
                        Timber.d("[GOAL DEBUG] Goal reached! Game complete in " + gameStateManager.getMoveCount().getValue() + " moves and " + gameStateManager.getSquaresMoved().getValue() + " squares moved");
                        
                        // Critical fix: Tell the GameStateManager the game is complete
                        gameStateManager.setGameComplete(true);
                        
                        announceForAccessibility("Goal reached! Game complete in " + 
                            gameStateManager.getMoveCount().getValue() + " moves and " +
                            gameStateManager.getSquaresMoved().getValue() + " squares moved");
                    } else {
                        Timber.d("[GOAL DEBUG] Robot moved");
                        announceForAccessibility(getRobotDescription(selectedRobot));
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
        // Use the AccessibilityManager to check if TalkBack is enabled
        AccessibilityManager accessibilityManager = (AccessibilityManager) getContext()
                .getSystemService(Context.ACCESSIBILITY_SERVICE);
        
        if (accessibilityManager != null && accessibilityManager.isTouchExplorationEnabled() && 
                gameStateManager != null) {
            float x = event.getX();
            float y = event.getY();
            
            // Convert touch coordinates to grid coordinates
            int gridX = (int) (x / cellSize);
            int gridY = (int) (y / cellSize);
            
            // Ensure coordinates are within bounds
            if (gridX < 0 || gridY < 0 || gridX >= gridWidth || gridY >= gridHeight) {
                return super.dispatchHoverEvent(event);
            }
            
            if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER ||
                    event.getAction() == MotionEvent.ACTION_HOVER_MOVE) {
                
                // Only trigger announcements when first entering a cell or when cell changes
                if (lastHoverX != gridX || lastHoverY != gridY) {
                    lastHoverX = gridX;
                    lastHoverY = gridY;
                    
                    // Check if there's a robot at this position
                    GameState state = gameStateManager.getCurrentState().getValue();
                    if (state != null) {
                        GameElement focusedRobot = state.getRobotAt(gridX, gridY);
                        
                        // Highlight the position
                        highlightedX = gridX;
                        highlightedY = gridY;
                        invalidate();
                        
                        // Announce what's at this position
                        String description = getPositionDescription(gridX, gridY);
                        announceForAccessibility(description);
                        Timber.d("[TALKBACK] Hover over cell %d,%d: %s", gridX, gridY, description);
                    }
                }
            } else if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
                // Clear highlighted cell when the hover exits the view
                highlightedX = -1;
                highlightedY = -1;
                invalidate();
            }
            
            // Let the accessibility framework process the event
            return super.dispatchHoverEvent(event);
        }
        
        return super.dispatchHoverEvent(event);
    }
    
    /**
     * Announce a message for accessibility and log it
     * @param message Message to announce
     */
    @Override
    public void announceForAccessibility(CharSequence message) {
        super.announceForAccessibility(message);
        // Log the message to console for debugging
        Timber.d("[TALKBACK] %s", message);
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
        
        // Find the robot's goal if available
        GameState state = gameStateManager.getCurrentState().getValue();
        if (state != null) {
            for (GameElement element : state.getGameElements()) {
                if (element.getType() == GameElement.TYPE_TARGET && element.getColor() == robot.getColor()) {
                    return color + " robot at position " + robot.getX() + ", " + robot.getY() + 
                           ". Its goal is at position " + element.getX() + ", " + element.getY();
                }
            }
        }
        
        return color + " robot at position " + robot.getX() + ", " + robot.getY();
    }
    
    /**
     * Get description of what's at a grid position for accessibility
     */
    private String getPositionDescription(int x, int y) {
        if (gameStateManager == null || gameStateManager.getCurrentState().getValue() == null) {
            return "Position " + x + ", " + y;
        }
        
        GameState state = gameStateManager.getCurrentState().getValue();
        StringBuilder description = new StringBuilder();
        description.append("Position " + x + ", " + y + ": ");
        
        // Check for robot
        GameElement robot = state.getRobotAt(x, y);
        if (robot != null) {
            String color = "Unknown";
            switch (robot.getColor()) {
                case 0: color = "Red"; break;
                case 1: color = "Green"; break;
                case 2: color = "Blue"; break;
                case 3: color = "Yellow"; break;
            }
            description.append(color + " robot. ");
            
            // Find the robot's goal
            for (GameElement element : state.getGameElements()) {
                if (element.getType() == GameElement.TYPE_TARGET && element.getColor() == robot.getColor()) {
                    description.append("Its goal is at position " + element.getX() + ", " + element.getY() + ". ");
                    break;
                }
            }
        }
        
        // Check for target
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_TARGET && element.getX() == x && element.getY() == y) {
                String color = "Unknown";
                switch (element.getColor()) {
                    case 0: color = "Red"; break;
                    case 1: color = "Green"; break;
                    case 2: color = "Blue"; break;
                    case 3: color = "Yellow"; break;
                    default: color = "Multi-colored";
                }
                description.append(color + " goal. ");
                break;
            }
        }
        
        // Check for walls
        boolean hasNorthWall = false;
        boolean hasSouthWall = false;
        boolean hasEastWall = false;
        boolean hasWestWall = false;
        
        // Check if there's a horizontal wall above this cell
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_HORIZONTAL_WALL && 
                element.getX() == x && element.getY() == y) {
                hasSouthWall = true;
            }
            if (element.getType() == GameElement.TYPE_HORIZONTAL_WALL && 
                element.getX() == x && element.getY() == y-1) {
                hasNorthWall = true;
            }
            if (element.getType() == GameElement.TYPE_VERTICAL_WALL && 
                element.getX() == x && element.getY() == y) {
                hasEastWall = true;
            }
            if (element.getType() == GameElement.TYPE_VERTICAL_WALL && 
                element.getX() == x-1 && element.getY() == y) {
                hasWestWall = true;
            }
        }
        
        // Add wall description
        if (hasNorthWall || hasSouthWall || hasEastWall || hasWestWall) {
            description.append("Walls on: ");
            if (hasNorthWall) description.append("North ");
            if (hasSouthWall) description.append("South ");
            if (hasEastWall) description.append("East ");
            if (hasWestWall) description.append("West ");
        } else if (robot == null) {
            description.append("Empty space");
        }
        
        return description.toString();
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
