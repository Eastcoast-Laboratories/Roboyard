package roboyard.eclabs.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import roboyard.eclabs.Constants;
import roboyard.eclabs.GridElement;
import roboyard.eclabs.R;
import roboyard.eclabs.util.BoardSizeManager;
import roboyard.eclabs.util.DifficultyManager;
import roboyard.eclabs.util.UIModeManager;
import roboyard.pm.ia.IGameMove;
import roboyard.pm.ia.ricochet.RRGameMove;
import roboyard.pm.ia.ricochet.ERRGameMove;
import roboyard.eclabs.util.SoundManager;
import roboyard.pm.ia.GameSolution;

import timber.log.Timber;

// Added imports for accessibility
import androidx.core.view.accessibility.AccessibilityManagerCompat;
import android.view.accessibility.AccessibilityManager;
import android.content.res.ColorStateList;
import android.graphics.Color;

/**
 * Modern UI implementation of the game screen.
 * all Android-native UI
 * the layout is defined in the xml file app/src/main/res/layout/fragment_modern_game.xml
 */
public class ModernGameFragment extends BaseGameFragment implements GameStateManager.SolutionCallback {
    
    private GameGridView gameGridView;
    private TextView moveCountTextView;
    private TextView squaresMovedTextView;
    private TextView difficultyTextView;
    private TextView boardSizeTextView;
    private Button backButton;
    private Button resetRobotsButton;
    private Button hintButton;
    private Button saveMapButton;
    private Button restartButton;
    private Button menuButton;
    private TextView timerTextView;
    private TextView statusTextView;
    
    // Class member to track if a robot is currently selected
    private boolean isRobotSelected = false;
    
    // TalkBack accessibility controls
    private ViewGroup accessibilityControlsContainer;
    private Button btnMoveNorth;
    private Button btnMoveSouth;
    private Button btnMoveEast;
    private Button btnMoveWest;
    private TextView txtSelectedRobot;
    private TextView txtRobotGoal;
    private Button btnToggleAccessibilityControls;
    private boolean accessibilityControlsVisible = false;
    
    // Timer variables
    private long startTime = 0L;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private boolean timerRunning = false;
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = SystemClock.elapsedRealtime() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            
            // Format time as mm:ss
            String timeStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
            timerTextView.setText(timeStr);
            
