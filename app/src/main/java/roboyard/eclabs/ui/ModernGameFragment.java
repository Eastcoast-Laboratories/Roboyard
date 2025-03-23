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
import java.util.Locale;

import roboyard.eclabs.Constants;
import roboyard.eclabs.GridElement;
import roboyard.eclabs.R;
import roboyard.eclabs.util.BoardSizeManager;
import roboyard.eclabs.util.DifficultyManager;
import roboyard.eclabs.util.UIModeManager;
import timber.log.Timber;

/**
 * Modern UI implementation of the game screen.
 * This provides a cleaner, more Android-native UI compared to the canvas-based version.
 */
public class ModernGameFragment extends BaseGameFragment {
    
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
    
    // Class member to track if a robot is currently selected
    private boolean isRobotSelected = false;
    
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
            String timeStr = String.format(Locale.getDefault(), "Time: %02d:%02d", minutes, seconds);
            timerTextView.setText(timeStr);
            
            // Continue updating the timer
            timerHandler.postDelayed(this, 500); // Update every half-second
        }
    };
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        
        // Update board size text
        updateBoardSizeText();
        
        // Observe game state changes
        gameStateManager.getCurrentState().observe(getViewLifecycleOwner(), this::updateGameState);
        gameStateManager.getMoveCount().observe(getViewLifecycleOwner(), this::updateMoveCount);
        gameStateManager.getSquaresMoved().observe(getViewLifecycleOwner(), this::updateSquaresMoved);
        
        // Observe game completion status
        gameStateManager.getCurrentState().observe(getViewLifecycleOwner(), state -> {
            if (state != null) {
                if (state.isComplete() || state.checkCompletion()) {
                    Timber.d("Game completed detected from state check");
                    if (timerRunning) {
                        stopTimer();
                        Toast.makeText(requireContext(), 
                            "Target reached in " + state.getMoveCount() + " moves!", 
                            Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        
        gameStateManager.isGameComplete().observe(getViewLifecycleOwner(), isComplete -> {
            if (isComplete != null && isComplete) {
                Timber.d("ModernGameFragment: Game completed from LiveData");
                stopTimer();
                
                Toast.makeText(requireContext(), 
                    "Target reached in " + gameStateManager.getMoveCount().getValue() + " moves and " + 
                    gameStateManager.getSquaresMoved().getValue() + " squares moved!", 
                    Toast.LENGTH_LONG).show();
            }
        });
        
        // Update difficulty text
        updateDifficulty();
        
        // Set up buttons
        setupButtons(view);
        
        // Start a new game if there's no current game state
        if (gameStateManager.getCurrentState().getValue() == null) {
            gameStateManager.startModernGame();
            startTimer();
        }
    }
    
    /**
     * Set up button click listeners
     */
    private void setupButtons(View view) {
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
        
        // Reset robots button - reset robots to starting positions without changing the map
        resetRobotsButton = view.findViewById(R.id.reset_robots_button);
        resetRobotsButton.setOnClickListener(v -> {
            Timber.d("ModernGameFragment: Reset robots button clicked");
            // Reset the robots
            gameStateManager.resetRobots();
            Toast.makeText(requireContext(), "Robots reset to starting positions", Toast.LENGTH_SHORT).show();
        });
        
        // Hint button - get a hint for the next move
        hintButton = view.findViewById(R.id.hint_button);
        hintButton.setOnClickListener(v -> {
            Timber.d("ModernGameFragment: Hint button clicked");
            // Get a hint from the game state manager
            gameStateManager.getHint();
        });
        
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
        
        // Restart button - restart the current game
        restartButton = view.findViewById(R.id.restart_button);
        restartButton.setOnClickListener(v -> {
            Timber.d("ModernGameFragment: Restart button clicked");
            // Start a new game
            gameStateManager.startModernGame();
            resetAndStartTimer();
        });
        
        // Menu button - go back to main menu
        menuButton = view.findViewById(R.id.menu_button);
        menuButton.setOnClickListener(v -> {
            Timber.d("ModernGameFragment: Menu button clicked");
            // Navigate back to main menu
            navigateTo(R.id.actionModernGameToMainMenu);
        });
    }
    
    private void updateGameState(GameState gameState) {
        if (gameState == null) {
            return;
        }
        
        // Update the game grid view with the new game state
        ArrayList<GridElement> gridElements = gameState.getGridElements();
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
    
    @Override
    public void onResume() {
        super.onResume();
        // Resume timer when fragment is resumed if game is in progress and not solved
        if (gameStateManager.getCurrentState().getValue() != null && !gameStateManager.isGameComplete().getValue()) {
            startTimer();
        }
    }
}
