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
import roboyard.eclabs.MainActivity;
import roboyard.eclabs.Preferences;
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
    private Button nextLevelButton;
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
    private Button btnSelectRobot; // New button for TalkBack robot selection
    private TextView txtSelectedRobot;
    private TextView txtRobotGoal;
    private Button btnToggleAccessibilityControls;
    private boolean accessibilityControlsVisible = false;
    
    // Timer variables
    private long startTime = 0L;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private boolean timerRunning = false;
    private final Runnable timerRunnable = new Runnable() {
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
        
        // Prevent automatic selection of gameGridView by setting focusable to false
        gameGridView.setFocusable(false);
        
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
            btnSelectRobot = accessibilityControlsContainer.findViewById(R.id.btn_select_robot); // New button for TalkBack robot selection
            
            // Set up directional button click listeners
            setupAccessibilityControls();
        }
        
        // Check for TalkBack OR accessibility preference enabled to show the button
        boolean accessibilityModeEnabled = false;
        
        // Check settings preference
        Preferences preferences = new Preferences();
        String accessibilityPref = preferences.getPreferenceValue(requireActivity(), "accessibilityMode");
        if (accessibilityPref != null && accessibilityPref.equals("true")) {
            accessibilityModeEnabled = true;
        }
        
        // Show button if either TalkBack or accessibility mode is enabled
        if (isTalkBackEnabled() || accessibilityModeEnabled) {
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
                
                // Show the Next Level button if this is a level game
                GameState state = gameStateManager.getCurrentState().getValue();
                if (state != null && state.getLevelId() > 0) {
                    // This is a level game, show the Next Level button
                    nextLevelButton.setVisibility(View.VISIBLE);
                    
                    // Update status text to show level complete message
                    updateStatusText(getString(R.string.level_complete), true);
                    
                    // Announce level completion for accessibility
                    String completionMessage = getString(R.string.level_complete) + 
                            " Level " + state.getLevelId() + " completed in " + 
                            gameStateManager.getMoveCount().getValue() + " moves.";
                    announceAccessibility(completionMessage);
                }
            } else {
                // Hide the Next Level button when game is not complete
                nextLevelButton.setVisibility(View.GONE);
            }
        });
        
        // Observe solver running state to update hint button text
        gameStateManager.isSolverRunning().observe(getViewLifecycleOwner(), isRunning -> {
            if (isRunning) {
                // Change hint button text to "Cancel" while calculating
                hintButton.setText(R.string.cancel_hint_button);
                updateStatusText("Calculating solution...", true);
            } else {
                // Reset hint button text
                hintButton.setText(R.string.hint_button);
                // Don't update the status text here - let callbacks handle it appropriately
                // This prevents text flashing/flickering between states
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
                updateStatusText("Hint calculation cancelled", true);
                return;
            }
            
            // Check if we have a solution object at all
            GameSolution solution = gameStateManager.getCurrentSolution();
            if (solution == null || solution.getMoves() == null || solution.getMoves().size() == 0) {
                Timber.d("[HINT] No solution available, calculating...");
                updateStatusText("Calculating solution...", true);
                
                // Start calculating a solution
                gameStateManager.calculateSolutionAsync(this);
                return;
            }
            
            // Check if we've already shown all available hints
            int currentStep = gameStateManager.getCurrentSolutionStep();
            int totalMoves = solution.getMoves().size();
            
            if (currentStep >= totalMoves) {
                String hint = "All hints have been shown (" + currentStep + "/" + totalMoves + "), resetting to first hint";
                Timber.d("[HINT] Displayed hint: " + hint);
                updateStatusText(hint, true);
                // Reset the solution step counter to show the first hint again
                gameStateManager.resetSolutionStep();
                return;
            }
            
            // Get a hint from the game state manager
            IGameMove hintMove = gameStateManager.getHint();
            Timber.d("[HINT] Received hint move: %s", hintMove);
            
            if (hintMove != null && hintMove instanceof RRGameMove rrMove) {
                // Cast to RRGameMove to access the proper methods

                // Log the details of the move
                Timber.d("[HINT] Robot color: %d, Direction: %d", 
                        rrMove.getColor(), rrMove.getDirection());
                
                // Convert the robotic move to human-readable text
                String robotColor = getRobotColorName(rrMove.getColor());
                String direction = getDirectionName(rrMove.getDirection());
                
                // Log details for debugging
                Timber.d("[HINT] Robot color ID: %d, mapped to color name: %s", 
                        rrMove.getColor(), robotColor);
                
                // Update the status text with the hint number and the hint itself
                String hintText = String.format("%d/%d: Move the %s robot %s",
                        currentStep + 1, totalMoves, robotColor, direction);
                updateStatusText(hintText, true);
                
                Timber.d("[HINT] Displayed hint: %s", hintText);
            } else {
                // No solution found
                updateStatusText("No solution available", true);
                Timber.d("[HINT] No valid hint move available");
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
                
                // Create a bundle with both saveMode arguments to ensure proper operation
                Bundle args = new Bundle();
                args.putBoolean("saveMode", true);
                args.putString("mode", "save");  // Also set the string mode parameter
                saveFragment.setArguments(args);
                
                Timber.d("ModernGameFragment: Navigating to SaveGameFragment in SAVE mode");
                
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
            updateStatusText("", false);
            // Announce the robots and their positions
            announceGameStart();
        });
        
        // (Button text: "Menu")
        // Menu button - go back to main menu
        menuButton = view.findViewById(R.id.menu_button);
        menuButton.setOnClickListener(v -> {
            Timber.d("ModernGameFragment: Menu button clicked");
            
            // Create a new MainMenuFragment instance
            MainMenuFragment menuFragment = new MainMenuFragment();
            navigateToDirect(menuFragment);
        });
        
        // Next Level button - go to the next level when a level is completed
        nextLevelButton = view.findViewById(R.id.next_level_button);
        nextLevelButton.setOnClickListener(v -> {
            Timber.d("ModernGameFragment: Next Level button clicked");
            // Get the current level ID
            GameState state = gameStateManager.getCurrentState().getValue();
            if (state != null) {
                int currentLevelId = state.getLevelId();
                int nextLevelId = currentLevelId + 1;
                Timber.d("ModernGameFragment: Moving from level %d to level %d", currentLevelId, nextLevelId);
                
                // Start the next level
                gameStateManager.startLevelGame(nextLevelId);
                
                // Reset timer
                stopTimer();
                startTimer();
                
                // Hide the Next Level button
                nextLevelButton.setVisibility(View.GONE);
                
                // Clear any hint text from the status display
                updateStatusText("", false);
                
                // Announce the new level
                announceAccessibility("Starting level " + nextLevelId);
                announceGameStart();
            }
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
        
        // Set up robot selection button
        if (btnSelectRobot != null) {
            btnSelectRobot.setOnClickListener(v -> cycleThroughRobots());
        }
        
        // Set up announce positions button
        Button btnAnnouncePositions = accessibilityControlsContainer.findViewById(R.id.btn_announce_positions);
        if (btnAnnouncePositions != null) {
            btnAnnouncePositions.setOnClickListener(v -> announceGameStart());
        }
    }
    
    /**
     * Cycle through available robots for TalkBack accessibility
     * This allows users to quickly switch between robots using a dedicated button
     */
    private void cycleThroughRobots() {
        GameState state = gameStateManager.getCurrentState().getValue();
        if (state == null) {
            announceAccessibility("No game in progress");
            return;
        }
        
        // Create a list of all robots in the game
        List<GameElement> robots = new ArrayList<>();
        for (GameElement element : state.getGameElements()) {
            if (element.isRobot()) {
                robots.add(element);
            }
        }
        
        if (robots.isEmpty()) {
            announceAccessibility("No robots available");
            return;
        }
        
        // Find current robot index
        int currentIndex = -1;
        GameElement selectedRobot = state.getSelectedRobot();
        if (selectedRobot != null) {
            for (int i = 0; i < robots.size(); i++) {
                if (robots.get(i).getColor() == selectedRobot.getColor()) {
                    currentIndex = i;
                    break;
                }
            }
        }
        
        // Select next robot in the list
        int nextIndex = (currentIndex + 1) % robots.size();
        GameElement nextRobot = robots.get(nextIndex);
        state.setSelectedRobot(nextRobot);
        
        // Update UI with the new selection
        updateRobotSelectionInfo(nextRobot);
        updateDirectionalButtons(nextRobot);
        
        // Play a sound to indicate selection changed
        playSound("move");
        
        // Announce the newly selected robot
        String colorName = getRobotColorNameByGridElement(nextRobot);
        announceAccessibility("Selected " + colorName + " robot");
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
        
        // Ensure starting position is valid and within bounds
        if (startX < 0 || startX >= state.getWidth() || startY < 0 || startY >= state.getHeight()) {
            Timber.e("Robot position outside game boundaries: %d, %d", startX, startY);
            // Reset robot to a valid position on the board
            startX = Math.max(0, Math.min(startX, state.getWidth() - 1));
            startY = Math.max(0, Math.min(startY, state.getHeight() - 1));
            robot.setX(startX);
            robot.setY(startY);
            gameGridView.invalidate();
            announceAccessibility("Robot repositioned to valid location");
        }
        
        // Find the farthest position the robot can move in this direction
        int endX = startX;
        int endY = startY;
        boolean hitWall = false;
        boolean hitRobot = false;
        
        // Check for movement in X direction
        if (dx != 0) {
            int step = dx > 0 ? 1 : -1;
            // Allow movement to any valid cell including 0 and 1
            for (int i = startX + step; i >= 0 && i < state.getWidth(); i += step) {
                boolean canMove = true;
                
                // Check if there's a wall between the current position and the next position
                if (dx > 0) { // Moving right
                    // Check for vertical wall at the current position
                    for (GameElement element : state.getGameElements()) {
                        if (element.getType() == GameElement.TYPE_VERTICAL_WALL && 
                            element.getX() == i+2 && element.getY() == startY) {
                            canMove = false;
                            hitWall = true;
                            break;
                        }
                    }
                } else { // Moving left
                    // Check for vertical wall at the previous position
                    for (GameElement element : state.getGameElements()) {
                        if (element.getType() == GameElement.TYPE_VERTICAL_WALL && 
                            element.getX() == i+1 && element.getY() == startY) {
                            canMove = false;
                            hitWall = true;
                            break;
                        }
                    }
                }
                
                // Check for robot at the position
                GameElement robotAtPosition = state.getRobotAt(i, startY);
                if (robotAtPosition != null) {
                    canMove = false;
                    hitRobot = true;
                }
                
                if (canMove) {
                    endX = i;
                } else {
                    break;
                }
            }
        }
        
        // Check for movement in Y direction
        if (dy != 0) {
            int step = dy > 0 ? 1 : -1;
            // Allow movement to any valid cell including 0 and 1
            for (int i = startY + step; i >= 0 && i < state.getHeight(); i += step) {
                boolean canMove = true;
                
                // Check if there's a wall between the current position and the next position
                if (dy > 0) { // Moving down
                    // Check for horizontal wall at the current position
                    for (GameElement element : state.getGameElements()) {
                        if (element.getType() == GameElement.TYPE_HORIZONTAL_WALL && 
                            element.getX() == startX && element.getY() == i+2) {
                            canMove = false;
                            hitWall = true;
                            break;
                        }
                    }
                } else { // Moving up
                    // Check for horizontal wall at the previous position
                    for (GameElement element : state.getGameElements()) {
                        if (element.getType() == GameElement.TYPE_HORIZONTAL_WALL && 
                            element.getX() == startX && element.getY() == i+1) {
                            canMove = false;
                            hitWall = true;
                            break;
                        }
                    }
                }
                
                // Check for robot at the position
                GameElement robotAtPosition = state.getRobotAt(startX, i);
                if (robotAtPosition != null) {
                    canMove = false;
                    hitRobot = true;
                }
                
                if (canMove) {
                    endY = i;
                } else {
                    break;
                }
            }
        }
        
        // Ensure final position is within bounds
        if (endX < 0 || endX >= state.getWidth() || endY < 0 || endY >= state.getHeight()) {
            Timber.e("Calculated end position outside game boundaries: %d, %d", endX, endY);
            // Restrict to valid bounds
            endX = Math.max(0, Math.min(endX, state.getWidth() - 1));
            endY = Math.max(0, Math.min(endY, state.getHeight() - 1));
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
            
            // FIX: Actually update the UI to show the robot movement
            gameGridView.invalidate();
            
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
        if (state == null) return;
        
        StringBuilder announcement = new StringBuilder();
        announcement.append("Game started. ");
        
        // Check if TalkBack is enabled
        if (isTalkBackEnabled()) {
            announcement.append("Accessibility mode is active. ");
            announcement.append("Use the Select Next Robot button to cycle through robots. ");
            announcement.append("Then use directional buttons to move the selected robot. ");
        }
        
        // Announce robots with concise format
        int robotCount = 0;
        for (GameElement element : state.getGameElements()) {
            if (element.isRobot()) {
                robotCount++;
                String color = getRobotColorNameByGridElement(element);
                int x = element.getX();
                int y = element.getY();
                
                // Find walls - check each direction
                List<String> walls = new ArrayList<>();
                
                // Check east wall
                if (!state.canRobotMoveTo(element, x + 1, y) && x + 1 < state.getWidth() && 
                    state.getRobotAt(x + 1, y) == null) {
                    walls.add("east");
                }
                
                // Check west wall
                if (!state.canRobotMoveTo(element, x - 1, y) && x - 1 >= 0 && 
                    state.getRobotAt(x - 1, y) == null) {
                    walls.add("west");
                }
                
                // Check north wall
                if (!state.canRobotMoveTo(element, x, y - 1) && y - 1 >= 0 && 
                    state.getRobotAt(x, y - 1) == null) {
                    walls.add("north");
                }
                
                // Check south wall
                if (!state.canRobotMoveTo(element, x, y + 1) && y + 1 < state.getHeight() && 
                    state.getRobotAt(x, y + 1) == null) {
                    walls.add("south");
                }
                
                // Build the concise description "[Robot color], [coordinates], [walls directions]"
                announcement.append(color).append(" robot, ")
                          .append(x).append("-").append(y);
                
                // Add walls if present
                if (!walls.isEmpty()) {
                    announcement.append(", walls ");
                    for (int i = 0; i < walls.size(); i++) {
                        announcement.append(walls.get(i));
                        if (i < walls.size() - 1) {
                            announcement.append(", ");
                        }
                    }
                }
                announcement.append(". ");
            }
        }
        
        announcement.append("Total robots: ").append(robotCount).append(". ");
        
        // Announce targets with concise format
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_TARGET) {
                String color = getRobotColorName(element.getColor());
                announcement.append(color).append(" target, ")
                          .append(element.getX()).append("-").append(element.getY()).append(". ");
            }
        }
        
        // Make the announcement
        announceAccessibility(announcement.toString());
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
        updateStatusText("", false);
        
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
            hintButton.setText(R.string.cancel_hint_button);
            updateStatusText("Calculating solution...", true);
            Timber.d("[SOLUTION SOLVER] ModernGameFragment: UI updated to show calculation in progress");
        });
    }

    @Override
    public void onSolutionCalculationCompleted(GameSolution solution) {
        Timber.d("[SOLUTION CALLBACK] ModernGameFragment.onSolutionCalculationCompleted ENTERED");
        if (solution != null && solution.getMoves() != null) {
            Timber.d("[SOLUTION SOLVER][MOVES] Solution has %d moves", solution.getMoves().size());
            
            // Check if solution has less than 4 moves and not in level mode
            int moveCount = solution.getMoves().size();
            GameState currentState = gameStateManager.getCurrentState().getValue();
            boolean isLevelMode = (currentState != null && currentState.getLevelId() > 0);
        } else {
            Timber.w("[SOLUTION CALLBACK] Solution or moves is null!");
        }
        
        // Force UI update on main thread with the correct message
        Timber.d("[SOLUTION CALLBACK] About to run on UI thread");
        requireActivity().runOnUiThread(() -> {
            Timber.d("[SOLUTION CALLBACK] Now running on UI thread");
            // Reset hint button text back to "Hint"
            hintButton.setText(R.string.hint_button);
            // Make sure status text is set and visible
            updateStatusText("Solution ready! Press hint", true);
            Timber.d("[SOLUTION CALLBACK] *** SOLUTION IS READY - UI UPDATED ***");
        });
    }

    @Override
    public void onSolutionCalculationFailed(String errorMessage) {
        Timber.d("[SOLUTION SOLVER] onSolutionCalculationFailed - %s", errorMessage);
        requireActivity().runOnUiThread(() -> {
            // Reset hint button text back to "Hint"
            hintButton.setText(R.string.hint_button);
            updateStatusText("Could not find a solution: " + errorMessage, true);
            Timber.d("[SOLUTION SOLVER] ModernGameFragment: UI updated to show error");
        });
    }
    
    /**
     * Helper method to update the status text with a consistent approach
     * @param message The message to display
     * @param isVisible Whether to make the status text visible
     */
    private void updateStatusText(String message, boolean isVisible) {
        Timber.d("[STATUS TEXT] Updating status text: '%s', visible: %b", message, isVisible);
        if (statusTextView != null) {
            statusTextView.setText(message);
            statusTextView.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE); // Use INVISIBLE instead of GONE to reserve space
        }
    }
}
