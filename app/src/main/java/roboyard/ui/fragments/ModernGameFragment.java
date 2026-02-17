package roboyard.ui.fragments;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import androidx.core.content.ContextCompat;

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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import roboyard.logic.core.Constants;
import roboyard.eclabs.R;
import roboyard.logic.core.GameState;
import roboyard.logic.core.GridElement;
import roboyard.logic.core.IGameMove;
import roboyard.pm.ia.ricochet.RRGameMove;
import roboyard.ui.util.SoundManager;
import roboyard.logic.core.GameSolution;

import roboyard.ui.components.GameGridView;
import roboyard.ui.components.GameStateManager;
import roboyard.ui.achievements.Achievement;
import roboyard.ui.achievements.AchievementManager;
import roboyard.ui.achievements.AchievementPopup;
import timber.log.Timber;

// Added imports for accessibility
import android.view.accessibility.AccessibilityManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;

import roboyard.logic.core.Preferences;
import roboyard.logic.core.GameElement;
import roboyard.ui.components.UIModeManager;

/**
 * Modern UI implementation of the game screen.
 * all Android-native UI
 * the layout is defined in the xml file app/src/main/res/layout/fragment_modern_game.xml
 */
public class ModernGameFragment extends BaseGameFragment implements GameStateManager.SolutionCallback {

    private static final int MAX_HINTS_UP_TO_LEVEL_10 = 4; // Maximum hints allowed for levels 1-10 (increase this for debugging)
    private static final int LEVEL_10_THRESHHOLD = 10; // this should be set to 10, increase only for debugging
    private static final int MAX_HINT_HISTORY = 6;
    private GameGridView gameGridView;
    private TextView moveCountTextView;
    private TextView squaresMovedTextView;
    private TextView difficultyTextView;

    private TextView uniqueMapIdTextView; // Added for unique map ID display
    private Button backButton;
    private Button resetRobotsButton;
    private ToggleButton hintButton;
    private Button saveMapButton;
    private Button newMapButton;
    private ImageButton diceButton; // Dice button for generating new map when generateNewMapEachTime is false
    private Button menuButton;
    private Button nextLevelButton;
    private Button optimalMovesButton; // Button to display optimal number of moves
    private Button layoutToggleButton; // Button to toggle layout direction in landscape mode
    private TextView timerTextView;
    private TextView statusTextView;
    // Hint navigation components
    private ViewGroup hintContainer;
    private TextView prevHintButton;
    private TextView nextHintButton;
    private ToggleButton liveMoveCounterToggle;
    private android.animation.ObjectAnimator eyeBlinkAnimator;
    
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
    private boolean accessibilityControlsVisible = false;

    // Hint managing
    private static final int NUM_FIXED_PRE_HINTS = 3; // Number of fixed pre-hints that always appear
    private int numPreHints = ThreadLocalRandom.current().nextInt(2, 5); // Randomize between 2-4 by default
    private int totalPossibleHints = 0; // Total possible hints including pre-hints
    private int currentHintStep = 0; // Current hint step (includes pre-hints and regular hints)
    private boolean showingPreHints = true; // Whether we're showing pre-hints (false after reset)

    // Timer variables
    private long startTime = 0L;
    private long elapsedTimeBeforePause = 0L;
    private long lastHistoryCheckTime = 0;
    private long lastAutosaveTime = 0;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private boolean timerRunning = false;
    private boolean autosaveRunning = false;
    private boolean historySaveRunning = false;
    private static final int AUTOSAVE_INTERVAL_MS = 60 * 1000; // 60 seconds
    private static final int HISTORY_CHECK_INTERVAL_MS = 3000; // Check every 3 seconds
    
    // Guard to prevent onLevelCompleted from being called multiple times for the same level
    private int lastCompletedLevelId = -1;
    private long lastCompletedTime = 0;
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
            
            // Check if we should perform autosave
            long currentTime = SystemClock.elapsedRealtime();
            if (autosaveRunning && currentTime - lastAutosaveTime >= AUTOSAVE_INTERVAL_MS) {
                autosave();
                lastAutosaveTime = currentTime;
            }
            
            // Check if we should update history
            if (historySaveRunning && currentTime - lastHistoryCheckTime >= HISTORY_CHECK_INTERVAL_MS) {
                checkHistorySave();
                lastHistoryCheckTime = currentTime;
            }
            
