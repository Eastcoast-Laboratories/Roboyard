package roboyard.eclabs.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import roboyard.eclabs.Constants;
import roboyard.eclabs.R;

/**
 * Main game play screen implementing proper accessibility support.
 * Replaces the GridGameScreen from the canvas-based implementation.
 */
public class GamePlayFragment extends BaseGameFragment {
    
    private GameGridView gameGridView;
    private TextView moveCountText;
    private TextView levelNameText;
    private Button hintButton;
    private Button resetButton;
    private Button saveButton;
    private Button menuButton;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_game_play, container, false);
        
        // Set up UI elements
        gameGridView = view.findViewById(R.id.game_grid_view);
        moveCountText = view.findViewById(R.id.move_count_text);
        levelNameText = view.findViewById(R.id.level_name_text);
        hintButton = view.findViewById(R.id.hint_button);
        resetButton = view.findViewById(R.id.reset_button);
        saveButton = view.findViewById(R.id.save_button);
        menuButton = view.findViewById(R.id.menu_button);
        
        // Set up game grid
        gameGridView.setGameStateManager(gameStateManager);
        
        // Set up button listeners with proper accessibility support
        setupButtons();
        
        // Observe game state changes
        observeGameState();
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // If no game is active, start a new one
        if (gameStateManager.getCurrentState().getValue() == null) {
            gameStateManager.startNewGame();
        }
    }
    
    /**
     * Set up button click listeners
     */
    private void setupButtons() {
        // Hint button - show next move
        hintButton.setOnClickListener(v -> {
            GameMove hint = gameStateManager.getHint();
            if (hint != null) {
                // Highlight the suggested move somehow
                showToast("Move " + hint.getRobotColorName() + " robot to row " + hint.getToY() + ", column " + hint.getToX());
            } else {
                showToast("No hint available");
            }
        });
        
        // Reset button - restart the current game
        resetButton.setOnClickListener(v -> {
            GameState currentState = gameStateManager.getCurrentState().getValue();
            if (currentState != null) {
                int levelId = currentState.getLevelId();
                if (levelId >= 0) {
                    // Reset level game
                    gameStateManager.loadLevel(levelId);
                } else {
                    // Reset random game
                    gameStateManager.startNewGame();
                }
                showToast("Game reset");
            }
        });
        
        // Save button - go to save game screen
        saveButton.setOnClickListener(v -> {
            // Use Navigation component to navigate to save screen in save mode
            NavDirections action = GamePlayFragmentDirections.actionGamePlayToSaveGame(true);
            navigateTo(action);
        });
        
        // Menu button - return to main menu
        menuButton.setOnClickListener(v -> {
            // Use Navigation component to navigate to main menu
            NavDirections action = GamePlayFragmentDirections.actionGamePlayToMainMenu();
            navigateTo(action);
        });
    }
    
    /**
     * Observe changes in the game state
     */
    private void observeGameState() {
        // Observe move count
        gameStateManager.getMoveCount().observe(getViewLifecycleOwner(), moves -> {
            moveCountText.setText("Moves: " + moves);
        });
        
        // Observe current state for level name
        gameStateManager.getCurrentState().observe(getViewLifecycleOwner(), state -> {
            if (state != null) {
                levelNameText.setText(state.getLevelName());
                // Update game grid view
                gameGridView.invalidate();
            }
        });
        
        // Observe game completion
        gameStateManager.isGameComplete().observe(getViewLifecycleOwner(), isComplete -> {
            if (isComplete) {
                GameState state = gameStateManager.getCurrentState().getValue();
                if (state != null) {
                    int moves = gameStateManager.getMoveCount().getValue();
                    showGameCompleteDialog(moves);
                }
            }
        });
    }
    
    /**
     * Show dialog when game is complete
     */
    private void showGameCompleteDialog(int moveCount) {
        // Create AlertDialog for game completion
        new AlertDialog.Builder(requireContext())
            .setTitle("Puzzle Complete!")
            .setMessage("You solved the puzzle in " + moveCount + " moves!")
            .setPositiveButton("Back to Menu", (dialog, which) -> {
                NavDirections action = GamePlayFragmentDirections.actionGamePlayToMainMenu();
                navigateTo(action);
            })
            .setNegativeButton("Play Again", (dialog, which) -> {
                GameState state = gameStateManager.getCurrentState().getValue();
                if (state != null && state.getLevelId() >= 0) {
                    gameStateManager.loadLevel(state.getLevelId());
                } else {
                    gameStateManager.startNewGame();
                }
            })
            .setNeutralButton("Save Game", (dialog, which) -> {
                NavDirections action = GamePlayFragmentDirections.actionGamePlayToSaveGame(true);
                navigateTo(action);
            })
            .setCancelable(false)
            .show();
    }
    
    @Override
    public String getScreenTitle() {
        return "Game";
    }
    
    /**
     * Get map data for minimap generation (used by GameButtonGotoSavedGame)
     * This was specifically mentioned in the memory as important
     */
    public int[][] getMapData() {
        GameState state = gameStateManager.getCurrentState().getValue();
        if (state != null) {
            return state.getMapData();
        }
        return null;
    }
    
    /**
     * Get the map name for history entries (mentioned in memory)
     */
    public String getMapName() {
        GameState state = gameStateManager.getCurrentState().getValue();
        if (state != null) {
            return state.getLevelName();
        }
        return "Unknown";
    }
}
