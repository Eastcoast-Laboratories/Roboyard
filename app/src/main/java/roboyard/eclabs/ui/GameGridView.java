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
    private final int focusedX = -1;
    private final int focusedY = -1;
    
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
    private static final float DEFAULT_ROBOT_SCALE = 1.1f; // 10% larger by default
    private final boolean enableRobotAnimation = true;
    private final HashMap<GameElement, Float> robotScaleMap = new HashMap<>(); // Track current scale for each robot
    private final GameElement focusedRobot = null; // Currently focused (hovered) robot
    private final android.view.animation.DecelerateInterpolator easeInterpolator = new android.view.animation.DecelerateInterpolator(1.5f); // Ease function
    private final android.os.Handler animationHandler = new android.os.Handler(android.os.Looper.getMainLooper()); // Animation handler
    private final long animationDuration = 300; // Animation duration in milliseconds
    
    // Wall configuration
    private static float WALL_THICKNESS_FACTOR = 0.675f; // Default is cellSize / 8 * 3 (3 times thicker)
    private static float WALL_OFFSET_FACTOR = 0.3f; // walls are a bit longer than the cellsoverlap)
    
    // Store starting positions of robots
    private final HashMap<Integer, int[]> robotStartingPositions = new HashMap<>(); // Map robot color to starting position [x,y]
    
    // Grid background
    private Drawable gridTileDrawable; 
    private Drawable backgroundLogo; // Background logo
    private Fragment fragment; // Parent fragment
    
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
        cellPaint.setAntiAlias(true);
        
        robotPaint = new Paint();
        robotPaint.setStyle(Paint.Style.FILL);
        robotPaint.setAntiAlias(true);
        
        targetPaint = new Paint();
        targetPaint.setStyle(Paint.Style.FILL);
        targetPaint.setAntiAlias(true);
        
        textPaint = new Paint();
        textPaint.setTextSize(40);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        
        gridPaint = new Paint();
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1);
        
        // Load robot drawables
        redRobotRight = ContextCompat.getDrawable(context, R.drawable.robot_red_right);
        yellowRobotRight = ContextCompat.getDrawable(context, R.drawable.robot_yellow_right);
        blueRobotRight = ContextCompat.getDrawable(context, R.drawable.robot_blue_right);
        greenRobotRight = ContextCompat.getDrawable(context, R.drawable.robot_green_right);
        
        redRobotLeft = ContextCompat.getDrawable(context, R.drawable.robot_red_left);
        yellowRobotLeft = ContextCompat.getDrawable(context, R.drawable.robot_yellow_left);
        blueRobotLeft = ContextCompat.getDrawable(context, R.drawable.robot_blue_left);
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
        
        // Draw grid cells - board background first
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                float left = x * cellSize;
                float top = y * cellSize;
                float right = left + cellSize;
                float bottom = top + cellSize;
                
                // Draw grid tile background
                if (gridTileDrawable != null) {
                    gridTileDrawable.setBounds((int)left, (int)top, (int)right, (int)bottom);
                    gridTileDrawable.draw(canvas);
                } else {
                    // Fallback to colored background if drawable is not available
                    cellPaint.setColor(Color.rgb(30, 30, 60));
                    canvas.drawRect(left, top, right, bottom, cellPaint);
                }
                
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
        
        // Draw the Roboyard logo in the center of the board AFTER the background tiles
        // but BEFORE the walls and other game elements
        if (backgroundLogo != null) {
            // Calculate the center of the board
            float boardWidth = gridWidth * cellSize;
            float boardHeight = gridHeight * cellSize;
            float centerX = boardWidth / 2;
            float centerY = boardHeight / 2;
            
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

                float offset = cellSize * WALL_OFFSET_FACTOR; 
                float left = x * cellSize - offset; 
                float top = y * cellSize; 
                float right = left + cellSize + 2 * offset;
                float wallThickness = cellSize * WALL_THICKNESS_FACTOR;

                // Draw horizontal wall - between y and y+1
                if (wallHorizontal != null) {
                    wallHorizontal.setBounds((int)left, (int)(top - wallThickness/2), 
                                             (int)right, (int)(top + wallThickness/2));
                    wallHorizontal.draw(canvas);
                } else {
                    Timber.d("Horizontal wall drawable not available");
                }
            } 
            else if (element.getType() == GameElement.TYPE_VERTICAL_WALL) {
                // Vertical walls are drawn between columns (separating cells horizontally)
                int x = element.getX(); 
                int y = element.getY();

                float offset = cellSize * WALL_OFFSET_FACTOR; 
                float left = x * cellSize; 
                float top = y * cellSize - offset;
                float bottom = top + cellSize + 2 * offset;
                float wallThickness = cellSize * WALL_THICKNESS_FACTOR;
                
                // Draw vertical wall - between x and x+1
                if (wallVertical != null) {
                    wallVertical.setBounds((int)(left - wallThickness/2), (int)top,
                                           (int)(left + wallThickness/2), (int)bottom);
                    wallVertical.draw(canvas);
                } else {
                    Timber.d("Vertical wall drawable not available");
                }
            }
        }
        
        // Draw starting positions of robots
        for (int color : robotStartingPositions.keySet()) {
            int[] position = robotStartingPositions.get(color);
            int x = position[0];
            int y = position[1];
            
            float left = x * cellSize;
            float top = y * cellSize;
            float right = left + cellSize;
            float bottom = top + cellSize;
            
            // Select appropriate robot drawable based on color
            Drawable robotDrawable = null;
            
            switch (color) {
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
            
            if (robotDrawable != null) {
                // Create a copy of the drawable to avoid affecting the original
                Drawable markerDrawable = robotDrawable.getConstantState().newDrawable().mutate();
                // Draw robot using drawable
                markerDrawable.setBounds((int)left, (int)top, (int)right, (int)bottom);
                markerDrawable.setAlpha(76); // Set alpha to 30% for transparency
                markerDrawable.draw(canvas);
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
        float scale = DEFAULT_ROBOT_SCALE;
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
            } else {
                // Initialize the scale in the map for future animations
                robotScaleMap.put(robot, DEFAULT_ROBOT_SCALE);
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
            
            // Create a copy of the drawable to ensure we don't affect other instances
            Drawable robotDrawableCopy = robotDrawable.getConstantState().newDrawable().mutate();
            robotDrawableCopy.setBounds(
                (int) scaledLeft,
                (int) scaledTop,
                (int) scaledRight,
                (int) scaledBottom
            );
            robotDrawableCopy.setAlpha(255); // Ensure full opacity for the actual robots
            robotDrawableCopy.draw(canvas);
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
                // Check if user tapped on the currently selected robot to deselect it
                if (gridX == selectedRobot.getX() && gridY == selectedRobot.getY()) {
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
                if (clickedRobot != null && clickedRobot != selectedRobot) {
                    // User clicked on a different robot, select it instead of moving
                    Timber.d("Selecting a different robot at (%d,%d)", gridX, gridY);
                    gameStateManager.handleGridTouch(gridX, gridY, action);
                    return true;
                }
                
                int robotX = selectedRobot.getX();
                int robotY = selectedRobot.getY();
                
                // Store original position for change detection
                int oldX = robotX;
                int oldY = robotY;
                
                // If the click is not on the same row AND not on the same column as the robot,
                // use a more intuitive direction detection algorithm based on position relative to robot
                if (robotX != gridX && robotY != gridY) {
                    // Calculate relative position from robot
                    int deltaX = gridX - robotX; // Positive: east, Negative: west
                    int deltaY = gridY - robotY; // Positive: south, Negative: north
                    
                    // Absolute values of the deltas to determine distance
                    int absDeltaX = Math.abs(deltaX);
                    int absDeltaY = Math.abs(deltaY);
                    
                    // Determine primary direction based on the position pattern as requested:
                    // North: directly above or within 2 cols to the side and at least 2 rows up
                    //        or within 3 rows up and 2 cols to the side
                    // South: directly below or within 2 cols to the side and at least 2 rows down
                    //        or within 3 rows down and 2 cols to the side
                    // East:  directly to the right or within 2 rows and at least 2 cols right
                    //        or within 3 cols right and 2 rows up/down
                    // West:  directly to the left or within 2 rows and at least 2 cols left
                    //        or within 3 cols left and 2 rows up/down
                    
                    boolean moveNorth = (deltaY < 0) && 
                                     ((absDeltaX == 0) || 
                                      (absDeltaX <= 2 && absDeltaY >= 2) || 
                                      (absDeltaX <= 2 && absDeltaY >= 3));
                    
                    boolean moveSouth = (deltaY > 0) && 
                                     ((absDeltaX == 0) || 
                                      (absDeltaX <= 2 && absDeltaY >= 2) || 
                                      (absDeltaX <= 2 && absDeltaY >= 3));
                    
                    boolean moveEast = (deltaX > 0) && 
                                    ((absDeltaY == 0) || 
                                     (absDeltaY <= 2 && absDeltaX >= 2) || 
                                     (absDeltaY <= 2 && absDeltaX >= 3));
                    
                    boolean moveWest = (deltaX < 0) && 
                                    ((absDeltaY == 0) || 
                                     (absDeltaY <= 2 && absDeltaX >= 2) || 
                                     (absDeltaY <= 2 && absDeltaX >= 3));
                    
                    // Determine the primary direction - priority: north, south, east, west
                    if (moveNorth) {
                        // North direction
                        gridX = robotX;
                        // Keep the y-coordinate as it is (it's already north of the robot)
                    } else if (moveSouth) {
                        // South direction
                        gridX = robotX;
                        // Keep the y-coordinate as it is (it's already south of the robot)
                    } else if (moveEast) {
                        // East direction
                        // Keep the x-coordinate as it is (it's already east of the robot)
                        gridY = robotY;
                    } else if (moveWest) {
                        // West direction
                        // Keep the x-coordinate as it is (it's already west of the robot)
                        gridY = robotY;
                    } else {
                        // If no clear direction, use absolute distance as fallback
                        if (absDeltaX <= absDeltaY) {
                            // Vertical movement is preferred
                            gridX = robotX;
                        } else {
                            // Horizontal movement is preferred
                            gridY = robotY;
                        }
                    }
                    
                    Timber.d("Adjusted movement to direction: (%d,%d), deltaX=%d, deltaY=%d", 
                            gridX, gridY, deltaX, deltaY);
                }
                // If click is already in same row or column, no adjustment needed - we keep the valid direction
                
                // Let GameStateManager handle the touch which will update counters properly
                gameStateManager.handleGridTouch(gridX, gridY, action);
                
                // Check if robot moved by comparing old and new positions
                Timber.d("[GOAL DEBUG] Selected robot moved from " + oldX + ", " + oldY + " to " + selectedRobot.getX() + ", " + selectedRobot.getY());
                if (oldX != selectedRobot.getX() || oldY != selectedRobot.getY()) {
                    // Shrink the robot back to default size when it starts moving
                    if (robotScaleMap.containsKey(selectedRobot) && robotScaleMap.get(selectedRobot) > DEFAULT_ROBOT_SCALE) {
                        animateRobotScale(selectedRobot, robotScaleMap.get(selectedRobot), DEFAULT_ROBOT_SCALE);
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
