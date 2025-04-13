package roboyard.ui.components;

import android.view.accessibility.AccessibilityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.content.ContextCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;

import roboyard.eclabs.R;
import roboyard.eclabs.ui.GameElement;
import roboyard.eclabs.ui.ModernGameFragment;
import roboyard.logic.core.Constants;
import roboyard.logic.core.GameState;
import roboyard.logic.core.GameLogic;
import roboyard.logic.core.GridElement;
import roboyard.logic.core.Preferences;
import roboyard.logic.core.WallModel;
import timber.log.Timber;

/**
 * Custom View that renders the game board grid and handles touch interactions.
 * Provides proper accessibility support for TalkBack.
 */
public class GameGridView extends View {
    // Configuration constants for robot paths
    private static final int PATH_STROKE_WIDTH = 6; // Width of the robot path lines
    private static final float BASE_ROBOT_OFFSET_RANGE = 25.0f; // Maximum random offset in pixels (will be -range to +range)
    private static final float PERPENDICULAR_OFFSET_STEP = 2.0f; // Pixels to offset per repeated traversal

    private GameStateManager gameStateManager;
    private Paint cellPaint;
    private Paint robotPaint;
    private Paint targetPaint;
    private Paint textPaint;
    private Paint gridPaint;
    private Paint[] pathPaints; // Paints for robot movement paths
    private float cellSize;
    
    // Grid dimensions
    private int gridWidth = 14;
    private int gridHeight = 14;
    
    // For accessibility - track the focused cell
    private final int focusedX = -1;
    private final int focusedY = -1;
    
    // Track robot movement paths
    private final HashMap<Integer, ArrayList<int[]>> robotPaths = new HashMap<>(); // Map robot color to list of positions [x,y]
    private final HashMap<Integer, float[]> robotBaseOffsets = new HashMap<>(); // Base offset for each robot
    private final HashMap<Integer, HashMap<String, Integer>> segmentCounts = new HashMap<>(); // Track segment traversal count
    
    // Robot drawables for each color
    private Drawable pinkRobotRight, yellowRobotRight, blueRobotRight, greenRobotRight;
    private Drawable pinkRobotLeft, yellowRobotLeft, blueRobotLeft, greenRobotLeft;
    
    // Wall drawables
    private Drawable wallHorizontal;
    private Drawable wallVertical;
    
    // Target drawables for each color
    private Drawable targetPinkDrawable;     // cr
    private Drawable targetGreenDrawable;   // cv
    private Drawable targetBlueDrawable;    // cb
    private Drawable targetYellowDrawable;  // cj
    private Drawable targetMultiDrawable;   // cm
    
    // Robot animation configuration
    public static final float INITIAL_SELECTED_ROBOT_SCALE = 1.5f; // much larger when clicked
    public static final float SELECTED_ROBOT_SCALE = 1.3f; // 50% larger, when still selected
    public static final float DEFAULT_ROBOT_SCALE = 1.1f; // a bit larger as a cell by default
    private final boolean enableRobotAnimation = true;
    private final HashMap<GameElement, Float> robotScaleMap = new HashMap<>(); // Track current scale for each robot
    private final GameElement focusedRobot = null; // Currently focused (hovered) robot
    private final android.view.animation.DecelerateInterpolator easeInterpolator = new android.view.animation.DecelerateInterpolator(1.5f); // Ease function
    private final android.os.Handler animationHandler = new android.os.Handler(android.os.Looper.getMainLooper()); // Animation handler
    private final long animationDuration = 300; // Animation duration in milliseconds
    
    // Store starting positions of robots
    private final HashMap<Integer, int[]> robotStartingPositions = new HashMap<>(); // Map robot color to starting position [x,y]
    
    // Grid background
    private Drawable gridTileDrawable; 
    private int[][] tileRotations; // Store rotation angle for each tile
    private Drawable backgroundLogo; // Background logo
    private Fragment fragment; // Parent fragment
    
    // For hover events
    private int lastHoverX = -1;
    private int lastHoverY = -1;
    private int highlightedX = -1;
    private int highlightedY = -1;

    // Touch tracking for sliding gestures
    private float startTouchX = -1;
    private float startTouchY = -1;
    private int touchStartGridX = -1;
    private int touchStartGridY = -1;
    private GameElement touchedRobot = null;
    private static final float MIN_SWIPE_DISTANCE = 30.0f; // Minimum distance in pixels to consider it a swipe

    // Flag to track if an accessibility action is in progress
    private boolean accessibilityActionInProgress = false;

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
        cellPaint.setAntiAlias(true);
        
        robotPaint = new Paint();
        robotPaint.setStyle(Paint.Style.FILL);
        robotPaint.setAntiAlias(true);
        
        targetPaint = new Paint();
        targetPaint.setStyle(Paint.Style.FILL);
        targetPaint.setAntiAlias(true);
        
        textPaint = new Paint();
        textPaint.setTextSize(60);  // Increased from 40 for better visibility
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        
        gridPaint = new Paint();
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(3);
        
        // Initialize path paints for all robot colors
        pathPaints = new Paint[10]; // Support up to COLOR_MULTI + 1
        
        // PINK robot
        pathPaints[Constants.COLOR_PINK] = createPathPaint(Color.rgb(255, 105, 180)); // Pink
        
        // GREEN robot
        pathPaints[Constants.COLOR_GREEN] = createPathPaint(Color.rgb(0, 177, 0)); // Green
        
        // BLUE robot
        pathPaints[Constants.COLOR_BLUE] = createPathPaint(Color.rgb(0, 0, 255)); // Blue
        
        // YELLOW robot
        pathPaints[Constants.COLOR_YELLOW] = createPathPaint(Color.rgb(177, 177, 0)); // Yellow
        
        // SILVER robot
        pathPaints[Constants.COLOR_SILVER] = createPathPaint(Color.rgb(192, 192, 192)); // Silver
        
        // RED robot
        pathPaints[Constants.COLOR_RED] = createPathPaint(Color.rgb(177, 0, 0)); // Red
        
