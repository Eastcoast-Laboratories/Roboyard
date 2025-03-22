package roboyard.eclabs.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

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
        
        // Set up the game grid view
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
        gameStateManager.isGameComplete().observe(getViewLifecycleOwner(), this::updateGameComplete);
        
        // Update difficulty text
        updateDifficulty();
        
        // Set up buttons
        setupButtons(view);
        
        // Start a new game if there's no current game state
        if (gameStateManager.getCurrentState().getValue() == null) {
            gameStateManager.startModernGame();
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
        
        // Save map button - save the current map for later use
        saveMapButton = view.findViewById(R.id.save_map_button);
        saveMapButton.setOnClickListener(v -> {
            Timber.d("ModernGameFragment: Save map button clicked");
            // Save the current map
            boolean saved = gameStateManager.saveCurrentMap();
            if (!saved) {
                Toast.makeText(requireContext(), "Error saving map", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Map saved successfully", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Restart button - restart the current game
        restartButton = view.findViewById(R.id.restart_button);
        restartButton.setOnClickListener(v -> {
            Timber.d("ModernGameFragment: Restart button clicked");
            // Start a new game
            gameStateManager.startModernGame();
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
    
    private void updateGameComplete(boolean isComplete) {
        if (isComplete) {
            // Show game complete dialog
            showGameCompleteDialog();
        }
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
     * Show a dialog when the game is complete
     */
    private void showGameCompleteDialog() {
        // Create a dialog to show game completion
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Game Complete!");
        builder.setMessage("You completed the game in " + gameStateManager.getMoveCount().getValue() + 
                " moves, and moved " + gameStateManager.getSquaresMoved().getValue() + " squares total.");
        builder.setPositiveButton("New Game", (dialog, which) -> {
            // Start a new game
            gameStateManager.startModernGame();
        });
        builder.setNegativeButton("Main Menu", (dialog, which) -> {
            // Navigate back to main menu
            navigateTo(R.id.actionModernGameToMainMenu);
        });
        builder.setCancelable(false);
        builder.show();
    }
    
    /**
     * Get the screen title for accessibility and UI
     * @return The screen title
     */
    @Override
    public String getScreenTitle() {
        return "Modern Game";
    }
}
