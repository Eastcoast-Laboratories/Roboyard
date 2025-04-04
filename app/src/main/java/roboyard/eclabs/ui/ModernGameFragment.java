package roboyard.eclabs.ui;

import android.content.Context;
import android.graphics.Color;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import roboyard.logic.core.Constants;
import roboyard.eclabs.R;
import roboyard.logic.core.GameState;
import roboyard.logic.core.GridElement;
import roboyard.pm.ia.IGameMove;
import roboyard.pm.ia.ricochet.RRGameMove;
import roboyard.eclabs.util.SoundManager;
import roboyard.pm.ia.GameSolution;

import roboyard.ui.components.GameGridView;
import roboyard.ui.components.GameStateManager;
import timber.log.Timber;

// Added imports for accessibility
import android.view.accessibility.AccessibilityManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;

import roboyard.logic.core.Preferences;

/**
 * Modern UI implementation of the game screen.
 * all Android-native UI
 * the layout is defined in the xml file app/src/main/res/layout/fragment_modern_game.xml
 */
public class ModernGameFragment extends BaseGameFragment implements GameStateManager.SolutionCallback {

    private static final int MAX_HINTS_UP_TO_LEVEL_10 = 4; // Maximum hints allowed for levels 1-10
    private GameGridView gameGridView;
    private TextView moveCountTextView;
    private TextView squaresMovedTextView;
    private TextView difficultyTextView;
    private TextView boardSizeTextView;
    private TextView uniqueMapIdTextView; // Added for unique map ID display
    private Button backButton;
    private Button resetRobotsButton;
    private ToggleButton hintButton;
    private Button saveMapButton;
    private Button newMapButton;
    private Button menuButton;
    private Button nextLevelButton;
    private Button optimalMovesButton; // Button to display optimal number of moves
    private TextView timerTextView;
    private TextView statusTextView;
    // Hint navigation components
    private ViewGroup hintContainer;
    private TextView prevHintButton;
    private TextView nextHintButton;
    
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

    // Hint managing
    private static final int NUM_FIXED_PRE_HINTS = 2; // Number of fixed pre-hints that always appear
    private int numPreHints = ThreadLocalRandom.current().nextInt(2, 5); // Randomize between 2-4 by default
    private int totalPossibleHints = 0; // Total possible hints including pre-hints
    private int currentHintStep = 0; // Current hint step (includes pre-hints and regular hints)
    private boolean showingPreHints = true; // Whether we're showing pre-hints (false after reset)

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
    