            // Continue updating the timer
            timerHandler.postDelayed(this, 500); // Update every half-second
        }
    };
    
    // Sound manager for game sound effects
    private SoundManager soundManager;
    
    // Achievement popup for showing unlock notifications
    private AchievementPopup achievementPopup;
    private List<Achievement> pendingAchievements = new ArrayList<>();
    
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
            btnMoveNorth.setText(getString(R.string.direction_north));
            btnMoveSouth.setText(getString(R.string.direction_south));
            btnMoveEast.setText(getString(R.string.direction_east));
            btnMoveWest.setText(getString(R.string.direction_west));
            
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
        
        String robotColorName = getLocalizedRobotColorNameByGridElement(selectedRobot);
        
        btnMoveNorth.setText(getString(R.string.robot_move_direction, robotColorName, getString(R.string.direction_north)));
        btnMoveSouth.setText(getString(R.string.robot_move_direction, robotColorName, getString(R.string.direction_south)));
        btnMoveEast.setText(getString(R.string.robot_move_direction, robotColorName, getString(R.string.direction_east)));
        btnMoveWest.setText(getString(R.string.robot_move_direction, robotColorName, getString(R.string.direction_west)));
        
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
            case 0: return Color.RED; // (Pink)
            case 1: return Color.GREEN;
            case 2: return Color.BLUE;
            case 3: return Color.YELLOW;
            case 4: return Color.GRAY; // (Silver)
            default: return Color.DKGRAY;
        }
    }
    
    
    /**
     * Update the robot selection information in the UI
     * @param robot The selected robot
     */
    private void updateRobotSelectionInfo(GameElement robot) {
        if (robot == null) {
            if (txtSelectedRobot != null) {
                txtSelectedRobot.setText(getString(R.string.no_robot_selected));
                txtSelectedRobot.setContentDescription(getString(R.string.no_robot_selected));
            }
            if (txtRobotGoal != null) {
                txtRobotGoal.setText("");
                txtRobotGoal.setContentDescription("");
            }
            announceAccessibility(getString(R.string.no_robot_selected));
            return;
        }
        
        // Get robot color and position
        String colorName = getLocalizedRobotColorNameByGridElement(robot);
        int x = robot.getX();
        int y = robot.getY();
        
        // Create content for the selected robot info
        String robotInfo = getString(R.string.robot_selected_info, colorName, x, y);
        
        // Update selected robot text
        if (txtSelectedRobot != null) {
            txtSelectedRobot.setText(robotInfo);
            txtSelectedRobot.setContentDescription(robotInfo);
        }
        
        // Find the robot's goal
        GameState state = gameStateManager.getCurrentState().getValue();
        if (state != null) {
            for (GameElement element : state.getGameElements()) {
                if (element.getType() == GameElement.TYPE_TARGET && element.getColor() == robot.getColor()) {
                    int goalX = element.getX();
                    int goalY = element.getY();
                    
                    // Create content for the goal info
                    String goalInfo = getString(R.string.robot_target_info, colorName, goalX, goalY);
                    
                    // Update goal text
                    if (txtRobotGoal != null) {
                        txtRobotGoal.setText(goalInfo);
                        txtRobotGoal.setContentDescription(goalInfo);
                    }
                    
                    // Announce selection and goal via TalkBack
                    String message = getString(R.string.robot_selected_a11y, colorName) + ". ";
                    message += getString(R.string.target_a11y, goalX + 1, goalY + 1); // target at position
                    announceAccessibility(message);
                    
                    // Announce possible moves
                    announcePossibleMoves(robot);
                    return;
                }
            }
            
            // No goal found for this robot
            String noGoalInfo = getString(R.string.no_target_for_robot);
            if (txtRobotGoal != null) {
                txtRobotGoal.setText(noGoalInfo);
                txtRobotGoal.setContentDescription(noGoalInfo);
            }
            announceAccessibility(getString(R.string.robot_selected_a11y, colorName));
            
            // Announce possible moves
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
                        Timber.d("[BACK] Detected robot at left edge, moving inward");
                        // Use animation system through GameStateManager instead of direct data update
                        moved = gameStateManager.moveRobotInDirection(1, 0); // Move right
                    } else if (robotX == boardWidth - 1) { // Right edge
                        Timber.d("[BACK] Detected robot at right edge, moving inward");
                        moved = gameStateManager.moveRobotInDirection(-1, 0); // Move left
                    } else if (robotY == 0) { // Top edge
                        Timber.d("[BACK] Detected robot at top edge, moving inward");
                        moved = gameStateManager.moveRobotInDirection(0, 1); // Move down
                    } else if (robotY == boardHeight - 1) { // Bottom edge
                        Timber.d("[BACK] Detected robot at bottom edge, moving inward");
                        moved = gameStateManager.moveRobotInDirection(0, -1); // Move up
                    }
                    
                    if (moved) {
                        // Robot moved - play sound and make announcements
                        // Toast.makeText(requireContext(), "Robot moved away from edge", Toast.LENGTH_SHORT).show();
                        Timber.d("[BACK] Robot moved away from edge, back captured");
                    } else {
                        // Deselect the robot if it couldn't be moved
                        gameState.setSelectedRobot(null);
                        isRobotSelected = false;
                        
                        // Refresh the view if it's initialized
                        if (gameGridView != null) {
                            gameGridView.invalidate();
                        }
                        
                        Context localizedContext = roboyard.ui.RoboyardApplication.getAppContext();
                        Toast.makeText(requireContext(), localizedContext.getString(R.string.robot_movement_canceled), Toast.LENGTH_SHORT).show();
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
        // Apply language settings before inflating the view
        applyLanguageSettings();
        
        // Choose layout based on orientation and preference
        int layoutId = R.layout.fragment_modern_game; // Default layout
        
        Configuration config = getResources().getConfiguration();
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // In landscape mode, check preference for layout direction
            boolean isGridRight = requireContext().getSharedPreferences("roboyard_prefs", Context.MODE_PRIVATE)
                    .getBoolean("landscape_grid_right", true); // Default: grid on right
            
            if (isGridRight) {
                layoutId = R.layout.fragment_modern_game; // Current layout (grid right)
            } else {
                layoutId = R.layout.fragment_modern_game_alt; // Alternative layout (grid left)
            }
            
            Timber.d("[LAYOUT_SELECTION] Landscape mode: using %s layout (grid_right=%s)", 
                    isGridRight ? "standard" : "alternative", isGridRight);
        }
        
        // Inflate the chosen layout
        View view = inflater.inflate(layoutId, container, false);
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize the game state manager
        gameStateManager = new ViewModelProvider(requireActivity()).get(GameStateManager.class);
        
        // Set activity reference in GameStateManager to fix history saving
        gameStateManager.setActivity(requireActivity());
        Timber.d("[HISTORY] Setting activity reference in GameStateManager during onViewCreated");
        
        // Resume map regeneration when entering game screen
        gameStateManager.resumeRegeneration();
        Timber.d("[SOLVER] Resumed map regeneration when entering game screen");
        
        // Initialize UI components
        gameGridView = view.findViewById(R.id.game_grid_view);
        moveCountTextView = view.findViewById(R.id.move_count_text);
        squaresMovedTextView = view.findViewById(R.id.squares_moved_text);
        difficultyTextView = view.findViewById(R.id.difficulty_text);

        uniqueMapIdTextView = view.findViewById(R.id.unique_map_id_text); // Initialize unique map ID text view
        timerTextView = view.findViewById(R.id.game_timer);
        statusTextView = view.findViewById(R.id.status_text);
        optimalMovesButton = view.findViewById(R.id.optimal_moves_button);
        layoutToggleButton = view.findViewById(R.id.layout_toggle_button);
        hintContainer = view.findViewById(R.id.hint_container);
        prevHintButton = view.findViewById(R.id.prev_hint_button);
        nextHintButton = view.findViewById(R.id.next_hint_button);
        liveMoveCounterToggle = view.findViewById(R.id.live_move_counter_toggle);
        
        // Set up close button for game info container
        ImageButton closeButton = view.findViewById(R.id.game_info_close_button);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> {
                View gameInfoContainer = view.findViewById(R.id.game_info_container);
                if (gameInfoContainer != null) {
                    gameInfoContainer.setElevation(-100); // Send to back with negative elevation
                    closeButton.setVisibility(View.GONE); // Hide the close button
                    view.requestLayout();
                }
            });
        }
        
        // Prevent automatic selection of gameGridView by setting focusable to false
        gameGridView.setFocusable(false);
        
        // Initialize sound manager
        soundManager = SoundManager.getInstance(requireContext());
        
        // Initialize achievement popup and listener
        ViewGroup rootView = (ViewGroup) view;
        achievementPopup = new AchievementPopup(requireContext(), rootView);
        AchievementManager.getInstance(requireContext()).setUnlockListener(achievement -> {
            Timber.d("[ACHIEVEMENT_POPUP] Achievement unlocked: %s", achievement.getId());
            pendingAchievements.add(achievement);
            // Show popup after a short delay to allow game completion UI to settle
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!pendingAchievements.isEmpty() && achievementPopup != null) {
                    achievementPopup.show(new ArrayList<>(pendingAchievements));
                    pendingAchievements.clear();
                }
            }, 500);
        });
        
        // Initialize accessibility controls
        accessibilityControlsContainer = view.findViewById(R.id.accessibility_container);
        
        if (accessibilityControlsContainer != null) {
            txtSelectedRobot = accessibilityControlsContainer.findViewById(R.id.txt_selected_robot);
            txtRobotGoal = accessibilityControlsContainer.findViewById(R.id.txt_robot_goal);
            btnMoveNorth = accessibilityControlsContainer.findViewById(R.id.btn_move_north);
            btnMoveSouth = accessibilityControlsContainer.findViewById(R.id.btn_move_south);
            btnMoveEast = accessibilityControlsContainer.findViewById(R.id.btn_move_east);
            btnMoveWest = accessibilityControlsContainer.findViewById(R.id.btn_move_west);
            btnSelectRobot = accessibilityControlsContainer.findViewById(R.id.btn_select_robot); // New button for TalkBack robot selection
            
            // Set up accessibility controls
            setupAccessibilityControls();
        }
        
        // Set up the game grid view
        gameGridView.setFragment(this);
        gameGridView.setGameStateManager(gameStateManager);
        
        // CRITICAL: Connect the GameGridView to the animation manager
        gameStateManager.setGameGridView(gameGridView);
        Timber.d("[ANIM] Connected GameGridView to animation system. Animations enabled: %s", 
                gameStateManager.areAnimationsEnabled());
        
        // Set up the UI mode manager
        UIModeManager uiModeManager = UIModeManager.getInstance(requireContext());
        uiModeManager.setUIMode(UIModeManager.MODE_MODERN);
        
        // Update difficulty and board size text
        updateDifficulty();
        
        // Check and unlock login streak achievements when entering game screen
        AchievementManager.getInstance(requireContext()).checkAndUnlockStreakAchievements();
        Timber.d("[ACHIEVEMENT] Checked login streak achievements on game screen entry");

        
        // Set up observers for game state
        setupObservers();

        // Set up button click listeners
        setupButtons(view);
        
        // Initialize the game
        initializeGame();
        
        // Start autosave
        startAutosave();
        
        // Start history saving
        startHistorySave();
        
        // Set up status text view to act as a button for showing the next hint
        statusTextView.setOnClickListener(v -> {
            Timber.d("[HINT_SYSTEM] Status text view clicked for next hint");
            showNextHint("status text view");
        });
        
        // Set up optimal moves button to also act as a next hint button
        optimalMovesButton.setOnClickListener(v -> {
            Timber.d("[HINT_SYSTEM] Optimal moves button clicked for next hint");
            // Only show next hint if the hint container is visible
            if (hintContainer.getVisibility() == View.VISIBLE && hintButton.isChecked()) {
                showNextHint("optimal moves button");
            }
        });
        
        // Set up unique map ID text view to also act as a next hint button
        uniqueMapIdTextView.setOnClickListener(v -> {
            Timber.d("[HINT_SYSTEM] Unique map ID text view clicked for next hint");
            // Only show next hint if the hint container is visible
            if (hintContainer.getVisibility() == View.VISIBLE && hintButton.isChecked()) {
                showNextHint("unique map ID text view");
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
            
            // Update difficulty display when game state changes (e.g., after loading or starting new game)
            updateDifficulty();
            
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
                    // Hide the small New Game button when showing the big completion buttons
                    newMapButton.setVisibility(View.GONE);
                    
                    if (state.getLevelId() > 0) {
                        // This is a level game, show the Next Level button
                        nextLevelButton.setVisibility(View.VISIBLE);
                        
                        // Hide accessibility controls if accessibility mode is enabled
                        // so user can click the completion buttons below
                        if (accessibilityControlsContainer != null && accessibilityControlsVisible) {
                            accessibilityControlsContainer.setVisibility(View.GONE);
                            Timber.d("[ACCESSIBILITY] Hidden controls on level complete to allow button access");
                        }
                        
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
                        // For beginner levels (1-10), always earn at least 1 star
                        if (stars < 1 && state.getLevelId() <= Constants.MIN_STAR_GUARANTEE_LEVEL) {
                            stars = 1;
                        }
                        
                        Timber.d("[STARS] gameStateManager: Calculated stars: %d", stars);

                        // Create stars string with UTF-8 stars
                        StringBuilder starString = new StringBuilder();
                        for (int i = 0; i < stars; i++) {
                            starString.append("★ ");
                        }
                        
                        // Update status text to show level complete message with stars
                        updateStatusText(getString(R.string.level_complete) + " " + starString.toString().trim(), true);
                        
                        String starsMessage = getString(R.string.level_stars_description, stars);
                        // Toast.makeText(requireContext(), starsMessage, Toast.LENGTH_LONG).show();

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
                        
                        // Trigger achievements for level completion (guard against multiple calls)
                        long elapsedTime = SystemClock.elapsedRealtime() - startTime;
                        int currentLevelId = state.getLevelId();
                        long currentTime = System.currentTimeMillis();
                        
                        // Only call onLevelCompleted if this is a different level or enough time has passed
                        if (currentLevelId != lastCompletedLevelId || (currentTime - lastCompletedTime) > 1000) {
                            Timber.d("[ACHIEVEMENT_GUARD] Calling onLevelCompleted for levelId=%d (last was %d)", currentLevelId, lastCompletedLevelId);
                            AchievementManager.getInstance(requireContext())
                                .onLevelCompleted(currentLevelId, playerMoves, optimalMoves, hintsUsed, stars, elapsedTime);
                            lastCompletedLevelId = currentLevelId;
                            lastCompletedTime = currentTime;
                        } else {
                            Timber.d("[ACHIEVEMENT_GUARD] Skipping duplicate onLevelCompleted call for levelId=%d", currentLevelId);
                        }
                    } else {
                        // This is a random game
                        
                        // Hide accessibility controls if accessibility mode is enabled
                        // so user can click the completion buttons below
                        if (accessibilityControlsContainer != null && accessibilityControlsVisible) {
                            accessibilityControlsContainer.setVisibility(View.GONE);
                            Timber.d("[ACCESSIBILITY] Hidden controls on random game complete to allow button access");
                        }
                        
                        // Show new game button instead of next level
                        nextLevelButton.setText(R.string.new_random_game_button);
                        nextLevelButton.setVisibility(View.VISIBLE);
                        
                        // Show completion message
                        String completionMessage = getString(R.string.random_game_complete);
                        String completionMessage_a11y = completionMessage +
                                " Completed in " + gameStateManager.getMoveCount().getValue() + " moves.";
                        
                        // Add optimal moves information if no hints were used
                        GameSolution solution = gameStateManager.getCurrentSolution();
                        if (solution != null && solution.getMoves() != null && !solution.getMoves().isEmpty() && currentHintStep < 4) {
                            int optimalMoves = solution.getMoves().size();
                            int actualMoves = gameStateManager.getMoveCount().getValue();
                            
                            if (actualMoves == optimalMoves) {
                                String extraMessage = getString(R.string.perfect_solution);
                                if (currentHintStep == 0){
                                    completionMessage += " \n" + extraMessage;
                                }
                                completionMessage_a11y += " " + extraMessage;
                                Timber.d("[COMPLETION_MESSAGE] Perfect solution: %d moves (optimal)", actualMoves);
                            } else {
                                 // show hint to optimal moves if nohints were used
                                // show the hint step if the hint was shown already how many total moves were used
                                String extraMessage;
                                if (optimalMoves < 1) {
                                    extraMessage = getString(R.string.no_solution_found) + "!";
                                    Timber.d("[COMPLETION_MESSAGE] Optimal is less than 1 move (" + optimalMoves + "), showing no solution found");
                                } else {
                                    extraMessage = getString(R.string.pre_hint_less_than_x, actualMoves);
                                    if (actualMoves == 1) {
                                        // Woekaround, if there is still a map with optimal solution 1 which was not solved by the solver in one moves
                                        extraMessage = getString(R.string.pre_hint_less_than_1);
                                    }
                                }
                                completionMessage += " \n" + extraMessage;
                                completionMessage_a11y += " " + extraMessage;
                                Timber.d("[COMPLETION_MESSAGE] Showing optimal moves: %d (actual: %d, no hints used)", optimalMoves, actualMoves);
                            }
                        }
                        
                        announceAccessibility(completionMessage_a11y);
                        updateStatusText(completionMessage, true);
                        
                        // Trigger achievements for random game completion
                        int playerMoves = gameStateManager.getMoveCount().getValue() != null ? 
                                gameStateManager.getMoveCount().getValue() : 0;
                        int optimalMoves = 0;
                        GameSolution sol = gameStateManager.getCurrentSolution();
                        if (sol != null && sol.getMoves() != null) {
                            optimalMoves = sol.getMoves().size();
                        }
                        int hintsUsed = state.getHintCount();
                        long elapsedTime = SystemClock.elapsedRealtime() - startTime;
                        // Reload preferences to ensure we have the latest difficulty setting
                        Preferences.reloadPreferences();
                        boolean isImpossibleMode = Preferences.difficulty == Constants.DIFFICULTY_IMPOSSIBLE; // Impossible mode
                        Timber.d("[ACHIEVEMENTS] Game completion: isImpossibleMode=%b (Preferences.difficulty=%d), optimalMoves=%d", 
                                isImpossibleMode, Preferences.difficulty, optimalMoves);
                        int robotCount = state.getRobots() != null ? state.getRobots().size() : 4;
                        // TODO: Get actual target count from game state when multi-target is implemented
                        int targetCount = 1;
                        int targetsNeeded = 1;
                        
                        AchievementManager.getInstance(requireContext())
                            .onRandomGameCompleted(playerMoves, optimalMoves, hintsUsed, elapsedTime,
                                isImpossibleMode, robotCount, targetCount, targetsNeeded);
                    }

                    // change the "Reset" Button to "Retry"
                    resetRobotsButton.setText(R.string.retry_button);
                    Timber.d("[UI] Changed reset button text to 'Retry'");
                }
            } else {
                // Show the small New Game button again when the game is not complete
                newMapButton.setVisibility(View.VISIBLE);
                
                // Hide the Next Level button when game is not complete
                nextLevelButton.setVisibility(View.GONE);
                
                // Reset button text just in case it was changed
                nextLevelButton.setText(R.string.next_level);
            }
        });
        
        // Observe solver running state to update hint button text and save map button state
        gameStateManager.isSolverRunning().observe(getViewLifecycleOwner(), isRunning -> {
            if (isRunning) {
                // Keep hint container visible during regeneration but hide toggle
                if (hintContainer != null) {
                    hintContainer.setVisibility(View.GONE);
                }
                if (liveMoveCounterToggle != null) {
                    liveMoveCounterToggle.setVisibility(View.GONE);
                }
                
                // Disable hint button during regeneration to prevent pressing
                hintButton.setEnabled(false);
                hintButton.setAlpha(0.5f);
                
                // Change hint button text to "Cancel" while calculating
                hintButton.setTextOn(getString(R.string.hint_cancel_button));
                hintButton.setTextOff(getString(R.string.hint_button));
                hintButton.setChecked(true);
                showSolverCalculatingMessage();
                
                // Disable save map button while solver is running (no solution yet)
                if (saveMapButton != null) {
                    saveMapButton.setEnabled(false);
                    saveMapButton.setAlpha(0.5f);
                }
            } else {
                // Check if timer should be reset after regeneration
                if (gameStateManager != null && gameStateManager.shouldResetTimerAfterRegeneration()) {
                    Timber.d("[TIMER] Solver stopped after regeneration - resetting timer to 0:00");
                    resetAndStartTimer();
                    gameStateManager.clearTimerResetFlag();
                }
                
                // Re-enable hint button after regeneration
                hintButton.setEnabled(true);
                hintButton.setAlpha(1.0f);
                
                // Reset hint button text
                hintButton.setChecked(false);
                // Don't update the status text here - let callbacks handle it appropriately
                // This prevents text flashing/flickering between states
                
                // Enable save map button when solver finishes (solution found or cancelled)
                if (saveMapButton != null && gameStateManager.getCurrentSolution() != null) {
                    saveMapButton.setEnabled(true);
                    saveMapButton.setAlpha(1.0f);
                }
            }
        });

        // Observe live move counter text
        gameStateManager.getLiveMoveCounterText().observe(getViewLifecycleOwner(), text -> {
            if (text != null && !text.isEmpty() && gameStateManager.isLiveMoveCounterEnabled()) {
                if (statusTextView != null) {
                    // Main text always dark green, only the delta in parentheses changes color
                    int darkGreen = Color.parseColor("#006400");
                    int deltaIdx = text.indexOf("(");
                    if (deltaIdx >= 0) {
                        SpannableString spannable = new SpannableString(text);
                        spannable.setSpan(new ForegroundColorSpan(darkGreen), 0, deltaIdx, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        int deviationColor = getDeviationColor(gameStateManager.getLiveMoveCounterDeviation().getValue());
                        spannable.setSpan(new ForegroundColorSpan(deviationColor), deltaIdx, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        statusTextView.setText(spannable);
                    } else {
                        statusTextView.setText(text);
                        statusTextView.setTextColor(darkGreen);
                    }
                    // Always use standard light-green background for live move counter
                    statusTextView.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.hint_text_fancy_background));
                }
            }
        });

        // Observe live solver calculating state for eye blink animation
        gameStateManager.isLiveSolverCalculating().observe(getViewLifecycleOwner(), isCalculating -> {
            if (liveMoveCounterToggle != null && liveMoveCounterToggle.isChecked()) {
                if (isCalculating) {
                    startEyeBlinkAnimation();
                } else {
                    stopEyeBlinkAnimation();
                }
            }
        });
    }
    
    /**
     * Check if a move matches the current hint and advance to the next hint if it does
     * @param state The current game state
     */
    private void checkIfMoveMatchesHint(GameState state) {
        // Update live move counter toggle visibility if we're on the exact solution pre-hint
        // This ensures the toggle appears immediately when the first move is made
        if (showingPreHints && currentHintStep == numPreHints && liveMoveCounterToggle != null) {
            int moveCount = state.getMoveCount();
            // Keep toggle visible if live move counter is enabled
            boolean visible = moveCount >= 1 || gameStateManager.isLiveMoveCounterEnabled();
            liveMoveCounterToggle.setVisibility(visible ? View.VISIBLE : View.GONE);
            Timber.d("[HINT_SYSTEM] Updated eye toggle visibility: moveCount=%d, liveMoveEnabled=%b, visible=%b", 
                moveCount, gameStateManager.isLiveMoveCounterEnabled(), visible);
        }
        
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
                        
                        // Get the hint move
                        IGameMove hintMove = solution.getMoves().get(normalHintIndex);
                        
                        // Get the last moved robot from the state
                        GameElement lastMovedRobot = state.getLastMovedRobot();
                        Integer lastMoveDirection = state.getLastMoveDirection();
                        
                        // If we have both the hint and the actual move information, compare them
                        if (hintMove != null && lastMovedRobot != null && lastMoveDirection != null) {
                            // Get the hint move details
                            int hintRobotColor = ((roboyard.pm.ia.ricochet.RRGameMove)hintMove).getColor();
                            int hintDirection = ((roboyard.pm.ia.ricochet.RRGameMove)hintMove).getDirection();
                            
                            // Log the hint and actual move details for debugging
                            Timber.d("[HINT_SYSTEM] Move verification - Hint robot: %d, Moved robot: %d, Hint direction: %d, Move direction: %d", 
                                hintRobotColor, lastMovedRobot.getColor(), hintDirection, lastMoveDirection);
                            
                            // Check if the robot color and direction match the hint
                            boolean robotMatches = (hintRobotColor == lastMovedRobot.getColor());
                            boolean directionMatches = (hintDirection == lastMoveDirection);
                            
                            // Only advance to the next hint if both robot and direction match
                            if (robotMatches && directionMatches) {
                                Timber.d("[HINT_SYSTEM] Move matches hint! Advancing to next hint");
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
                            } else {
                                Timber.d("[HINT_SYSTEM] Move doesn't match hint - robot match: %b, direction match: %b", 
                                    robotMatches, directionMatches);
                            }
                        } else {
                            Timber.d("[HINT_SYSTEM] Missing information to verify hint match");
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
            
            // Check if hint container is visible (hints are being shown)
            if (hintContainer != null && hintContainer.getVisibility() == View.VISIBLE && currentHintStep > 0) {
                // Go back one step in hints
                currentHintStep--;
                // Update the current solution step
                gameStateManager.resetSolutionStep();
                for (int i = 0; i < currentHintStep; i++) {
                    gameStateManager.incrementSolutionStep();
                }
                Timber.d("[HINT_SYSTEM] Moving to previous hint: step=%d of %d", currentHintStep, totalPossibleHints - 1);
                
                // Display the previous hint
                GameSolution solution = gameStateManager.getCurrentSolution();
                if (solution != null) {
                    int totalMoves = solution.getMoves().size();
                    GameState state = gameStateManager.getCurrentState().getValue();
                    
                    // Show the appropriate hint based on the currentHintStep
                    if (showingPreHints && currentHintStep < (numPreHints + NUM_FIXED_PRE_HINTS)) {
                        showPreHint(solution, totalMoves, currentHintStep);
                    } else {
                        int normalHintIndex = showingPreHints ? currentHintStep - (numPreHints + NUM_FIXED_PRE_HINTS) : currentHintStep;
                        showNormalHint(solution, state, totalMoves, normalHintIndex);
                    }
                }
            }
            
            // Standard undo functionality (original code)
            // Undo the last move
            if (gameStateManager.undoLastMove()) {
                // Force grid view update after undo
                if (gameGridView != null) {
                    // Remove only the last path segment instead of clearing all paths
                    gameGridView.undoLastPathSegment();
                    // Force the grid view to redraw completely
                    gameGridView.invalidate();
                    
                    // Update the game state display to reflect the undone move
                    if (currentState != null) {
                        updateGameState(currentState);
                    }
                }
            } else {
                Toast.makeText(requireContext(), getString(R.string.nothing_to_undo), Toast.LENGTH_SHORT).show();
            }
        });
        
        // Button text: "Reset"
        // Reset robots button - reset robots to starting positions without changing the map
        resetRobotsButton = view.findViewById(R.id.reset_robots_button);
        resetRobotsButton.setOnClickListener(v -> {
            Timber.d("[ROBOTS] ModernGameFragment: Reset robots button clicked");
            // Use resetGame() which provides a more complete soft reset
            gameStateManager.resetGame();
            
            // Hide hint container and reset hint state
            if (hintButton != null && hintButton.isChecked()) {
                hintButton.setChecked(false); // triggers onCheckedChanged → slideUpHintContainer
                Timber.d("[HINT_SYSTEM] Reset: unchecked hint button");
            }
            currentHintStep = 0;
            showingPreHints = true;
            gameStateManager.resetSolutionStep();
            Timber.d("[HINT_SYSTEM] Reset: cleared hint state");
            
            // change the "Reset" Button back to "Reset"
            resetRobotsButton.setText(R.string.reset_button);
            Timber.d("[UI] Changed reset button text back to 'Reset'");

            // Get the current state after reset
            GameState stateAfterReset = gameStateManager.getCurrentState().getValue();
            Timber.d("[ROBOTS] After reset: currentState is %s", stateAfterReset == null ? "null" : "available");
            
            if (stateAfterReset != null) {
                // Clear animation positions for all robots
                for (GameElement element : stateAfterReset.getGameElements()) {
                    if (element.getType() == GameElement.TYPE_ROBOT) {
                        // Clear any animation positions by explicitly setting them to match logical positions
                        element.clearAnimationPosition();
                        Timber.d("[ROBOTS] Cleared animation position for robot color %d at position (%d,%d)", 
                            element.getColor(), element.getX(), element.getY());
                    }
                }
                
                // Clear robot movement paths
                gameGridView.clearRobotPaths();
                
                // Force a complete redraw of the game grid
                gameGridView.invalidate();
                
                // Update all UI elements to match the reset state
                updateGameState(stateAfterReset);
                
                // Force an additional invalidation to ensure UI updates
                requireActivity().runOnUiThread(() -> {
                    gameGridView.invalidate();
                    Timber.d("[ROBOTS] Forced additional UI invalidation after reset");
                });
                
                // Make the close button visible again
                ImageButton closeButton = view.findViewById(R.id.game_info_close_button);
                if (closeButton != null) {
                    closeButton.setVisibility(View.VISIBLE);
                }
                
                // Restore accessibility controls if they were hidden
                if (accessibilityControlsContainer != null && accessibilityControlsVisible) {
                    accessibilityControlsContainer.setVisibility(View.VISIBLE);
                    Timber.d("[ACCESSIBILITY] Restored controls on retry");
                }
                
                // Play a sound to indicate reset
                playSound("move");
                
                // For accessibility, announce the reset
                announceAccessibility(getString(R.string.robots_reset));
            }
        });
        
        // (Button text: "Hint") - this is an onCheckedChanged listener instead of onClick
        // so each time hintButton.setChecked() is called, the listener is triggered
        // Hint button - show a hint for the current game
        hintButton = view.findViewById(R.id.hint_button);
        Timber.d("[HINT_SYSTEM] Setting up hint toggle button");
        hintButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Timber.d("[HINT_SYSTEM] Hint button toggled: %s", isChecked ? "ON" : "OFF");
            
            if (isChecked) {
                // Show hint container with slide down animation
                slideDownHintContainer();
                
                // Remove vertical padding from status text when hint is visible (keep horizontal)
                if (statusTextView != null) {
                    statusTextView.setPadding(16, 0, 16, 0);
                    Timber.d("[HINT_SYSTEM] Removed vertical padding from status text view");
                }
                
                // Make info box padding compact when hints are visible
                View gameInfoContainer = getView().findViewById(R.id.game_info_container);
                if (gameInfoContainer != null) {
                    gameInfoContainer.setPadding(16, 0, 16, 5);
                    Timber.d("[HINT_SYSTEM] Set compact info box padding when hints ON");
                }
                
                Timber.d("[HINT_SYSTEM] Showing hint container and navigation buttons");
                
                // Check if we have a solution object at all
                GameSolution solution = gameStateManager.getCurrentSolution();
                if (solution == null || solution.getMoves() == null || solution.getMoves().isEmpty()) {
                    Timber.d("[HINT_SYSTEM] No solution available, calculating...");
                    showSolverCalculatingMessage();
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
                
                // WICHTIG: Info Box Padding NACH showPreHint/showNormalHint setzen (kompakt)
                View gameInfoContainer2 = getView().findViewById(R.id.game_info_container);
                if (gameInfoContainer2 != null) {
                    gameInfoContainer2.setPadding(16, 0, 16, 5);
                    Timber.d("[HINT_SYSTEM] RE-SET compact info box padding AFTER hint display");
                }
                
                // Update arrow visibility based on current hint position
                updateHintNavigationArrows();
            } else {
                // Hide hint container with slide up animation
                slideUpHintContainer();
                prevHintButton.setVisibility(View.GONE);
                nextHintButton.setVisibility(View.GONE);
                
                // Keep consistent padding (no vertical padding needed)
                if (statusTextView != null) {
                    statusTextView.setPadding(16, 0, 16, 0);
                    Timber.d("[HINT_SYSTEM] Maintained consistent padding for status text view");
                }
                
                // Set bigger info box padding when hints are hidden
                View gameInfoContainer = getView().findViewById(R.id.game_info_container);
                if (gameInfoContainer != null) {
                    gameInfoContainer.setPadding(16, 16, 16, 16);
                    Timber.d("[HINT_SYSTEM] Set bigger info box padding when hints OFF");
                }
                
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
                
                // Update arrow visibility
                updateHintNavigationArrows();
            } else {
                Timber.d("[HINT_SYSTEM] Already at first hint, can't go back further");
            }
        });
        
        // Set up next hint button
        nextHintButton.setOnClickListener(v -> {
            Timber.d("[HINT_SYSTEM] Next hint button clicked");
            showNextHint("next hint button");
        });
        
        // Set up live move counter eye toggle — restore persisted state first
        if (liveMoveCounterToggle != null) {
            boolean savedEnabled = Preferences.liveMoveCounterEnabled;
            liveMoveCounterToggle.setOnCheckedChangeListener(null);
            liveMoveCounterToggle.setChecked(savedEnabled);
            if (savedEnabled) {
                gameStateManager.setLiveMoveCounterEnabled(true);
                // Show hint container with just status text + eye toggle
                if (hintContainer != null) {
                    hintContainer.setVisibility(View.VISIBLE);
                    prevHintButton.setVisibility(View.GONE);
                    nextHintButton.setVisibility(View.GONE);
                }
                liveMoveCounterToggle.setVisibility(View.VISIBLE);
                gameStateManager.triggerLiveSolver();
            }
            liveMoveCounterToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Timber.d("[LIVE_SOLVER] Eye toggle changed: %s", isChecked ? "ON" : "OFF");
                gameStateManager.setLiveMoveCounterEnabled(isChecked);
                if (isChecked) {
                    // Show hint container with status text + eye toggle (no hint arrows)
                    if (hintContainer != null && hintContainer.getVisibility() != View.VISIBLE) {
                        hintContainer.setVisibility(View.VISIBLE);
                        prevHintButton.setVisibility(View.GONE);
                        nextHintButton.setVisibility(View.GONE);
                    }
                    // Trigger an immediate solve for the current position
                    gameStateManager.triggerLiveSolver();
                } else {
                    // If hint button is not checked, hide the container
                    if (hintButton != null && !hintButton.isChecked() && hintContainer != null) {
                        hintContainer.setVisibility(View.GONE);
                    }
                }
            });
        }
        
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
        
        // Dice button for generating new map (only visible when generateNewMapEachTime is false and not in level game)
        diceButton = view.findViewById(R.id.dice_button);
        updateDiceButtonVisibility();

        // "New Game" Button
        newMapButton.setOnClickListener(v -> {
            Timber.d("ModernGameFragment: Restart button clicked. calling startModernGame()");

            // If generateNewMapEachTime is off, check if current map ratio matches settings
            if (!Preferences.generateNewMapEachTime) {
                GameState curState = gameStateManager.getCurrentState().getValue();
                if (curState != null) {
                    int mapW = curState.getWidth();
                    int mapH = curState.getHeight();
                    int settW = Preferences.boardSizeWidth;
                    int settH = Preferences.boardSizeHeight;
                    if (mapW != settW || mapH != settH) {
                        Toast.makeText(requireContext(),
                                getString(R.string.map_dimensions_mismatch, mapW, mapH, settW, settH),
                                Toast.LENGTH_LONG).show();
                        Timber.d("[NEW_GAME] Map ratio mismatch: map=%dx%d, settings=%dx%d — forcing new walls",
                                mapW, mapH, settW, settH);
                    }
                }
            }

            // Clear robot paths BEFORE starting a new game
            if (gameGridView != null) {
                gameGridView.clearRobotPaths();
                Timber.d("[ROBOTS] Cleared robot paths before starting new game");
            }
            
			// Reset move counts and history explicitly to ensure all counters are zeroed
	        gameStateManager.resetMoveCountsAndHistory();
	        Timber.d("[NEW_GAME] Reset move counts and game history");

            // Start a new game
            gameStateManager.startModernGame();
            // Reset timer to 0 for the new game
            resetAndStartTimer();
            
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
            
            // Make the close button visible again
            ImageButton closeButton = view.findViewById(R.id.game_info_close_button);
            if (closeButton != null) {
                closeButton.setVisibility(View.VISIBLE);
            }
            
            // Restore accessibility controls if they were hidden
            if (accessibilityControlsContainer != null && accessibilityControlsVisible) {
                accessibilityControlsContainer.setVisibility(View.VISIBLE);
                Timber.d("[ACCESSIBILITY] Restored controls on restart");
            }
            
            // Clear any hint text from the status display
            updateStatusText("", false);
            
            // Force another path clearing to ensure all paths are removed
            if (gameGridView != null) {
                gameGridView.clearRobotPaths();
                gameGridView.invalidate();
                Timber.d("[ROBOT_PATHS] Forced additional path clearing after new game start");
            }
            
            // Announce the robots and their positions
            announceGameStart();
        });
        
        // (Button text: "Menu")
        // Menu button - go back to level selection
        menuButton = view.findViewById(R.id.menu_button);
        menuButton.setOnClickListener(v -> {
            Timber.d("ModernGameFragment: Menu button clicked - returning to level selection");
            
            // if playing a level game
            if(isLevelGame) {
                // Create a new LevelSelectionFragment instance
                LevelSelectionFragment levelSelectionFragment = new LevelSelectionFragment();
                navigateToDirect(levelSelectionFragment);
            } else {
                // Create a new MainMenuFragment instance
                MainMenuFragment mainMenuFragment = new MainMenuFragment();
                navigateToDirect(mainMenuFragment);
            }
        });
        
        // Layout toggle button - only available in landscape mode
        if (layoutToggleButton != null) {
            layoutToggleButton.setOnClickListener(v -> {
                Timber.d("ModernGameFragment: Layout toggle button clicked");
                // Add fancy button animation
                addButtonClickAnimation(v);
                toggleLayoutDirection();
            });
            
            // Set initial button text based on current preference
            Configuration config = getResources().getConfiguration();
            if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                boolean isGridRight = requireContext().getSharedPreferences("roboyard_prefs", Context.MODE_PRIVATE)
                        .getBoolean("landscape_grid_right", true);
                layoutToggleButton.setText(isGridRight ? "⇄" : "⇆");
            }
        }
        
        // "Next Level" button - go to the next level when a level is completed
        // big "new Game" button - in random game mode
        nextLevelButton = view.findViewById(R.id.next_level_button);
        nextLevelButton.setOnClickListener(v -> {
            Timber.d("ModernGameFragment: Next Level button clicked");
            
            // Reset achievement game session flags for new game
            AchievementManager.getInstance(requireContext()).onNewGameStarted();
            
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
                    
                    // Clear robot paths
                    if (gameGridView != null) {
                        gameGridView.clearRobotPaths();
                        gameGridView.invalidate();
                    }
                    
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
                    
                    // Restore accessibility controls if they were hidden on completion
                    if (accessibilityControlsContainer != null && accessibilityControlsVisible) {
                        accessibilityControlsContainer.setVisibility(View.VISIBLE);
                        Timber.d("[ACCESSIBILITY] Restored controls on next level");
                    }
                    
                    // Hide the optimal moves button when advancing to next level
                    if (optimalMovesButton != null) {
                        optimalMovesButton.setVisibility(View.GONE);
                    }
                    
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
                    
                    // Update difficulty display to reflect current settings (not savegame)
                    updateDifficulty();
                    Timber.d("[NEW_GAME] Updated difficulty display after starting new random game");
                    
                    // Clear robot paths
                    if (gameGridView != null) {
                        gameGridView.clearRobotPaths();
                        gameGridView.invalidate();
                    }
                    
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
                    
                    // Hide the optimal moves button when starting a new random game
                    if (optimalMovesButton != null) {
                        optimalMovesButton.setVisibility(View.GONE);
                    }
                }

                // Reset move counts and history explicitly to ensure all counters are zeroed
                gameStateManager.resetMoveCountsAndHistory();
                Timber.d("[NEW_GAME] Reset move counts and game history");
            }
        });
    }
    
    /**
     * Set up accessibility controls
     */
    private void setupAccessibilityControls() {
        // Check if TalkBack is enabled or if accessibility mode is enabled in preferences
        boolean talkBackEnabled = isTalkBackEnabled();
        boolean shouldShowControls = talkBackEnabled || Preferences.accessibilityMode;
        
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
        
        // If TalkBack is enabled or accessibility mode is enabled, show the accessibility controls automatically
        if (shouldShowControls) {
            accessibilityControlsVisible = true;
            accessibilityControlsContainer.setVisibility(View.VISIBLE);
            Timber.d("Accessibility mode active - automatically showing accessibility controls");
        } else {
            accessibilityControlsVisible = false;
            accessibilityControlsContainer.setVisibility(View.GONE);
            Timber.d("[ACCESSIBILITY_MODE] Accessibility mode inactive - hiding controls");
        }
    }
    
    /**
     * Cycle through available robots for accessibility mode
     * This method is called when the "Select Robot" button is pressed
     */
    private void cycleThroughRobots() {
        GameState state = gameStateManager.getCurrentState().getValue();
        if (state == null) {
            announceAccessibility(getString(R.string.no_game_in_progress));
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
            announceAccessibility(getString(R.string.no_robots_available));
            return;
        }
        
        // Find current robot index
        int currentIndex = -1;
        GameElement currentRobot = state.getSelectedRobot();
        if (currentRobot != null) {
            for (int i = 0; i < robots.size(); i++) {
                if (robots.get(i).getColor() == currentRobot.getColor()) {
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
        
        Timber.d("[ACCESSIBILITY_ROBOT] Cycled to robot: %s", getLocalizedRobotColorNameByGridElement(nextRobot));
        
        // Announce the newly selected robot with more detailed information
        if (isTalkBackEnabled() || Preferences.accessibilityMode) {
            String robotColor = getLocalizedRobotColorNameByGridElement(nextRobot);
            String announcementMessage = getString(R.string.robot_selected_a11y, robotColor);
            
            // Check for goal robot by examining goal elements in the game state
            boolean hasGoal = false;
            for (GameElement element : state.getGameElements()) {
                // Check if element is a goal post and matches the robot color
                if (element.getType() == GameElement.TYPE_TARGET && element.getColor() == nextRobot.getColor()) {
                    hasGoal = true;
                    break;
                }
            }
            
            if (hasGoal) {
                // announcementMessage += ". This robot has a goal to reach.";
            } else {
                // announcementMessage += ". This robot does not have a goal.";
            }
            
            // Try to get the robot position from the game grid
            int[] coords = findRobotPosition(nextRobot);
            if (coords != null) {
                int x = coords[0];
                int y = coords[1];
                announcementMessage += String.format(" at position %d, %d.", x + 1, y + 1);
            }
            
            announceAccessibility(announcementMessage);
            Timber.d("[ACCESSIBILITY_ROBOT] Robot cycling announcement: %s", announcementMessage);
            
            // Try to provide haptic feedback when cycling robots if available
            try {
                if (getActivity() != null) {
                    Object vibratorService = getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibratorService != null) {
                        // Use reflection to avoid direct import of Vibrator class which might not be available
                        Method vibrateMethod = vibratorService.getClass().getMethod("vibrate", long.class);
                        vibrateMethod.invoke(vibratorService, 50L);
                        Timber.d("[ACCESSIBILITY_ROBOT] Provided haptic feedback for robot selection");
                    }
                }
            } catch (Exception e) {
                // Silently ignore vibration errors - it's not critical functionality
                Timber.d("[ACCESSIBILITY_ROBOT] Could not provide haptic feedback: %s", e.getMessage());
            }
            
            // Instead of directly highlighting through gameGridView, we'll use the fact that
            // we've updated the selected robot in the game state, which should handle highlighting
            Timber.d("[ACCESSIBILITY_ROBOT] Updated robot selection in game state");
        }
    }
    
    /**
     * Helper method to find a robot's position on the game grid
     * @param robot The robot element to locate
     * @return array of [x, y] coordinates or null if unavailable
     */
    private int[] findRobotPosition(GameElement robot) {
        if (robot == null) {
            return null;
        }
        
        try {
            GameState state = gameStateManager.getCurrentState().getValue();
            if (state != null) {
                // Try to find the robot by examining the game state
                List<GameElement> elements = state.getGameElements();
                for (GameElement element : elements) {
                    if (element.isRobot() && element.getColor() == robot.getColor()) {

                        try {
                            Timber.d("[ACCESSIBILITY_ROBOT] Using gameGridView to show robot position");
                            return new int[]{element.getX(), element.getY()};
                        } catch (Exception e) {
                            Timber.e("[ACCESSIBILITY_ROBOT] Error finding robot position: %s", e.getMessage());
                            return null;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Timber.e("[ACCESSIBILITY_ERROR] Error finding robot position: %s", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Move the selected robot in the specified direction.
     * 
     * Note: This method is only called by the accessibility directional buttons
     * (btnMoveNorth, btnMoveSouth, btnMoveEast, btnMoveWest). Sound effects,
     * achievement tracking, and collision detection are handled by GameGridView
     * to avoid code duplication.
     * 
     * @param dx Horizontal movement (-1 = left, 1 = right)
     * @param dy Vertical movement (-1 = up, 1 = down)
     */
    private void moveRobotInDirection(int dx, int dy) {
        Timber.d("[ACCESSIBILITY] Moving robot in direction: dx=%d, dy=%d", dx, dy);
        GameState state = gameStateManager.getCurrentState().getValue();
        if (state == null || state.getSelectedRobot() == null) {
            announceAccessibility(getString(R.string.no_robot_selected));
            return;
        }
        
        GameElement robot = state.getSelectedRobot();
        int startX = robot.getX();
        int startY = robot.getY();
        
        // Use GameGridView to handle the movement - it will handle sounds, achievements, and announcements
        boolean moved = gameGridView.moveRobotInDirectionWithEffects(robot, dx, dy);
        
        if (moved) {
            // Calculate distance for accessibility announcement
            int endX = robot.getX();
            int endY = robot.getY();
            int dist = Math.abs(endX - startX) + Math.abs(endY - startY);
            
            // Announce the movement for accessibility
            String robotColor = getLocalizedRobotColorNameByGridElement(robot);
            announceAccessibility(robotColor + " moved " + dist + " squares");
            Timber.d("[ACCESSIBILITY] Robot %s moved %d squares", robotColor, dist);
        } else {
            // Did not move - sound already played by GameGridView
            announceAccessibility(getString(R.string.cannot_move_in_this_direction));
            Timber.d("[ACCESSIBILITY] Movement blocked in direction dx=%d, dy=%d", dx, dy);
        }
    }
    
    /**
     * Handle robot movement sounds and achievements (DRY principle).
     * This method is called from both GameGridView (normal touch) and moveRobotInDirection (accessibility buttons).
     * 
     * @param state The current game state
     * @param movingRobot The robot that moved
     * @param hitRobotElement The robot that was hit (null if no collision)
     * @param hitWall True if robot hit a wall
     * @param source "GameGridView" or "Accessibility" for logging
     */
    public void handleRobotMovementSounds(GameState state, GameElement movingRobot, 
                                         GameElement hitRobotElement, boolean hitWall, String source) {
        if (state == null || movingRobot == null) {
            return;
        }
        
        // Play appropriate sound
        if (hitRobotElement != null) {
            // Play robot-specific collision sound based on attacker and target robot IDs
            int attackerRobotId = movingRobot.getColor();
            int targetRobotId = hitRobotElement.getColor();
            playSoundWithRobotIds("hit_robot", attackerRobotId, targetRobotId);
            // Track robot touch for gimme_five achievement
            trackRobotTouch(state, movingRobot, hitRobotElement);
            Timber.d("[SOUND][%s] Robot collision detected - hit_robot sound played (robot %d hits robot %d)", source, attackerRobotId, targetRobotId);
        } else if (hitWall) {
            playSound("hit_wall");
            Timber.d("[SOUND][%s] Wall collision detected - hit_wall sound played", source);
        } else {
            playSound("move");
            Timber.d("[SOUND][%s] Normal movement - move sound played", source);
        }
    }
    
    /**
     * Track robot touch for gimme_five achievement.
     * Called when a robot hits another robot.
     * 
     * @param state The current game state
     * @param movingRobot The robot that moved
     * @param hitRobot The robot that was hit
     */
    public void trackRobotTouch(GameState state, GameElement movingRobot, GameElement hitRobot) {
        Timber.d("[ACHIEVEMENTS][GIMME_FIVE] Tracking robot touch: Robot %s touched %s", movingRobot, hitRobot);
        if (state == null || movingRobot == null || hitRobot == null) {
            return;
        }
        
        // Get the list of robots to determine indices
        java.util.List<GameElement> robots = state.getRobots();
        if (robots == null || robots.size() < 2) {
            return;
        }
        
        int movingRobotIndex = robots.indexOf(movingRobot);
        int hitRobotIndex = robots.indexOf(hitRobot);
        int robotCount = robots.size();
        
        if (movingRobotIndex >= 0 && hitRobotIndex >= 0) {
            AchievementManager.getInstance(requireContext())
                    .onRobotTouched(movingRobotIndex, hitRobotIndex, robotCount);
            Timber.d("[ACHIEVEMENTS][GIMME_FIVE] Robot %d touched robot %d (total robots: %d)", 
                    movingRobotIndex, hitRobotIndex, robotCount);
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
        
        Timber.d("Announcing possible moves for %s robot", getLocalizedRobotColorNameByGridElement(robot));
        
        GameState state = gameStateManager.getCurrentState().getValue();
        if (state == null) {
            return;
        }
        
        int x = robot.getX();
        int y = robot.getY();
        
        // Build the announcement message with detailed information about possible moves
        StringBuilder announcement = new StringBuilder();
        announcement.append(getString(R.string.possible_moves_a11y)).append(": ");
        
        // Check east movement (right)
        int eastDistance = 0;
        String eastObstacle = getString(R.string.edge_a11y);
        int obstacleX = x;
        for (int i = x + 1; i < state.getWidth(); i++) {
            if (state.canRobotMoveTo(robot, i, y)) {
                eastDistance++;
                obstacleX = i;
            } else {
                // Found an obstacle
                GameElement robotAtPosition = state.getRobotAt(i, y);
                if (robotAtPosition != null) {
                    eastObstacle = getLocalizedRobotColorNameByGridElement(robotAtPosition);
                    
                    // Check if the robot is at its target
                    if (state.isRobotAtTarget(robotAtPosition)) {
                        eastObstacle += " " + getString(R.string.target_reached_a11y);
                    }
                } else {
                    eastObstacle = getString(R.string.wall_a11y);
                }
                break;
            }
        }
        if (eastDistance > 0) {
            String untilString;
            if (eastObstacle.equals(getString(R.string.edge_a11y))) {
                untilString = getString(R.string.until_masculine);
            } else if (eastObstacle.equals(getString(R.string.wall_a11y))) {
                untilString = getString(R.string.until_feminine);
            } else {
                untilString = getString(R.string.until);
            }
            announcement.append(eastDistance).append(" ").append(getString(R.string.squares_east)).append(" ").append(untilString).append(" ").append(eastObstacle).append(", ");
        } else {
            announcement.append(getString(R.string.no_movement_east)).append(", ");
        }
        
        // Check west movement (left)
        int westDistance = 0;
        String westObstacle = getString(R.string.edge_a11y);
        for (int i = x - 1; i >= 0; i--) {
            if (state.canRobotMoveTo(robot, i, y)) {
                westDistance++;
            } else {
                // Found an obstacle
                GameElement robotAtPosition = state.getRobotAt(i, y);
                if (robotAtPosition != null) {
                    westObstacle = getLocalizedRobotColorNameByGridElement(robotAtPosition);
                    
                    // Check if the robot is at its target
                    if (state.isRobotAtTarget(robotAtPosition)) {
                        westObstacle += " " + getString(R.string.target_reached_a11y);
                    }
                } else {
                    westObstacle = getString(R.string.wall_a11y);
                }
                break;
            }
        }
        if (westDistance > 0) {
            String untilString;
            if (westObstacle.equals(getString(R.string.edge_a11y))) {
                untilString = getString(R.string.until_masculine);
            } else if (westObstacle.equals(getString(R.string.wall_a11y))) {
                untilString = getString(R.string.until_feminine);
            } else {
                untilString = getString(R.string.until);
            }
            announcement.append(westDistance).append(" ").append(getString(R.string.squares_west)).append(" ").append(untilString).append(" ").append(westObstacle).append(", ");
        } else {
            announcement.append(getString(R.string.no_movement_west)).append(", ");
        }
        
        // Check north movement (up)
        int northDistance = 0;
        String northObstacle = getString(R.string.edge_a11y);
        for (int i = y - 1; i >= 0; i--) {
            if (state.canRobotMoveTo(robot, x, i)) {
                northDistance++;
            } else {
                // Check if we hit a robot or a wall
                GameElement robotAtPosition = state.getRobotAt(x, i);
                if (robotAtPosition != null) {
                    northObstacle = getLocalizedRobotColorNameByGridElement(robotAtPosition);
                    
                    // Check if the robot is at its target
                    if (state.isRobotAtTarget(robotAtPosition)) {
                        northObstacle += " " + getString(R.string.target_reached_a11y);
                    }
                } else {
                    northObstacle = getString(R.string.wall_a11y);
                }
                break;
            }
        }
        if (northDistance > 0) {
            String untilString;
            if (northObstacle.equals(getString(R.string.edge_a11y))) {
                untilString = getString(R.string.until_masculine);
            } else if (northObstacle.equals(getString(R.string.wall_a11y))) {
                untilString = getString(R.string.until_feminine);
            } else {
                untilString = getString(R.string.until);
            }
            announcement.append(northDistance).append(" ").append(getString(R.string.squares_north)).append(" ").append(untilString).append(" ").append(northObstacle).append(", ");
        } else {
            announcement.append(getString(R.string.no_movement_north)).append(", ");
        }
        
        // Check south movement (down)
        int southDistance = 0;
        String southObstacle = getString(R.string.edge_a11y);
        for (int i = y + 1; i < state.getHeight(); i++) {
            if (state.canRobotMoveTo(robot, x, i)) {
                southDistance++;
            } else {
                // Check if we hit a robot or a wall
                GameElement robotAtPosition = state.getRobotAt(x, i);
                if (robotAtPosition != null) {
                    southObstacle = getLocalizedRobotColorNameByGridElement(robotAtPosition);
                    
                    // Check if the robot is at its target
                    if (state.isRobotAtTarget(robotAtPosition)) {
                        southObstacle += " " + getString(R.string.target_reached_a11y);
                    }
                } else {
                    southObstacle = getString(R.string.wall_a11y);
                }
                break;
            }
        }
        if (southDistance > 0) {
            String untilString;
            if (southObstacle.equals(getString(R.string.edge_a11y))) {
                untilString = getString(R.string.until_masculine);
            } else if (southObstacle.equals(getString(R.string.wall_a11y))) {
                untilString = getString(R.string.until_feminine);
            } else {
                untilString = getString(R.string.until);
            }
            announcement.append(southDistance).append(" ").append(getString(R.string.squares_south)).append(" ").append(untilString).append(" ").append(southObstacle).append(".");
        } else {
            announcement.append(getString(R.string.no_movement_south));
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
        
        // Auto-select a robot matching the target color
        selectRobotWithTargetColor();
        
        // Get the currently selected robot
        GameElement selectedRobot = state.getSelectedRobot();
        
        // Announce only the target at game start
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_TARGET) {
                String targetColor = getLocalizedTargetColorName(element.getColor());
                // Use format parameters directly in the target_a11y string
                announcement.append(targetColor).append(" ")
                          .append(getString(R.string.target_a11y, element.getX() + 1, element.getY() + 1))
                          .append(". ");
                break; // Only announce one target
            }
        }
        
        // If a robot is selected, announce it as well
        if (selectedRobot != null) {
            String robotColor = getLocalizedRobotColorNameAdjByGridElement(selectedRobot);
            int x = selectedRobot.getX();
            int y = selectedRobot.getY();
            
            // Find walls - check each direction
            List<String> walls = new ArrayList<>();
            
            // Check east wall
            if (!state.canRobotMoveTo(selectedRobot, x + 1, y) && x + 1 < state.getWidth() && 
                state.getRobotAt(x + 1, y) == null) {
                walls.add("east");
            }
            
            // Check west wall
            if (!state.canRobotMoveTo(selectedRobot, x - 1, y) && x - 1 >= 0 && 
                state.getRobotAt(x - 1, y) == null) {
                walls.add("west");
            }
            
            // Check north wall
            if (!state.canRobotMoveTo(selectedRobot, x, y - 1) && y - 1 >= 0 && 
                state.getRobotAt(x, y - 1) == null) {
                walls.add("north");
            }
            
            // Check south wall
            if (!state.canRobotMoveTo(selectedRobot, x, y + 1) && y + 1 < state.getHeight() && 
                state.getRobotAt(x, y + 1) == null) {
                walls.add("south");
            }
            
            // Build the concise description
            String robotPosition = String.format("%d,%d", x + 1, y + 1);
            announcement.append(robotColor).append(" ").append(getString(R.string.robot_a11y).replace("%1$s", "").replace("%2$d", "").replace("%3$d", "").trim())
                      .append(" ").append(robotPosition);
            
            // Add walls if present
            if (!walls.isEmpty()) {
                announcement.append(", ").append(getString(R.string.walls_a11y)).append(" ");
                for (int i = 0; i < walls.size(); i++) {
                    announcement.append(walls.get(i));
                    if (i < walls.size() - 1) {
                        announcement.append(", ");
                    }
                }
            }
            announcement.append(". ");
        }
        
        // Make the announcement
        announceAccessibility(announcement.toString());
    }
    
    /**
     * Find and select a robot that matches the color of a target
     * @return true if a matching robot was found and selected, false otherwise
     */
    private boolean selectRobotWithTargetColor() {
        GameState state = gameStateManager.getCurrentState().getValue();
        if (state == null) return false;
        
        // First, identify the target
        GameElement targetElement = null;
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_TARGET) {
                targetElement = element;
                break; // Only consider one target for simplicity
            }
        }
        
        // If no target found, cannot proceed
        if (targetElement == null) return false;
        
        // Now find a robot matching the target's color
        for (GameElement element : state.getGameElements()) {
            if (element.isRobot() && element.getColor() == targetElement.getColor()) {
                // Found a matching robot, select it
                state.setSelectedRobot(element);
                isRobotSelected = true;
                
                // Update UI elements for the selected robot
                updateRobotSelectionInfo(element);
                updateDirectionalButtons(element);
                
                // Force the game grid view to redraw to show the selection
                if (gameGridView != null) {
                    gameGridView.invalidate();
                }
                
                // Announce the selection of this robot
                announcePossibleMoves(element);
                
                Timber.d("[ACCESSIBILITY] Auto-selected %s robot matching target color", 
                        getLocalizedRobotColorNameByGridElement(element));
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Re-measure the game grid to fix scaling after app loses/regains focus
        if (gameGridView != null) {
            // Get current state and update game grid (same as reset button)
            GameState currentState = gameStateManager != null ? gameStateManager.getCurrentState().getValue() : null;
            if (currentState != null) {
                updateGameState(currentState); // This fixes the scaling problem when the app is losing focus and resuming game
                Timber.d("[GRID_LAYOUT] onResume: called updateGameState to fix scaling");
            }
            
            gameGridView.requestLayout();
            gameGridView.invalidate();
            Timber.d("[GRID_LAYOUT] onResume: requested layout recalculation for game grid");
        }
        
        // Refresh activity reference in GameStateManager during resume
        if (gameStateManager != null) {
            gameStateManager.setActivity(requireActivity());
            Timber.d("[HISTORY] Refreshing activity reference in GameStateManager during onResume");
        }
        
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
        
        // Resume history saving when fragment is resumed
        if (!historySaveRunning) {
            startHistorySave();
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

        
        // Update unique map ID text
        updateUniqueMapIdText(state);
    }
    
    private void updateMoveCount(Integer count) {
        if (moveCountTextView != null && count != null) {
            moveCountTextView.setText(getString(R.string.moves_count, count));
        }
    }
    
    private void updateSquaresMoved(Integer squares) {
        if (squaresMovedTextView != null && squares != null) {
            squaresMovedTextView.setText(getString(R.string.squares_moved, squares));
        }
    }
    
    private void updateDifficulty() {
        // Get difficulty string directly from GameStateManager
        String difficultyString = gameStateManager.getLocalizedDifficultyString();
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
            //boardSizeTextView.setText(getString(R.string.board_size, boardWidth, boardHeight));
        } else {
            // If no game state yet, get it
            Timber.d("[BOARD_SIZE_DEBUG] ModernGameFragment.updateBoardSizeText() from BoardSizeManager: %dx%d", Preferences.boardSizeWidth, Preferences.boardSizeHeight);
            //boardSizeTextView.setText(getString(R.string.board_size, Preferences.boardSizeWidth, Preferences.boardSizeHeight));
        }
    }
    
    /**
     * Update dice button visibility based on generateNewMapEachTime setting and game type
     * Dice button is only visible when:
     * - generateNewMapEachTime is false (user wants to keep the same map)
     * - Not in level game mode (only for random games)
     */
    private void updateDiceButtonVisibility() {
        if (diceButton == null) return;
        
        GameState state = gameStateManager.getCurrentState().getValue();
        boolean isLevelGame = state != null && state.getLevelId() > 0;
        
        // Show dice button only when generateNewMapEachTime is false AND not in level game
        boolean showDiceButton = !Preferences.generateNewMapEachTime && !isLevelGame;
        diceButton.setVisibility(showDiceButton ? View.VISIBLE : View.GONE);
        
        Timber.d("[DICE_BUTTON] Visibility updated: generateNewMapEachTime=%s, isLevelGame=%s, visible=%s",
                Preferences.generateNewMapEachTime, isLevelGame, showDiceButton);
        
        // Set up click listener if not already set
        diceButton.setOnClickListener(v -> {
            Timber.d("[DICE_BUTTON] Dice button clicked - generating new map");
            
            // Set the flag to force new map generation once
            roboyard.logic.core.MapGenerator.forceGenerateNewMapOnce = true;
            
            // Clear robot paths before starting new game
            if (gameGridView != null) {
                gameGridView.clearRobotPaths();
                Timber.d("[DICE_BUTTON] Cleared robot paths before generating new map");
            }
            
            // Reset move counts and history
            gameStateManager.resetMoveCountsAndHistory();
            
            // Start a new game (this will use the forceGenerateNewMapOnce flag)
            gameStateManager.startModernGame();
            
            // Reset timer
            stopTimer();
            startTimer();
            
            // Randomize pre-hints count for the new game
            numPreHints = ThreadLocalRandom.current().nextInt(2, 5);
            Timber.d("[DICE_BUTTON] Randomized pre-hints for new game: %d", numPreHints);
            
            // Reset hint system for new game
            gameStateManager.resetSolutionStep();
            showingPreHints = true;
            hintButton.setEnabled(true);
            hintButton.setAlpha(1.0f);
            
            // Hide the optimal moves button when starting a new game
            if (optimalMovesButton != null) {
                optimalMovesButton.setVisibility(View.GONE);
            }
            
            Timber.d("[DICE_BUTTON] New map generated successfully");
        });
    }
    
    private void updateUniqueMapIdText(GameState state) {
        if (state == null) {
            return;
        }
        
        // Get map name from GameStateManager for debugging
        String gameManagerMapName = gameStateManager.getLevelName();
        String gameStateMapName = state.getLevelName();
        Timber.d("[MAPNAME] GameState levelName: '%s', GameStateManager levelName: '%s'", 
                gameStateMapName, gameManagerMapName);
        
        // Get the unique map ID
        String uniqueMapId = state.getUniqueMapId();
        
        // Update the unique map ID text view
        // Check if this is a level game and include level name
        if (state.getLevelId() > 0) {
            // For level game - display board size with level name/number
            String levelText = getString(R.string.level_id_text, state.getLevelId());
            uniqueMapIdTextView.setText(levelText);
            Timber.d("[MAPNAME] Showing level ID text: %s", levelText);
        } 
        // Check for valid map name from GameState
        else if (gameStateMapName != null && !gameStateMapName.isEmpty() ) {
            uniqueMapIdTextView.setText(gameStateMapName);
            Timber.d("[MAPNAME] Showing map name from GameState: %s", gameStateMapName);
        } else if (uniqueMapIdTextView != null) {
            uniqueMapIdTextView.setText(getString(R.string.unique_map_id, uniqueMapId));
            Timber.d("[MAPNAME] Showing unique map ID as fallback: %s", uniqueMapId);
        }
    }
    
    /**
     * Update the optimal moves button with the given number of moves
     * @param optimalMoves Number of optimal moves
     * @param showButton Whether to show the button
     */
    private void updateOptimalMovesButton(int optimalMoves, boolean showButton) {
        if (optimalMovesButton != null) {
            optimalMovesButton.setText(String.valueOf(optimalMoves));
            optimalMovesButton.setSingleLine(true);
            optimalMovesButton.setEllipsize(android.text.TextUtils.TruncateAt.END);
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
                case 4: // Gray (Silver)
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
            // Start the game timer for history tracking
            if (gameStateManager != null) {
                gameStateManager.startGameTimer();
            }
            // Subtract already elapsed time so the timer continues from where it was
            startTime = SystemClock.elapsedRealtime() - elapsedTimeBeforePause;
            lastHistoryCheckTime = SystemClock.elapsedRealtime();
            lastAutosaveTime = SystemClock.elapsedRealtime();
            timerHandler.post(timerRunnable);
            timerRunning = true;
            historySaveRunning = true;
            autosaveRunning = true;
        }
    }
    
    /**
     * Stop the timer
     */
    private void stopTimer() {
        if (timerRunning) {
            // Save elapsed time so we can resume from the same point
            elapsedTimeBeforePause = SystemClock.elapsedRealtime() - startTime;
            timerHandler.removeCallbacks(timerRunnable);
            timerRunning = false;
            historySaveRunning = false;
            autosaveRunning = false;
            // Persist to ViewModel so it survives fragment recreation
            if (gameStateManager != null) {
                gameStateManager.saveUiTimerElapsed(elapsedTimeBeforePause);
            }
        }
    }
    
    /**
     * Reset and start the timer
     */
    private void resetAndStartTimer() {
        stopTimer();
        elapsedTimeBeforePause = 0L;
        startTime = SystemClock.elapsedRealtime();
        lastHistoryCheckTime = startTime;
        lastAutosaveTime = startTime;
        timerTextView.setText("00:00");
        // Reset ViewModel timer state for new game
        if (gameStateManager != null) {
            gameStateManager.resetUiTimer();
        }
        startTimer();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        stopTimer();
    }
    
    /**
     * Play a sound effect
     * @param soundType Type of sound to play ("move", "hit_wall", "hit_robot", "win")
     */
    public void playSound(String soundType) {
        // Only play sound if it's enabled in global preferences
        if (soundManager != null && roboyard.logic.core.Preferences.soundEnabled) {
            Timber.d("[SOUND][ACHIEVEMENTS][GIMME_FIVE]: Playing sound %s", soundType);
            soundManager.playSound(soundType);
        } else if (soundManager == null) {
            Timber.e("[SOUND][ACHIEVEMENTS][GIMME_FIVE]: SoundManager is null, cannot play sound %s", soundType);
        } else {
            Timber.d("[SOUND][ACHIEVEMENTS][GIMME_FIVE]: Sound disabled in preferences, not playing %s", soundType);
        }
    }
    
    /**
     * Play a robot-specific collision sound
     * @param soundType Type of sound to play (typically "hit_robot")
     * @param attackerRobotId ID of the robot that moved (0-4)
     * @param targetRobotId ID of the robot that was hit (0-4)
     */
    public void playSoundWithRobotIds(String soundType, int attackerRobotId, int targetRobotId) {
        // Only play sound if it's enabled in global preferences
        if (soundManager != null && roboyard.logic.core.Preferences.soundEnabled) {
            Timber.d("[SOUND][ACHIEVEMENTS][GIMME_FIVE]: Playing sound %s (robot %d hits robot %d)", soundType, attackerRobotId, targetRobotId);
            soundManager.playSound(soundType, attackerRobotId, targetRobotId);
        } else if (soundManager == null) {
            Timber.e("[SOUND][ACHIEVEMENTS][GIMME_FIVE]: SoundManager is null, cannot play sound %s", soundType);
        } else {
            Timber.d("[SOUND][ACHIEVEMENTS][GIMME_FIVE]: Sound disabled in preferences, not playing %s", soundType);
        }
    }



    /**
     * Get the direction name from a move direction for accessibility messages
     * @param direction Direction constant from ERRGameMove
     * @return Human-readable direction name
     */
    private String getLocalizedDirectionName_a11y(int direction) {
        
        switch (direction) {
            case 1: // ERRGameMove.UP.getDirection()
                return getString(R.string.direction_up);
            case 4: // ERRGameMove.DOWN.getDirection()
                return getString(R.string.direction_down);
            case 2: // ERRGameMove.RIGHT.getDirection()
                return getString(R.string.direction_right);
            case 8: // ERRGameMove.LEFT.getDirection()
                return getString(R.string.direction_left);
            default:
                return "unknown direction";
        }
    }

    /**
     * Get the direction name from a move direction
     * @param direction Direction constant from ERRGameMove
     * @return Human-readable direction name
     */
    private String getDirectionArrow(int direction) {
        // Return a placeholder character that will be replaced with SVG drawable
        switch (direction) {
            case 1: // UP
                return "↑";
            case 2: // RIGHT
                return "→";
            case 4: // DOWN
                return "↓";
            case 8: // LEFT
                return "←";
            default:
                return "?";
        }
    }

    /**
     * Replace arrow characters in hint text with SVG triangle drawables
     * Arrows maintain consistent size regardless of text context (history vs current move)
     */
    private CharSequence formatHintWithSVGArrows(String hintText) {
        android.text.SpannableString spannable = new android.text.SpannableString(hintText);
        int textColor = android.graphics.Color.BLACK;
        float textSize = 48f; // Default text size in pixels
        
        try {
            // Try to get the text color and size from the status text view if available
            if (statusTextView != null) {
                textColor = statusTextView.getCurrentTextColor();
                textSize = statusTextView.getTextSize(); // Get current text size in pixels
            }
        } catch (Exception e) {
            Timber.d("[HINT_ARROWS] Could not get text properties, using defaults");
        }

        // Scale arrow size to text size (slightly larger than text for visibility)
        int arrowSize = (int) (textSize * 1.2f);
        Timber.d("[HINT_ARROWS] Text size: %.1f px, Arrow size: %d px", textSize, arrowSize);
        
        String[] arrows = {"↑", "→", "↓", "←"};
        int[] directions = {1, 2, 4, 8};

        for (int i = 0; i < arrows.length; i++) {
            String arrow = arrows[i];
            int direction = directions[i];
            int index = hintText.indexOf(arrow);
            
            while (index >= 0) {
                roboyard.ui.utils.DirectionArrowDrawable drawable = 
                    new roboyard.ui.utils.DirectionArrowDrawable(direction, textColor, arrowSize);
                
                // Use FixedSizeImageSpan to prevent scaling relative to text size
                roboyard.ui.utils.FixedSizeImageSpan imageSpan = 
                    new roboyard.ui.utils.FixedSizeImageSpan(drawable, arrowSize, arrowSize);
                spannable.setSpan(imageSpan, index, index + 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                
                index = hintText.indexOf(arrow, index + 1);
            }
        }
        
        return spannable;
    }

    /**
     * Get localized (translated) color name for a robot ID
     */
    private String getLocalizedRobotColorName(int robotId) {
        Timber.d("[HINT_DEBUG] getLocalizedRobotColorName called with ID: %d", robotId);

        switch (robotId) {
            case Constants.COLOR_PINK: return getString(R.string.color_pink);
            case Constants.COLOR_GREEN: return getString(R.string.color_green);
            case Constants.COLOR_BLUE: return getString(R.string.color_blue);
            case Constants.COLOR_YELLOW: return getString(R.string.color_yellow);
            case Constants.COLOR_SILVER: return getString(R.string.color_silver);
            default:
                return getString(R.string.unknown_color, robotId);
        }
    }

    /**
     * Get localized (translated) color name for a robot ID in dative
     */
    private String getLocalizedRobotColorNameDative(int robotId) {
        Timber.d("[HINT_DEBUG] getLocalizedRobotColorNameDative called with ID: %d", robotId);

        switch (robotId) {
            case Constants.COLOR_PINK: return getString(R.string.color_pink_dative);
            case Constants.COLOR_GREEN: return getString(R.string.color_green_dative);
            case Constants.COLOR_BLUE: return getString(R.string.color_blue_dative);
            case Constants.COLOR_YELLOW: return getString(R.string.color_yellow_dative);
            case Constants.COLOR_SILVER: return getString(R.string.color_silver_dative);
            default:
                return getString(R.string.unknown_color, robotId);
        }
    }
    
    /**
     * Get localized (translated) color name for a target in proper adjective form (e.g., "Pinkes" for "Pinkes Ziel")
     */
    private String getLocalizedTargetColorName(int robotId) {
        switch (robotId) {
            case Constants.COLOR_PINK: return getString(R.string.color_pink_target);
            case Constants.COLOR_GREEN: return getString(R.string.color_green_target);
            case Constants.COLOR_BLUE: return getString(R.string.color_blue_target);
            case Constants.COLOR_YELLOW: return getString(R.string.color_yellow_target);
            case Constants.COLOR_SILVER: return getString(R.string.color_silver_target);
            default:
                return getString(R.string.unknown_color, robotId);
        }
    }
    
    /**
     * Get localized (translated) color name for a robot
     */
    private String getLocalizedRobotColorNameByGridElement(GameElement robot) {
        if (robot == null) return "";
        
        int c = robot.getColor();
        switch (c) {
            case 0: return getString(R.string.color_pink);
            case 1: return getString(R.string.color_green);
            case 2: return getString(R.string.color_blue);
            case 3: return getString(R.string.color_yellow);
            case 4: return getString(R.string.color_silver);
            default:
                Timber.e("Unknown robot color: '%d'", c);
                return getString(R.string.unknown_color, c);
        }
    }
    
    /**
     * Get localized (translated) color name for a robot in proper adjective form (e.g., "Pinker" for "Pinker Roboter")
     */
    private String getLocalizedRobotColorNameAdj(int robotId) {
        switch (robotId) {
            case Constants.COLOR_PINK: return getString(R.string.color_pink_adj);
            case Constants.COLOR_GREEN: return getString(R.string.color_green_adj);
            case Constants.COLOR_BLUE: return getString(R.string.color_blue_adj);
            case Constants.COLOR_YELLOW: return getString(R.string.color_yellow_adj);
            case Constants.COLOR_SILVER: return getString(R.string.color_silver_adj);
            default:
                return getString(R.string.unknown_color, robotId);
        }
    }
    
    /**
     * Get localized (translated) color name for a robot in proper adjective form
     */
    private String getLocalizedRobotColorNameAdjByGridElement(GameElement robot) {
        if (robot == null) return "";
        return getLocalizedRobotColorNameAdj(robot.getColor());
    }
    
    /**
     * Get color name for a robot ID (internal usage only)
     * Returns fixed English names regardless of locale for internal identification.
     */
    private String getRobotColorName(int robotId) {
        Timber.d("[HINT_DEBUG] getRobotColorName called with ID: %d", robotId);
        
        switch (robotId) {
            case Constants.COLOR_PINK: return "Pink";
            case Constants.COLOR_GREEN: return "Green";
            case Constants.COLOR_BLUE: return "Blue";
            case Constants.COLOR_YELLOW: return "Yellow";
            case Constants.COLOR_SILVER: return "Silver";
            default: return "Unknown: " + robotId;
        }
    }
    

    /**
     * Apply language settings
     */
    private void applyLanguageSettings() {
        try {
            // Get saved language setting
            String languageCode = roboyard.logic.core.Preferences.appLanguage;
            Timber.d("ROBOYARD_LANGUAGE: Loading saved language in game screen: %s", languageCode);
            
            // Apply language change
            Locale locale = new Locale(languageCode);
            Locale.setDefault(locale);
            
            Resources resources = requireContext().getResources();
            Configuration config = new Configuration(resources.getConfiguration());
            config.setLocale(locale);
            
            resources.updateConfiguration(config, resources.getDisplayMetrics());
        } catch (Exception e) {
            Timber.e(e, "ROBOYARD_LANGUAGE: Error loading language settings in game screen");
        }
    }
    
    /**
     * Start history saving
     */
    private void startHistorySave() {
        historySaveRunning = true;
        lastHistoryCheckTime = SystemClock.elapsedRealtime();
        Timber.d("[HISTORY] History saving started");
    }
    
    /**
     * Shows a message that the A.I. is calculating a solution with restart counter and last solution info
     */
    private void showSolverCalculatingMessage() {
        int solverRestartCount = gameStateManager.getSolverRestartCount();
        int lastMoves = gameStateManager.getLastSolutionMinMoves();
        
        Timber.d("[SOLVER_STATUS][DIAG] Building status message: restartCount=%d, lastMoves=%d", 
                solverRestartCount, lastMoves);
        
        String messageBase = getString(R.string.ai_calculating);
        String counterInfo = "";
        
        // Add restart counter and last solution info if applicable
        if (solverRestartCount > 3) { // Only show counter after some restarts
            counterInfo = String.format(Locale.getDefault(), " (%d", solverRestartCount);
            
            // Add last minimum moves if we have any
            if (lastMoves > 0) {
                counterInfo += String.format(Locale.getDefault(), "/%d", lastMoves);
                Timber.d("[SOLVER_STATUS][DIAG] Adding last minimum moves: %d", lastMoves);
            } else {
                Timber.d("[SOLVER_STATUS][DIAG] No last minimum moves available (lastMoves=%d)", lastMoves);
            }
            
            counterInfo += ")";
        }
        
        // Display calculation status visually but don't announce it through accessibility
        if (statusTextView != null) {
            statusTextView.setText(messageBase + counterInfo);
            statusTextView.setVisibility(View.VISIBLE);
            
            // Prevent this particular status from being announced via accessibility
            // by setting it as an announcement for another accessibility event type
            statusTextView.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_NONE);
        }
        Timber.d("[SOLVER_STATUS] %s (not announced to accessibility)", messageBase + counterInfo);
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
        
        // Hide the eye toggle by default; only the exact solution pre-hint shows it
        // But keep it visible if live move counter is enabled
        if (liveMoveCounterToggle != null && currentHintStep != numPreHints) {
            liveMoveCounterToggle.setVisibility(
                    gameStateManager.isLiveMoveCounterEnabled() ? View.VISIBLE : View.GONE);
        }
        
        // Calculate total number of pre-hints (regular + fixed)
        int totalPreHints = numPreHints + NUM_FIXED_PRE_HINTS;
        
        // Regular pre-hints come first (decreasing "less than X" hints)
        if (currentHintStep < numPreHints) {
            // Calculate offset for "less than X" hints (starting with largest offset)
            int offset = numPreHints - currentHintStep;
            int hintValue = totalMoves + offset;
            
            preHintText = getString(R.string.pre_hint_less_than_x, hintValue);
            Timber.d("[HINT_SYSTEM] Showing regular pre-hint %d/%d: less than %d moves", 
                    currentHintStep + 1, numPreHints, hintValue);
        }
        // First fixed pre-hint: Show exact solution length
        else if (currentHintStep == numPreHints) {
            preHintText = getString(R.string.pre_hint_exact_solution, totalMoves);
            Timber.d("[HINT_SYSTEM] Showing exact solution length: %d moves", totalMoves);
            
            // Show an additional toast message for the exact solution hint
            // Toast.makeText(requireContext(), getString(R.string.solution_found, totalMoves), Toast.LENGTH_SHORT).show();
            
            // Show the optimal moves button when the optimal moves are available
            updateOptimalMovesButton(totalMoves, true);
            
            // Show the live move counter eye toggle from this hint onwards (always visible from here)
            if (liveMoveCounterToggle != null) {
                liveMoveCounterToggle.setVisibility(View.VISIBLE);
                Timber.d("[HINT_SYSTEM] Toggle button now visible from exact solution hint onwards");
            }
        }
        // Second fixed pre-hint: Show involved robot colors
        else if (currentHintStep == numPreHints + 1) {
            // Create hint message with the correct hint number format
            StringBuilder message = new StringBuilder();
            int displayHintNumber = currentHintStep + 1;
            int totalPossibleHints = numPreHints + NUM_FIXED_PRE_HINTS + solution.getMoves().size();
            message.append(displayHintNumber).append("/").append(totalPossibleHints).append(": ");
            message.append(getString(R.string.pre_hint_involved_robots)).append(" ");
            
            // Analyze ALL moves to see which robots are involved
            ArrayList<String> robotsInvolved = new ArrayList<>();
            List<IGameMove> moves = solution.getMoves();
            for (IGameMove move : moves) {
                if (move instanceof RRGameMove) {
                    RRGameMove rrMove = (RRGameMove) move;
                    String robotColor = getLocalizedRobotColorName(rrMove.getColor());
                    // Only add if not already in the list
                    if (!robotsInvolved.contains(robotColor)) {
                        robotsInvolved.add(robotColor);
                    }
                }
            }
            
            // Format the list of robots with commas and "and" for the last item
            for (int i = 0; i < robotsInvolved.size(); i++) {
                if (i == robotsInvolved.size() - 1 && robotsInvolved.size() > 1) {
                    // For the last item with multiple items, add 'and' without comma
                    message.append(getString(R.string.and)).append(" ").append(robotsInvolved.get(i));
                    if (Locale.getDefault().getLanguage().equals("en")) {
                        // hardcoded word "robots" at the end of the hint only in english
                        message.append(robotsInvolved.size() > 1 ? " robots" : " robot");
                    }
                } else if (i == robotsInvolved.size() - 1) {
                    // For a single item or the very last one
                    message.append(robotsInvolved.get(i));
                    if (Locale.getDefault().getLanguage().equals("en")) {
                        // hardcoded word "robot" at the end of the hint only in english
                        message.append(" robot");
                    }
                } else if (i == robotsInvolved.size() - 2) {
                    // Second-to-last item - add without trailing comma before 'and'
                    message.append(robotsInvolved.get(i)).append(" ");
                } else {
                    // Any other item - add with comma
                    message.append(robotsInvolved.get(i)).append(", ");
                }
            }
            
            preHintText = message.toString();
            Timber.d("[HINT_SYSTEM] Showing involved robot colors: %s", preHintText);
        }
        // Last fixed pre-hint: Show which robot to move first
        else if (currentHintStep == numPreHints + 2) {
            if (!solution.getMoves().isEmpty() && solution.getMoves().get(0) instanceof RRGameMove) {
                RRGameMove firstMove = (RRGameMove) solution.getMoves().get(0);
                String robotColorName = getLocalizedRobotColorNameDative(firstMove.getColor());
                
                preHintText = getString(R.string.pre_hint_first_move, robotColorName); // "Move the X robot first"
                Timber.d("[HINT_SYSTEM] Showing which robot to move first: %s", robotColorName);
            } else {
                // Fallback if we can't determine the first robot
                preHintText = getString(R.string.no_solution_found);
                Timber.d("[HINT_SYSTEM] Showing fallback (couldn't determine first robot)");
            }
        }
        // Fallback for any other case
        else {
            preHintText = getString(R.string.pre_hint_ready);
            Timber.d("[HINT_SYSTEM] Showing fallback pre-hint message");
        }
        
        // Display the pre-hint
        updateStatusText(preHintText, true);
        Timber.d("[HINT_SYSTEM] Displayed pre-hint: %s", preHintText);
        // Announce hint
        announceAccessibility(preHintText);
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
        
        // Hide the eye toggle during normal hints, unless live move counter is active
        if (liveMoveCounterToggle != null && !gameStateManager.isLiveMoveCounterEnabled()) {
            liveMoveCounterToggle.setVisibility(View.GONE);
        }
        
        // Validate that the hint index is within bounds
        if (hintIndex < 0 || hintIndex >= totalMoves) {
            Timber.e("[HINT_SYSTEM] Invalid hint index: %d (total moves: %d)", hintIndex, totalMoves);
            updateStatusText(getString(R.string.all_hints_shown), true);
            return;
        }
        
        // Track hint usage for achievements - only count real hints (not pre-hints)
        if (currentState != null) {
            currentState.incrementHintCount();
            AchievementManager.getInstance(requireContext()).onHintUsed();
            Timber.d("[HINT_SYSTEM] Hint counted for achievements: hintCount=%d", currentState.getHintCount());
        }
        
        try {
            // Get the specific move for this hint
            IGameMove hintMove = solution.getMoves().get(hintIndex);
            
            if (hintMove instanceof RRGameMove rrMove) {
                // Get the robot's color name - use the color from the move
                String robotColorName = getLocalizedRobotColorName(rrMove.getColor());
                
                // Get the direction arrow (UTF-8 arrow that will be replaced by SVG)
                String directionArrow = getDirectionArrow(rrMove.getDirection());
                
                // Calculate the hint number to display (1-based)
                int displayHintNumber = hintIndex + 1;
                
                // Log details to help with debugging
                Timber.d("[HINT_SYSTEM] Robot color: %d, Direction: %d, Solution hash: %d", 
                        rrMove.getColor(), rrMove.getDirection(), System.identityHashCode(solution));
                
                // Create the hint message with shortened format
                StringBuilder hintMessage = new StringBuilder();
                // Create a separate accessibility hint message that only includes the current step
                StringBuilder accessibilityHintMessage = new StringBuilder();
                // Get the accessibility-specific direction name
                String directionNameA11y = getLocalizedDirectionName_a11y(rrMove.getDirection());
                // For accessibility, just announce the current step without the history
                accessibilityHintMessage.append(displayHintNumber).append(". ")
                    .append(robotColorName).append(" ").append(directionNameA11y);
                Timber.d("[ACCESSIBILITY][HINT] Simplified hint: %s", accessibilityHintMessage.toString());
                
                // Create the visual hint message with full history
                hintMessage.append(displayHintNumber).append(". "); // append(totalMoves).append(": ");
                
                // For the first hint, just show which robot to move
                if (hintIndex == 0) {
                    hintMessage.append(robotColorName).append(" ").append(directionArrow);
                    Timber.d("[HINT_SYSTEM] First hint format: %s", hintMessage.toString());
                } else {
                    // For subsequent hints, first show abbreviated previous moves
                    // Get previous moves (up to MAX_HINT_HISTORY)
                    int startIndex = Math.max(0, hintIndex - MAX_HINT_HISTORY);
                    
                    // Add abbreviated previous moves
                    String lastColorName = null;
                    for (int i = startIndex; i < hintIndex; i++) {
                        if (i == startIndex && startIndex > 0) {
                            // If we're not showing all previous moves, add an ellipsis
                            hintMessage.append("...,");
                        }
                        
                        IGameMove prevMove = solution.getMoves().get(i);
                        if (prevMove instanceof RRGameMove prevRRMove) {
                            // Get abbreviated color and direction
                            String prevColorName = getColorAbbreviation(getLocalizedRobotColorName(prevRRMove.getColor()));
                            String prevDirectionArrow = getDirectionSymbol(getDirectionArrow(prevRRMove.getDirection()));
                            
                            // Only add color abbreviation if color changed from previous move
                            if (lastColorName == null || !prevColorName.equals(lastColorName)) {
                                hintMessage.append(prevColorName);
                            }
                            hintMessage.append(prevDirectionArrow);
                            lastColorName = prevColorName;

                            // Add comma if not the last previous move
                            if (i < hintIndex - 1) {
                                hintMessage.append(",");
                            }
                        }
                    }
                    
                    // Add current move
                    // String colorAbbreviation = getColorAbbreviation(robotColorName);
                    hintMessage.append(", ").append(robotColorName + " ").append(directionArrow);
                    Timber.d("[HINT_SYSTEM] Subsequent hint format: %s", hintMessage.toString());
                }
                
                // Update the status text
                updateStatusText(hintMessage.toString(), true);
                Timber.d("[HINT_SYSTEM] Displayed hint: %s", hintMessage);

                // Announce hint
                announceAccessibility(accessibilityHintMessage.toString());
        		
                if (hintIndex == 0) {
                    Timber.d("[HINT_SYSTEM] First normal hint shown");
                }
            } else {
                // Error in hint system
                Timber.e("[HINT_SYSTEM] Failed to get a valid hint move");
                updateStatusText(getString(R.string.no_valid_hint), true);
            }
        } catch (Exception e) {
            Timber.e(e, "[HINT_SYSTEM] Error displaying normal hint #%d", hintIndex + 1);
            updateStatusText(getString(R.string.error_displaying_hint), true);
        }
    }

    private String getColorAbbreviation(String colorName) {
        Locale currentLocale = getResources().getConfiguration().getLocales().get(0);
        String languageCode = currentLocale.getLanguage();
        
        // Spezielle Behandlung für Deutsch
        if (languageCode.equals("de")) {
            // Prüfen auf deutsche Farbduplikate (Grün/Gelb)
            if (colorName.equalsIgnoreCase("Grün") || colorName.startsWith("Gr")) {
                return "Gr";
            } else if (colorName.equalsIgnoreCase("Gelb") || colorName.startsWith("Ge")) {
                return "Ge";
            }
        }
        
        // Bei allen anderen Sprachen oder Farben den ersten Buchstaben verwenden
        if (colorName != null && !colorName.isEmpty()) {
            return String.valueOf(colorName.charAt(0)).toUpperCase();
        } else {
            return "?";
        }
    }
    
    private String getDirectionSymbol(String directionName) {
        // Die Pfeile werden bereits von getDirectionArrow() zurückgegeben
        return directionName;
    }
    
    /**
     * Update hint navigation arrow visibility based on current hint position and level restrictions
     */
    private void updateHintNavigationArrows() {
        GameSolution solution = gameStateManager.getCurrentSolution();
        if (solution == null || solution.getMoves().isEmpty()) {
            // No solution yet, hide both arrows
            prevHintButton.setVisibility(View.GONE);
            nextHintButton.setVisibility(View.GONE);
            return;
        }
        
        int totalMoves = solution.getMoves().size();
        int totalPossibleHints = totalMoves + numPreHints + NUM_FIXED_PRE_HINTS;
        
        // Hide prev arrow if at first hint
        if (currentHintStep == 0) {
            prevHintButton.setVisibility(View.GONE);
        } else {
            prevHintButton.setVisibility(View.VISIBLE);
        }
        
        // Check if next hint is allowed
        boolean canShowNextHint = true;
        GameState currentState = gameStateManager.getCurrentState().getValue();
        
        // For level games > 10, no hints allowed
        if (currentState != null && currentState.getLevelId() > LEVEL_10_THRESHHOLD) {
            canShowNextHint = false;
        }
        // For level games 1-10, limit to MAX_HINTS_UP_TO_LEVEL_10
        else if (currentState != null && currentState.getLevelId() > 0 && currentState.getLevelId() <= LEVEL_10_THRESHHOLD) {
            if (currentHintStep >= MAX_HINTS_UP_TO_LEVEL_10 - 1) {
                canShowNextHint = false;
            }
        }
        
        // Hide next arrow if at last hint or if next hint is not allowed
        if (!canShowNextHint || currentHintStep >= totalPossibleHints - 1) {
            nextHintButton.setVisibility(View.GONE);
        } else {
            nextHintButton.setVisibility(View.VISIBLE);
        }
        
        Timber.d("[HINT_SYSTEM] Updated arrow visibility: step=%d/%d, canShowNext=%b, prev=%s, next=%s",
                currentHintStep, totalPossibleHints - 1, canShowNextHint,
                prevHintButton.getVisibility() == View.VISIBLE ? "visible" : "hidden",
                nextHintButton.getVisibility() == View.VISIBLE ? "visible" : "hidden");
    }
    
    /**
     * Common method to show the next hint with level restrictions
     * used by status text view, next hint button, etc.
     */
    private void showNextHint(String source) {
        // Check if this is a level game with level > 10 (no hints allowed)
        GameState currentState = gameStateManager.getCurrentState().getValue();
        if (currentState != null && currentState.getLevelId() > LEVEL_10_THRESHHOLD) {
            Timber.d("[HINT_SYSTEM] Level > 10, hints are disabled for %s", source);
            return;
        }
        
        // For level games 1-10, limit to only 2 hints
        if (currentState != null && currentState.getLevelId() > 0 && currentState.getLevelId() <= LEVEL_10_THRESHHOLD) {
            // Limit to only the first two hints for levels 1-10
            if (currentHintStep >= MAX_HINTS_UP_TO_LEVEL_10) {
                Timber.d("[HINT_SYSTEM] Level 1-10 reached maximum allowed hints (%d) for %s, currentHintStep=%d", MAX_HINTS_UP_TO_LEVEL_10, source, currentHintStep);
                return;
            }
        }
        
        GameSolution solution = gameStateManager.getCurrentSolution();
        if (solution == null || solution.getMoves().isEmpty()) {
            Timber.d("[HINT_SYSTEM] No solution available for next hint from %s", source);
            return;
        }
        
        int totalMoves = solution.getMoves().size();
        totalPossibleHints = totalMoves + numPreHints + NUM_FIXED_PRE_HINTS;
        
        // Increment hint step if possible
        if (currentHintStep < totalPossibleHints - 1) {
            currentHintStep++;
            Timber.d("[HINT_SYSTEM] Moving to next hint: step=%d of %d (from %s)", currentHintStep, totalPossibleHints - 1, source);
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
            
            // Update arrow visibility
            updateHintNavigationArrows();
        } else {
            Timber.d("[HINT_SYSTEM] Already at last hint, can't go further (from %s)", source);
        }
    }

    /**
     * Helper method to update the status text with a consistent approach
     * @param message The message to display
     * @param isVisible Whether to make the status text visible
     */
    private void updateStatusText(String message, boolean isVisible) {
        Timber.d("[STATUS_TEXT] Updating status text: '%s', visible: %b", message, isVisible);
        if (statusTextView != null) {
            // Format hint text with SVG arrows instead of UTF-8 characters
            CharSequence formattedText = formatHintWithSVGArrows(message);
            statusTextView.setText(formattedText);
            // Reset text color to default black (live counter sets it to green)
            statusTextView.setTextColor(android.graphics.Color.parseColor("#1A1A1A"));
            statusTextView.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE); // Use INVISIBLE instead of GONE to reserve space
            
            // Ensure the hint container is visible when showing a hint
            int lightgreen = Color.parseColor("#b5f874");
            int darkgreen = Color.parseColor("#008f00");
            if (isVisible && hintContainer != null) {
                Timber.d("[HINT_SYSTEM] Ensuring hint container is visible when showing hint text");
                
                // Add fancy fade-in animation if container was previously hidden
                if (hintContainer.getVisibility() != View.VISIBLE) {
                    hintContainer.setVisibility(View.VISIBLE);
                    android.view.animation.Animation fadeIn = android.view.animation.AnimationUtils.loadAnimation(
                        requireContext(), R.anim.hint_fade_in);
                    hintContainer.startAnimation(fadeIn);
                } else {
                    hintContainer.setVisibility(View.VISIBLE);
                }
                
                // Check if the hint message contains a robot color name and change background color accordingly
                String lowerMessage = message.toLowerCase();
                GradientDrawable backgroundDrawable = new GradientDrawable();
                backgroundDrawable.setShape(GradientDrawable.RECTANGLE);
                backgroundDrawable.setColor(Color.parseColor("#1976D2")); // Default blue
                backgroundDrawable.setCornerRadius(8);
                
                // Default to using the drawable resource
                boolean useCustomColors = false;
                
                // Change background color based on robot color mentioned in hint
                // TODO: localization strings instead of all extra
                if (lowerMessage.contains("red") || lowerMessage.contains("rot")) {
                    backgroundDrawable.setColor(Color.parseColor("#f77070"));
                    backgroundDrawable.setStroke(3, Color.RED);
                    useCustomColors = true;
                } else if (lowerMessage.contains("blue") || lowerMessage.contains("blau")) {
                    backgroundDrawable.setColor(Color.parseColor("#71a6ff"));
                    backgroundDrawable.setStroke(3, Color.BLUE);
                    useCustomColors = true;
                } else if (lowerMessage.contains("green") || lowerMessage.contains("grun")) {
                    backgroundDrawable.setColor(lightgreen);
                    backgroundDrawable.setStroke(3, darkgreen);
                    useCustomColors = true;
                } else if (lowerMessage.contains("yellow") || lowerMessage.contains("gelb")) {
                    backgroundDrawable.setColor(Color.parseColor("#fffe71"));
                    backgroundDrawable.setStroke(3, Color.parseColor("#DAA520"));
                    useCustomColors = true;
                } else if (lowerMessage.contains("pink") || lowerMessage.contains("rosa") || lowerMessage.contains("purple") || lowerMessage.contains("violet")) {
                    backgroundDrawable.setColor(Color.parseColor("#eb91ff"));
                    backgroundDrawable.setStroke(3, Color.parseColor("#800080"));
                    useCustomColors = true;
                } else if (lowerMessage.contains("silver") || lowerMessage.contains("silber")) {
                    backgroundDrawable.setColor(Color.parseColor("#c0c0c0"));
                    backgroundDrawable.setStroke(3, Color.GRAY); // (Silver)
                    useCustomColors = true;
                } else if (lowerMessage.contains("orange") || lowerMessage.contains("orange")) {
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
                
                // No vertical padding for compact hint display
                statusTextView.setPadding(16, 0, 16, 0);

                // Set content description for accessibility
                statusTextView.setContentDescription(message);
                if (isTalkBackEnabled() || Preferences.accessibilityMode) {
                    announceAccessibility(message);
                    Timber.d("[ACCESSIBILITY_MODE] Announced hint text in accessibility mode: %s", message);
                }
            } else {
                // Reset background to default green with rounded corners when hiding the hint
                statusTextView.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.status_text_background));
            }
        }
    }
    
    /**
     * Slides the hint container down with smooth animation
     */
    private void slideDownHintContainer() {
        if (hintContainer == null) return;
        
        // Get info box for synchronized animation
        View gameInfoContainer = getView().findViewById(R.id.game_info_container);
        
        // Clear any previous animation and reset state
        hintContainer.clearAnimation();
        hintContainer.animate().cancel();
        if (gameInfoContainer != null) {
            gameInfoContainer.clearAnimation();
            gameInfoContainer.animate().cancel();
        }
        
        // Make sure container is visible first to measure height
        hintContainer.setVisibility(View.VISIBLE);
        hintContainer.setAlpha(0f);
        hintContainer.setTranslationY(0f); // Reset position first
        
        // Post to ensure layout is measured
        hintContainer.post(() -> {
            // Get the measured height or use a default
            int height = hintContainer.getHeight();
            if (height == 0) {
                height = 120; // Default height in pixels
            }
            
            // Start from above the screen
            hintContainer.setTranslationY(-height);
            
            // Animate hint container slide down with ease transition
            hintContainer.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .setListener(null) // Clear any previous listener
                .start();
            
            // Animate info box slide down synchronously
            if (gameInfoContainer != null) {
                gameInfoContainer.setTranslationY(-height); // Start from same position as hint container
                gameInfoContainer.animate()
                    .translationY(0f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
            }
        });
        
        Timber.d("[HINT_SYSTEM] Sliding hint container and info box DOWN with synchronized animation");
    }
    
    /**
     * Slides the hint container up with smooth animation
     */
    private void slideUpHintContainer() {
        if (hintContainer == null) return;
        
        // Get info box for synchronized animation
        View gameInfoContainer = getView().findViewById(R.id.game_info_container);
        
        // Clear any previous animation
        hintContainer.clearAnimation();
        hintContainer.animate().cancel();
        if (gameInfoContainer != null) {
            gameInfoContainer.clearAnimation();
            gameInfoContainer.animate().cancel();
        }
        
        // Get the current height
        int height = hintContainer.getHeight();
        if (height == 0) {
            height = 120; // Default height in pixels
        }
        
        // Animate hint container slide up with ease transition
        hintContainer.animate()
            .translationY(-height)
            .alpha(0f)
            .setDuration(300)
            .setInterpolator(new android.view.animation.AccelerateInterpolator())
            .setListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    // Reset all properties for next animation
                    hintContainer.setTranslationY(0f);
                    hintContainer.setAlpha(1f);
                    hintContainer.clearAnimation();
                    // If live move counter is active, keep container visible with just status + eye toggle
                    if (gameStateManager.isLiveMoveCounterEnabled()) {
                        hintContainer.setVisibility(View.VISIBLE);
                        prevHintButton.setVisibility(View.GONE);
                        nextHintButton.setVisibility(View.GONE);
                        if (liveMoveCounterToggle != null) {
                            liveMoveCounterToggle.setVisibility(View.VISIBLE);
                        }
                        Timber.d("[HINT_SYSTEM] Animation UP completed, keeping container for live move counter");
                    } else {
                        hintContainer.setVisibility(View.GONE);
                        Timber.d("[HINT_SYSTEM] Animation UP completed, container hidden");
                    }
                }
            })
            .start();
        
        // Animate info box slide up synchronously
        if (gameInfoContainer != null) {
            gameInfoContainer.animate()
                .translationY(-height)
                .setDuration(300)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .setListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        // Reset info box position
                        gameInfoContainer.setTranslationY(0f);
                        gameInfoContainer.clearAnimation();
                    }
                })
                .start();
        }
        
        Timber.d("[HINT_SYSTEM] Sliding hint container and info box UP with synchronized animation");
    }
    
    
    /**
     * Initialize a new game
     */
    private void initializeGame() {
        Timber.d("ModernGameFragment: initializeGame() called");
        
        // Get current game state (robotCount and targetColors are already set
        // correctly during game creation - by Preferences for random games,
        // or by actual target count for levels and external maps)
        GameState currentState = gameStateManager.getCurrentState().getValue();
        
        // Check if this is a random game (not a level) and randomize pre-hints
        if (currentState != null && currentState.getLevelId() <= 0) {
            // Randomize pre-hints for random games when we initialize
            int randomHintCount = ThreadLocalRandom.current().nextInt(2, 5);
            Timber.d("[HINT] Randomized hint count: %d", randomHintCount);
            numPreHints = randomHintCount;
        }
        
        // Clear any previous hint or status text and hide hint UI
        updateStatusText("", false);
        if (hintContainer != null) {
            hintContainer.setVisibility(View.GONE);
        }
        // Hide live move counter toggle when new map is accepted
        if (liveMoveCounterToggle != null) {
            liveMoveCounterToggle.setVisibility(View.GONE);
        }
        if (hintButton != null && hintButton.isChecked()) {
            hintButton.setChecked(false);
        }
        currentHintStep = 0;
        showingPreHints = true;

        // Resume timer from ViewModel if it was running AND no new game was loaded
        // If a new game was loaded (from SaveGame/History/Level), reset the timer to 0
        if (gameStateManager != null && gameStateManager.wasUiTimerRunning() && !gameStateManager.isNewGameLoaded()) {
            elapsedTimeBeforePause = gameStateManager.getUiTimerElapsedMs();
            Timber.d("[TIMER] Resuming timer from ViewModel: %d ms", elapsedTimeBeforePause);
            startTimer();
            // Clear the flag after resuming
            gameStateManager.clearNewGameLoadedFlag();
        } else {
            // Reset timer for new games or when no timer was running
            resetAndStartTimer();
            // Clear the flag after resetting
            if (gameStateManager != null) {
                gameStateManager.clearNewGameLoadedFlag();
            }
        }
        
        // Clear robot paths
        if (gameGridView != null) {
            gameGridView.clearRobotPaths();
            gameGridView.invalidate();
            Timber.d("[ROBOT_PATHS] Cleared robot paths during game initialization");
        }

        // Reset move counts and history explicitly to ensure all counters are zeroed
        gameStateManager.resetMoveCountsAndHistory();
        Timber.d("[GAME_INIT] Reset move counts and game history");

        // Auto-select robot that matches the target color
        selectRobotWithTargetColor();
    }

    @Override
    public void onSolutionCalculationStarted() {
        Timber.d("ModernGameFragment: Solution calculation started");
        requireActivity().runOnUiThread(() -> {
            // Update hint button text to "Cancel"
            hintButton.setTextOn(getString(R.string.cancel_button));
            hintButton.setTextOff(getString(R.string.hint_button));
            hintButton.setChecked(true);
            showSolverCalculatingMessage();
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
                hintContainer.setVisibility(View.GONE); // Hide hint container
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
        
        // hide the hint text
        updateStatusText(getString(R.string.solution_found), false);
        Timber.d("[HINT] UI updated to show solution found");
        
        // Initialize the optimal moves button value
        updateOptimalMovesButton(solution.getMoves().size(), false);
    }

    @Override
    public void onSolutionCalculationFailed(String errorMessage) {
        Timber.d("ModernGameFragment: Solution calculation failed - %s", errorMessage);
        
        // Check if the fragment is still attached to an activity before proceeding
        if (!isAdded()) {
            Timber.w("ModernGameFragment: Fragment not attached to activity, skipping UI update");
            return;
        }
        
        requireActivity().runOnUiThread(() -> {
            // Reset hint button text back to "Hint"
            hintButton.setChecked(false);
            updateStatusText(getString(R.string.solution_error, errorMessage), true);
            Timber.d("ModernGameFragment: UI updated to show error");
        });
    }
    
    
    /**
     * Start autosave
     */
    private void startAutosave() {
        if (!autosaveRunning) {
            long currentTime = SystemClock.elapsedRealtime();
            if (currentTime - lastAutosaveTime >= AUTOSAVE_INTERVAL_MS) {
                autosave();
                lastAutosaveTime = currentTime;
            }
            autosaveRunning = true;
        }
    }
    
    
    /**
     * Perform autosave
     */
    private void autosave() {
        // Only autosave if game is in progress, not solved, and NOT a level game
        if (gameStateManager != null && !gameStateManager.isGameComplete().getValue()) {
            GameState currentState = gameStateManager.getCurrentState().getValue();
            
            // Check if this is a level game (levelId > 0 means it's a level game)
            if (currentState != null && currentState.getLevelId() > 0) {
                Timber.d("[AUTOSAVE] Skipping autosave for level game (levelId: %d)", currentState.getLevelId());
                return;
            }
            
            Timber.d("[AUTOSAVE] Performing autosave to slot 0 (random game)");
            boolean saved = gameStateManager.saveGame(0, true); // Autosave to slot 0
            if (saved) {
                Timber.d("[AUTOSAVE] Game successfully autosaved to slot 0");
            } else {
                Timber.e("[AUTOSAVE] Failed to autosave game to slot 0");
            }
        }
    }
    
    /**
     * Toggle layout direction in landscape mode
     */
    private void toggleLayoutDirection() {
        // Check if we're in landscape mode
        Configuration config = getResources().getConfiguration();
        if (config.orientation != Configuration.ORIENTATION_LANDSCAPE) {
            Timber.d("[LAYOUT_TOGGLE] Not in landscape mode, ignoring toggle");
            return;
        }
        
        // Get current layout direction from shared preferences
        boolean isGridRight = requireContext().getSharedPreferences("roboyard_prefs", Context.MODE_PRIVATE)
                .getBoolean("landscape_grid_right", true); // Default: grid on right
        
        // Toggle the preference
        boolean newGridRight = !isGridRight;
        requireContext().getSharedPreferences("roboyard_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("landscape_grid_right", newGridRight)
                .apply();
        
        Timber.d("[LAYOUT_TOGGLE] Toggled layout: grid_right=%s -> %s", isGridRight, newGridRight);
        
        // Update button text to show current state
        if (layoutToggleButton != null) {
            layoutToggleButton.setText(newGridRight ? "⇄" : "⇆");
        }
        
        // Recreate the activity to apply the new layout
        requireActivity().recreate();
    }
    
    /**
     * Add fancy button click animation
     */
    private void addButtonClickAnimation(View button) {
        // Scale down animation
        android.view.animation.ScaleAnimation scaleDown = new android.view.animation.ScaleAnimation(
            1.0f, 0.95f, 1.0f, 0.95f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f);
        scaleDown.setDuration(100);
        scaleDown.setInterpolator(new android.view.animation.AccelerateInterpolator());
        
        // Scale up animation
        android.view.animation.ScaleAnimation scaleUp = new android.view.animation.ScaleAnimation(
            0.95f, 1.0f, 0.95f, 1.0f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f);
        scaleUp.setDuration(100);
        scaleUp.setStartOffset(100);
        scaleUp.setInterpolator(new android.view.animation.OvershootInterpolator());
        
        // Animation set
        android.view.animation.AnimationSet animationSet = new android.view.animation.AnimationSet(false);
        animationSet.addAnimation(scaleDown);
        animationSet.addAnimation(scaleUp);
        
        button.startAnimation(animationSet);
    }
    
    /**
     * Get color for deviation value:
     * 0=green, 1=green-yellow, 2=dark yellow, 3=orange, 4=red, 5+=dark-red
     */
    private int getDeviationColor(Integer deviation) {
        if (deviation == null || deviation <= 0) return Color.parseColor("#006400"); // Green
        switch (deviation) {
            case 1: return Color.parseColor("#7CB342"); // Green-yellow
            case 2: return Color.parseColor("#C6A700"); // Dark yellow
            case 3: return Color.parseColor("#E65100"); // Orange
            case 4: return Color.parseColor("#D50000"); // Red
            default: return Color.parseColor("#8B0000"); // Dark red (5+)
        }
    }

    /**
     * Start blinking animation on the eye toggle while the live solver is calculating
     */
    private void startEyeBlinkAnimation() {
        if (liveMoveCounterToggle == null) return;
        stopEyeBlinkAnimation();
        eyeBlinkAnimator = android.animation.ObjectAnimator.ofFloat(liveMoveCounterToggle, "alpha", 1f, 0.2f);
        eyeBlinkAnimator.setDuration(400);
        eyeBlinkAnimator.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
        eyeBlinkAnimator.setRepeatMode(android.animation.ObjectAnimator.REVERSE);
        eyeBlinkAnimator.start();
    }

    /**
     * Stop blinking animation on the eye toggle
     */
    private void stopEyeBlinkAnimation() {
        if (eyeBlinkAnimator != null) {
            eyeBlinkAnimator.cancel();
            eyeBlinkAnimator = null;
        }
        if (liveMoveCounterToggle != null) {
            liveMoveCounterToggle.setAlpha(1f);
        }
    }

    private void checkHistorySave() {
        // Only check for history saving if game is in progress and not already saved
        if (gameStateManager != null && !gameStateManager.isGameComplete().getValue()) {
            GameState currentState = gameStateManager.getCurrentState().getValue();
            
            // Check if this is a level game (levelId > 0 means it's a level game)
            if (currentState != null && currentState.getLevelId() > 0) {
                Timber.d("[HISTORY] Skipping history save for level game (levelId: %d)", currentState.getLevelId());
                return;
            }
            
            Timber.d("[HISTORY] Checking if game should be saved to history (random game)");
            gameStateManager.updateGameTimer(); // This handles threshold checking and saving
        }
    }
}