        // BROWN robot
        pathPaints[Constants.COLOR_BROWN] = createPathPaint(Color.rgb(165, 42, 42)); // Brown
        
        // ORANGE robot
        pathPaints[Constants.COLOR_ORANGE] = createPathPaint(Color.rgb(177, 165, 0)); // Orange
        
        // WHITE robot
        pathPaints[Constants.COLOR_WHITE] = createPathPaint(Color.rgb(255, 255, 255)); // White
        
        // Load robot drawables
        pinkRobotRight = ContextCompat.getDrawable(context, R.drawable.robot_pink_right);
        yellowRobotRight = ContextCompat.getDrawable(context, R.drawable.robot_yellow_right);
        blueRobotRight = ContextCompat.getDrawable(context, R.drawable.robot_blue_right);
        greenRobotRight = ContextCompat.getDrawable(context, R.drawable.robot_green_right);
        
        pinkRobotLeft = ContextCompat.getDrawable(context, R.drawable.robot_pink_left);
        yellowRobotLeft = ContextCompat.getDrawable(context, R.drawable.robot_yellow_left);
        blueRobotLeft = ContextCompat.getDrawable(context, R.drawable.robot_blue_left);
        greenRobotLeft = ContextCompat.getDrawable(context, R.drawable.robot_green_left);
        
        // Load wall drawables
        wallHorizontal = ContextCompat.getDrawable(context, R.drawable.mh);
        wallVertical = ContextCompat.getDrawable(context, R.drawable.mv);
        
        // Load target drawables
        targetPinkDrawable = ContextCompat.getDrawable(context, R.drawable.target_pink);
        targetGreenDrawable = ContextCompat.getDrawable(context, R.drawable.target_green);
        targetBlueDrawable = ContextCompat.getDrawable(context, R.drawable.target_blue);
        targetYellowDrawable = ContextCompat.getDrawable(context, R.drawable.target_yellow);
        targetMultiDrawable = ContextCompat.getDrawable(context, R.drawable.target_multi);
        
        // Load grid tile background
        gridTileDrawable = ContextCompat.getDrawable(context, R.drawable.grid_tiles);
        
        // Load background logo
        backgroundLogo = ContextCompat.getDrawable(context, R.drawable.roboyard);
        if (backgroundLogo != null) {
            // Ensure the drawable is properly configured
            backgroundLogo.setAlpha(255); // Full opacity
            Timber.d("Roboyard logo loaded successfully");
        } else {
            Timber.e("Failed to load Roboyard logo drawable");
        }
        
        // Initialize tile rotations with random values (0, 90, 180, 270 degrees)
        initializeTileRotations();
        
        // Set up accessibility support
        setFocusable(true);
        setClickable(true);
        
        // Suppress the entire view from accessibility announcements
        // This prevents "gameboard grid, double-click to activate" announcements
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        
        Timber.d("ROBOYARD_ACCESSIBILITY", "Set GameGridView accessibility to NO");
        
        // Configure accessibility delegate for TalkBack support
        ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                // Remove all announcements for this view
                info.setClassName(GameGridView.class.getName());
                info.setContentDescription(null);
                info.setVisibleToUser(false);
                