            // Continue updating the timer
            timerHandler.postDelayed(this, 500); // Update every half-second
        }
    };
    
    // Sound manager for game sound effects
    private SoundManager soundManager;
    
    /**
     * Check if TalkBack is enabled
     * @return true if TalkBack is enabled
     */
    private boolean isTalkBackEnabled() {
        AccessibilityManager am = (AccessibilityManager) requireContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        return am != null && am.isEnabled() && am.isTouchExplorationEnabled();
    }
    
    /**
     * Update directional button text and colors based on selected robot
     */
    private void updateDirectionalButtons(GameElement selectedRobot) {
        if (selectedRobot == null) {
            btnMoveNorth.setText("North");
            btnMoveSouth.setText("South");
            btnMoveEast.setText("East");
            btnMoveWest.setText("West");
            
            // Reset colors
            btnMoveNorth.setBackgroundResource(R.drawable.button_rounded_blue_outline);
            btnMoveSouth.setBackgroundResource(R.drawable.button_rounded_blue_outline);
            btnMoveEast.setBackgroundResource(R.drawable.button_rounded_blue_outline);
            btnMoveWest.setBackgroundResource(R.drawable.button_rounded_blue_outline);
            return;
        }
        
        // Set the color based on the robot
        int backgroundColor = getColorForRobot(selectedRobot);
        ColorStateList colorStateList = ColorStateList.valueOf(backgroundColor);
        
        String robotColorName = getRobotColorNameByGridElement(selectedRobot);
        
        btnMoveNorth.setText(robotColorName + " North");
        btnMoveSouth.setText(robotColorName + " South");
        btnMoveEast.setText(robotColorName + " East");
        btnMoveWest.setText(robotColorName + " West");
        
        // Set button backgrounds to match robot color
        btnMoveNorth.setBackgroundTintList(colorStateList);
        btnMoveSouth.setBackgroundTintList(colorStateList);
        btnMoveEast.setBackgroundTintList(colorStateList);
        btnMoveWest.setBackgroundTintList(colorStateList);
        
        // Log the button information
        Timber.d("[TALKBACK] Direction buttons updated for %s robot", robotColorName);
    }
    
    /**
     * Get color for a robot
     */
    private int getColorForRobot(GameElement robot) {
        if (robot == null) return Color.BLUE;
        
        switch (robot.getColor()) {
            case 0: return Color.RED;
            case 1: return Color.GREEN;
            case 2: return Color.BLUE;
            case 3: return Color.YELLOW;
            default: return Color.DKGRAY;
        }
    }
    
    /**
     * Find target for a specific robot
     */
    private GameElement findTargetForRobot(GameElement robot, GameState state) {
        if (robot == null || state == null) return null;
        
        // Find the target with the same color as the robot
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_TARGET && element.getColor() == robot.getColor()) {
                return element;
            }
        }
        
        return null;
    }
    
    /**
     * Update the robot selection information in the UI
     * @param robot The selected robot
     */
    private void updateRobotSelectionInfo(GameElement robot) {
        if (robot == null) {
            if (txtSelectedRobot != null) {
                txtSelectedRobot.setText("No robot selected");
            }
            if (txtRobotGoal != null) {
                txtRobotGoal.setText("");
            }
            announceAccessibility("No robot selected");
            return;
        }
        
        // Get robot color and position
        String colorName = getRobotColorNameByGridElement(robot);
        int x = robot.getX();
        int y = robot.getY();
        
        // Update selected robot text
        if (txtSelectedRobot != null) {
            txtSelectedRobot.setText("Selected: " + colorName + " robot at (" + x + ", " + y + ")");
        }
        
        // Find the robot's goal
        GameState state = gameStateManager.getCurrentState().getValue();
        if (state != null) {
            for (GameElement element : state.getGameElements()) {
                if (element.getType() == GameElement.TYPE_TARGET && element.getColor() == robot.getColor()) {
                    int goalX = element.getX();
                    int goalY = element.getY();
                    
                    // Update goal text
                    if (txtRobotGoal != null) {
                        txtRobotGoal.setText("Goal: (" + goalX + ", " + goalY + ")");
                    }
                    
                    // Announce selection and goal via TalkBack
                    String message = "Selected " + colorName + " robot at (" + x + ", " + y + "). ";
                    message += "Its goal is at (" + goalX + ", " + goalY + ")";
                    announceAccessibility(message);
                    
                    // Announce possible moves
                    announcePossibleMoves(robot);
                    return;
                }
            }
            
            // No goal found for this robot
            if (txtRobotGoal != null) {
                txtRobotGoal.setText("No goal for this robot");
            }
            announceAccessibility("Selected " + colorName + " robot at (" + x + ", " + y + "). No goal found.");
            
            // Announce possible moves even if there's no goal
            announcePossibleMoves(robot);
        }
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the sound manager
        soundManager = SoundManager.getInstance(requireContext());
        
        // Handle back button/gesture with improved behavior
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Get the game state manager
                GameStateManager gameStateManager = new ViewModelProvider(requireActivity()).get(GameStateManager.class);
                GameState gameState = gameStateManager.getCurrentState().getValue();
                
                // Check if a robot is currently selected/active
                if (gameState != null && gameState.getSelectedRobot() != null) {
                    // A robot is active, cancel selection instead of going back
                    Timber.d("Back pressed with active robot - canceling robot selection");
                    
                    // Move robot away from the edge if it's at one
                    GameElement robot = gameState.getSelectedRobot();
                    int robotX = robot.getX();
                    int robotY = robot.getY();
                    int boardWidth = gameState.getWidth();
                    int boardHeight = gameState.getHeight();
                    
                    // Check if robot is at an edge and move it inward
                    boolean moved = false;
                    if (robotX == 0) { // Left edge
                        moved = gameState.moveRobotTo(robot, 1, robotY);
                        Timber.d("Moving robot from left edge inward: %s", moved ? "success" : "failed");
                    } else if (robotX == boardWidth - 1) { // Right edge
                        moved = gameState.moveRobotTo(robot, boardWidth - 2, robotY);
                        Timber.d("Moving robot from right edge inward: %s", moved ? "success" : "failed");
                    } else if (robotY == 0) { // Top edge
                        moved = gameState.moveRobotTo(robot, robotX, 1);
                        Timber.d("Moving robot from top edge inward: %s", moved ? "success" : "failed");
                    } else if (robotY == boardHeight - 1) { // Bottom edge
                        moved = gameState.moveRobotTo(robot, robotX, boardHeight - 2);
                        Timber.d("Moving robot from bottom edge inward: %s", moved ? "success" : "failed");
                    }
                    
                    if (moved) {
                        Toast.makeText(requireContext(), "Robot moved away from edge", Toast.LENGTH_SHORT).show();
                    } else {
                        // Deselect the robot if it couldn't be moved
                        gameState.setSelectedRobot(null);
                        isRobotSelected = false;
                        
                        // Refresh the view if it's initialized
                        if (gameGridView != null) {
                            gameGridView.invalidate();
                        }
                        
                        Toast.makeText(requireContext(), "Robot movement canceled", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // No robot is active, allow normal back navigation
                    Timber.d("Back pressed without active robot - allowing navigation");
                    this.remove(); // Remove this callback
                    requireActivity().onBackPressed(); // Continue with back navigation
                }
            }
        });
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_modern_game, container, false);
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize the game state manager
        gameStateManager = new ViewModelProvider(requireActivity()).get(GameStateManager.class);
        
        // Initialize UI components
        gameGridView = view.findViewById(R.id.game_grid_view);
        moveCountTextView = view.findViewById(R.id.move_count_text);
        squaresMovedTextView = view.findViewById(R.id.squares_moved_text);
        difficultyTextView = view.findViewById(R.id.difficulty_text);
        boardSizeTextView = view.findViewById(R.id.board_size_text);
        timerTextView = view.findViewById(R.id.game_timer);
        statusTextView = view.findViewById(R.id.status_text_view);
        
        // Initialize sound manager
        soundManager = SoundManager.getInstance(requireContext());
        
        // Initialize accessibility controls
        btnToggleAccessibilityControls = view.findViewById(R.id.btn_toggle_accessibility);
        accessibilityControlsContainer = view.findViewById(R.id.accessibility_container);
        
        if (accessibilityControlsContainer != null) {
            txtSelectedRobot = accessibilityControlsContainer.findViewById(R.id.txt_selected_robot);
            txtRobotGoal = accessibilityControlsContainer.findViewById(R.id.txt_robot_goal);
            btnMoveNorth = accessibilityControlsContainer.findViewById(R.id.btn_move_north);
            btnMoveSouth = accessibilityControlsContainer.findViewById(R.id.btn_move_south);
            btnMoveEast = accessibilityControlsContainer.findViewById(R.id.btn_move_east);
            btnMoveWest = accessibilityControlsContainer.findViewById(R.id.btn_move_west);
            
            // Set up directional button click listeners
            setupAccessibilityControls();
        }
        
        // Check for TalkBack and show accessibility controls toggle if needed
        if (isTalkBackEnabled()) {
            btnToggleAccessibilityControls.setVisibility(View.VISIBLE);
            btnToggleAccessibilityControls.setOnClickListener(v -> toggleAccessibilityControls());
        }
        
        // Set up the game grid view
        gameGridView.setFragment(this);
        gameGridView.setGameStateManager(gameStateManager);
        
        // Set up the UI mode manager
        UIModeManager uiModeManager = UIModeManager.getInstance(requireContext());
        uiModeManager.setUIMode(UIModeManager.MODE_MODERN);
        
        // Get board size from BoardSizeManager
        BoardSizeManager boardSizeManager = BoardSizeManager.getInstance(requireContext());
        int boardWidth = boardSizeManager.getBoardWidth();
        int boardHeight = boardSizeManager.getBoardHeight();
        
        // Update difficulty and board size text
        updateDifficulty();
        updateBoardSizeText();
        
        // Set up observers for game state
        setupObservers();

        // Set up button click listeners
        setupButtons(view);
        
        // Initialize the game
        initializeGame();
    }
    
    /**
     * Set up observers for game state
     */
    private void setupObservers() {
        // Observe current game state
        gameStateManager.getCurrentState().observe(getViewLifecycleOwner(), this::updateGameState);
        
        // Observe move count
        gameStateManager.getMoveCount().observe(getViewLifecycleOwner(), this::updateMoveCount);
        
        // Observe squares moved
        gameStateManager.getSquaresMoved().observe(getViewLifecycleOwner(), this::updateSquaresMoved);
        
        // Observe game completion
        gameStateManager.isGameComplete().observe(getViewLifecycleOwner(), isComplete -> {
            if (isComplete) {
                playSound("win");
            }
        });
        
        // Observe solver running state to update hint button text
        gameStateManager.isSolverRunning().observe(getViewLifecycleOwner(), isRunning -> {
            if (isRunning) {
                // Change hint button text to "Cancel" while calculating
                hintButton.setText("Cancel");
                statusTextView.setText("Calculating solution...");
                statusTextView.setVisibility(View.VISIBLE);
            } else {
                // Reset hint button text
                hintButton.setText("Hint");
                statusTextView.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Set up button click listeners
     */
    private void setupButtons(View view) {
        // (Button text: "Back")
        // Back button - undo the last robot movement
        backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            Timber.d("ModernGameFragment: Back button clicked");
            // Undo the last move
            if (gameStateManager.undoLastMove()) {
                Toast.makeText(requireContext(), "Move undone", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Nothing to undo", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Button text: "Reset"
        // Reset robots button - reset robots to starting positions without changing the map
        resetRobotsButton = view.findViewById(R.id.reset_robots_button);
        resetRobotsButton.setOnClickListener(v -> {
            Timber.d("ModernGameFragment: Reset robots button clicked");
            // Reset the robots
            gameStateManager.resetRobots();
            Toast.makeText(requireContext(), "Robots reset to starting positions", Toast.LENGTH_SHORT).show();
        });
        
        // (Button text: "Hint")
        // Hint button - get hint from the solver
        hintButton = view.findViewById(R.id.hint_button);
        hintButton.setOnClickListener(v -> {
            Timber.d("ModernGameFragment: Hint button clicked");
            
            // Check if solver is currently running
            if (Boolean.TRUE.equals(gameStateManager.isSolverRunning().getValue())) {
                // If solver is running, pressing the button should cancel
                Timber.d("ModernGameFragment: Cancelling solver");
                gameStateManager.cancelSolver();
                statusTextView.setText("Hint calculation cancelled");
                statusTextView.setVisibility(View.VISIBLE);
                return;
            }
            
            // Check if we have a solution object at all
            GameSolution solution = gameStateManager.getCurrentSolution();
            if (solution == null || solution.getMoves() == null) {
                Timber.d("ModernGameFragment: No solution available, calculating...");
                statusTextView.setText("Calculating solution... Please wait a moment and try again.");
                statusTextView.setVisibility(View.VISIBLE);
                
                // Start calculating a solution
                gameStateManager.calculateSolutionAsync(this);
                return;
            }
            
            // Check if we've already shown all available hints
            int currentStep = gameStateManager.getCurrentSolutionStep();
            int totalMoves = solution.getMoves().size();
            
            if (currentStep >= totalMoves) {
                Timber.d("ModernGameFragment: All hints have been shown (%d/%d), resetting to first hint", currentStep, totalMoves);
                // Reset the solution step counter to show the first hint again
                gameStateManager.resetSolutionStep();
                
                // Now get the first hint
                IGameMove hintMove = gameStateManager.getHint();
                
                if (hintMove != null && hintMove instanceof RRGameMove) {
                    // Cast to RRGameMove to access the proper methods
                    RRGameMove rrMove = (RRGameMove) hintMove;
                    
                    // Convert the robotic move to human-readable text
                    String robotColor = getRobotColorName(rrMove.getColor());
                    String direction = getDirectionName(rrMove.getDirection());
                    
                    // Update the status text with the hint (now showing hint 1 again)
                    String hintText = String.format("Hint 1/%d: Move the %s robot %s", 
                            totalMoves, robotColor, direction);
                    statusTextView.setText(hintText);
                    statusTextView.setVisibility(View.VISIBLE);
                }
                return;
            }
            
            // Get a hint from the game state manager
            IGameMove hintMove = gameStateManager.getHint();
            Timber.d("ModernGameFragment: Received hint move: %s", hintMove);
            
            if (hintMove != null && hintMove instanceof RRGameMove) {
                // Cast to RRGameMove to access the proper methods
                RRGameMove rrMove = (RRGameMove) hintMove;
                
                // Log the details of the move
                Timber.d("[HINT] ModernGameFragment: Robot color: %d, Direction: %d", 
                        rrMove.getColor(), rrMove.getDirection());
                
                // Convert the robotic move to human-readable text
                String robotColor = getRobotColorName(rrMove.getColor());
                String direction = getDirectionName(rrMove.getDirection());
                
                // Log details for debugging
                Timber.d("[HINT] Robot color ID: %d, mapped to color name: %s", 
                        rrMove.getColor(), robotColor);
                
                // Update the status text with the hint number and the hint itself
                String hintText = String.format("Hint %d/%d: Move the %s robot %s", 
                        currentStep, totalMoves, robotColor, direction);
                statusTextView.setText(hintText);
                statusTextView.setVisibility(View.VISIBLE);
                
                Timber.d("ModernGameFragment: Displayed hint: %s", hintText);
            } else {
                // No solution found
                statusTextView.setText("No solution available");
                statusTextView.setVisibility(View.VISIBLE);
                Timber.d("ModernGameFragment: No valid hint move available");
            }
        });
        
        // (Button text: "Save Map")
        // Save map button - navigate to save screen
        saveMapButton = view.findViewById(R.id.save_map_button);
        saveMapButton.setOnClickListener(v -> {
            Timber.d("ModernGameFragment: Save map button clicked");
            try {
                // Create a new SaveGameFragment instance
                SaveGameFragment saveFragment = new SaveGameFragment();
                
                // Create a bundle with the saveMode argument
                Bundle args = new Bundle();
                args.putBoolean("saveMode", true);
                saveFragment.setArguments(args);
                
                // Perform the fragment transaction
                requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, saveFragment)
                    .addToBackStack(null)
                    .commit();
                
                Timber.d("Navigation to save screen completed using fragment transaction");
            } catch (Exception e) {
                Timber.e(e, "Error navigating to save screen");
                Toast.makeText(requireContext(), "Cannot navigate to save screen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        // (Button text: "New Game")
        // Restart button - restart the current game
        restartButton = view.findViewById(R.id.restart_button);
        restartButton.setOnClickListener(v -> {
            Timber.d("ModernGameFragment: Restart button clicked. calling startModernGame()");
            // Start a new game
            gameStateManager.startModernGame();
            // Reset timer
            stopTimer();
            startTimer();
            // Clear any hint text from the status display
            statusTextView.setText("");
            statusTextView.setVisibility(View.GONE);
            // Announce the robots and their positions
            announceGameStart();
        });
        
        // (Button text: "Menu")
        // Menu button - go back to main menu
        menuButton = view.findViewById(R.id.menu_button);
        menuButton.setOnClickListener(v -> {
            Timber.d("ModernGameFragment: Menu button clicked");
            // Navigate back to main menu
            navigateTo(R.id.actionModernGameToMainMenu);
        });
    }
    
    /**
     * Set up accessibility controls
     */
    private void setupAccessibilityControls() {
        // Set up directional button click listeners
        btnMoveNorth.setOnClickListener(v -> moveRobotInDirection(0, -1));
        btnMoveSouth.setOnClickListener(v -> moveRobotInDirection(0, 1));
        btnMoveEast.setOnClickListener(v -> moveRobotInDirection(1, 0));
        btnMoveWest.setOnClickListener(v -> moveRobotInDirection(-1, 0));
    }
    
    /**
     * Toggle accessibility controls visibility
     */
    private void toggleAccessibilityControls() {
        accessibilityControlsVisible = !accessibilityControlsVisible;
        accessibilityControlsContainer.setVisibility(
                accessibilityControlsVisible ? View.VISIBLE : View.GONE);
        
        // Update the text on the toggle button
        btnToggleAccessibilityControls.setText(
                accessibilityControlsVisible ? "Hide Controls" : "Accessibility Controls");
        
        // Announce to screen reader
        if (accessibilityControlsVisible) {
            announceAccessibility("Accessibility controls shown");
            
            // Update with current selection info
            GameState state = gameStateManager.getCurrentState().getValue();
            if (state != null) {
                GameElement selectedRobot = state.getSelectedRobot();
                updateRobotSelectionInfo(selectedRobot);
                updateDirectionalButtons(selectedRobot);
            }
        } else {
            announceAccessibility("Accessibility controls hidden");
        }
    }
    
    /**
     * Move the selected robot in the specified direction
     * 
     * Note: This method is only called by the accessibility directional buttons
     * (btnMoveNorth, btnMoveSouth, btnMoveEast, btnMoveWest) and not by normal touch
     * interactions on the GameGridView, which handles movement directly.
     * 
     * @param dx Horizontal movement (-1 = left, 1 = right)
     * @param dy Vertical movement (-1 = up, 1 = down)
     */
    private void moveRobotInDirection(int dx, int dy) {
        Timber.d("Moving robot in direction: dx=%d, dy=%d", dx, dy);
        GameState state = gameStateManager.getCurrentState().getValue();
        if (state == null || state.getSelectedRobot() == null) {
            announceAccessibility("No robot selected");
            return;
        }
        
        GameElement robot = state.getSelectedRobot();
        int startX = robot.getX();
        int startY = robot.getY();
        
        // Find the farthest position the robot can move in this direction
        int endX = startX;
        int endY = startY;
        boolean hitWall = false;
        boolean hitRobot = false;
        
        // Check for movement in X direction
        if (dx != 0) {
            int step = dx > 0 ? 1 : -1;
            for (int i = startX + step; i >= 0 && i < state.getWidth(); i += step) {
                if (state.canRobotMoveTo(robot, i, startY)) {
                    endX = i;
                } else {
                    // Check if we hit a robot or a wall
                    GameElement robotAtPosition = state.getRobotAt(i, startY);
                    if (robotAtPosition != null) {
                        hitRobot = true;
                    } else {
                        hitWall = true;
                    }
                    break;
                }
            }
        }
        
        // Check for movement in Y direction
        if (dy != 0) {
            int step = dy > 0 ? 1 : -1;
            for (int i = startY + step; i >= 0 && i < state.getHeight(); i += step) {
                if (state.canRobotMoveTo(robot, startX, i)) {
                    endY = i;
                } else {
                    // Check if we hit a robot or a wall
                    GameElement robotAtPosition = state.getRobotAt(startX, i);
                    if (robotAtPosition != null) {
                        hitRobot = true;
                    } else {
                        hitWall = true;
                    }
                    break;
                }
            }
        }
        
        // Calculate the distance moved
        int dist = Math.abs(endX - startX) + Math.abs(endY - startY);
        
        // Did the robot move?
        if (dist > 0) {
            // Update the robot's position in the game state
            robot.setX(endX);
            robot.setY(endY);
            
            // Update the game state
            gameStateManager.setMoveCount(gameStateManager.getMoveCount().getValue() + 1);
            gameStateManager.setSquaresMoved(gameStateManager.getSquaresMoved().getValue() + dist);
            
            // Play the appropriate sound effect
            Timber.d("[SOUND] Playing sound " + (hitRobot ? "hit_robot" : hitWall ? "hit_wall" : "move"));
            if (hitRobot) {
                playSound("hit_robot");
            } else if (hitWall) {
                playSound("hit_wall");
            } else {
                playSound("move");
            }
            
            // Announce the move
            announceAccessibility(getRobotColorNameByGridElement(robot) + 
                    " robot moved to " + endX + ", " + endY);
            
            // Check for goal completion
            if (state.isRobotAtTarget(robot)) {
                gameStateManager.setGameComplete(true);
                announceAccessibility("Goal reached! Game complete in " + 
                        gameStateManager.getMoveCount().getValue() + " moves and " +
                        gameStateManager.getSquaresMoved().getValue() + " squares moved");
                
                // Play win sound
                playSound("win");
            } else {
                // After the move, announce possible moves in the new position
                announcePossibleMoves(robot);
            }
        } else {
            // Did not move, play wall hit sound
            playSound("hit_wall");
            announceAccessibility("Cannot move in this direction");
        }
    }
    
    /**
     * Helper method to log and announce accessibility messages
     * This ensures accessibility messages are both announced via TalkBack and logged to console
     * @param message The message to announce
     */
    private void announceAccessibility(String message) {
        // Log to console with [TALKBACK] prefix
        Timber.d("[TALKBACK] %s", message);
        
        // Announce via TalkBack if available
        if (gameGridView != null) {
            gameGridView.announceForAccessibility(message);
        }
    }
    
    /**
     * Announce the possible moves for the selected robot in each direction
     * @param robot The selected robot
     */
    public void announcePossibleMoves(GameElement robot) {
        if (robot == null) {
            return;
        }
        
        Timber.d("Announcing possible moves for %s robot", getRobotColorNameByGridElement(robot));
        
        GameState state = gameStateManager.getCurrentState().getValue();
        if (state == null) {
            return;
        }
        
        int x = robot.getX();
        int y = robot.getY();
        
        // Build the announcement message with detailed information about possible moves
        StringBuilder announcement = new StringBuilder();
        announcement.append("Possible moves: ");
        
        // Check east movement (right)
        int eastDistance = 0;
        String eastObstacle = "edge";
        int obstacleX = x;
        for (int i = x + 1; i < state.getWidth(); i++) {
            if (state.canRobotMoveTo(robot, i, y)) {
                eastDistance++;
                obstacleX = i;
            } else {
                // Found an obstacle
                GameElement robotAtPosition = state.getRobotAt(i, y);
                if (robotAtPosition != null) {
                    eastObstacle = getRobotColorNameByGridElement(robotAtPosition) + " robot";
                    
                    // Check if the robot is at its target
                    if (state.isRobotAtTarget(robotAtPosition)) {
                        eastObstacle += " with target reached";
                    }
                } else {
                    eastObstacle = "wall";
                }
                break;
            }
        }
        if (eastDistance > 0) {
            announcement.append(eastDistance).append(" squares east until ").append(eastObstacle).append(", ");
        } else {
            announcement.append("no movement east, ");
        }
        
        // Check west movement (left)
        int westDistance = 0;
        String westObstacle = "edge";
        for (int i = x - 1; i >= 0; i--) {
            if (state.canRobotMoveTo(robot, i, y)) {
                westDistance++;
            } else {
                // Found an obstacle
                GameElement robotAtPosition = state.getRobotAt(i, y);
                if (robotAtPosition != null) {
                    westObstacle = getRobotColorNameByGridElement(robotAtPosition) + " robot";
                    
                    // Check if the robot is at its target
                    if (state.isRobotAtTarget(robotAtPosition)) {
                        westObstacle += " with target reached";
                    }
                } else {
                    westObstacle = "wall";
                }
                break;
            }
        }
        if (westDistance > 0) {
            announcement.append(westDistance).append(" squares west until ").append(westObstacle).append(", ");
        } else {
            announcement.append("no movement west, ");
        }
        
        // Check north movement (up)
        int northDistance = 0;
        String northObstacle = "edge";
        for (int i = y - 1; i >= 0; i--) {
            if (state.canRobotMoveTo(robot, x, i)) {
                northDistance++;
            } else {
                // Check if we hit a robot or a wall
                GameElement robotAtPosition = state.getRobotAt(x, i);
                if (robotAtPosition != null) {
                    northObstacle = getRobotColorNameByGridElement(robotAtPosition) + " robot";
                    
                    // Check if the robot is at its target
                    if (state.isRobotAtTarget(robotAtPosition)) {
                        northObstacle += " with target reached";
                    }
                } else {
                    northObstacle = "wall";
                }
                break;
            }
        }
        if (northDistance > 0) {
            announcement.append(northDistance).append(" squares north until ").append(northObstacle).append(", ");
        } else {
            announcement.append("no movement north, ");
        }
        
        // Check south movement (down)
        int southDistance = 0;
        String southObstacle = "edge";
        for (int i = y + 1; i < state.getHeight(); i++) {
            if (state.canRobotMoveTo(robot, x, i)) {
                southDistance++;
            } else {
                // Check if we hit a robot or a wall
                GameElement robotAtPosition = state.getRobotAt(x, i);
                if (robotAtPosition != null) {
                    southObstacle = getRobotColorNameByGridElement(robotAtPosition) + " robot";
                    
                    // Check if the robot is at its target
                    if (state.isRobotAtTarget(robotAtPosition)) {
                        southObstacle += " with target reached";
                    }
                } else {
                    southObstacle = "wall";
                }
                break;
            }
        }
        if (southDistance > 0) {
            announcement.append(southDistance).append(" squares south until ").append(southObstacle);
        } else {
            announcement.append("no movement south");
        }
        
        // Announce the message
        announceAccessibility(announcement.toString());
    }
    
    /**
     * Announce all robots and targets at the start of the game
     */
    private void announceGameStart() {
        GameState state = gameStateManager.getCurrentState().getValue();
        if (state == null) {
            return;
        }
        
        // Build a list of all robots and their corresponding targets
        StringBuilder gameStartMessage = new StringBuilder("Game started with ");
        List<GameElement> robots = new ArrayList<>();
        Map<Integer, GameElement> targets = new HashMap<>();
        
        // First collect all robots and targets
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_ROBOT) {
                robots.add(element);
            } else if (element.getType() == GameElement.TYPE_TARGET) {
                targets.put(element.getColor(), element);
            }
        }
        
        // Then build the message
        gameStartMessage.append(robots.size()).append(" robots. ");
        
        for (GameElement robot : robots) {
            String colorName = getRobotColorNameByGridElement(robot);
            int x = robot.getX();
            int y = robot.getY();
            
            gameStartMessage.append(colorName).append(" robot at position ").append(x).append(", ").append(y);
            
            // Add target information if available
            GameElement target = targets.get(robot.getColor());
            if (target != null) {
                gameStartMessage.append(" with target at ").append(target.getX()).append(", ").append(target.getY());
            }
            
            gameStartMessage.append(". ");
        }
        
        // Announce the game start message
        announceAccessibility(gameStartMessage.toString());
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Announce game start on resume
        if (gameStateManager != null && gameStateManager.getCurrentState().getValue() != null) {
            announceGameStart();
        }
        
        // Resume timer when fragment is resumed if game is in progress and not solved
        if (gameStateManager.getCurrentState().getValue() != null && !gameStateManager.isGameComplete().getValue()) {
            startTimer();
        }
    }
    
    /**
     * Update the game state
     */
    private void updateGameState(GameState state) {
        if (state == null) {
            return;
        }
        
        // Update the game grid view with the new game state
        ArrayList<GridElement> gridElements = state.getGridElements();
        gameGridView.setGridElements(gridElements);
        gameGridView.invalidate(); // Force redraw
        
        // Update board size text
        updateBoardSizeText();
    }
    
    private void updateMoveCount(int moveCount) {
        moveCountTextView.setText("Moves: " + moveCount);
    }
    
    private void updateSquaresMoved(int squaresMoved) {
        squaresMovedTextView.setText("Squares moved: " + squaresMoved);
    }
    
    private void updateDifficulty() {
        // Get difficulty string directly from GameStateManager
        String difficultyString = gameStateManager.getDifficultyString();
        difficultyTextView.setText("Difficulty: " + difficultyString);
    }
    
    private void updateBoardSizeText() {
        // Get the current game state
        GameState currentState = gameStateManager.getCurrentState().getValue();
        
        // If there's an active game state, use its dimensions
        if (currentState != null) {
            int boardWidth = currentState.getWidth();
            int boardHeight = currentState.getHeight();
            Timber.d("[BOARD_SIZE_DEBUG] ModernGameFragment.updateBoardSizeText() from GameState: %dx%d", boardWidth, boardHeight);
            boardSizeTextView.setText(String.format(Locale.getDefault(), "Board: %dx%d", boardWidth, boardHeight));
        } else {
            // If no game state yet, get from BoardSizeManager
            BoardSizeManager boardSizeManager = BoardSizeManager.getInstance(requireContext());
            int boardWidth = boardSizeManager.getBoardWidth();
            int boardHeight = boardSizeManager.getBoardHeight();
            Timber.d("[BOARD_SIZE_DEBUG] ModernGameFragment.updateBoardSizeText() from BoardSizeManager: %dx%d", boardWidth, boardHeight);
            boardSizeTextView.setText(String.format(Locale.getDefault(), "Board: %dx%d", boardWidth, boardHeight));
        }
    }
    
    /**
     * Get the screen title for accessibility and UI
     * @return The screen title
     */
    @Override
    public String getScreenTitle() {
        return "Modern Game";
    }
    
    /**
     * Start the timer
     */
    private void startTimer() {
        if (!timerRunning) {
            startTime = SystemClock.elapsedRealtime();
            timerHandler.postDelayed(timerRunnable, 0);
            timerRunning = true;
        }
    }
    
    /**
     * Stop the timer
     */
    private void stopTimer() {
        if (timerRunning) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunning = false;
        }
    }
    
    /**
     * Reset and start the timer
     */
    private void resetAndStartTimer() {
        stopTimer();
        startTimer();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Pause timer when fragment is paused
        stopTimer();
    }
    
    /**
     * Play a sound effect
     * @param soundType Type of sound to play ("move", "hit_wall", "hit_robot", "win")
     */
    public void playSound(String soundType) {
        if (soundManager != null) {
            Timber.d("ModernGameFragment: Playing sound %s", soundType);
            soundManager.playSound(soundType);
        } else {
            Timber.e("ModernGameFragment: SoundManager is null, cannot play sound %s", soundType);
        }
    }
    
    /**
     * Get the direction name from a move direction
     * @param direction Direction constant from ERRGameMove
     * @return Human-readable direction name
     */
    private String getDirectionName(int direction) {
        switch (direction) {
            case 1: // ERRGameMove.UP.getDirection()
                return "up";
            case 4: // ERRGameMove.DOWN.getDirection()
                return "down";
            case 2: // ERRGameMove.RIGHT.getDirection()
                return "right";
            case 8: // ERRGameMove.LEFT.getDirection()
                return "left";
            default: 
                return "unknown direction";
        }
    }
    
    /**
     * Get color name for a robot ID
     */
    private String getRobotColorName(int robotId) {
        // Log the color ID for debugging purposes
        Timber.d("[HINT_DEBUG] getRobotColorName called with ID: %d", robotId);
        
        // Check if the robotId is an Android RGB color value rather than an index
        if (robotId < 0) { // RGB colors as integers are usually negative in Android
            Timber.d("[HINT] Detected RGB color value instead of index: %d", robotId);
            // Map common Android color constants to their names
            if (robotId == Color.RED) return "Red";
            if (robotId == Color.GREEN) return "Green";
            if (robotId == Color.BLUE) return "Blue";
            if (robotId == Color.YELLOW) return "Yellow";
            
            // Convert to standard 0-3 index based on RGB value
            if ((robotId & 0xFF0000) != 0) return "Red";
            if ((robotId & 0x00FF00) != 0) return "Green";
            if ((robotId & 0x0000FF) != 0) return "Blue";
            if ((robotId & 0xFFFF00) == 0xFFFF00) return "Yellow";
        }
        
        // Standard index-based mapping
        switch (robotId) {
            case Constants.COLOR_RED: return "Red";
            case Constants.COLOR_GREEN: return "Green";
            case Constants.COLOR_BLUE: return "Blue";
            case Constants.COLOR_YELLOW: return "Yellow";
            default: return "Unknown: " + robotId;
        }
    }

     /**
     * Get color name for a robot
     */
    private String getRobotColorNameByGridElement(GameElement robot) {
        if (robot == null) return "";
        
        int c = robot.getColor();
        switch (c) {
            case 0: return "Red";
            case 1: return "Green";
            case 2: return "Blue";
            case 3: return "Yellow";
            default: return "Unknown: " + c;
        }
    }
    
    /**
     * Initialize a new game
     */
    private void initializeGame() {
        Timber.d("[SOLUTION SOLVER] ModernGameFragment: initializeGame() called");
        
        // Check if we have a gameStateManager
        if (gameStateManager == null) {
            gameStateManager = new ViewModelProvider(requireActivity()).get(GameStateManager.class);
        }
        
        // Get the game state
        GameState currentState = gameStateManager.getCurrentState().getValue();
        if (currentState == null) {
            Timber.d("[SOLUTION SOLVER] ModernGameFragment: No game state exists, this should not happen!");
            // crash the app, we should never get here, there should be only one game state, this is a bug. it gets created in MainMenuFragment, when you click on the start button
            throw new RuntimeException("No game state exists");
        } else {
            Timber.d("[SOLUTION SOLVER] ModernGameFragment: Using existing game state with %d robots",
                      currentState.getRobots().size());
        }
        
        // Clear any previous hint or status text
        statusTextView.setText("");
        statusTextView.setVisibility(View.GONE);
        
        // Start the timer
        startTimer();
        
        // Announce game start
        announceGameStart();
    }

    @Override
    public void onSolutionCalculationStarted() {
        Timber.d("[SOLUTION SOLVER] ModernGameFragment: onSolutionCalculationStarted");
        requireActivity().runOnUiThread(() -> {
            // Update hint button text to "Cancel"
            hintButton.setText("Cancel");
            statusTextView.setText("Calculating solution...");
            statusTextView.setVisibility(View.VISIBLE);
            Timber.d("[SOLUTION SOLVER] ModernGameFragment: UI updated to show calculation in progress");
        });
    }

    @Override
    public void onSolutionCalculationCompleted(GameSolution solution) {
        Timber.d("[SOLUTION SOLVER] ModernGameFragment: onSolutionCalculationCompleted - solution: %s", solution);
        if (solution != null && solution.getMoves() != null) {
            Timber.d("[SOLUTION SOLVER] ModernGameFragment: Solution has %d moves", solution.getMoves().size());
        } else {
            Timber.w("[SOLUTION SOLVER] ModernGameFragment: Solution or moves is null!");
        }
        
        requireActivity().runOnUiThread(() -> {
            // Reset hint button text back to "Hint"
            hintButton.setText("Hint");
            statusTextView.setText("Hint ready! Press hint button again.");
            statusTextView.setVisibility(View.VISIBLE);
            Timber.d("[SOLUTION SOLVER] ModernGameFragment: UI updated to show hint is ready");
        });
    }

    @Override
    public void onSolutionCalculationFailed(String errorMessage) {
        Timber.d("[SOLUTION SOLVER] ModernGameFragment: onSolutionCalculationFailed - %s", errorMessage);
        requireActivity().runOnUiThread(() -> {
            // Reset hint button text back to "Hint"
            hintButton.setText("Hint");
            statusTextView.setText("Could not find a solution: " + errorMessage);
            statusTextView.setVisibility(View.VISIBLE);
            Timber.d("[SOLUTION SOLVER] ModernGameFragment: UI updated to show error");
        });
    }
}