    // Autosave variables
    private final Handler autosaveHandler = new Handler(Looper.getMainLooper());
    private boolean autosaveRunning = false;
    private static final int AUTOSAVE_INTERVAL_MS = 60 * 1000; // 60 seconds
    private final Runnable autosaveRunnable = new Runnable() {
        @Override
        public void run() {
            // Only autosave if game is in progress and not solved
            if (gameStateManager != null && !gameStateManager.isGameComplete().getValue()) {
                Timber.d("[AUTOSAVE] Performing autosave to slot 0");
                boolean saved = gameStateManager.saveGame(0); // Save to slot 0
                if (saved) {
                    Timber.d("[AUTOSAVE] Game successfully autosaved to slot 0");
                } else {
                    Timber.e("[AUTOSAVE] Failed to autosave game to slot 0");
                }
            }
            
            // Schedule next autosave
            autosaveHandler.postDelayed(this, AUTOSAVE_INTERVAL_MS);
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
        uniqueMapIdTextView = view.findViewById(R.id.unique_map_id_text); // Initialize unique map ID text view
        timerTextView = view.findViewById(R.id.game_timer);
        statusTextView = view.findViewById(R.id.status_text_view);
        optimalMovesButton = view.findViewById(R.id.optimal_moves_button);
        hintContainer = view.findViewById(R.id.hint_container);
        prevHintButton = view.findViewById(R.id.prev_hint_button);
        nextHintButton = view.findViewById(R.id.next_hint_button);
        
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
        
        // Show button if either TalkBack or accessibility mode is enabled
        if (isTalkBackEnabled() || Preferences.accessibilityMode) {
            btnToggleAccessibilityControls.setVisibility(View.VISIBLE);
            btnToggleAccessibilityControls.setOnClickListener(v -> toggleAccessibilityControls());
        }
        
        // Set up the game grid view
        gameGridView.setFragment(this);
        gameGridView.setGameStateManager(gameStateManager);
        
        // Set up the UI mode manager
        UIModeManager uiModeManager = UIModeManager.getInstance(requireContext());
        uiModeManager.setUIMode(UIModeManager.MODE_MODERN);
        
        // Update difficulty and board size text
        updateDifficulty();
        updateBoardSizeText();
        
        // Set up observers for game state
        setupObservers();

        // Set up button click listeners
        setupButtons(view);
        
        // Initialize the game
        initializeGame();
        
        // Start autosave
        startAutosave();
        
        // Set up status text view to act as a button for showing the next hint
        statusTextView.setOnClickListener(v -> {
            Timber.d("[HINT_SYSTEM] Status text view clicked for next hint");
            
            // Check if this is a level game with level > 10 (no hints allowed)
            GameState currentState = gameStateManager.getCurrentState().getValue();
            if (currentState != null && currentState.getLevelId() > 10) {
                Timber.d("[HINT_SYSTEM] Level > 10, hints are disabled for status text view");
                return;
            }
            
            // For level games 1-10, limit to only 2 hints
            if (currentState != null && currentState.getLevelId() > 0 && currentState.getLevelId() <= 10) {
                // Limit to only the first two hints for levels 1-10
                if (currentHintStep >= MAX_HINTS_UP_TO_LEVEL_10) {
                    Timber.d("[HINT_SYSTEM] Level 1-10 reached maximum allowed hints (%d) for status text view", MAX_HINTS_UP_TO_LEVEL_10);
                    return;
                }
            }
            
            GameSolution solution = gameStateManager.getCurrentSolution();
            if (solution == null || solution.getMoves().isEmpty()) {
                Timber.d("[HINT_SYSTEM] No solution available for next hint");
                return;
            }
            
            int totalMoves = solution.getMoves().size();
            totalPossibleHints = totalMoves + numPreHints + NUM_FIXED_PRE_HINTS;
            
            // Increment hint step if possible
            if (currentHintStep < totalPossibleHints - 1) {
                currentHintStep++;
                Timber.d("[HINT_SYSTEM] Moving to next hint: step=%d of %d", currentHintStep, totalPossibleHints - 1);
                GameState currentGameState = gameStateManager.getCurrentState().getValue();
                
                // Show the appropriate hint
                if (showingPreHints && currentHintStep < (numPreHints + NUM_FIXED_PRE_HINTS)) {
                    showPreHint(solution, totalMoves, currentHintStep);
                } else {
                    int normalHintIndex = showingPreHints ? currentHintStep - (numPreHints + NUM_FIXED_PRE_HINTS) : currentHintStep;
                    showNormalHint(solution, currentGameState, totalMoves, normalHintIndex);
                }
                
                // Update the game state manager's current solution step
                gameStateManager.resetSolutionStep();
                for (int i = 0; i < currentHintStep; i++) {
                    gameStateManager.incrementSolutionStep();
                }
                Timber.d("[HINT_SYSTEM] Updated solution step to %d", currentHintStep);
            } else {
                Timber.d("[HINT_SYSTEM] Already at last hint, can't go further");
            }
        });
        
        // Set up optimal moves button to also act as a next hint button
        optimalMovesButton.setOnClickListener(v -> {
            Timber.d("[HINT_SYSTEM] Optimal moves button clicked for next hint");
            // Only show next hint if the hint container is visible
            if (hintContainer.getVisibility() == View.VISIBLE && hintButton.isChecked()) {
                // Check if this is a level game with level > 10 (no hints allowed)
                GameState currentState = gameStateManager.getCurrentState().getValue();
                if (currentState != null && currentState.getLevelId() > 10) {
                    Timber.d("[HINT_SYSTEM] Level > 10, hints are disabled for optimal moves button");
                    return;
                }
                
                // For level games 1-10, limit to only 2 hints
                if (currentState != null && currentState.getLevelId() > 0 && currentState.getLevelId() <= 10) {
                    // Limit to only the first two hints for levels 1-10
                    if (currentHintStep >= MAX_HINTS_UP_TO_LEVEL_10) {
                        Timber.d("[HINT_SYSTEM] Level 1-10 reached maximum allowed hints (%d) for optimal moves button", MAX_HINTS_UP_TO_LEVEL_10);
                        return;
                    }
                }
                
                showNextHint();
            }
        });
        
        // Set up unique map ID text view to also act as a next hint button
        uniqueMapIdTextView.setOnClickListener(v -> {
            Timber.d("[HINT_SYSTEM] Unique map ID text view clicked for next hint");
            // Only show next hint if the hint container is visible
            if (hintContainer.getVisibility() == View.VISIBLE && hintButton.isChecked()) {
                // Check if this is a level game with level > 10 (no hints allowed)
                GameState currentState = gameStateManager.getCurrentState().getValue();
                if (currentState != null && currentState.getLevelId() > 10) {
                    Timber.d("[HINT_SYSTEM] Level > 10, hints are disabled for unique map ID text view");
                    return;
                }
                
                // For level games 1-10, limit to only 2 hints
                if (currentState != null && currentState.getLevelId() > 0 && currentState.getLevelId() <= 10) {
                    // Limit to only the first two hints for levels 1-10
                    if (currentHintStep >= 2) {
                        Timber.d("[HINT_SYSTEM] Level 1-10 reached maximum allowed hints (2) for unique map ID text view");
                        return;
                    }
                }
                
                showNextHint();
            }
        });
        
        // Make the unique map ID text view clickable
        uniqueMapIdTextView.setClickable(true);
        uniqueMapIdTextView.setFocusable(true);
        uniqueMapIdTextView.setBackgroundResource(android.R.drawable.list_selector_background);
    }
    
    /**
     * Set up observers for game state
     */
    private void setupObservers() {
        // Observe current game state
        gameStateManager.getCurrentState().observe(getViewLifecycleOwner(), state -> {
            updateGameState(state);
            
            // Check if a hint is being shown and if the move matches the current hint
            checkIfMoveMatchesHint(state);
        });
        
        // Observe move count
        gameStateManager.getMoveCount().observe(getViewLifecycleOwner(), this::updateMoveCount);
        
        // Observe squares moved
        gameStateManager.getSquaresMoved().observe(getViewLifecycleOwner(), this::updateSquaresMoved);
        
        // Observe game completion
        gameStateManager.isGameComplete().observe(getViewLifecycleOwner(), isComplete -> {
            if (isComplete) {
                // Stop the timer for both level and random games when completed
                stopTimer();
                
                playSound("win");
                
                // Get the current game state
                GameState state = gameStateManager.getCurrentState().getValue();
                if (state != null) {
                    if (state.getLevelId() > 0) {
                        // This is a level game, show the Next Level button
                        nextLevelButton.setVisibility(View.VISIBLE);
                        
                        // Get stars for display
                        int playerMoves = gameStateManager.getMoveCount().getValue() != null ? 
                                gameStateManager.getMoveCount().getValue() : 0;
                        int optimalMoves = 0;
                        GameSolution solution = gameStateManager.getCurrentSolution();
                        if (solution != null && solution.getMoves() != null && solution.getMoves().size() > 0) {
                            optimalMoves = solution.getMoves().size();
                            Timber.d("Solution object hash (completion): %s", System.identityHashCode(solution));
                        }
                        int hintsUsed = state.getHintCount();
                        int stars = gameStateManager.calculateStars(playerMoves, optimalMoves, hintsUsed);
                        
                        Timber.d("[STARS] gameStateManager: Calculated stars: %d", stars);

                        // Create stars string with UTF-8 stars
                        StringBuilder starString = new StringBuilder();
                        for (int i = 0; i < stars; i++) {
                            starString.append("★ ");
                        }
                        
                        // Update status text to show level complete message with stars
                        updateStatusText(getString(R.string.level_complete) + " " + starString.toString().trim(), true);
                        
                        String starsMessage = getString(R.string.level_stars_description, stars);
                        Toast.makeText(requireContext(), starsMessage, Toast.LENGTH_LONG).show();
                        
                        // Announce level completion for accessibility
                        String completionMessage = getString(R.string.level_complete) + 
                                " Level " + state.getLevelId() + " completed in " + 
                                gameStateManager.getMoveCount().getValue() + " moves. ";
                        
                        // Add a verbal description of stars for accessibility
                        if (stars == 1) {
                            completionMessage += "You earned 1 star.";
                        } else {
                            completionMessage += "You earned " + stars + " stars.";
                        }
                        
                        announceAccessibility(completionMessage);
                    } else {
                        // This is a random game
                        updateStatusText(getString(R.string.random_game_complete), true);
                        
                        // Show new game button instead of next level
                        nextLevelButton.setText(R.string.new_random_game_button);
                        nextLevelButton.setVisibility(View.VISIBLE);
                        
                        // Show completion message
                        String completionMessage = getString(R.string.random_game_complete) +
                                " Completed in " + gameStateManager.getMoveCount().getValue() + " moves.";
                        announceAccessibility(completionMessage);
                        Toast.makeText(requireContext(), completionMessage, Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                // Hide the Next Level button when game is not complete
                nextLevelButton.setVisibility(View.GONE);
                
                // Reset button text just in case it was changed
                nextLevelButton.setText(R.string.next_level);
            }
        });
        
        // Observe solver running state to update hint button text
        gameStateManager.isSolverRunning().observe(getViewLifecycleOwner(), isRunning -> {
            if (isRunning) {
                // Change hint button text to "Cancel" while calculating
                hintButton.setTextOn("Hide");
                hintButton.setTextOff("💡Hint");
                hintButton.setChecked(true);
                updateStatusText("AI calculating solution...", true);
            } else {
                // Reset hint button text
                hintButton.setChecked(false);
                // Don't update the status text here - let callbacks handle it appropriately
                // This prevents text flashing/flickering between states
            }
        });
    }
    
    /**
     * Check if a move matches the current hint and advance to the next hint if it does
     * @param state The current game state
     */
    private void checkIfMoveMatchesHint(GameState state) {
        // Check if a hint is being shown
        if (hintContainer.getVisibility() == View.VISIBLE) {
            Timber.d("[HINT_SYSTEM] Checking if move matches hint");
            // Get the current solution
            GameSolution solution = gameStateManager.getCurrentSolution();
            
            // Check if the solution is valid and has moves
            if (solution != null && solution.getMoves() != null && !solution.getMoves().isEmpty()) {
                // Only check normal hints (not pre-hints)
                if (!showingPreHints || currentHintStep >= (numPreHints + NUM_FIXED_PRE_HINTS)) {
                    // Calculate the index for the normal hint
                    int normalHintIndex = showingPreHints ? currentHintStep - (numPreHints + NUM_FIXED_PRE_HINTS) : currentHintStep;
                    
                    // Make sure we're within bounds
                    if (normalHintIndex >= 0 && normalHintIndex < solution.getMoves().size()) {
                        // Get the current move count
                        int currentMoveCount = state.getMoveCount();
                        Timber.d("[HINT_SYSTEM] Current move count: %d", currentMoveCount);
                        
                        // We can't directly check if the move matches, but we can advance the hint
                        // when a move is made and the hint is being shown
                        if (currentMoveCount > 0 && hintButton.isChecked()) {
                            // Advance to the next hint
                            int totalMoves = solution.getMoves().size();
                            totalPossibleHints = totalMoves + numPreHints + NUM_FIXED_PRE_HINTS;
                            
                            // Increment hint step if possible
                            if (currentHintStep < totalPossibleHints - 1) {
                                currentHintStep++;
                                Timber.d("[HINT_SYSTEM] Auto-advancing to next hint: step=%d", currentHintStep);
                                
                                // Show the appropriate hint after a short delay
                                new Handler().postDelayed(() -> {
                                    // Show the appropriate hint
                                    if (showingPreHints && currentHintStep < (numPreHints + NUM_FIXED_PRE_HINTS)) {
                                        showPreHint(solution, totalMoves, currentHintStep);
                                    } else {
                                        int nextNormalHintIndex = showingPreHints ? 
                                                currentHintStep - (numPreHints + NUM_FIXED_PRE_HINTS) : currentHintStep;
                                        showNormalHint(solution, state, totalMoves, nextNormalHintIndex);
                                    }
                                    
                                    // Update the current solution step
                                    gameStateManager.resetSolutionStep();
                                    for (int i = 0; i < currentHintStep; i++) {
                                        gameStateManager.incrementSolutionStep();
                                    }
                                    Timber.d("[HINT_SYSTEM] Updated solution step to %d after auto-advance", currentHintStep);
                                }, 1000); // 1 second delay
                            } else {
                                Timber.d("[HINT_SYSTEM] Already at last hint, can't auto-advance further");
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Set up button click listeners
     */
    private void setupButtons(View view) {
        GameState currentState = gameStateManager.getCurrentState().getValue();
        boolean isLevelGame;
        if (currentState != null && currentState.getLevelId() > 0) {
            isLevelGame = true;
        } else {
            isLevelGame = false;
        }

        // (Button text: "Back")
        // Back button - undo the last robot movement
        backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            Timber.d("ModernGameFragment: Back button clicked");
            // Undo the last move
            if (gameStateManager.undoLastMove()) {
//                Toast.makeText(requireContext(), "Move undone", Toast.LENGTH_SHORT).show();
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
        // Hint button - show a hint for the current game
        hintButton = view.findViewById(R.id.hint_button);
        Timber.d("[HINT_SYSTEM] Setting up hint toggle button");
        hintButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Timber.d("[HINT_SYSTEM] Hint button toggled: %s", isChecked ? "ON" : "OFF");
            
            if (isChecked) {
                // Show hint container when button is checked
                hintContainer.setVisibility(View.VISIBLE);
                prevHintButton.setVisibility(View.VISIBLE);
                nextHintButton.setVisibility(View.VISIBLE);
                Timber.d("[HINT_SYSTEM] Showing hint container and navigation buttons");
                
                // Check if we have a solution object at all
                GameSolution solution = gameStateManager.getCurrentSolution();
                if (solution == null || solution.getMoves() == null || solution.getMoves().isEmpty()) {
                    Timber.d("[HINT_SYSTEM] No solution available, calculating...");
                    updateStatusText("AI calculating solution...", true);
                    
                    // Start calculating a solution
                    gameStateManager.calculateSolutionAsync(this);
                    return;
                }
                
                // If we already have a solution, show the current hint
                GameState currentGameState = gameStateManager.getCurrentState().getValue();
                int totalMoves = solution.getMoves().size();
                int currentStep = gameStateManager.getCurrentSolutionStep();
                Timber.d("[HINT_SYSTEM] Showing current hint: step=%d, totalMoves=%d", currentStep, totalMoves);
                
                // Check if we're showing pre-hints or normal hints
                if (showingPreHints && currentStep < (numPreHints + NUM_FIXED_PRE_HINTS)) {
                    showPreHint(solution, totalMoves, currentStep);
                } else {
                    int normalHintIndex = showingPreHints ? currentStep - (numPreHints + NUM_FIXED_PRE_HINTS) : currentStep;
                    showNormalHint(solution, currentGameState, totalMoves, normalHintIndex);
                }
            } else {
                // Hide hint container when button is unchecked
                hintContainer.setVisibility(View.INVISIBLE);
                prevHintButton.setVisibility(View.INVISIBLE);
                nextHintButton.setVisibility(View.INVISIBLE);
                
                // Reset hint step to start from the beginning next time
                currentHintStep = 0;
                gameStateManager.resetSolutionStep();
                
                Timber.d("[HINT_SYSTEM] Hiding hint container and navigation buttons, reset to first hint");
            }
        });
        
        // Set up previous hint button
        prevHintButton.setOnClickListener(v -> {
            Timber.d("[HINT_SYSTEM] Previous hint button clicked");
            GameSolution solution = gameStateManager.getCurrentSolution();
            if (solution == null || solution.getMoves().isEmpty()) {
                Timber.d("[HINT_SYSTEM] No solution available for previous hint");
                return;
            }
            
            // Decrement hint step if possible
            if (currentHintStep > 0) {
                currentHintStep--;
                Timber.d("[HINT_SYSTEM] Moving to previous hint: step=%d", currentHintStep);
                GameState currentGameState = gameStateManager.getCurrentState().getValue();
                int totalMoves = solution.getMoves().size();
                
                // Show the appropriate hint
                if (showingPreHints && currentHintStep < (numPreHints + NUM_FIXED_PRE_HINTS)) {
                    showPreHint(solution, totalMoves, currentHintStep);
                } else {
                    int normalHintIndex = showingPreHints ? currentHintStep - (numPreHints + NUM_FIXED_PRE_HINTS) : currentHintStep;
                    showNormalHint(solution, currentGameState, totalMoves, normalHintIndex);
                }
                
                // Update the game state manager's current solution step
                gameStateManager.resetSolutionStep();
                for (int i = 0; i < currentHintStep; i++) {
                    gameStateManager.incrementSolutionStep();
                }
                Timber.d("[HINT_SYSTEM] Updated solution step to %d", currentHintStep);
            } else {
                Timber.d("[HINT_SYSTEM] Already at first hint, can't go back further");
            }
        });
        
        // Set up next hint button
        nextHintButton.setOnClickListener(v -> {
            Timber.d("[HINT_SYSTEM] Next hint button clicked");
            GameSolution solution = gameStateManager.getCurrentSolution();
            if (solution == null || solution.getMoves().isEmpty()) {
                Timber.d("[HINT_SYSTEM] No solution available for next hint");
                return;
            }
            
            int totalMoves = solution.getMoves().size();
            totalPossibleHints = totalMoves + numPreHints + NUM_FIXED_PRE_HINTS;
            
            // Increment hint step if possible
            if (currentHintStep < totalPossibleHints - 1) {
                currentHintStep++;
                Timber.d("[HINT_SYSTEM] Moving to next hint: step=%d of %d", currentHintStep, totalPossibleHints - 1);
                GameState currentGameState = gameStateManager.getCurrentState().getValue();
                
                // Show the appropriate hint
                if (showingPreHints && currentHintStep < (numPreHints + NUM_FIXED_PRE_HINTS)) {
                    showPreHint(solution, totalMoves, currentHintStep);
                } else {
                    int normalHintIndex = showingPreHints ? currentHintStep - (numPreHints + NUM_FIXED_PRE_HINTS) : currentHintStep;
                    showNormalHint(solution, currentGameState, totalMoves, normalHintIndex);
                }
                
                // Update the game state manager's current solution step
                gameStateManager.resetSolutionStep();
                for (int i = 0; i < currentHintStep; i++) {
                    gameStateManager.incrementSolutionStep();
                }
                Timber.d("[HINT_SYSTEM] Updated solution step to %d", currentHintStep);
            } else {
                Timber.d("[HINT_SYSTEM] Already at last hint, can't go further");
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
        newMapButton = view.findViewById(R.id.new_map_button);
        // Hide the restart and save button in level games
        if(isLevelGame) {
            newMapButton.setVisibility(View.GONE);
            saveMapButton.setVisibility(View.GONE);
        } else {
            newMapButton.setVisibility(View.VISIBLE);
            saveMapButton.setVisibility(View.VISIBLE);
        }
        
        newMapButton.setOnClickListener(v -> {
            Timber.d("ModernGameFragment: Restart button clicked. calling startModernGame()");
            // Start a new game
            gameStateManager.startModernGame();
            // Reset timer
            stopTimer();
            startTimer();
            
            // Randomize pre-hints count for the new game
            numPreHints = ThreadLocalRandom.current().nextInt(2, 5);
            Timber.d("[HINT] Randomized pre-hints for new game: %d", numPreHints);
            
            // Reset hint system for new game
            gameStateManager.resetSolutionStep();
            showingPreHints = true;
            hintButton.setEnabled(true);
            hintButton.setAlpha(1.0f);
            Timber.d("[HINT] Reset hint system for new random game via New Game button");
            
            // Hide the optimal moves button when starting a new game
            if (optimalMovesButton != null) {
                optimalMovesButton.setVisibility(View.GONE);
            }
            
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
            GameState gameState = gameStateManager.getCurrentState().getValue();
            if (gameState != null) {
                if (gameState.getLevelId() > 0) {
                    // Level game - go to next level
                    int currentLevelId = gameState.getLevelId();
                    int nextLevelId = currentLevelId + 1;
                    Timber.d("ModernGameFragment: Moving from level %d to level %d", currentLevelId, nextLevelId);
                    
                    // Start the next level
                    gameStateManager.startLevelGame(nextLevelId);
                    
                    // Reset timer
                    stopTimer();
                    startTimer();
                    
                    // Reset hint system for next level
                    gameStateManager.resetSolutionStep();
                    showingPreHints = true;
                    hintButton.setEnabled(true);
                    hintButton.setAlpha(1.0f);
                    Timber.d("[HINT] Reset hint system for next level");
                    
                    // Hide the Next Level button
                    nextLevelButton.setVisibility(View.GONE);
                    
                    // Clear any hint text from the status display
                    updateStatusText("", false);
                    
                    // Announce the new level
                    announceAccessibility("Starting level " + nextLevelId);
                    announceGameStart();
                    
                    // Hide restart button for level games
                    newMapButton.setVisibility(View.GONE);
                } else {
                    // Random game - start a new random game
                    gameStateManager.startModernGame();
                    
                    // Reset timer
                    stopTimer();
                    startTimer();
                    
                    // Reset hint system for new random game
                    gameStateManager.resetSolutionStep();
                    showingPreHints = true;
                    hintButton.setEnabled(true);
                    hintButton.setAlpha(1.0f);
                    Timber.d("[HINT] Reset hint system for new random game");
                    
                    // Hide the Next Level button
                    nextLevelButton.setVisibility(View.GONE);
                    
                    // Clear any hint text from the status display
                    updateStatusText("", false);
                    
                    // Announce new game
                    announceAccessibility("Starting new random game");
                    announceGameStart();
                    
                    // Show restart button for random games
                    newMapButton.setVisibility(View.VISIBLE);
                }
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
                            element.getX() == i && element.getY() == startY) {
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
                            element.getX() == startX && element.getY() == i) {
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
        
        // Resume autosave when fragment is resumed
        if (!autosaveRunning) {
            startAutosave();
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
        
        // Update unique map ID text
        updateUniqueMapIdText(state);
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
        difficultyTextView.setText(difficultyString);
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
            // If no game state yet, get it
            Timber.d("[BOARD_SIZE_DEBUG] ModernGameFragment.updateBoardSizeText() from BoardSizeManager: %dx%d", Preferences.boardSizeWidth, Preferences.boardSizeHeight);
            boardSizeTextView.setText(String.format(Locale.getDefault(), "Board: %dx%d", Preferences.boardSizeWidth, Preferences.boardSizeHeight));
        }
    }
    
    private void updateUniqueMapIdText(GameState state) {
        if (state == null) {
            return;
        }
        
        // Get the unique map ID
        String uniqueMapId = state.getUniqueMapId();
        
        // Update the unique map ID text view
        // Check if this is a level game and include level name
        if (state.getLevelId() > 0) {
            // For level game - display board size with level name/number
            String levelText = "Level " + state.getLevelId();
            uniqueMapIdTextView.setText(levelText);
        } else if (uniqueMapIdTextView != null) {
            uniqueMapIdTextView.setText("" + uniqueMapId);
        }
    }
    
    private void updateOptimalMovesButton(int optimalMoves, boolean showButton) {
        if (optimalMovesButton != null) {
            optimalMovesButton.setText(String.valueOf(optimalMoves));
            optimalMovesButton.setVisibility(showButton ? View.VISIBLE : View.GONE);
            
            // Change button background color based on move count (cycle through 5 colors)
            int colorIndex = optimalMoves % 5;
            int backgroundColor;
            
            switch (colorIndex) {
                case 0: // Red
                    backgroundColor = Color.parseColor("#F44336");
                    break;
                case 1: // Green
                    backgroundColor = Color.parseColor("#4CAF50");
                    break;
                case 2: // Yellow
                    backgroundColor = Color.parseColor("#FFEB3B");
                    optimalMovesButton.setTextColor(Color.BLACK); // Black text on yellow background
                    return; // Return early to keep black text
                case 3: // Blue
                    backgroundColor = Color.parseColor("#2196F3");
                    break;
                case 4: // Gray
                    backgroundColor = Color.parseColor("#9E9E9E");
                    break;
                default:
                    backgroundColor = Color.parseColor("#1976D2"); // Default blue
                    break;
            }
            
            // Create drawable with the selected color but keep white border
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setColor(backgroundColor);
            drawable.setCornerRadius(32 * getResources().getDisplayMetrics().density); // 32dp corner radius
            drawable.setStroke(4 * (int) getResources().getDisplayMetrics().density, Color.WHITE); // 4dp white border
            
            optimalMovesButton.setBackground(drawable);
            optimalMovesButton.setTextColor(Color.WHITE); // Reset to white text for most colors
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
        
        // Pause autosave when fragment is paused
        stopAutosave();
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
        Timber.d("[HINT_DEBUG] getRobotColorName called with ID: %d", robotId);
        
        switch (robotId) {
            case Constants.COLOR_PINK: return "Pink";
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
     * Shows a pre-hint message based on the current hint step
     * @param solution Current game solution
     * @param totalMoves Total number of moves in the solution
     * @param currentHintStep Current hint step (0-based index)
     */
    private void showPreHint(GameSolution solution, int totalMoves, int currentHintStep) {
        String preHintText;
        Timber.d("[HINT_SYSTEM] Showing pre-hint #%d (total pre-hints: %d + %d fixed)", 
                currentHintStep + 1, numPreHints, NUM_FIXED_PRE_HINTS);
        
        // Calculate total number of pre-hints (regular + fixed)
        int totalPreHints = numPreHints + NUM_FIXED_PRE_HINTS;
        
        // Regular pre-hints come first (decreasing "less than X" hints)
        if (currentHintStep < numPreHints) {
            // Calculate offset for "less than X" hints (starting with largest offset)
            int offset = numPreHints - currentHintStep;
            int hintValue = totalMoves + offset;
            
            preHintText = "The AI found a solution in less than " + hintValue + " moves";
            Timber.d("[HINT_SYSTEM] Showing regular pre-hint %d/%d: less than %d moves", 
                    currentHintStep + 1, numPreHints, hintValue);
        }
        // Next fixed pre-hint: Show exact solution length
        else if (currentHintStep == numPreHints) {
            preHintText = "The AI found a solution in " + totalMoves + " moves";
            Timber.d("[HINT_SYSTEM] Showing exact solution length: %d moves", totalMoves);
            
            // Show an additional toast message for the exact solution hint
            Toast.makeText(requireContext(), 
                "Solution found: " + totalMoves + " moves", 
                Toast.LENGTH_SHORT).show();
        }
        // Last fixed pre-hint: Show which robot to move first
        else if (currentHintStep == numPreHints + 1) {
            if (!solution.getMoves().isEmpty() && solution.getMoves().get(0) instanceof RRGameMove) {
                RRGameMove firstMove = (RRGameMove) solution.getMoves().get(0);
                String robotColorName = getRobotColorName(firstMove.getColor());
                preHintText = "Move the " + robotColorName + " robot first";
                Timber.d("[HINT_SYSTEM] Showing which robot to move first: %s", robotColorName);
            } else {
                // Fallback if we can't determine the first robot
                preHintText = "No solution found";
                Timber.d("[HINT_SYSTEM] Showing fallback (couldn't determine first robot)");
            }
        }
        // Fallback for any other case
        else {
            preHintText = "Ready to show step-by-step hints";
            Timber.d("[HINT_SYSTEM] Showing fallback pre-hint message");
        }
        
        // Display the pre-hint
        updateStatusText(preHintText, true);
        Timber.d("[HINT_SYSTEM] Displayed pre-hint: %s", preHintText);
    }
    
    /**
     * Shows a normal hint with robot movement information
     * @param solution Current game solution
     * @param currentState Current game state
     * @param totalMoves Total number of moves in the solution
     * @param hintIndex Index of the hint to show (0-based)
     */
    private void showNormalHint(GameSolution solution, GameState currentState, int totalMoves, int hintIndex) {
        Timber.d("[HINT_SYSTEM] showNormalHint called with hintIndex: %d", hintIndex);
        // Validate that the hint index is within bounds
        if (hintIndex < 0 || hintIndex >= totalMoves) {
            Timber.e("[HINT_SYSTEM] Invalid hint index: %d (total moves: %d)", hintIndex, totalMoves);
            updateStatusText("All hints have been shown", true);
            return;
        }
        
        try {
            // Get the specific move for this hint
            IGameMove hintMove = solution.getMoves().get(hintIndex);
            Timber.d("[HINT_SYSTEM] Showing normal hint #%d: %s", hintIndex + 1, hintMove);
            
            if (hintMove instanceof RRGameMove rrMove) {
                // Get the robot's color name - use the color from the move
                String robotColorName = getRobotColorName(rrMove.getColor());
                
                // Get the direction name
                String directionName = getDirectionName(rrMove.getDirection());
                
                // Calculate the hint number to display (1-based)
                int displayHintNumber = hintIndex + 1;
                
                // Log details to help with debugging
                Timber.d("[HINT_SYSTEM] Robot color: %d, Direction: %d, Solution hash: %d", 
                        rrMove.getColor(), rrMove.getDirection(), System.identityHashCode(solution));
                
                // Create the hint message with shortened format
                StringBuilder hintMessage = new StringBuilder();
                hintMessage.append(displayHintNumber).append("/").append(totalMoves).append(": ");
                
                // For the first hint, just show the robot color and direction
                if (hintIndex == 0) {
                    hintMessage.append(robotColorName).append(" ").append(directionName);
                    Timber.d("[HINT_SYSTEM] First hint format: %s", hintMessage.toString());
                } else {
                    // For subsequent hints, first show abbreviated previous moves
                    // Get previous moves (up to 4)
                    int startIndex = Math.max(0, hintIndex - 4);
                    
                    // Add abbreviated previous moves
                    for (int i = startIndex; i < hintIndex; i++) {
                        if (i == startIndex && startIndex > 0) {
                            // If we're not showing all previous moves, add an ellipsis
                            hintMessage.append("...,");
                        }
                        
                        IGameMove prevMove = solution.getMoves().get(i);
                        if (prevMove instanceof RRGameMove prevRRMove) {
                            // Get abbreviated color and direction
                            String prevColorName = getRobotColorName(prevRRMove.getColor());
                            String prevDirectionName = getDirectionName(prevRRMove.getDirection());
                            
                            // Add first letter of each
                            hintMessage.append(prevColorName.charAt(0))
                                    .append(prevDirectionName.charAt(0));
                            
                            // Add comma if not the last previous move
                            if (i < hintIndex - 1) {
                                hintMessage.append(",");
                            }
                        }
                    }
                    
                    // Add current move
                    hintMessage.append(", ").append(robotColorName).append(" ").append(directionName);
                    Timber.d("[HINT_SYSTEM] Subsequent hint format: %s", hintMessage.toString());
                }
                
                // Update the status text
                updateStatusText(hintMessage.toString(), true);
                Timber.d("[HINT_SYSTEM] Displayed hint: %s", hintMessage);
                
                // Show the optimal moves button after the first normal hint is shown
                if (hintIndex == 0) {
                    Timber.d("[HINT_SYSTEM] First normal hint shown, displaying optimal moves button");
                    updateOptimalMovesButton(totalMoves, true);
                }
            } else {
                // Error in hint system
                Timber.e("[HINT_SYSTEM] Failed to get a valid hint move");
                updateStatusText("No valid hint available", true);
            }
        } catch (Exception e) {
            Timber.e(e, "[HINT_SYSTEM] Error displaying normal hint #%d", hintIndex + 1);
            updateStatusText("Error displaying hint", true);
        }
    }
    
    /**
     * Shows the next hint in the sequence
     */
    private void showNextHint() {
        Timber.d("[HINT_SYSTEM] Showing next hint");
        GameSolution solution = gameStateManager.getCurrentSolution();
        if (solution == null || solution.getMoves().isEmpty()) {
            Timber.d("[HINT_SYSTEM] No solution available for next hint");
            return;
        }
        
        // Check if this is a level game with level > 10 (no hints allowed)
        GameState currentState = gameStateManager.getCurrentState().getValue();
        if (currentState != null && currentState.getLevelId() > 10) {
            Timber.d("[HINT_SYSTEM] Level > 10, hints are disabled");
            return;
        }
        
        // For level games 1-10, limit to only 2 hints
        if (currentState != null && currentState.getLevelId() > 0 && currentState.getLevelId() <= 10) {
            // Limit to only the first two hints for levels 1-10
            if (currentHintStep >= 2) {
                Timber.d("[HINT_SYSTEM] Level 1-10 reached maximum allowed hints (2)");
                return;
            }
        }
        
        int totalMoves = solution.getMoves().size();
        totalPossibleHints = totalMoves + numPreHints + NUM_FIXED_PRE_HINTS;
        
        // Increment hint step if possible
        if (currentHintStep < totalPossibleHints - 1) {
            currentHintStep++;
            Timber.d("[HINT_SYSTEM] Moving to next hint: step=%d of %d", currentHintStep, totalPossibleHints - 1);
            
            // Show the appropriate hint
            if (showingPreHints && currentHintStep < (numPreHints + NUM_FIXED_PRE_HINTS)) {
                showPreHint(solution, totalMoves, currentHintStep);
            } else {
                int normalHintIndex = showingPreHints ? currentHintStep - (numPreHints + NUM_FIXED_PRE_HINTS) : currentHintStep;
                showNormalHint(solution, currentState, totalMoves, normalHintIndex);
            }
            
            // Update the game state manager's current solution step
            gameStateManager.resetSolutionStep();
            for (int i = 0; i < currentHintStep; i++) {
                gameStateManager.incrementSolutionStep();
            }
            Timber.d("[HINT_SYSTEM] Updated solution step to %d", currentHintStep);
        } else {
            Timber.d("[HINT_SYSTEM] Already at last hint, can't advance further");
        }
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
            
            // Ensure the hint container is visible when showing a hint
            int lightgreen = Color.parseColor("#b5f874");
            int darkgreen = Color.parseColor("#008f00");
            if (isVisible && hintContainer != null) {
                Timber.d("[HINT_SYSTEM] Ensuring hint container is visible when showing hint text");
                hintContainer.setVisibility(View.VISIBLE);
                
                // Check if the hint message contains a robot color name and change background color accordingly
                String lowerMessage = message.toLowerCase();
                GradientDrawable backgroundDrawable = new GradientDrawable();
                backgroundDrawable.setCornerRadius(8);
                
                // Default to using the drawable resource
                boolean useCustomColors = false;
                
                // Change background color based on robot color mentioned in hint
                if (lowerMessage.contains("red")) {
                    backgroundDrawable.setColor(Color.parseColor("#f77070"));
                    backgroundDrawable.setStroke(3, Color.RED);
                    useCustomColors = true;
                } else if (lowerMessage.contains("blue")) {
                    backgroundDrawable.setColor(Color.parseColor("#71a6ff"));
                    backgroundDrawable.setStroke(3, Color.BLUE);
                    useCustomColors = true;
                } else if (lowerMessage.contains("green")) {
                    backgroundDrawable.setColor(lightgreen);
                    backgroundDrawable.setStroke(3, darkgreen);
                    useCustomColors = true;
                } else if (lowerMessage.contains("yellow")) {
                    backgroundDrawable.setColor(Color.parseColor("#fffe71"));
                    backgroundDrawable.setStroke(3, Color.parseColor("#DAA520"));
                    useCustomColors = true;
                } else if (lowerMessage.contains("pink") || lowerMessage.contains("purple") || lowerMessage.contains("violet")) {
                    backgroundDrawable.setColor(Color.parseColor("#eb91ff"));
                    backgroundDrawable.setStroke(3, Color.parseColor("#800080"));
                    useCustomColors = true;
                } else if (lowerMessage.contains("orange")) {
                    backgroundDrawable.setColor(Color.parseColor("#ffa77f"));
                    backgroundDrawable.setStroke(3, Color.parseColor("#FFA500"));
                    useCustomColors = true;
                }
                
                // Apply the background drawable to the status text view
                if (useCustomColors) {
                    statusTextView.setBackground(backgroundDrawable);
                } else {
                    // Use the default drawable resource
                    statusTextView.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.status_text_background));
                }
                
                // Add some padding for better text readability
                statusTextView.setPadding(16, 8, 16, 8);
            } else {
                // Reset background to default green with rounded corners when hiding the hint
                statusTextView.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.status_text_background));
                statusTextView.setPadding(16, 8, 16, 8);
            }
        }
    }
    
    /**
     * Initialize a new game
     */
    private void initializeGame() {
        Timber.d("ModernGameFragment: initializeGame() called");
        
        // Apply settings to game state
        GameState currentState = gameStateManager.getCurrentState().getValue();
        if (currentState != null) {
            currentState.setRobotCount(roboyard.logic.core.Preferences.robotCount);
            currentState.setTargetColors(roboyard.logic.core.Preferences.targetColors);
        }
        
        // Check if this is a random game (not a level) and randomize pre-hints
        if (currentState != null && currentState.getLevelId() <= 0) {
            // Randomize pre-hints for random games when we initialize
            int randomHintCount = ThreadLocalRandom.current().nextInt(2, 5);
            Timber.d("[HINT] Randomized hint count: %d", randomHintCount);
            numPreHints = randomHintCount;
            
            // Auto-calculate solution for random games
            // This ensures onSolutionCalculationCompleted gets called
            Timber.d("[HINT] Auto-calculating solution for random game");
            gameStateManager.calculateSolutionAsync(this);
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
        Timber.d("ModernGameFragment: Solution calculation started");
        requireActivity().runOnUiThread(() -> {
            // Update hint button text to "Cancel"
            hintButton.setTextOn("Cancel");
            hintButton.setTextOff("💡Hint");
            hintButton.setChecked(true);
            updateStatusText("AI is thinking...", true);
            Timber.d("ModernGameFragment: UI updated to show calculation in progress");
        });
    }

    @Override
    public void onSolutionCalculationCompleted(GameSolution solution) {
        Timber.d("[HINT] Solution calculation completed. Solution has %d moves",
                solution.getMoves().size());
        
        // Initialize hints variables
        totalPossibleHints = solution.getMoves().size();
        GameState currentState = gameStateManager.getCurrentState().getValue();
        boolean isLevelGame;
        if (currentState != null && currentState.getLevelId() > 0) {
            isLevelGame = true;
        } else {
            isLevelGame = false;
        }

        Timber.d("[HINT] Game state analysis: currentState=%s, levelId=%d, isLevelGame=%b", 
                currentState != null ? "present" : "null",
                currentState != null ? currentState.getLevelId() : -1,
                isLevelGame);
        
        // Set number of pre-hints based on game type
        if (isLevelGame) {
            Timber.d("[HINT] Level game detected - no pre-hints");
            numPreHints = 0; // No pre-hints for level games
            if (currentState != null && currentState.getLevelId() <= 10) {
                // For levels 1-10, allow two normal hints (handled in hint click)
                hintButton.setEnabled(true);
                hintButton.setAlpha(1.0f);
                Timber.d("[HINT] Level 1-10 - enabling hint button with 2 hint limit");
            } else {
                // For levels > 10, disable hint button immediately
                hintButton.setEnabled(false);
                hintButton.setAlpha(0.5f);
                hintButton.setChecked(false); // Ensure it's unchecked
                hintContainer.setVisibility(View.INVISIBLE); // Hide hint container
                Timber.d("[HINT] Level > 10 - disabling hint button completely");
            }
        } else {
            // Random game - Show pre-hints
            Timber.d("[HINT] Random game detected - randomizing pre-hints");
            // randomize between 2-4:
            int randomHintCount = ThreadLocalRandom.current().nextInt(2, 5);
            Timber.d("[HINT] Randomized hint count: %d", randomHintCount);
            numPreHints = randomHintCount;
            hintButton.setEnabled(true);
            hintButton.setAlpha(1.0f);
        }
        
        // Reset hint counter and show pre-hints initially
        currentHintStep = 0;
        showingPreHints = true;
        
        // Log hint setup
        Timber.d("[HINT] Hint system initialized: totalMoves=%d, numPreHints=%d, isLevelGame=%b", 
                totalPossibleHints, numPreHints, isLevelGame);
        
        // Reset hint button text back to "Hint"
        hintButton.setChecked(false);
        updateStatusText("AI found a solution!", true);
        Timber.d("[HINT] UI updated to show solution found");
        
        // Initialize the optimal moves button value
        updateOptimalMovesButton(solution.getMoves().size(), false);
    }

    @Override
    public void onSolutionCalculationFailed(String errorMessage) {
        Timber.d("ModernGameFragment: Solution calculation failed - %s", errorMessage);
        requireActivity().runOnUiThread(() -> {
            // Reset hint button text back to "Hint"
            hintButton.setChecked(false);
            updateStatusText("Could not find a solution: " + errorMessage, true);
            Timber.d("ModernGameFragment: UI updated to show error");
        });
    }
    
    /**
     * Check if the game is complete (all robots on their target positions).
     * 
     * @return true if all robots are on their correct targets.
     */
    private boolean checkGameCompletion() {
        GameState currentState = gameStateManager.getCurrentState().getValue();
        if (currentState == null) {
            return false;
        }
        
        // Use the new method to check if all robots are at their targets
        boolean allRobotsAtTargets = currentState.areAllRobotsAtTargets();
        Timber.d("[GAME COMPLETION] All robots at targets: %s", allRobotsAtTargets);
        
        return allRobotsAtTargets;
    }
    
    /**
     * Start autosave
     */
    private void startAutosave() {
        if (!autosaveRunning) {
            autosaveHandler.postDelayed(autosaveRunnable, AUTOSAVE_INTERVAL_MS);
            autosaveRunning = true;
        }
    }
    
    /**
     * Stop autosave
     */
    private void stopAutosave() {
        if (autosaveRunning) {
            autosaveHandler.removeCallbacks(autosaveRunnable);
            autosaveRunning = false;
        }
    }
}