                // Remove actions
                info.removeAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK);
                info.removeAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_FOCUS);
                info.setClickable(false);
                info.setFocusable(false);
                
                Timber.d("ROBOYARD_ACCESSIBILITY", "Initialized accessibility info with suppressions");
            }
            
            @Override
            public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
                super.onInitializeAccessibilityEvent(host, event);
                event.setClassName(GameGridView.class.getName());
            }
        });
    }
    
    /**
     * Helper method to create path paint with consistent settings
     * @param color Base color for the path
     * @return Configured Paint object
     */
    private Paint createPathPaint(int color) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(PATH_STROKE_WIDTH);
        paint.setColor(color);
        paint.setAlpha(128); // 50% transparency
        paint.setAntiAlias(true);
        return paint;
    }
    
    /**
     * Initialize tile rotations with random values
     */
    private void initializeTileRotations() {
        // Only initialize if not already initialized or if dimensions have changed
        if (tileRotations == null || tileRotations.length != gridWidth || tileRotations[0].length != gridHeight) {
            tileRotations = new int[gridWidth][gridHeight];
            for (int i = 0; i < gridWidth; i++) {
                for (int j = 0; j < gridHeight; j++) {
                    tileRotations[i][j] = (int) (Math.random() * 4) * 90; // Random rotation (0, 90, 180, 270 degrees)
                }
            }
            Timber.d("Initialized tile rotations for %dx%d grid", gridWidth, gridHeight);
        }
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
            
            // Store starting positions of robots when game state is first set
            storeRobotStartingPositions(state);
        }
        invalidate();
    }
    
    /**
     * Store the starting positions of all robots in the game
     * @param state Current game state
     */
    private void storeRobotStartingPositions(GameState state) {
        if (state == null) return;
        
        // Check if this is a new game by comparing robot positions with stored positions
        boolean isNewGame = false;
        
        // If we have no stored positions yet, this must be the first game
        if (robotStartingPositions.isEmpty()) {
            isNewGame = true;
        } else {
            // Compare current robot positions with stored starting positions
            // If any robot is at a different position than what we have stored,
            // and it matches its initial position in the GameState, it's a new game
            for (GameElement element : state.getGameElements()) {
                if (element.getType() == GameElement.TYPE_ROBOT) {
                    int color = element.getColor();
                    int x = element.getX();
                    int y = element.getY();
                    
                    // If we have this color stored and the position is different
                    if (robotStartingPositions.containsKey(color)) {
                        int[] storedPos = robotStartingPositions.get(color);
                        // If current position doesn't match stored starting position
                        if (storedPos[0] != x || storedPos[1] != y) {
                            // Check if this is a new game (robot at its initial position)
                            // or just a moved robot in the current game
                            if (state.initialRobotPositions != null && 
                                state.initialRobotPositions.containsKey(color)) {
                                int[] initialPos = state.initialRobotPositions.get(color);
                                if (initialPos[0] == x && initialPos[1] == y) {
                                    // Robot is at its initial position in GameState but different
                                    // from our stored position - this is a new game
                                    isNewGame = true;
                                    break;
                                }
                            }
                        }
                    } else {
                        // We don't have this color stored yet, so store it
                        isNewGame = true;
                        break;
                    }
                }
            }
        }
        
        // Only clear and update if this is a new game
        if (isNewGame) {
            // Clear existing positions
            robotStartingPositions.clear();
            
            // Store positions of all robots
            for (GameElement element : state.getGameElements()) {
                if (element.getType() == GameElement.TYPE_ROBOT) {
                    robotStartingPositions.put(element.getColor(), new int[] {element.getX(), element.getY()});
                    Timber.d("Stored starting position for robot color %d at (%d,%d)", 
                            element.getColor(), element.getX(), element.getY());
                }
            }
            
            Timber.d("Updated robot starting positions for new game");
        }
    }
    
    /**
     * Set up robot animation observers
     */
    private void setupRobotAnimationObservers() {
        if (fragment != null && gameStateManager != null) {
            gameStateManager.getCurrentState().observe(fragment.getViewLifecycleOwner(), state -> {
                if (state != null) {
                    // Store the starting positions of robots whenever the game state changes
                    // This ensures that starting positions are updated when a new game is created
                    storeRobotStartingPositions(state);
                    
                    GameElement selectedRobot = state.getSelectedRobot();
                    
                    // Update robot scales based on selection
                    for (GameElement element : state.getGameElements()) {
                        if (element.getType() == GameElement.TYPE_ROBOT) {
                            if (element == selectedRobot) {
                                // If this is the first selection of this robot
                                if (!robotScaleMap.containsKey(element) || robotScaleMap.get(element) == DEFAULT_ROBOT_SCALE || robotScaleMap.get(element) == INITIAL_SELECTED_ROBOT_SCALE) {
                                    // Robot first selected - animate to initial large scale
                                    animateRobotScale(element, DEFAULT_ROBOT_SCALE, INITIAL_SELECTED_ROBOT_SCALE);
                                } else if (robotScaleMap.get(element) == INITIAL_SELECTED_ROBOT_SCALE && state.getMoveCount() > 0) {
                                    // After first move, shrink to regular selected scale
                                    animateRobotScale(element, INITIAL_SELECTED_ROBOT_SCALE, SELECTED_ROBOT_SCALE);
                                } else if (robotScaleMap.get(element) < SELECTED_ROBOT_SCALE) {
                                    // If somehow the robot is selected but has a scale smaller than SELECTED_ROBOT_SCALE
                                    // This can happen after game reloads or when reselecting a previously moved robot
                                    animateRobotScale(element, robotScaleMap.get(element), SELECTED_ROBOT_SCALE);
                                }
                                // Otherwise keep current scale if it's already at SELECTED_ROBOT_SCALE
                            } else if (robotScaleMap.containsKey(element) && robotScaleMap.get(element) > DEFAULT_ROBOT_SCALE) {
                                // Robot deselected - animate shrinking back to DEFAULT_ROBOT_SCALE (not 1.0f)
                                animateRobotScale(element, robotScaleMap.get(element), DEFAULT_ROBOT_SCALE);
                            } else if (!robotScaleMap.containsKey(element)) {
                                // Initialize unselected robot with default scale
                                robotScaleMap.put(element, DEFAULT_ROBOT_SCALE);
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
    public void animateRobotScale(GameElement robot, float fromScale, float toScale) {
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
    
    /**
     * Animate a robot's scale from one size to another with completion callback
     * @param robot The robot to animate
     * @param fromScale Starting scale
     * @param toScale Target scale
     * @param duration Custom duration in milliseconds
     * @param completionCallback Callback to run when animation completes
     */
    public void animateRobotScaleWithCallback(GameElement robot, float fromScale, float toScale, long duration, Runnable completionCallback) {
        if (robot == null || !enableRobotAnimation) return;
        
        // Initialize scale if not present
        if (!robotScaleMap.containsKey(robot)) {
            robotScaleMap.put(robot, fromScale);
        }
        
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(fromScale, toScale);
        animator.setDuration(duration);
        animator.setInterpolator(easeInterpolator);
        animator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            robotScaleMap.put(robot, scale);
            invalidate();
        });
        
        // Add completion callback
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                robotScaleMap.put(robot, toScale);
                if (completionCallback != null) {
                    completionCallback.run();
                }
            }
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
        return !hasTopWall && !hasBottomWall;
        
        // Default to horizontal for isolated walls
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
        
        int colorId = targetElement.getColor();
        // Use the GameLogic color methods for more maintainable code
        switch (colorId) {
            case Constants.COLOR_PINK: return targetPinkDrawable;
            case Constants.COLOR_GREEN: return targetGreenDrawable;
            case Constants.COLOR_BLUE: return targetBlueDrawable;
            case Constants.COLOR_YELLOW: return targetYellowDrawable;
            case Constants.COLOR_MULTI: // intentional fallthrough
            default: 
                Timber.d("[RENDER] Using multi-colored target for color ID: %d", colorId);
                return targetMultiDrawable;
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
        
        // Calculate offsets to center the board
        float offsetX = (getWidth() - (gridWidth * cellSize)) / 2f;
        float offsetY = (getHeight() - (gridHeight * cellSize)) / 2f;
        
        // Draw grid cells - board background first
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                float left = offsetX + (x * cellSize);
                float top = offsetY + (y * cellSize);
                float right = left + cellSize;
                float bottom = top + cellSize;
                
                // Draw grid tile background
                if (gridTileDrawable != null) {
                    // Save the canvas state before rotating
                    canvas.save();
                    
                    // Calculate the center point of the tile
                    float centerX = left + cellSize / 2;
                    float centerY = top + cellSize / 2;
                    
                    // Safely get rotation angle - ensure we don't access out of bounds
                    int rotationAngle = 0;
                    if (tileRotations != null && x < tileRotations.length && y < tileRotations[0].length) {
                        rotationAngle = tileRotations[x][y];
                    }
                    
                    // Rotate the canvas around the center point of the tile
                    canvas.rotate(rotationAngle, centerX, centerY);
                    
                    // Set bounds and draw the rotated tile
                    gridTileDrawable.setBounds((int)left, (int)top, (int)right, (int)bottom);
                    gridTileDrawable.draw(canvas);
                    
                    // Restore the canvas to its original state
                    canvas.restore();
                } else {
                    // Fallback to colored background if drawable is not available
                    cellPaint.setColor(Color.rgb(30, 30, 60));
                    canvas.drawRect(left, top, right, bottom, cellPaint);
                }
                
                // Draw grid lines (disabled)
                gridPaint.setColor(Color.parseColor("#4ae600")); // green gridstrokes
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
        
        // Draw the Roboyard logo in the center of the board AFTER the background tiles
        // but BEFORE the walls and other game elements
        if (backgroundLogo != null) {
            // Calculate the center of the board
            float centerX = offsetX + (gridWidth * cellSize) / 2;
            float centerY = offsetY + (gridHeight * cellSize) / 2;
            
            // Make the logo exactly 2x2 squares in size
            float logoSize = cellSize * 2; // Exactly 2 cells wide and high
            
            // Set bounds for the logo centered on the board - the center of the logo should be at the center of the board
            backgroundLogo.setBounds(
                (int)(centerX - logoSize/2),
                (int)(centerY - logoSize/2),
                (int)(centerX + logoSize/2),
                (int)(centerY + logoSize/2)
            );
            
            // Draw the logo with full opacity
            backgroundLogo.setAlpha(255);
            backgroundLogo.draw(canvas);
        }
        
        // Draw targets
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_TARGET) {
                float left = offsetX + (element.getX() * cellSize);
                float top = offsetY + (element.getY() * cellSize);
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
        // Create wall model from game state
        WallModel wallModel = WallModel.fromGameElements(
            state.getGameElements(),
            gridWidth, gridHeight
        );
        
        // Create renderer with current cell size
        WallRenderer renderer = new WallRenderer(
            wallModel, cellSize, wallHorizontal, wallVertical
        );
        
        // Draw walls using renderer
        renderer.drawWalls(canvas, offsetX, offsetY);
        
        // Draw starting positions of robots
        for (int color : robotStartingPositions.keySet()) {
            int[] position = robotStartingPositions.get(color);
            int x = position[0];
            int y = position[1];
            
            float left = offsetX + (x * cellSize);
            float top = offsetY + (y * cellSize);
            float right = left + cellSize;
            float bottom = top + cellSize;
            
            // Select appropriate robot drawable based on color
            Drawable robotDrawable = null;
            
            switch (color) {
                case 0: // Pink
                    robotDrawable = pinkRobotRight;
                    break;
                case 1: // Green
                    robotDrawable = greenRobotRight;
                    break;
                case 2: // Blue
                    robotDrawable = blueRobotRight;
                    break;
                case 3: // Yellow
                    robotDrawable = yellowRobotRight;
                    break;
            }
            
            if (robotDrawable != null) {
                // Create a copy of the drawable to avoid affecting the original
                Drawable markerDrawable = robotDrawable.getConstantState().newDrawable().mutate();
                // Draw robot using drawable
                markerDrawable.setBounds((int)left, (int)top, (int)right, (int)bottom);
                markerDrawable.setAlpha(55); // Set alpha to 20% transparency for the semi-transparent robot markers for starting positions
                markerDrawable.draw(canvas);
            }
        }
        
        // Draw robot movement paths
        for (int color : robotPaths.keySet()) {
            ArrayList<int[]> path = robotPaths.get(color);
            HashMap<String, Integer> robotSegments = segmentCounts.get(color);
            float[] baseOffset = robotBaseOffsets.get(color);
            
            if (path != null && !path.isEmpty() && baseOffset != null && color < pathPaints.length && pathPaints[color] != null) {
                Paint pathPaint = pathPaints[color];
                
                // Start from the first point
                float prevX = offsetX + (path.get(0)[0] * cellSize) + cellSize / 2;
                float prevY = offsetY + (path.get(0)[1] * cellSize) + cellSize / 2;
                
                for (int i = 1; i < path.size(); i++) {
                    int[] pos = path.get(i);
                    int[] prevPos = path.get(i-1);
                    
                    // Calculate center point of the cell
                    float x = offsetX + (pos[0] * cellSize) + cellSize / 2;
                    float y = offsetY + (pos[1] * cellSize) + cellSize / 2;
                    
                    // Create segment key to look up traversal count
                    String segmentKey = prevPos[0] + "," + prevPos[1] + ":" + pos[0] + "," + pos[1];
                    int count = robotSegments.getOrDefault(segmentKey, 1);
                    
                    // Calculate perpendicular direction to the line segment
                    float dx = x - prevX;
                    float dy = y - prevY;
                    float length = (float) Math.sqrt(dx*dx + dy*dy);
                    
                    // Avoid division by zero
                    if (length > 0.001f) {
                        // Normalize and rotate 90 degrees to get perpendicular direction
                        float perpX = -dy / length;
                        float perpY = dx / length;
                        
                        // Add perpendicular offset based on traversal count
                        float perpOffset = (count - 1) * PERPENDICULAR_OFFSET_STEP; // 2 pixels per repeated traversal
                        
                        // Apply both base offset and perpendicular offset
                        canvas.drawLine(
                            prevX + baseOffset[0] + perpX * perpOffset,
                            prevY + baseOffset[1] + perpY * perpOffset,
                            x + baseOffset[0] + perpX * perpOffset,
                            y + baseOffset[1] + perpY * perpOffset,
                            pathPaint
                        );
                    } else {
                        // Fallback for zero-length segments
                        canvas.drawLine(
                            prevX + baseOffset[0],
                            prevY + baseOffset[1],
                            x + baseOffset[0],
                            y + baseOffset[1],
                            pathPaint
                        );
                    }
                    
                    prevX = x;
                    prevY = y;
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
        Drawable robotDrawable;
        int robotColor = robot.getColor();
        boolean isSelected = robot.isSelected();
        
        // Get the correct robot sprite based on color
        if (robotColor == GameElement.COLOR_BLUE) {
            robotDrawable = robot.getDirectionX() < 0 ? blueRobotLeft : blueRobotRight;
        } else if (robotColor == GameElement.COLOR_YELLOW) {
            robotDrawable = robot.getDirectionX() < 0 ? yellowRobotLeft : yellowRobotRight;
        } else if (robotColor == GameElement.COLOR_GREEN) {
            robotDrawable = robot.getDirectionX() < 0 ? greenRobotLeft : greenRobotRight;
        } else { // Pink (default)
            robotDrawable = robot.getDirectionX() < 0 ? pinkRobotLeft : pinkRobotRight;
        }
        
        // Calculate position
        float offsetX = (getWidth() - (gridWidth * cellSize)) / 2f;
        float offsetY = (getHeight() - (gridHeight * cellSize)) / 2f;
        
        float left = (robot.getX() * cellSize) + offsetX;
        float top = (robot.getY() * cellSize) + offsetY;
        
        // If robot has animation position, use that instead of logical position
        if (robot.hasAnimationPosition()) {
            if (GameLogic.hasDebugLogging()) {
                Timber.d("[ANIM] Using animation position for robot %d: (%.2f,%.2f)", robot.getColor(), robot.getAnimationX(), robot.getAnimationY());
            }
            left = (robot.getAnimationX() * cellSize) + offsetX;
            top = (robot.getAnimationY() * cellSize) + offsetY;
        }
        
        // Scale for selection
        float scale = DEFAULT_ROBOT_SCALE;
        if (robotScaleMap.containsKey(robot)) {
            scale = robotScaleMap.get(robot);
        } else if (isSelected) {
            scale = SELECTED_ROBOT_SCALE;
            robotScaleMap.put(robot, scale);
        } else {
            robotScaleMap.put(robot, scale);
        }
        
        // Calculate dimensions
        float size = cellSize * scale;
        float offsetX2 = (cellSize - size) / 2f;
        float offsetY2 = (cellSize - size) / 2f;
        
        // Set the bounds
        robotDrawable.setBounds(
                (int) (left + offsetX2), 
                (int) (top + offsetY2),
                (int) (left + offsetX2 + size), 
                (int) (top + offsetY2 + size));
        
        // Draw the sprite
        robotDrawable.draw(canvas);
        
        // Draw position text when accessibility features are enabled
        if (isSelected && isAccessibilityActive()) {
            canvas.drawText(robot.getX() + "," + robot.getY(), 
                    left + cellSize / 2, 
                    top + cellSize / 2, 
                    textPaint);
        }
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Get the raw screen coordinates
        float rawX = event.getRawX();
        float rawY = event.getRawY();
        
        // Check if this might be an edge gesture (like Android back)
        int[] location = new int[2];
        getLocationOnScreen(location);
        boolean isLeftEdgeGesture = rawX - location[0] < 20; // Within 20px of left edge
        boolean isRightEdgeGesture = (location[0] + getWidth() - rawX) < 20; // Within 20px of right edge
        
        // For left/right edge gestures, handle them as robot movements
        if ((isLeftEdgeGesture || isRightEdgeGesture) && event.getAction() == MotionEvent.ACTION_DOWN) {
            // Process edge swipes on ACTION_DOWN
            Timber.d("[BACK] DETECTED edge gesture: %s", isLeftEdgeGesture ? "LEFT" : "RIGHT");
            
            GameState state = gameStateManager.getCurrentState().getValue();
            if (state != null) {
                GameElement robot = state.getSelectedRobot();
                if (robot != null) {
                    // Determine direction based on which edge
                    int dx = isLeftEdgeGesture ? -1 : 1; // Left edge = move left, Right edge = move right
                    
                    Timber.d("[BACK] MOVING Robot %d from (%d,%d) with dx=%d", 
                            robot.getColor(), robot.getX(), robot.getY(), dx);
                    
                    // Move the robot 
                    boolean moved = gameStateManager.moveRobotInDirection(dx, 0);
                    Timber.d("[BACK] Movement result: %s", moved ? "SUCCESS" : "FAILED");
                    
                    // This is important - return true to consume the event
                    return true;
                } else {
                    Timber.d("[BACK] No robot selected to move");
                }
            } else {
                Timber.d("[BACK] No game state available");
            }
        }
        
        // Default behavior for normal touches
        return super.dispatchTouchEvent(event);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Timber.d("[ANIM] GameGridView.onTouchEvent: action=%d", event.getAction());
        
        // Skip if a reset or accessibility action is in progress
        if (accessibilityActionInProgress) {
            return true;
        }
        
        // Get the raw screen coordinates
        float rawX = event.getRawX();
        float rawY = event.getRawY();
        
        // Check if this might be an edge gesture (like Android back)
        int[] location = new int[2];
        getLocationOnScreen(location);
        boolean isLeftEdgeGesture = rawX - location[0] < 20; // Within 20px of left edge
        boolean isRightEdgeGesture = (location[0] + getWidth() - rawX) < 20; // Within 20px of right edge
        
        // Log edge gesture detection
        if (isLeftEdgeGesture || isRightEdgeGesture) {
            Timber.d("[BACK] DETECTED: Left=%s, Right=%s, Action=%d, RawX=%f, ViewX=%d", 
                    isLeftEdgeGesture, isRightEdgeGesture, event.getAction(), rawX, location[0]);
        }
        
        // Convert screen coordinates to grid coordinates
        float x = event.getX();
        float y = event.getY();
        
        // Calculate grid offsets to center the board
        float offsetX = (getWidth() - (gridWidth * cellSize)) / 2f;
        float offsetY = (getHeight() - (gridHeight * cellSize)) / 2f;
        
        // Adjust for grid offset
        float adjustedX = x - offsetX;
        float adjustedY = y - offsetY;
        
        // Convert to grid coordinates
        int gridX = (int) (adjustedX / cellSize);
        int gridY = (int) (adjustedY / cellSize);
        
        // Handle edge gestures as robot movements
        if ((isLeftEdgeGesture || isRightEdgeGesture) && event.getAction() == MotionEvent.ACTION_DOWN) {
            // Process edge swipes on ACTION_DOWN
            Timber.d("[BACK] Processing edge gesture as robot movement on ACTION_DOWN");
            GameState state = gameStateManager.getCurrentState().getValue();
            if (state != null) {
                GameElement robot = state.getSelectedRobot();
                if (robot != null) {
                    // Determine direction based on which edge
                    int dx = isLeftEdgeGesture ? -1 : 1; // Left edge = move left, Right edge = move right
                    
                    Timber.d("[BACK] MOVING: Robot %d with dx=%d", robot.getColor(), dx);
                    try {
                        boolean moved = gameStateManager.moveRobotInDirection(dx, 0);
                        Timber.d("[BACK] Movement result: %s", moved ? "SUCCESS" : "FAILED");
                    } catch (Exception e) {
                        Timber.e(e, "[BACK] Error moving robot");
                    }
                    return true;
                } else {
                    Timber.d("[BACK] No robot selected");
                }
            } else {
                Timber.d("[BACK] No game state available");
            }
        }
        
        // Bounds check for non-edge gestures
        if (gridX < 0 || gridX >= gridWidth || gridY < 0 || gridY >= gridHeight) {
            // Outside grid, ignore
            return true;
        }
        
        // Capture action for accessibility announcements
        int action = event.getAction();
        GameState state = gameStateManager.getCurrentState().getValue();
        
        // Handle sliding gestures and regular taps
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (state != null) {
                    // Store initial touch position
                    startTouchX = x;
                    startTouchY = y;
                    touchStartGridX = gridX;
                    touchStartGridY = gridY;
                    
                    // Check if a robot was touched
                    touchedRobot = state.getRobotAt(gridX, gridY);
                    
                    // Announce selection for accessibility
                    if (touchedRobot != null) {
                        announceForAccessibility("Selected " + getRobotDescription(touchedRobot));
                    } else {
                        announceForAccessibility(getPositionDescription(gridX, gridY));
                    }
                }
                return true;
                
            case MotionEvent.ACTION_MOVE:
                // Only process if we have a valid starting point and a touched robot
                if (startTouchX >= 0 && startTouchY >= 0 && touchedRobot != null && state != null) {
                    // Calculate the distance moved
                    float deltaX = x - startTouchX;
                    float deltaY = y - startTouchY;
                    float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                    
                    // Only process if the distance exceeds the minimum swipe threshold
                    if (distance >= MIN_SWIPE_DISTANCE) {
                        // Determine the dominant direction (horizontal or vertical)
                        int dx = 0;
                        int dy = 0;
                        
                        if (Math.abs(deltaX) > Math.abs(deltaY)) {
                            // Horizontal swipe
                            dx = deltaX > 0 ? 1 : -1; // Right or left
                        } else {
                            // Vertical swipe
                            dy = deltaY > 0 ? 1 : -1; // Down or up
                        }
                        
                        // Select the robot
                        state.setSelectedRobot(touchedRobot);
                        
                        // Use the improved moveRobotInDirection method which handles animation
                        Timber.d("[SWIPE] Moving robot in direction: dx=%d, dy=%d", dx, dy);
                        gameStateManager.moveRobotInDirection(dx, dy);
                        
                        // Reset tracking variables to prevent further processing in ACTION_UP
                        startTouchX = -1;
                        startTouchY = -1;
                        touchedRobot = null;
                        
                        return true;
                    }
                }
                return true;
                
            case MotionEvent.ACTION_UP:
                if (state != null) {
                    // Check if this was a tap (no significant movement)
                    boolean isTap = (startTouchX >= 0 && 
                                    Math.abs(x - startTouchX) < MIN_SWIPE_DISTANCE && 
                                    Math.abs(y - startTouchY) < MIN_SWIPE_DISTANCE);
                    
                    // Reset tracking variables
                    float oldStartX = startTouchX;
                    float oldStartY = startTouchY;
                    int oldTouchStartGridX = touchStartGridX;
                    int oldTouchStartGridY = touchStartGridY;
                    GameElement oldTouchedRobot = touchedRobot;
                    
                    startTouchX = -1;
                    startTouchY = -1;
                    touchStartGridX = -1;
                    touchStartGridY = -1;
                    touchedRobot = null;
                    
                    // Handle tap behavior
                    if (isTap) {
                        GameElement selectedRobot = state.getSelectedRobot();
                        
                        // Check if user tapped on the currently selected robot to deselect it
                        if (selectedRobot != null && gridX == selectedRobot.getX() && gridY == selectedRobot.getY()) {
                            // User tapped on the currently selected robot, deselect it
                            Timber.d("Deselecting robot at (%d,%d)", gridX, gridY);
                            state.setSelectedRobot(null);
                            animateRobotScale(selectedRobot, SELECTED_ROBOT_SCALE, DEFAULT_ROBOT_SCALE); // Animate back to default size
                            announceForAccessibility(getRobotDescription(selectedRobot) + " deselected");
                            invalidate();
                            return true;
                        }
                        
                        // Check if the user clicked on another robot - if so, select it instead of moving
                        GameElement clickedRobot = state.getRobotAt(gridX, gridY);
                        if (clickedRobot != null && (selectedRobot == null || clickedRobot != selectedRobot)) {
                            // User clicked on a different robot, select it
                            Timber.d("Selecting a different robot at (%d,%d)", gridX, gridY);
                            gameStateManager.handleGridTouch(gridX, gridY, action);
                            return true;
                        }
                        
                        // Handle robot movement if a robot is selected
                        if (selectedRobot != null) {
                            int robotX = selectedRobot.getX();
                            int robotY = selectedRobot.getY();
                            
                            // Determine movement direction based on tap location relative to robot
                            if (robotX == gridX || robotY == gridY) {
                                // Direct movement along row or column
                                int dx = 0;
                                int dy = 0;
                                
                                if (robotX == gridX) {
                                    // Moving vertically
                                    dy = gridY > robotY ? 1 : -1; // Down or up
                                } else {
                                    // Moving horizontally
                                    dx = gridX > robotX ? 1 : -1; // Right or left
                                }
                                
                                // Use the improved moveRobotInDirection method which handles animation
                                Timber.d("[TOUCH] Moving robot in direction: dx=%d, dy=%d", dx, dy);
                                gameStateManager.moveRobotInDirection(dx, dy);
                                return true;
                            } else {
                                // Diagonal movement - determine which direction to prioritize
                                int deltaX = gridX - robotX;
                                int deltaY = gridY - robotY;
                                
                                if (Math.abs(deltaX) > Math.abs(deltaY)) {
                                    // Horizontal movement takes priority
                                    int dx = deltaX > 0 ? 1 : -1; // Left edge = move left, Right edge = move right
                                    
                                    Timber.d("[TOUCH] Moving robot horizontally: dx=%d", dx);
                                    gameStateManager.moveRobotInDirection(dx, 0);
                                } else {
                                    // Vertical movement takes priority
                                    int dy = deltaY > 0 ? 1 : -1;
                                    Timber.d("[TOUCH] Moving robot vertically: dy=%d", dy);
                                    gameStateManager.moveRobotInDirection(0, dy);
                                }
                                return true;
                            }
                        }
                    }
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_CANCEL:
                // Reset tracking variables
                startTouchX = -1;
                startTouchY = -1;
                touchStartGridX = -1;
                touchStartGridY = -1;
                touchedRobot = null;
                return true;
        }
        
        // Fall back to original behavior for other actions
        return true;
    }
    
    /**
     * Handle effects after a robot has moved (sound, animation, game completion check)
     */
    void handleRobotMovementEffects(GameState state, GameElement selectedRobot, int oldX, int oldY) {
        Timber.d("[ANIM] handleRobotMovementEffects: Robot %d moved from (%d,%d) to (%d,%d)", 
                selectedRobot.getColor(), oldX, oldY, selectedRobot.getX(), selectedRobot.getY());
        
        // Shrink the robot back to default size when it starts moving
        if (robotScaleMap.containsKey(selectedRobot) && robotScaleMap.get(selectedRobot) > DEFAULT_ROBOT_SCALE) {
            animateRobotScale(selectedRobot, robotScaleMap.get(selectedRobot), DEFAULT_ROBOT_SCALE);
        }
        
        // Update the robot's path
        updateRobotPath(selectedRobot, oldX, oldY, selectedRobot.getX(), selectedRobot.getY());
        
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
            Timber.d("[GOAL DEBUG] Target reached! Game complete in " + gameStateManager.getMoveCount().getValue() + " moves and " + gameStateManager.getSquaresMoved().getValue() + " squares moved");
            
            // Critical fix: Tell the GameStateManager the game is complete
            gameStateManager.setGameComplete(true);
            
            announceForAccessibility("Target reached! Game complete in " + 
                gameStateManager.getMoveCount().getValue() + " moves and " +
                gameStateManager.getSquaresMoved().getValue() + " squares moved");
        } else {
            Timber.d("[bbb    n          ddf DEBUG] Robot moved");
            announceForAccessibility(getRobotDescription(selectedRobot));
        }
    }
    
    /**
     * Update the robot's movement path
     * @param robot The robot that moved
     * @param fromX Starting X position
     * @param fromY Starting Y position
     * @param toX Ending X position
     * @param toY Ending Y position
     */
    private void updateRobotPath(GameElement robot, int fromX, int fromY, int toX, int toY) {
        if (robot == null) return;
        
        int color = robot.getColor();

        // Initialize data structures if they don't exist
        if (!robotPaths.containsKey(color)) {
            // Generate a consistent base offset for this robot
            float offsetX = (float) (Math.random() * 2 * BASE_ROBOT_OFFSET_RANGE - BASE_ROBOT_OFFSET_RANGE);
            float offsetY = (float) (Math.random() * 2 * BASE_ROBOT_OFFSET_RANGE - BASE_ROBOT_OFFSET_RANGE);
            robotBaseOffsets.put(color, new float[]{offsetX, offsetY});
            
            // Initialize path and segment count
            robotPaths.put(color, new ArrayList<>());
            segmentCounts.put(color, new HashMap<>());
            
            // Add the starting position
            robotPaths.get(color).add(new int[]{fromX, fromY});
        }
        
        // Create a path segment key (direction matters)
        String segmentKey = fromX + "," + fromY + ":" + toX + "," + toY;
        
        // Get the segment count map for this robot
        HashMap<String, Integer> robotSegments = segmentCounts.get(color);
        
        // Update the segment traversal count
        int count = robotSegments.getOrDefault(segmentKey, 0) + 1;
        robotSegments.put(segmentKey, count);
        
        // Add the new position to the path
        robotPaths.get(color).add(new int[]{toX, toY});
        
        // Redraw the view
        invalidate();
    }
    
    /**
     * Clear all robot paths
     */
    public void clearRobotPaths() {
        robotPaths.clear();
        robotBaseOffsets.clear();
        segmentCounts.clear();
        invalidate();
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
            int gridX = (int) ((x - (getWidth() - (gridWidth * cellSize)) / 2) / cellSize);
            int gridY = (int) ((y - (getHeight() - (gridHeight * cellSize)) / 2) / cellSize);
            
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
     * Get a description of a robot for accessibility
     */
    private String getRobotDescription(GameElement robot) {
        if (robot == null) return "";
        
        String color = GameLogic.getColorName(robot.getColor(), true);
        
        // Find the robot's target if available
        GameState state = gameStateManager.getCurrentState().getValue();
        if (state != null) {
            for (GameElement element : state.getGameElements()) {
                if (element.getType() == GameElement.TYPE_TARGET && element.getColor() == robot.getColor()) {
                    return color + " " + getContext().getString(R.string.robot_position_a11y) + " " + robot.getX() + ", " + robot.getY() + 
                           ". " + getContext().getString(R.string.target_position_a11y) + " " + element.getX() + ", " + element.getY();
                }
            }
        }
        
        return color + " " + getContext().getString(R.string.robot_position_a11y) + " " + robot.getX() + ", " + robot.getY();
    }
    
    /**
     * Get description of what's at a grid position for accessibility
     */
    private String getPositionDescription(int x, int y) {
        if (gameStateManager == null || gameStateManager.getCurrentState().getValue() == null) {
            return getContext().getString(R.string.position_a11y) + " " + x + ", " + y;
        }
        
        GameState state = gameStateManager.getCurrentState().getValue();
        StringBuilder description = new StringBuilder();
        description.append(getContext().getString(R.string.position_a11y) + " " + x + ", " + y + ": ");
        
        // Check for robot
        GameElement robot = state.getRobotAt(x, y);
        if (robot != null) {
            String color = GameLogic.getColorName(robot.getColor(), true);
            description.append(color + " " + getContext().getString(R.string.robot_a11y) + ". ");
            
            // Find the robot's target
            for (GameElement element : state.getGameElements()) {
                if (element.getType() == GameElement.TYPE_TARGET && element.getColor() == robot.getColor()) {
                    description.append(String.format(getContext().getString(R.string.target_a11y), element.getX(), element.getY()) + ". ");
                    break;
                }
            }
        }
        
        // Check for target
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_TARGET && element.getX() == x && element.getY() == y) {
                String color = GameLogic.getColorName(element.getColor(), true);
                if (element.getColor() == Constants.COLOR_MULTI) {
                    color = getContext().getString(R.string.multicolored_a11y);
                }
                description.append(color + " " + String.format(getContext().getString(R.string.target_a11y), element.getX(), element.getY()) + ". ");
                break;
            }
        }
        
        // Check for walls
        boolean hasNorthWall = false;
        boolean hasSouthWall = false;
        boolean hasEastWall = false;
        boolean hasWestWall = false;
        
        // Check for walls around this cell
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_HORIZONTAL_WALL) {
                // Horizontal wall at the north edge of the cell
                if (element.getX() == x && element.getY() == y) {
                    hasNorthWall = true;
                }
                // Horizontal wall at the south edge of the cell
                if (element.getX() == x && element.getY() == y + 1) {
                    hasSouthWall = true;
                }
            }
            
            if (element.getType() == GameElement.TYPE_VERTICAL_WALL) {
                // Vertical wall at the west edge of the cell
                if (element.getX() == x && element.getY() == y) {
                    hasWestWall = true;
                }
                // Vertical wall at the east edge of the cell
                if (element.getX() == x + 1 && element.getY() == y) {
                    hasEastWall = true;
                }
            }
        }
        
        // Add wall description
        if (hasNorthWall || hasSouthWall || hasEastWall || hasWestWall) {
            description.append(getContext().getString(R.string.walls_on_a11y) + ": ");
            if (hasNorthWall) description.append(getContext().getString(R.string.north_a11y) + " ");
            if (hasSouthWall) description.append(getContext().getString(R.string.south_a11y) + " ");
            if (hasEastWall) description.append(getContext().getString(R.string.east_a11y) + " ");
            if (hasWestWall) description.append(getContext().getString(R.string.west_a11y) + " ");
        } else if (robot == null) {
            description.append(getContext().getString(R.string.empty_space_a11y));
        }
        
        return description.toString();
    }
    
    /**
     * Set grid elements directly from an ArrayList of GridElements
     * This is used by ModernGameFragment to update the grid view
     * @param gridElements List of grid elements to display
     */
    public void setGridElements(ArrayList<GridElement> gridElements) {
        if (gridElements == null || gridElements.isEmpty()) {
            Timber.e("[ROBOTS] setGridElements: gridElements is null or empty");
            return;
        }
        
        Timber.d("[ROBOTS] setGridElements: Setting %d grid elements", gridElements.size());
        
        // Find the maximum x and y values to determine grid dimensions
        int maxX = 0;
        int maxY = 0;
        for (GridElement element : gridElements) {
            maxX = Math.max(maxX, element.getX());
            maxY = Math.max(maxY, element.getY());
        }
        
        // Update grid dimensions if needed
        boolean dimensionsChanged = (maxX + 1 != gridWidth) || (maxY + 1 != gridHeight);
        if (dimensionsChanged) {
            gridWidth = maxX + 1;
            gridHeight = maxY + 1;
            Timber.d("[ROBOTS] Grid dimensions updated to %dx%d", gridWidth, gridHeight);
            
            // Re-initialize tile rotations for the new dimensions
            initializeTileRotations();
        }
        
        // Log positions of robots for debugging
        for (GridElement element : gridElements) {
            if (element.getType() != null && element.getType().startsWith("robot_")) {
                // Extract robot color from type (robot_red, robot_blue, etc.)
                String colorStr = element.getType().substring(6); // Remove "robot_" prefix
                int colorId = -1;
                switch (colorStr) {
                    case "red": colorId = 0; break;
                    case "green": colorId = 1; break;
                    case "blue": colorId = 2; break;
                    case "yellow": colorId = 3; break;
                    case "silver": colorId = 4; break;
                }
                Timber.d("[ROBOTS] setGridElements: Robot %s at position (%d,%d)", 
                        colorStr, element.getX(), element.getY());
            }
        }
        
        // Force redraw
        invalidate();
    }

    private boolean isAccessibilityActive() {
        // Check system TalkBack status
        AccessibilityManager accessibilityManager = (AccessibilityManager) getContext()
                .getSystemService(Context.ACCESSIBILITY_SERVICE);
        
        boolean talkbackActive = accessibilityManager != null && accessibilityManager.isTouchExplorationEnabled();
        
        boolean isActive = talkbackActive || Preferences.accessibilityMode;
        // Timber.d("[ACCESSIBILITY] Coordinate display: %s (TalkBack: %s, App setting: %s)", isActive ? "showing" : "hidden", talkbackActive ? "enabled" : "disabled", Preferences.accessibilityMode ? "enabled" : "disabled");
        
        return isActive;
    }
}
