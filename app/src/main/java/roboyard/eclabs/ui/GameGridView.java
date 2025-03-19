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
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.core.content.ContextCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import roboyard.eclabs.R;

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
    
    /**
     * Constructor for programmatic creation
     */
    public GameGridView(Context context) {
        super(context);
        init();
    }
    
    /**
     * Constructor for XML inflation
     */
    public GameGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    /**
     * Constructor for XML inflation with style
     */
    public GameGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    /**
     * Initialize paint objects and accessibility support
     */
    private void init() {
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
        Context context = getContext();
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
     * Set the game state manager
     * @param manager Game state manager
     */
    public void setGameStateManager(GameStateManager manager) {
        this.gameStateManager = manager;
        if (gameStateManager != null && gameStateManager.getCurrentState().getValue() != null) {
            GameState state = gameStateManager.getCurrentState().getValue();
            gridWidth = state.getWidth();
            gridHeight = state.getHeight();
        }
        invalidate();
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
        
        // Draw grid cells
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                float left = x * cellSize;
                float top = y * cellSize;
                float right = left + cellSize;
                float bottom = top + cellSize;
                
                // Set color based on cell type
                int cellType = state.getCellType(x, y);
                if (cellType == 1) { // Wall
                    Drawable wallDrawable;
                    if (isHorizontalWall(state, x, y)) {
                        wallDrawable = wallHorizontal;
                    } else {
                        wallDrawable = wallVertical;
                    }
                    
                    if (wallDrawable != null) {
                        // Draw wall using drawable
                        wallDrawable.setBounds((int)left, (int)top, (int)right, (int)bottom);
                        wallDrawable.draw(canvas);
                    } else {
                        // Fallback to color
                        cellPaint.setColor(Color.DKGRAY);
                        canvas.drawRect(left, top, right, bottom, cellPaint);
                    }
                } else if (cellType == 2) { // Target
                    // Find the target element for this position to determine color
                    GameElement targetElement = null;
                    for (GameElement element : state.getGameElements()) {
                        if (element.getType() == GameElement.TYPE_TARGET && 
                            element.getX() == x && element.getY() == y) {
                            targetElement = element;
                            break;
                        }
                    }
                    
                    Drawable targetDrawable = getTargetDrawable(targetElement);
                    if (targetDrawable != null) {
                        // Draw target using drawable
                        targetDrawable.setBounds((int)left, (int)top, (int)right, (int)bottom);
                        targetDrawable.draw(canvas);
                    } else {
                        // Fallback to color
                        cellPaint.setColor(Color.rgb(60, 60, 90));
                        canvas.drawRect(left, top, right, bottom, cellPaint);
                    }
                } else { // Empty
                    cellPaint.setColor(Color.rgb(30, 30, 60));
                    canvas.drawRect(left, top, right, bottom, cellPaint);
                }
                
                // Highlight focused cell for accessibility
                if (x == focusedX && y == focusedY) {
                    cellPaint.setColor(Color.rgb(80, 80, 120));
                    canvas.drawRect(left, top, right, bottom, cellPaint);
                }
                
                // Draw grid lines
                gridPaint.setColor(Color.rgb(40, 40, 70));
                canvas.drawRect(left, top, right, bottom, gridPaint);
            }
        }
        
        // Draw robots
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
                
                // Highlight selected robot
                if (element.isSelected() || (state.getSelectedRobot() == element)) {
                    // Create a white rectangle behind the robot to highlight it
                    Paint highlightPaint = new Paint();
                    highlightPaint.setColor(Color.WHITE);
                    highlightPaint.setStyle(Paint.Style.STROKE);
                    highlightPaint.setStrokeWidth(4);
                    canvas.drawRect(left, top, right, bottom, highlightPaint);
                }
                
                // Draw the robot using the drawable
                if (robotDrawable != null) {
                    robotDrawable.setBounds((int)left, (int)top, (int)right, (int)bottom);
                    robotDrawable.draw(canvas);
                } else {
                    // Fallback to circle if drawable not available
                    float centerX = (element.getX() + 0.5f) * cellSize;
                    float centerY = (element.getY() + 0.5f) * cellSize;
                    float radius = cellSize * 0.4f;
                    
                    switch (element.getColor()) {
                        case 0: robotPaint.setColor(Color.RED); break;
                        case 1: robotPaint.setColor(Color.GREEN); break;
                        case 2: robotPaint.setColor(Color.BLUE); break;
                        case 3: robotPaint.setColor(Color.YELLOW); break;
                        default: robotPaint.setColor(Color.MAGENTA); break;
                    }
                    
                    canvas.drawCircle(centerX, centerY, radius, robotPaint);
                }
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
        
        // Handle the touch event
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Handle touch down - select robot
                GameState state = gameStateManager.getCurrentState().getValue();
                if (state != null) {
                    GameElement robot = state.getRobotAt(gridX, gridY);
                    if (robot != null) {
                        // Select this robot
                        state.setSelectedRobot(robot);
                        announceForAccessibility("Selected " + getRobotDescription(robot));
                        invalidate();
                        return true;
                    }
                }
                break;
                
            case MotionEvent.ACTION_UP:
                // Handle touch up - move selected robot
                state = gameStateManager.getCurrentState().getValue();
                if (state != null && state.getSelectedRobot() != null) {
                    GameElement selectedRobot = state.getSelectedRobot();
                    
                    // Try to move the robot
                    boolean moved = state.moveRobotTo(selectedRobot, gridX, gridY);
                    if (moved) {
                        // Update move count using proper method
                        gameStateManager.incrementMoveCount();
                        int newMoveCount = gameStateManager.getMoveCount().getValue();
                        
                        // Check if game is complete
                        if (state.checkCompletion()) {
                            gameStateManager.setGameComplete(true);
                            announceForAccessibility("Goal reached! Game complete in " + newMoveCount + " moves");
                        } else {
                            announceForAccessibility(getRobotDescription(selectedRobot) + " moved");
                        }
                        
                        invalidate();
                        return true;
                    }
                }
                break;
        }
        
        return super.onTouchEvent(event);
    }
    
    /**
     * Handle hover events for accessibility (TalkBack)
     */
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
            return super.dispatchHoverEvent(event);
        }
        
        if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER ||
            event.getAction() == MotionEvent.ACTION_HOVER_MOVE) {
            
            // Update focused cell if changed
            if (focusedX != gridX || focusedY != gridY) {
                focusedX = gridX;
                focusedY = gridY;
                invalidate();
                
                // Announce what's at this position
                GameState state = gameStateManager.getCurrentState().getValue();
                if (state != null) {
                    GameElement robot = state.getRobotAt(gridX, gridY);
                    if (robot != null) {
                        announceForAccessibility(getRobotDescription(robot));
                    } else {
                        int cellType = state.getCellType(gridX, gridY);
                        if (cellType == 1) {
                            announceForAccessibility("Wall at position " + gridX + ", " + gridY);
                        } else if (cellType == 2) {
                            announceForAccessibility("Target at position " + gridX + ", " + gridY);
                        } else {
                            announceForAccessibility("Empty space at position " + gridX + ", " + gridY);
                        }
                    }
                }
            }
            
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
            // Clear focus when exiting
            focusedX = -1;
            focusedY = -1;
            invalidate();
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
}
