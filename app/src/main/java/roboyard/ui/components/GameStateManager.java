package roboyard.ui.components;

import android.app.Application;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import roboyard.eclabs.FileReadWrite;
import roboyard.eclabs.GameHistoryManager;
import roboyard.eclabs.MapObjects;
import roboyard.eclabs.R;
import roboyard.eclabs.RoboyardApplication;
import roboyard.eclabs.ui.GameElement;
import roboyard.eclabs.ui.LevelCompletionData;
import roboyard.eclabs.ui.LevelCompletionManager;
import roboyard.eclabs.util.BrailleSpinner;
import roboyard.eclabs.util.SolutionAnimator;
import roboyard.eclabs.util.SolverManager;
import roboyard.logic.core.Constants;
import roboyard.logic.core.GameHistoryEntry;
import roboyard.logic.core.GameLogic;
import roboyard.logic.core.GameState;
import roboyard.logic.core.GridElement;
import roboyard.logic.core.Preferences;
import roboyard.logic.core.WallStorage;
import roboyard.pm.ia.GameSolution;
import roboyard.pm.ia.IGameMove;
import roboyard.ui.animation.RobotAnimationManager;
import timber.log.Timber;

/**
 * Central state manager for the game.
 * Handles game state, navigation, and communication between fragments.
 * This replaces the previous GameManager with a more Android-native approach.
 */
public class GameStateManager extends AndroidViewModel implements SolverManager.SolverListener {


    // Minimum required moves for each difficulty level (as per documentation)
    private static final int MIN_MOVES_BEGINNER = 4;      // 4-6 moves
    private static final int MAX_MOVES_BEGINNER = 6;    // 4-6 moves
    private static final int MIN_MOVES_ADVANCED = 6;    // 6-10 moves
    private static final int MAX_MOVES_ADVANCED = 10;    // 6-10 moves
    private static final int MIN_MOVES_INSANE = 10;     // 10+ moves
    private static final int MIN_MOVES_IMPOSSIBLE = 17; // 17+ moves

    private boolean validateDifficulty = true;
    private int regenerationCount = 0;
    private static final int MAX_AUTO_REGENERATIONS = 999;

    // Game state
    private final MutableLiveData<GameState> currentState = new MutableLiveData<>();
    private final MutableLiveData<Integer> moveCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> squaresMoved = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isGameComplete = new MutableLiveData<>(false);

    // Move history for undo functionality
    private final ArrayList<GameState> stateHistory = new ArrayList<>();
    private final ArrayList<Integer> squaresMovedHistory = new ArrayList<>();

    // Game settings
    private final MutableLiveData<Boolean> soundEnabled = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> isSolverRunning = new MutableLiveData<>(false);

    // Solver
    private SolverManager solver;
    private Context context;

    // Minimap
    private String currentMapName = "";
    private long startTime = 0;
    private Bitmap minimap = null;

    // Solution state
    private GameSolution currentSolution = null;
    private int currentSolutionStep = 0;

    // Track hint usage for level completion statistics
    private int hintsShown = 0;
    // Track unique robots used for level completion statistics
    private final Set<Integer> robotsUsed = new HashSet<>();

    // Robot animation manager
    private RobotAnimationManager robotAnimationManager;

    // Animation settings - made slower for more visible effect (minDuration = 100f,) 
    private boolean animationsEnabled = true;
    private float accelerationDuration = 300f;  // Reduced from 300f for faster animations
    private float maxSpeed = 1500f;             // Higher speed but not extreme
    private float decelerationDuration = 50f;  // Reduced from 400f for faster animations
    private float overshootPercentage = 0.20f;  // Keep original value, extreme values cause memory issues
    private float springBackDuration = 220f;    // Reduced from 400f for faster animations
    private long animationFrameDelay = 25;     // Animation frame delay in ms (default Android is ~16ms = 60fps)

    private boolean isResetting = false;
    private GameGridView gameGridView;

    // Track solver restart count and last solution minimums for UI display
    private int solverRestartCount = 0;
    private int lastSolutionMinMoves = 0;

    // Game history tracking variables
    private long gameStartTime;
    private int totalPlayTime = 0;
    private boolean isHistorySaved = false;
    private static final int HISTORY_SAVE_THRESHOLD = 3; // 3 seconds threshold for saving to history (reduced for testing)

    // Reference to the current activity - will be updated by getActivity() and setActivity() methods
    private WeakReference<Activity> activityRef;

    // Store the difficulty level from a deep link
    private int deepLinkDifficulty = -1;

    public GameStateManager(Application application) {
        super(application);
        // We'll use lazy initialization for solver now - do not create it here

        context = application.getApplicationContext();

        // Initialize robotAnimationManager
        robotAnimationManager = new RobotAnimationManager(this);
    }

    /**
     * Get the solver manager instance, initializing it if necessary
     *
     * @return The solver manager instance
     */
    private SolverManager getSolverManager() {
        Timber.d("[SOLUTION_SOLVER][DIAGNOSTIC] GameStateManager.getSolverManager(): Getting SolverManager singleton instance");
        SolverManager solverManager = SolverManager.getInstance();

        // Set solver listener if not already set
        if (solverManager.getListener() == null) {
            solverManager.setListener(this);
            Timber.d("[SOLUTION_SOLVER][DIAGNOSTIC] GameStateManager registered itself as the SolverListener");
        }
        return solverManager;
    }

    /**
     * Start a new random game
     */
    public void startNewGame() {
        Timber.d("GameStateManager: startNewGame() called");

        startModernGame();
    }

    /**
     * Start a new modern game
     */
    public void startModernGame() {
        Timber.d("GameStateManager: startModernGame() called");

        // Reset any existing solver state to ensure a clean calculation for the new game
        SolverManager solverManager = getSolverManager();
        solverManager.resetInitialization();
        solverManager.cancelSolver(); // Cancel any running solver process

        // Clear any existing solution to prevent it from being reused
        currentSolution = null;
        currentSolutionStep = 0;
        resetSolverRestartCount();
        resetLastSolutionMinMoves();

        // Reset regeneration counter
        regenerationCount = 0;

        // Create a new valid game (will regenerate if solution is too simple)
        createValidGame(Preferences.boardSizeWidth, Preferences.boardSizeHeight);

        // Record start time
        startTime = System.currentTimeMillis();

        Timber.d("GameStateManager: startModernGame() complete");
    }

    /**
     * Start a level game with the modern UI
     *
     * @param levelId Level ID to load
     */
    public void startLevelGame(int levelId) {
        Timber.d("GameStateManager: startLevelGame() called with levelId: %d", levelId);

        // If solver is already running, don't create a new game state to avoid mismatch
        if (Boolean.TRUE.equals(isSolverRunning.getValue())) {
            Timber.d("[SOLUTION_SOLVER] startLevelGame: Solver already running, not creating new game state");
            return;
        }

        // Reset any existing solver state to ensure a clean calculation for the new level
        SolverManager solverManager = getSolverManager();
        solverManager.resetInitialization();
        solverManager.cancelSolver(); // Cancel any running solver process

        // Clear any existing solution to prevent it from being reused
        currentSolution = null;
        currentSolutionStep = 0;

        // Load level from assets
        GameState state = GameState.loadLevel(getApplication(), levelId);
        state.setLevelId(levelId);
        state.setLevelName("Level " + levelId);

        // Set reference to this GameStateManager in the new state
        state.setGameStateManager(this);

        // Set the current state
        currentState.setValue(state);
        currentMapName = "Level-" + levelId;

        // Reset move counts and history
        setMoveCount(0);
        resetSquaresMoved(); // reset squares moved count
        setGameComplete(false);
        stateHistory.clear();
        squaresMovedHistory.clear();

        // Initialize the solver with the grid elements from the loaded level
        ArrayList<GridElement> gridElements = state.getGridElements();
        Timber.d("[SOLUTION_SOLVER] Initializing solver with %d grid elements from level %d",
                gridElements.size(), levelId);
        getSolverManager().initialize(gridElements);

        // Start calculating the solution automatically
        calculateSolutionAsync(null);

        // Record start time
        startTime = System.currentTimeMillis();

        Timber.d("GameStateManager: startLevelGame() complete for level %d", levelId);

        resetStatistics();
    }

    /**
     * Load a specific level
     *
     * @param levelId Level ID to load
     */
    public void loadLevel(int levelId) {
        // Load level from assets
        GameState newState = GameState.loadLevel(getApplication(), levelId);
        newState.setLevelId(levelId);

        // Set reference to this GameStateManager in the new state
        newState.setGameStateManager(this);

        currentState.setValue(newState);
        moveCount.setValue(0);
        isGameComplete.setValue(false);

        // Initialize the solver with grid elements
        ArrayList<GridElement> gridElements = newState.getGridElements();
        getSolverManager().initialize(gridElements);
    }

    /**
     * Load a saved game
     *
     * @param saveId Save slot ID
     */
    public void loadGame(int saveId) {
        if (saveId >= 0) {
            // Load saved game using the original method
            GameState newState = GameState.loadSavedGame(getApplication(), saveId);
            if (newState != null) {
                // Apply the loaded game state using the shared method
                applyLoadedGameState(newState);
                Timber.d("Successfully loaded game from slot %d", saveId);
            } else {
                Timber.e("Failed to load game state from slot %d", saveId);
            }
        }
    }

    /**
     * Apply a loaded GameState to the current game
     * This method contains the common logic used by both saved games and deep links
     *
     * @param newState The new GameState to apply
     * @return true if successful, false otherwise
     */
    public boolean applyLoadedGameState(GameState newState) {
        // Set reference to this GameStateManager in the new state
        newState.setGameStateManager(this);

        // Analyze the loaded data and show all elements loaded in the log
        Timber.d("[GAME_LOAD] Analyzing loaded game data");
        Timber.d("[GAME_LOAD] Board size: %d x %d", newState.getWidth(), newState.getHeight());
        Timber.d("[GAME_LOAD] Map name: %s", newState.getLevelName());

        // Log all game elements
        int robotCount = 0;
        int targetCount = 0;
        int horizontalWallCount = 0;
        int verticalWallCount = 0;
        for (GameElement element : newState.getGameElements()) {
            switch (element.getType()) {
                case GameElement.TYPE_ROBOT:
                    robotCount++;
                    Timber.d("[GAME_LOAD] Found robot at (%d,%d) with color %d",
                            element.getX(), element.getY(), element.getColor());
                    break;
                case GameElement.TYPE_TARGET:
                    targetCount++;
                    Timber.d("[GAME_LOAD] Found target at (%d,%d) with color %d",
                            element.getX(), element.getY(), element.getColor());
                    break;
                case GameElement.TYPE_HORIZONTAL_WALL:
                    horizontalWallCount++;
                    break;
                case GameElement.TYPE_VERTICAL_WALL:
                    verticalWallCount++;
                    break;
            }
        }

        // Log summary of elements
        Timber.d("[GAME_LOAD] Element summary - Robots: %d, Targets: %d, Horizontal walls: %d, Vertical walls: %d",
                robotCount, targetCount, horizontalWallCount, verticalWallCount);

        // Store the map name from the loaded state
        this.currentMapName = newState.getLevelName();
        Timber.d("[MAPNAME] GameStateManager - Set currentMapName to: %s", this.currentMapName);

        // Check for targets in the board data (cellType and targetColors)
        int boardTargetCount = 0;
        for (int y = 0; y < newState.getHeight(); y++) {
            for (int x = 0; x < newState.getWidth(); x++) {
                if (newState.getCellType(x, y) == Constants.TYPE_TARGET) {
                    boardTargetCount++;
                    int targetColor = newState.getTargetColor(x, y);
                    Timber.d("[GAME_LOAD] Found board target at (%d,%d) with color %d", x, y, targetColor);
                }
            }
        }
        Timber.d("[GAME_LOAD] Board target count: %d", boardTargetCount);

        // If there are no targets in gameElements but there are targets in the board data,
        // we need to recreate the GameElements for the targets
        if (targetCount == 0 && boardTargetCount > 0) {
            Timber.w("[GAME_LOAD] No targets found in gameElements but %d targets found in board data. Recreating targets.", boardTargetCount);
            // Recreate target elements based on board data
            for (int y = 0; y < newState.getHeight(); y++) {
                for (int x = 0; x < newState.getWidth(); x++) {
                    if (newState.getCellType(x, y) == Constants.TYPE_TARGET) {
                        int color = newState.getTargetColor(x, y);
                        GameElement target = new GameElement(GameElement.TYPE_TARGET, x, y);
                        target.setColor(color);
                        newState.getGameElements().add(target);
                        Timber.d("[GAME_LOAD] Recreated target element at (%d,%d) with color %d", x, y, color);
                    }
                }
            }
        }

        // If there are no targets at all, throw an exception
        if (targetCount == 0 && boardTargetCount == 0) {
            String errorMessage = "No targets found in loaded game data";
            Throwable t = new Throwable();
            Timber.e(t, "[GAME_LOAD] %s", errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        // Ensure robots are reset to their initial positions
        newState.resetRobotPositions();
        Timber.d("[GAME_LOAD] Robots reset to initial positions after loading");

        // Synchronize targets to ensure board array matches gameElements
        int syncedTargets = newState.synchronizeTargets();
        if (syncedTargets > 0) {
            Timber.d("[GAME_LOAD] Synchronized %d targets after loading", syncedTargets);
        }

        // Update LiveData values
        currentState.setValue(newState);
        moveCount.setValue(newState.getMoveCount());
        isGameComplete.setValue(newState.isComplete());

        // Initialize solver with grid elements
        initializeSolverForState(newState);

        return true;
    }

    /**
     * Initialize the solver with grid elements from a game state
     *
     * @param state The game state to initialize the solver with
     */
    private void initializeSolverForState(GameState state) {
        // Create a GridElements list that properly includes the targets
        ArrayList<GridElement> gridElements = new ArrayList<>();

        // Convert GameElements to GridElements for the solver
        for (GameElement element : state.getGameElements()) {
            GridElement gridElement = null;
            switch (element.getType()) {
                case GameElement.TYPE_ROBOT:
                    String robotType = "robot_" + GameLogic.getColorName(element.getColor(), false);
                    gridElement = new GridElement(element.getX(), element.getY(), robotType);
                    Timber.d("[SOLVER_INIT] Added robot GridElement: %s at (%d,%d)", robotType, element.getX(), element.getY());
                    break;

                case GameElement.TYPE_TARGET:
                    // Get the raw color value from the element
                    int targetColorId = element.getColor();
                    String colorName = GameLogic.getColorName(targetColorId, false);
                    String targetType = "target_" + colorName;
                    
                    // Detailed logging for target conversion
                    Timber.d("[POSSIBLE_UNREACHEABLE_CODE][SOLUTION_SOLVER_TARGET] Converting GameElement target with color ID %d (%s) to GridElement type '%s'",
                            targetColorId, (targetColorId == Constants.COLOR_MULTI ? "MULTI" : colorName), targetType);
                    
                    // Create GridElement with the correct type
                    gridElement = new GridElement(element.getX(), element.getY(), targetType);
                    
                    // More verbose logging for multi-color targets
                    if (targetColorId == Constants.COLOR_MULTI) {
                        Timber.d("[POSSIBLE_UNREACHEABLE_CODE][SOLUTION_SOLVER_TARGET] Created multi-color target GridElement: %s at (%d,%d)", 
                                targetType, element.getX(), element.getY());
                    } else {
                        Timber.d("[POSSIBLE_UNREACHEABLE_CODE][SOLVER_INIT] Added target GridElement: %s at (%d,%d)", targetType, element.getX(), element.getY());
                    }
                    break;

                case GameElement.TYPE_HORIZONTAL_WALL:
                    gridElement = new GridElement(element.getX(), element.getY(), "mh");
                    break;

                case GameElement.TYPE_VERTICAL_WALL:
                    gridElement = new GridElement(element.getX(), element.getY(), "mv");
                    break;
            }

            if (gridElement != null) {
                gridElements.add(gridElement);
            }
        }

        // Initialize solver with our properly constructed grid elements
        getSolverManager().initialize(gridElements);
        Timber.d("[SOLVER_INIT] Initialized solver with %d grid elements including robots and targets", gridElements.size());
    }

    /**
     * Load a history entry
     *
     * @param historyId History entry ID to load
     */
    public void loadHistoryEntry(int historyId) {
        // TODO: Implement history entry loading
        GameState newState = GameState.createRandom();
        newState.setLevelId(historyId);

        // Set reference to this GameStateManager in the new state
        newState.setGameStateManager(this);

        currentState.setValue(newState);
        moveCount.setValue(0);
        isGameComplete.setValue(false);

        // Initialize the solver with grid elements
        ArrayList<GridElement> gridElements = newState.getGridElements();
        getSolverManager().initialize(gridElements);
    }

    /**
     * Load a game from a history entry
     *
     * @param mapPath Path to the history entry file
     */
    public void loadHistoryEntry(String mapPath) {
        Timber.d("Loading history entry: %s", mapPath);

        try {
            // Load game state from file
            File historyFile = new File(mapPath);
            if (!historyFile.exists()) {
                Timber.e("History file does not exist: %s", mapPath);
                return;
            }

            // TODO: Implement proper loading from history file

            Timber.d("Loaded history entry");
        } catch (Exception e) {
            Timber.e(e, "Error loading history entry: %s", mapPath);
        }
    }

    /**
     * Save the current game to a slot
     *
     * @param saveId The save slot ID
     * @return true if the game was saved successfully, false otherwise
     */
    public boolean saveGame(int saveId) {
        Timber.d("Saving game to slot %d", saveId);

        // Get the current game state
        GameState gameState = currentState.getValue();
        if (gameState == null) {
            Timber.e("Cannot save game: No valid GameState available");
            // Try to debug why the current state is null
            Timber.d("Current state: %s", currentState == null ? "null MutableLiveData" : "MutableLiveData exists but value is null");
            Timber.d("Has saved state history: %s", stateHistory.isEmpty() ? "no" : "yes, with " + stateHistory.size() + " entries");
            return false;
        }

        try {
            // Create save directory if it doesn't exist
            File saveDir = new File(getContext().getFilesDir(), Constants.SAVE_DIRECTORY);
            if (!saveDir.exists()) {
                if (!saveDir.mkdirs()) {
                    Timber.e("Failed to create save directory");
                    return false;
                }
            }

            // Create save file
            String fileName = Constants.SAVE_FILENAME_PREFIX + saveId + Constants.SAVE_FILENAME_EXTENSION;
            File saveFile = new File(saveDir, fileName);

            // Serialize game state to JSON
            String saveData = gameState.serialize();

            // Add additional important metadata if not already present
            StringBuilder enhancedSaveData = new StringBuilder(saveData);

            // Check if we need to add a DIFFICULTY tag - find first line end
            if (!saveData.contains("DIFFICULTY:")) {
                int difficultyLevel = Preferences.difficulty; // Get current difficulty setting
                int endOfFirstLine = saveData.indexOf("\n");
                if (endOfFirstLine > 0) {
                    String difficultyTag = "DIFFICULTY:" + difficultyLevel + ";";
                    // Insert right after first semicolon
                    int insertPos = saveData.indexOf(";", 0) + 1;
                    enhancedSaveData.insert(insertPos, difficultyTag);
                    Timber.d("[SAVEDATA] Added difficulty tag: %s", difficultyTag);
                }
            }

            // Add board size if not already present
            if (!saveData.contains("SIZE:")) {
                int width = gameState.getWidth();
                int height = gameState.getHeight();
                int endOfFirstLine = enhancedSaveData.indexOf("\n");
                if (endOfFirstLine > 0) {
                    String sizeTag = "SIZE:" + width + "," + height + ";";
                    // Insert right after first semicolon and any other tags we've added
                    int insertPos = enhancedSaveData.indexOf(";", 0) + 1;
                    enhancedSaveData.insert(insertPos, sizeTag);
                    Timber.d("[SAVEDATA] Added size tag: %s", sizeTag);
                }
            }

            // Add completion status if not already present
            if (!saveData.contains("SOLVED:")) {
                boolean solved = gameState.isComplete();
                String solvedTag = "SOLVED:" + solved + ";";
                int endOfFirstLine = enhancedSaveData.indexOf("\n");
                if (endOfFirstLine > 0) {
                    // Insert right after first semicolon and any other tags we've added
                    int insertPos = enhancedSaveData.indexOf(";", 0) + 1;
                    enhancedSaveData.insert(insertPos, solvedTag);
                    Timber.d("[SAVEDATA] Added solved tag: %s", solvedTag);
                }
            }

            // Write the save data to the file
            try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                fos.write(enhancedSaveData.toString().getBytes());
                Timber.d("Game saved to %s", saveFile.getAbsolutePath());

                // VERIFICATION: Read back the save file and check for targets
                String savedContent = readSaveFileContent(saveFile);
                if (savedContent == null || !validateSaveContainsTargets(savedContent, saveFile)) {
                    // This is a fatal error - delete the corrupt save file
                    Timber.e("[SAVE_VERIFICATION] FATAL ERROR: Save file validation failed - no targets found");
                    Timber.e("[SAVE_VERIFICATION] FATAL: Game state information before throw:");
                    Timber.e("[SAVE_VERIFICATION] Width: %d, Height: %d", gameState.getWidth(), gameState.getHeight());
                    int targetCount = 0;
                    for (int y = 0; y < gameState.getHeight(); y++) {
                        for (int x = 0; x < gameState.getWidth(); x++) {
                            if (gameState.getCellType(x, y) == Constants.TYPE_TARGET) {
                                targetCount++;
                                Timber.e("[SAVE_VERIFICATION] Target found at (%d,%d) with color %d",
                                        x, y, gameState.getTargetColor(x, y));
                            }
                        }
                    }
                    Throwable t = new Throwable();
                    Timber.e(t, "[SAVE_VERIFICATION] Total targets in CURRENT game state: %d", targetCount);
                    Timber.e("[SAVE_VERIFICATION] Save file content before deletion (first 200 chars): %s",
                            savedContent.length() > 200 ? savedContent.substring(0, 200) + "..." : savedContent);
                    saveFile.delete();
                    throw new IllegalStateException("Save file validation failed: No targets found in saved game");
                }

                return true;
            } catch (IOException e) {
                Timber.e("Error saving game: %s", e.getMessage());
                return false;
            }
        } catch (IllegalStateException e) {
            // Log the detailed error for debugging
            Timber.e("[SAVE_ERROR] %s", e.getMessage());
            // Rethrow as this is a fatal error that should never occur
            throw new RuntimeException("FATAL: " + e.getMessage(), e);
        } catch (Exception e) {
            Timber.e("Unexpected error saving game: %s", e.getMessage());
            return false;
        }
    }

    /**
     * Validates that a save file contains at least one target.
     * In Roboyard, all games MUST have targets - this is a fatal requirement.
     *
     * @param saveData The save data string to validate
     * @param saveFile The save file reference (for logging)
     * @return true if the save contains targets, false otherwise
     */
    private boolean validateSaveContainsTargets(String saveData, File saveFile) {
        Timber.d("[SAVE_VERIFICATION] Validating save file: %s", saveFile.getName());

        // Check for dedicated TARGET_SECTION section
        if (saveData.contains("TARGET_SECTION:")) {
            // Look for TARGET_SECTION: entries which must be present
            if (saveData.contains("TARGET_SECTION:")) {
                Timber.d("[SAVE_VERIFICATION] Save file contains TARGET_SECTION section and TARGET_SECTION: entries");
                return true;
            }
        }

        // Check for target cell types in board data
        String[] lines = saveData.split("\n");
        for (String line : lines) {
            if (line.contains(Constants.TYPE_TARGET + ":")) {
                Timber.d("[SAVE_VERIFICATION] Save file contains target cell types in board data");
                return true;
            }
        }

        // Log the full save data for diagnostics when no targets are found
        Timber.e("[SAVE_VERIFICATION] NO TARGETS FOUND IN SAVE DATA:");
        String[] logLines = saveData.split("\n");
        for (int i = 0; i < Math.min(logLines.length, 50); i++) { // Limit to 50 lines
            Timber.e("[SAVE_VERIFICATION] Line %d: %s", i, logLines[i]);
        }

        return false;
    }

    /**
     * Reads the content of a save file.
     *
     * @param saveFile The save file to read
     * @return The content of the save file as a string, or null if reading fails
     */
    private String readSaveFileContent(File saveFile) {
        try (FileInputStream fis = new FileInputStream(saveFile);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            return content.toString();
        } catch (IOException e) {
            Timber.e("Error reading save file: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Load a saved game from a specific slot
     *
     * @param context The context
     * @param slotId  The save slot ID
     * @return True if successful, false otherwise
     */
    public boolean loadSavedGame(Context context, int slotId) {
        try {
            // Get save file path
            String savePath = FileReadWrite.getSaveGamePath((Activity) context, slotId);
            String saveData = FileReadWrite.loadAbsoluteData(savePath);

            if (saveData != null && !saveData.isEmpty()) {
                // Extract metadata
                Map<String, String> metadata = extractMetadataFromSaveData(saveData);

                // Store metadata for access by other methods
                if (metadata != null) {
                    if (metadata.containsKey("MAPNAME")) {
                        this.currentMapName = metadata.get("MAPNAME");
                    }

                    if (metadata.containsKey("TIME")) {
                        try {
                            this.startTime = Long.parseLong(metadata.get("TIME"));
                        } catch (NumberFormatException e) {
                            Timber.e("Invalid time format: %s", metadata.get("TIME"));
                        }
                    }
                }

                // Create minimap for this save
                try {
                    this.minimap = createMinimapFromSaveData(context, saveData);
                } catch (Exception e) {
                    Timber.e(e, "Error creating minimap for slot %d", slotId);
                }

                return true;
            }

            return false;
        } catch (Exception e) {
            Timber.e(e, "Error loading saved game from slot %d", slotId);
            return false;
        }
    }

    /**
     * Extract metadata from save data
     *
     * @param saveData The save data string
     * @return A map containing the metadata or null if no metadata was found
     */
    public static Map<String, String> extractMetadataFromSaveData(String saveData) {
        Map<String, String> metadata = new HashMap<>();

        // Check if the save data has a metadata line
        if (saveData != null && saveData.startsWith("#")) {
            // Extract the first line
            int endOfFirstLine = saveData.indexOf('\n');
            if (endOfFirstLine > 0) {
                String metadataLine = saveData.substring(1, endOfFirstLine);

                // Parse metadata entries (MAPNAME:name;TIME:seconds;MOVES:count;)
                String[] entries = metadataLine.split(";");
                for (String entry : entries) {
                    String[] keyValue = entry.split(":");
                    if (keyValue.length == 2) {
                        metadata.put(keyValue[0], keyValue[1]);
                    }
                }
            }
        }

        return metadata;
    }

    /**
     * Handle a touch on the game grid
     *
     * @param x      Grid X coordinate
     * @param y      Grid Y coordinate
     * @param action Touch action type
     * @return
     */
    public boolean handleGridTouch(int x, int y, int action) {
        Timber.d("[TOUCH] Handle grid touch at (%d,%d) with action %d", x, y, action);

        GameState state = getCurrentState().getValue();
        if (state != null) {
            // Get the currently selected robot
            GameElement selectedRobot = state.getSelectedRobot();

            if (action == MotionEvent.ACTION_UP) {
                // Check if the user tapped on a robot - if so, select it
                GameElement touchedRobot = state.getRobotAt(x, y);

                if (touchedRobot != null) {
                    // User tapped on a robot - select it
                    Timber.d("[TOUCH] Selecting robot at (%d,%d)", x, y);
                    state.setSelectedRobot(touchedRobot);

                    // Add used robot to the set
                    if (touchedRobot.getColor() >= 0) {
                        robotsUsed.add(touchedRobot.getColor());
                    }

                    currentState.setValue(state);
                } else if (selectedRobot != null) {
                    // User tapped on an empty space - try to move the selected robot
                    int robotX = selectedRobot.getX();
                    int robotY = selectedRobot.getY();

                    // Determine movement direction
                    int dx = 0;
                    int dy = 0;

                    if (robotX == x) {
                        // Moving vertically
                        dy = y > robotY ? 1 : -1;
                    } else if (robotY == y) {
                        // Moving horizontally
                        dx = x > robotX ? 1 : -1;
                    } else {
                        // Diagonal tap - determine primary direction
                        int deltaX = x - robotX;
                        int deltaY = y - robotY;

                        if (Math.abs(deltaX) > Math.abs(deltaY)) {
                            // Horizontal movement takes priority
                            dx = deltaX > 0 ? 1 : -1;
                            dy = 0;
                        } else {
                            // Vertical movement takes priority
                            dx = 0;
                            dy = deltaY > 0 ? 1 : -1;
                        }
                    }

                    // Move the robot using the animation system
                    if (dx != 0 || dy != 0) {
                        Timber.d("[TOUCH] Moving robot in direction dx=%d, dy=%d", dx, dy);
                        moveRobotInDirection(dx, dy);
                    }
                }
            }
        }
        return true;
    }

    /**
     * Move the selected robot in a specific direction until it hits an obstacle
     * This method can be called by both touch interactions and accessibility controls
     *
     * @param dx Horizontal direction (-1 = left, 0 = no movement, 1 = right)
     * @param dy Vertical direction (-1 = up, 0 = no movement, 1 = down)
     * @return True if the robot moved, false otherwise
     */
    public boolean moveRobotInDirection(int dx, int dy) {
        GameState state = getCurrentState().getValue();
        if (state == null || state.getSelectedRobot() == null) {
            return false;
        }

        // If game is resetting, ignore movement commands
        if (isResetting) {
            return false;
        }

        GameElement robot = state.getSelectedRobot();
        int startX = robot.getX();
        int startY = robot.getY();

        // Initialize end position to the current position (in case no movement is possible)
        int endX = startX;
        int endY = startY;

        // Update the robot's direction if moving horizontally
        if (dx != 0) {
            robot.setDirectionX(dx); // Set facing direction
        }

        // Flags to determine which sound to play
        boolean hitWall = false;
        boolean hitRobot = false;

        // Before making a move, push the current state to history for undo functionality
        // We don't need a deep copy - we'll save the state before making any changes
        Timber.d("[ROBOTS] Saving current state to history before move. History size before: %d", stateHistory.size());

        // Create a snapshot of the current state with all elements
        GameState stateBeforeMove = new GameState(state.getWidth(), state.getHeight());

        // Copy the board data (walls, targets, etc.)
        for (int y = 0; y < state.getHeight(); y++) {
            for (int x = 0; x < state.getWidth(); x++) {
                int cellType = state.getCellType(x, y);
                stateBeforeMove.setCellType(x, y, cellType);

                // Also copy target colors if this is a target
                if (cellType == Constants.TYPE_TARGET) {
                    int targetColor = state.getTargetColor(x, y);
                    stateBeforeMove.setTargetColor(x, y, targetColor);
                }
            }
        }

        // Copy all game elements including robots, walls, and targets
        List<GameElement> elements = state.getGameElements();
        if (elements != null) {
            for (GameElement element : elements) {
                // Add the element based on its type
                if (element.getType() == GameElement.TYPE_ROBOT) {
                    stateBeforeMove.addRobot(element.getX(), element.getY(), element.getColor());

                    // Find the newly added robot and set its direction
                    for (GameElement newElement : stateBeforeMove.getGameElements()) {
                        if (newElement.getType() == GameElement.TYPE_ROBOT &&
                                newElement.getColor() == element.getColor() &&
                                newElement.getX() == element.getX() &&
                                newElement.getY() == element.getY()) {
                            newElement.setDirectionX(element.getDirectionX());
                            break;
                        }
                    }
                } else if (element.getType() == GameElement.TYPE_HORIZONTAL_WALL) {
                    stateBeforeMove.addHorizontalWall(element.getX(), element.getY());
                } else if (element.getType() == GameElement.TYPE_VERTICAL_WALL) {
                    stateBeforeMove.addVerticalWall(element.getX(), element.getY());
                } else if (element.getType() == GameElement.TYPE_TARGET) {
                    stateBeforeMove.addTarget(element.getX(), element.getY(), element.getColor());
                }
            }
        }

        // Copy other game state variables that have setters
        stateBeforeMove.setLevelId(state.getLevelId());
        stateBeforeMove.setLevelName(state.getLevelName());
        stateBeforeMove.setMoveCount(state.getMoveCount());
        stateBeforeMove.setCompleted(state.isComplete());

        // Copy the hint count
        for (int i = 0; i < state.getHintCount(); i++) {
            stateBeforeMove.incrementHintCount();
        }

        stateBeforeMove.setUniqueMapId(state.getUniqueMapId());

        // Store initial robot positions for robot reset functionality
        if (state.initialRobotPositions != null) {
            stateBeforeMove.initialRobotPositions = new HashMap<>();
            for (Map.Entry<Integer, int[]> entry : state.initialRobotPositions.entrySet()) {
                int[] positionCopy = new int[]{entry.getValue()[0], entry.getValue()[1]};
                stateBeforeMove.initialRobotPositions.put(entry.getKey(), positionCopy);
            }
        }

        // Save the complete state for undo
        stateHistory.add(stateBeforeMove);
        squaresMovedHistory.add(getSquaresMoved().getValue());
        Timber.d("[ROBOTS] Saved complete state to history. History size now: %d", stateHistory.size());

        // Check for movement in X direction
        if (dx != 0) {
            int step = dx > 0 ? 1 : -1;
            for (int i = startX + step; i >= 0 && i < state.getWidth(); i += step) {
                if (state.canRobotMoveTo(robot, i, startY)) {
                    endX = i;
                } else {
                    // Found an obstacle
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
                    // Found an obstacle
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
        int distanceMoved = Math.abs(endX - startX) + Math.abs(endY - startY);

        if (distanceMoved > 0) {
            // Create a copy of the current position
            final int originalX = startX;
            final int originalY = startY;
            final int targetX = endX;
            final int targetY = endY;

            // Log the movement initiation
            Timber.d("[ROBOT][HINT_SYSTEM] Movement INITIATED: Robot %d moving from (%d,%d) to (%d,%d)",
                    robot.getColor(), originalX, originalY, targetX, targetY);

            setSquaresMoved(getSquaresMoved().getValue() + distanceMoved);

            // Increment move count
            // The value in this GameStateManager (UI)
            setMoveCount(getMoveCount().getValue() + 1);

            // Also update the move count in the GameState object itself (logic)
            // This ensures state.getMoveCount() returns the correct value
            state.setMoveCount(getMoveCount().getValue());

            // Track the robot and direction for hint verification
            int directionConstant = 0;
            if (dx > 0) directionConstant = 2; // RIGHT
            else if (dx < 0) directionConstant = 8; // LEFT 
            else if (dy < 0) directionConstant = 1; // UP
            else if (dy > 0) directionConstant = 4; // DOWN

            // Store the last moved robot and direction in the GameState
            state.setLastMovedRobot(robot);
            state.setLastMoveDirection(directionConstant);

            Timber.d("[HINT_SYSTEM] Robot moved: color=%d, direction=%d", robot.getColor(), directionConstant);
            Timber.d("[HINT_SYSTEM] Updated moveCount in GameState to %d", state.getMoveCount());

            // Create completion callback for when animation finishes
            Runnable completionCallback = () -> {
                // Update the robot's actual position after animation completes
                robot.setX(targetX);
                robot.setY(targetY);

                // Check for game completion after animation
                if (state.areAllRobotsAtTargets()) {
                    setGameComplete(true);
                }

                // Notify observers that the state has changed
                LiveData<GameState> currentStateLiveData = getCurrentState();
                if (currentStateLiveData instanceof MutableLiveData) {
                    ((MutableLiveData<GameState>) currentStateLiveData).setValue(state);
                }
            };

            // Queue this movement for animation
            if (animationsEnabled && robotAnimationManager != null) {
                Timber.d("[ANIM] Attempting to queue robot animation with manager=%s", robotAnimationManager != null ? "active" : "null");

                // Make absolutely sure the GameGridView is connected
                if (gameGridView != null && robotAnimationManager.getGameGridView() == null) {
                    Timber.d("[ANIM] Fixing GameGridView connection to animation manager");
                    robotAnimationManager.setGameGridView(gameGridView);
                }

                // Capture current position for path tracking
                final int oldX = robot.getX();
                final int oldY = robot.getY();

                // Create enhanced completion callback that tracks paths
                Runnable enhancedCallback = () -> {
                    // First run the original completion callback
                    completionCallback.run();

                    // Then update the path tracking in GameGridView
                    if (gameGridView != null) {
                        gameGridView.handleRobotMovementEffects(state, robot, oldX, oldY);
                    }

                    // Add this robot to the used set
                    if (robot.getColor() >= 0) {
                        robotsUsed.add(robot.getColor());
                    }
                };

                // Queue the animation with the enhanced callback
                robotAnimationManager.queueRobotMove(robot, originalX, originalY, targetX, targetY, enhancedCallback);
            } else {
                // Immediate mode - update position without animation
                Timber.d("[ANIM] Animations disabled or manager null (enabled=%b, manager=%s), moving robot immediately",
                        animationsEnabled, robotAnimationManager != null ? "exists" : "null");
                robot.setX(targetX);
                robot.setY(targetY);

                // Check for game completion - use areAllRobotsAtTargets to properly check win conditions
                if (state.areAllRobotsAtTargets()) {
                    setGameComplete(true);
                }

                // Notify observers
                LiveData<GameState> currentStateLiveData = getCurrentState();
                if (currentStateLiveData instanceof MutableLiveData) {
                    ((MutableLiveData<GameState>) currentStateLiveData).setValue(state);
                }
            }
            return true;
        }

        return false;
    }

    /**
     * Handle deep link to a specific level
     *
     * @param levelId Level ID to load
     */
    public void handleDeepLink(int levelId) {
        loadLevel(levelId);
    }

    /**
     * Get a hint for the next move
     *
     * @return The next move according to the solver, or null if no solution exists
     */
    public IGameMove getHint() {
        if (currentSolution != null && currentSolution.getMoves() != null &&
                currentSolutionStep < currentSolution.getMoves().size()) {

            // Increment hint counter for level completion statistics
            hintsShown++;

            IGameMove move = currentSolution.getMoves().get(currentSolutionStep);
            incrementSolutionStep();
            return move;
        }
        return null;
    }

    /**
     * Check if a valid solution is available
     *
     * @return true if a solution is available, false otherwise
     */
    public boolean hasSolution() {
        return currentSolution != null && currentSolution.getMoves() != null &&
                currentSolutionStep < currentSolution.getMoves().size();
    }

    /**
     * Navigate to specific level screen (beginner, intermediate, advanced, expert)
     *
     * @param screenId Screen ID from Constants (SCREEN_LEVEL_BEGINNER, etc)
     */
    public void navigateToLevelScreen(int screenId) {
        // Store the selected level screen for the GamePlayFragment to use
        MutableLiveData<Integer> levelScreen = new MutableLiveData<>(screenId);

        // The GamePlayFragment will check this value and load the appropriate level screen
        // This corresponds to the Constants.SCREEN_LEVEL_BEGINNER etc. values
        switch (screenId) {
            case Constants.SCREEN_LEVEL_BEGINNER:
                // Load first beginner level (level 1)
                loadLevel(1);
                break;
            case Constants.SCREEN_LEVEL_INTERMEDIATE:
                // Load first intermediate level (level 36)
                loadLevel(36);
                break;
            case Constants.SCREEN_LEVEL_ADVANCED:
                // Load first advanced level (level 71)
                loadLevel(71);
                break;
            case Constants.SCREEN_LEVEL_EXPERT:
                // Load first expert level (level 106)
                loadLevel(106);
                break;
            default:
                // Default to beginner level 1
                loadLevel(1);
                break;
        }
    }

    /**
     * Navigate to main menu
     */
    public void navigateToMainMenu() {
        Timber.d("GameStateManager: navigateToMainMenu() called");
        // Use the NavController to navigate to the main menu fragment
        NavController navController = Navigation.findNavController(
                (androidx.fragment.app.FragmentActivity) context, R.id.nav_host_fragment);
        navController.navigate(R.id.actionGlobalMainMenu);
    }

    /**
     * Navigate to settings screen
     */
    public void navigateToSettings() {
        Timber.d("GameStateManager: navigateToSettings() called");
        // Use the NavController to navigate to the settings fragment
        NavController navController = Navigation.findNavController(
                (androidx.fragment.app.FragmentActivity) context, R.id.nav_host_fragment);
        navController.navigate(R.id.actionGlobalSettings);
    }

    /**
     * Navigate to save screen
     *
     * @param saveMode True for save mode, false for load mode
     */
    public void navigateToSaveScreen(boolean saveMode) {
        Timber.d("Navigating to save screen, saveMode=%s", saveMode);
        // Check if we have a valid state before navigation
        GameState currentGameState = currentState.getValue();
        Timber.d("Current state before navigation: %s", currentGameState != null ? "valid" : "null");

        if (currentGameState == null) {
            Timber.e("Cannot navigate to save screen: No valid GameState available");
            return;
        }

        // We should only proceed if the context is a FragmentActivity
        if (!(context instanceof androidx.fragment.app.FragmentActivity activity)) {
            Timber.e("Cannot navigate to save screen: context is not a FragmentActivity");
            return;
        }

        // Get the current activity context

        // Check if we're on the main thread
        if (android.os.Looper.getMainLooper().getThread() == Thread.currentThread()) {
            // We're on the main thread, safe to navigate
            performNavigation(activity, saveMode);
        } else {
            // We're on a background thread, post navigation to main thread
            Timber.d("GameStateManager: Posting navigation to main thread");
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                Timber.d("GameStateManager: Now on main thread, proceeding with navigation");
                performNavigation(activity, saveMode);
            });
        }
    }

    /**
     * Perform the actual navigation on the main thread
     */
    private void performNavigation(androidx.fragment.app.FragmentActivity activity, boolean saveMode) {
        try {
            // Create a bundle to pass the save mode parameter
            android.os.Bundle args = new android.os.Bundle();
            args.putBoolean("saveMode", saveMode);

            // Find the NavController for the current fragment
            androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(
                    activity, R.id.nav_host_fragment);

            // Get the current destination ID
            int currentDestId = navController.getCurrentDestination().getId();
            Timber.d("Current destination ID: %d", currentDestId);

            // Choose the appropriate action based on the current fragment
            if (currentDestId == R.id.gameCanvasFragment) {
                Timber.d("Navigating from GameCanvasFragment to SaveGame");
                navController.navigate(R.id.actionGameToSaveGame, args);
            } else if (currentDestId == R.id.gamePlayFragment) {
                Timber.d("Navigating from GamePlayFragment to SaveGame");
                navController.navigate(R.id.actionGamePlayToSaveGame, args);
            } else {
                // For other fragments, use the global action
                Timber.d("Using global action to navigate to SaveGame");
                navController.navigate(R.id.actionGlobalSaveGame, args);
            }
        } catch (Exception e) {
            // Log any navigation errors
            Timber.e(e, "Navigation error");
        }
    }

    /**
     * Update the context to a valid activity context for fragment navigation
     *
     * @param activityContext The activity context
     */
    public void updateContext(Context activityContext) {
        if (activityContext instanceof androidx.fragment.app.FragmentActivity) {
            Timber.d("GameStateManager: Updating context to activity: %s", activityContext.getClass().getSimpleName());
            this.context = activityContext;
        }
    }

    /**
     * Get the context from application
     *
     * @return Application context
     */
    private Context getContext() {
        return getApplication().getApplicationContext();
    }

    /**
     * Getters for LiveData to observe
     */
    public LiveData<GameState> getCurrentState() {
        return currentState;
    }

    public LiveData<Integer> getMoveCount() {
        return moveCount;
    }

    public LiveData<Integer> getSquaresMoved() {
        return squaresMoved;
    }

    public LiveData<Boolean> isGameComplete() {
        return isGameComplete;
    }

    public LiveData<Boolean> getSoundEnabled() {
        return soundEnabled;
    }

    public LiveData<Boolean> isSolverRunning() {
        return isSolverRunning;
    }

    /**
     * Setters for game settings
     */
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled.setValue(enabled);
        // Also update the static Preferences value to ensure consistency
        roboyard.logic.core.Preferences.setSoundEnabled(enabled);
    }

    /**
     * Increment the move count
     */
    public void incrementMoveCount() {
        Integer currentCount = moveCount.getValue();
        if (currentCount != null) {
            moveCount.setValue(currentCount + 1);
        }
    }

    /**
     * Undo the last move if possible
     *
     * @return true if a move was undone, false otherwise
     */
    public boolean undoLastMove() {
        // Check if there's anything to undo
        if (stateHistory.isEmpty()) {
            Timber.d("[ROBOTS] undoLastMove: No history to undo, stateHistory is empty");
            return false;
        }

        // Get the previous state and restore it
        GameState previousState = stateHistory.remove(stateHistory.size() - 1);
        Timber.d("[ROBOTS] undoLastMove: Removed previous state from history, remaining history size: %d", stateHistory.size());

        if (previousState != null) {
            // Also restore the squares moved count
            if (!squaresMovedHistory.isEmpty()) {
                int previousSquaresMoved = squaresMovedHistory.remove(squaresMovedHistory.size() - 1);
                squaresMoved.setValue(previousSquaresMoved);
                Timber.d("[ROBOTS] undoLastMove: Restored squares moved to: %d", previousSquaresMoved);
            } else {
                Timber.d("[ROBOTS] undoLastMove: No squares moved history to restore");
            }

            // Restore the state
            currentState.setValue(previousState);
            Timber.d("[ROBOTS] undoLastMove: Restored previous game state");

            // Decrement move count
            int moves = moveCount.getValue();
            moveCount.setValue(Math.max(0, moves - 1));
            Timber.d("[ROBOTS][HINT_SYSTEM] undoLastMove: Decremented move count to: %d", Math.max(0, moves - 1));

            // Reset game complete flag if it was set
            if (isGameComplete.getValue()) {
                isGameComplete.setValue(false);
                Timber.d("[ROBOTS] undoLastMove: Reset game complete flag");
            }

            return true;
        } else {
            Timber.e("[ROBOTS] undoLastMove: Previous state was null, this should not happen");
        }

        return false;
    }

    /**
     * Set the move count
     *
     * @param count New move count
     */
    public void setMoveCount(int count) {
        moveCount.setValue(count);
    }

    /**
     * Add squares moved to the counter
     *
     * @param squares Number of squares moved
     */
    public void addSquaresMoved(int squares) {
        Integer current = squaresMoved.getValue();
        if (current != null) {
            squaresMoved.setValue(current + squares);
        }
    }

    /**
     * Reset squares moved counter
     */
    public void resetSquaresMoved() {
        squaresMoved.setValue(0);
        squaresMovedHistory.clear();
    }

    /**
     * Set the squares moved count
     *
     * @param squares Number of squares moved
     */
    public void setSquaresMoved(int squares) {
        squaresMoved.setValue(squares);
    }

    /**
     * Set whether the game is complete
     *
     * @param complete Whether the game is complete
     */
    public void setGameComplete(boolean complete) {
        GameState state = currentState.getValue();
        if (state != null) {
            Timber.d("Setting game complete: %s for level %d", complete, state.getLevelId());
            state.setCompleted(complete);
            isGameComplete.setValue(complete);

            // Save level completion data if this is a level game
            if (complete && state.getLevelId() > 0) {
                Timber.d("[SAVE] [STARS] Game completed, saving level completion data for level %d", state.getLevelId());
                LevelCompletionData data = saveLevelCompletionData(state);

                // Now save the prepared data
                if (data != null) {
                    LevelCompletionManager manager = LevelCompletionManager.getInstance(context);
                    manager.saveLevelCompletionData(data);
                    Timber.d("Saved level completion data: %s", data);
                }

                // Show a toast to indicate the level was completed
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context, "Level " + state.getLevelId() + " completed!", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    /**
     * Save level completion data when a level is completed
     *
     * @param state The completed game state
     * @return The prepared LevelCompletionData object (does not save it)
     */
    private LevelCompletionData saveLevelCompletionData(GameState state) {
        int levelId = state.getLevelId();
        if (levelId <= 0) {
            Timber.d("Not saving completion data - not a level game (levelId=%d)", levelId);
            return null; // Not a level game
        }

        Timber.d("Preparing completion data for level %d", levelId);

        // Get the level completion manager
        LevelCompletionManager manager = LevelCompletionManager.getInstance(context);

        // Get or create completion data for this level
        LevelCompletionData data = manager.getLevelCompletionData(levelId);

        // Update completion data
        data.setCompleted(true);
        data.setHintsShown(hintsShown);
        data.setTimeNeeded(System.currentTimeMillis() - startTime);
        data.setMovesNeeded(moveCount.getValue() != null ? moveCount.getValue() : 0);
        data.setRobotsUsed(robotsUsed.size());
        data.setSquaresSurpassed(squaresMoved.getValue() != null ? squaresMoved.getValue() : 0);

        // Set optimal moves if we have a solution
        int optimalMoves = 0;
        if (currentSolution != null && currentSolution.getMoves() != null) {
            optimalMoves = currentSolution.getMoves().size();
            data.setOptimalMoves(optimalMoves);
        }

        // Calculate stars based on the criteria
        int playerMoves = moveCount.getValue() != null ? moveCount.getValue() : 0;
        int starCount = calculateStars(playerMoves, optimalMoves, hintsShown);
        data.setStars(starCount);

        Timber.d("[STARS] gameStateManager: Level %d completed with %d moves (optimal: %d), %d hints, earned %d stars",
                levelId, playerMoves, optimalMoves, hintsShown, starCount);

        // Return the prepared data without saving it
        Timber.d("Prepared level completion data: %s", data);
        return data;
    }

    /**
     * Calculate star rating based on player performance
     * <p>
     * Star allocation rules:
     * - 4 stars: Hyper-optimal solution (better than solver's optimal solution)
     * - 3 stars: Optimal solution (same as solver) with no hints
     * - 2 stars: One move more than optimal with no hints, OR optimal with one hint
     * - 1 star: Optimal solution with two hints, OR two moves more than optimal with no hints
     * - 0 stars: All other cases
     *
     * @param playerMoves  Number of moves used by player
     * @param optimalMoves Optimal number of moves from solver
     * @param hintsUsed    Number of hints used
     * @return Number of stars earned (0-4)
     */
    public int calculateStars(int playerMoves, int optimalMoves, int hintsUsed) {
        if (optimalMoves <= 0) {
            Timber.d("[stars] No optimal solution available");
            return 0; // No optimal solution available
        }

        // Calculate stars based on the rules
        if (playerMoves < optimalMoves) {
            // hyper-Optimal solution (better than solver's solution)
            Timber.d("[stars] hyper-optimal solution! 4 stars");
            return 4;
        } else if (playerMoves == optimalMoves && hintsUsed == 0) {
            // Optimal solution (or better) and no hints
            Timber.d("[stars] optimal solution! 3 stars");
            return 3;
        } else if ((playerMoves == optimalMoves + 1 && hintsUsed == 0) ||
                (playerMoves == optimalMoves && hintsUsed == 1)) {
            // One move more than optimal with no hints OR optimal with one hint
            return 2;
        } else if ((playerMoves == optimalMoves && hintsUsed == 2) ||
                (playerMoves == optimalMoves + 2 && hintsUsed == 0)) {
            // Optimal with two hints OR two moves more than optimal with no hints
            return 1;
        } else {
            // All other cases
            return 0;
        }
    }

    /**
     * Get the total number of stars earned across all levels
     *
     * @return Total number of stars
     */
    public int getTotalStars() {
        LevelCompletionManager manager = LevelCompletionManager.getInstance(context);
        return manager.getTotalStars();
    }

    /**
     * Get the level name
     *
     * @return The current level name
     */
    public String getLevelName() {
        return currentMapName;
    }

    /**
     * Get the start time
     *
     * @return The start time as a long value
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Create a minimap from save data
     *
     * @param context  The context
     * @param saveData The save data
     * @return The minimap bitmap
     */
    private Bitmap createMinimapFromSaveData(Context context, String saveData) {
        // Skip metadata line if present
        if (saveData.startsWith("#")) {
            int newlineIndex = saveData.indexOf('\n');
            if (newlineIndex >= 0) {
                saveData = saveData.substring(newlineIndex + 1);
            }
        }

        // Extract grid elements
        List<GridElement> elements = MapObjects.extractDataFromString(saveData, true);

        // Create a simple minimap
        return createMinimap(context, elements, 100, 100);
    }

    /**
     * Get a minimap for the current game state
     *
     * @param context The context
     * @param width   The minimap width
     * @param height  The minimap height
     * @return The minimap bitmap
     */
    public Bitmap getMiniMap(Context context, int width, int height) {
        if (minimap != null) {
            return minimap;
        }

        // If no minimap is available, create a placeholder
        Bitmap placeholder = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(placeholder);
        canvas.drawColor(Color.LTGRAY);
        return placeholder;
    }

    /**
     * Create a minimap from grid elements
     *
     * @param context  The context
     * @param elements The grid elements
     * @param width    The minimap width
     * @param height   The minimap height
     * @return The minimap bitmap
     */
    private Bitmap createMinimap(Context context, List<GridElement> elements, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        // If no elements, return empty bitmap
        if (elements == null || elements.isEmpty()) {
            return bitmap;
        }

        // Determine grid bounds
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = 0, maxY = 0;

        for (GridElement element : elements) {
            int x = element.getX();
            int y = element.getY();
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        // Calculate scaling
        float gridWidth = maxX - minX + 2; // Add padding
        float gridHeight = maxY - minY + 2;
        float scaleX = width / gridWidth;
        float scaleY = height / gridHeight;
        float scale = Math.min(scaleX, scaleY);

        // Draw elements
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        for (GridElement element : elements) {
            int x = element.getX() - minX + 1; // Add padding
            int y = element.getY() - minY + 1;
            float left = x * scale;
            float top = y * scale;
            float right = (x + 1) * scale;
            float bottom = (y + 1) * scale;

            if (element.getType().startsWith("robot_")) {
                // Draw robots
                paint.setColor(Color.RED);
                canvas.drawCircle((left + right) / 2, (top + bottom) / 2, scale / 2, paint);
            } else if (element.getType().startsWith("target_")) {
                // Draw targets
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2);
                paint.setColor(Color.RED);
                canvas.drawRect(left, top, right, bottom, paint);
                paint.setStyle(Paint.Style.FILL);
            } else if (element.getType().equals("mv")) {
                // Draw walls
                paint.setColor(Color.DKGRAY);
                canvas.drawRect(left + scale / 3, top, right - scale / 3, bottom, paint);
            } else if (element.getType().equals("mh")) {
                // Draw walls
                paint.setColor(Color.DKGRAY);
                canvas.drawRect(left, top + scale / 3, right, bottom - scale / 3, paint);
            }
        }

        return bitmap;
    }

    /**
     * Get the current difficulty level from Preferences
     *
     * @return Current difficulty level
     */
    public int getDifficulty() {
        return Preferences.difficulty;
    }

    /**
     * Get a string representation of the current difficulty level
     *
     * @return String representation of the current difficulty level
     */
    public String getLocalizedDifficultyString() {
        // Verwende den bereits lokalisierten Anwendungskontext
        Context localizedContext = RoboyardApplication.getAppContext();

        Timber.d("[DIFFICULTY] getLocalizedDifficultyString() called, using difficulty level %d", getDifficulty());

        switch (getDifficulty()) {
            case Constants.DIFFICULTY_BEGINNER:
                return localizedContext.getString(R.string.difficulty_beginner);
            case Constants.DIFFICULTY_ADVANCED:
                return localizedContext.getString(R.string.difficulty_advanced);
            case Constants.DIFFICULTY_INSANE:
                return localizedContext.getString(R.string.difficulty_insane);
            case Constants.DIFFICULTY_IMPOSSIBLE:
                return localizedContext.getString(R.string.difficulty_impossible);
            default:
                return localizedContext.getString(R.string.difficulty_unknown);
        }
    }

    /**
     * Set the difficulty level in Preferences
     *
     * @param difficulty New difficulty level
     */
    public void setDifficulty(int difficulty) {
        Preferences.difficulty = difficulty;
    }

    /**
     * Start the game timer for history tracking
     */
    public void startGameTimer() {
        gameStartTime = System.currentTimeMillis();
        isHistorySaved = false;
        Timber.d("[HISTORY] Game timer started");
    }

    /**
     * Update the game timer and check if game should be saved to history
     */
    public void updateGameTimer() {
        if (!isHistorySaved && currentState.getValue() != null) {
            int elapsedSeconds = (int) ((System.currentTimeMillis() - gameStartTime) / 1000);
            totalPlayTime = elapsedSeconds;

            // Save to history after threshold time of play
            if (totalPlayTime >= HISTORY_SAVE_THRESHOLD) {
                Timber.d("[HISTORY] Threshold reached (%d seconds), saving game to history", HISTORY_SAVE_THRESHOLD);
                saveToHistory();
                isHistorySaved = true;
            }
        }
    }

    /**
     * Save the current game state to history
     */
    private void saveToHistory() {
        try {
            GameState gameState = currentState.getValue();
            if (gameState == null) {
                Timber.e("[HISTORY] Cannot save to history: no current game state");
                return;
            }

            // Get activity from weak reference to avoid memory leaks
            Activity activity = getActivity();
            if (activity == null) {
                Timber.e("[HISTORY] Cannot save to history: no activity");
                return;
            }

            // Initialize GameHistoryManager if needed
            GameHistoryManager.initialize(activity);

            // Get next available history index
            int historyIndex = GameHistoryManager.getNextHistoryIndex(activity);
            String historyFileName = "history_" + historyIndex + ".txt";
            String historyPath = "history/" + historyFileName;

            // Get a proper map name directly from the game state
            String mapName = null;

            // Get level name from the game state
            if (gameState != null) {
                mapName = gameState.getLevelName();
                Timber.d("[HISTORY] Retrieved level name from game state: %s", mapName);

                // If level name is not set properly, use level ID
                if (mapName == null || mapName.isEmpty() || "XXXXX".equals(mapName)) {
                    int levelId = gameState.getLevelId();
                    if (levelId > 0) {
                        mapName = "Level " + levelId;
                        Timber.d("[HISTORY] Using level ID to generate map name: %s", mapName);
                    } else {
                        // Random game
                        mapName = "Random Map #" + historyIndex;
                        Timber.d("[HISTORY] Using fallback random map name: %s", mapName);
                    }
                }
            } else {
                // No game state available
                mapName = "Game " + historyIndex;
                Timber.e("[HISTORY] ERROR: No game state available, using default name: %s", mapName);
            }

            // For now, use a simplified approach for saving the game state
            // This avoids calling the missing methods
            String saveData = gameState.toString();
            FileReadWrite.writePrivateData(activity, historyPath, saveData);

            // Use static values for missing board dimensions
            int boardWidth = 16; // Default value
            int boardHeight = 16; // Default value
            String boardSize = boardWidth + "x" + boardHeight;

            // Simplified approach for preview image
            String previewImagePath = "history/" + historyFileName + "_preview.txt";
            FileReadWrite.writePrivateData(activity, previewImagePath, "");

            // Create history entry with available information
            GameHistoryEntry entry = new GameHistoryEntry(
                    historyPath,
                    mapName,
                    System.currentTimeMillis(),
                    totalPlayTime,
                    moveCount.getValue(),
                    0, // We don't have solution move count
                    boardSize,
                    previewImagePath
            );

            // Add entry to history index
            GameHistoryManager.addHistoryEntry(activity, entry);

            Timber.d("[HISTORY] Game saved to history: %s (Map name: '%s')", historyPath, mapName);
        } catch (Exception e) {
            Timber.e("[HISTORY] Error saving game to history: %s", e.getMessage());
        }
    }

    /**
     * Asynchronously calculates the solution for the current game state
     * and returns the result via callback
     *
     * @param callback The callback to receive the solution when it's ready
     */
    public void calculateSolutionAsync(final SolutionCallback callback) {
        // Don't start a new calculation if one is already running
        if (Boolean.TRUE.equals(isSolverRunning.getValue())) {
            Timber.d("[SOLUTION_SOLVER][calculateSolutionAsync] Solver already running, ignoring duplicate request");
            return;
        }

        // Increment solver restart count
        solverRestartCount++;
        Timber.d("[SOLUTION_SOLVER][calculateSolutionAsync] Solver restart count: %d", solverRestartCount);

        this.solutionCallback = callback;
        Timber.d("[SOLUTION_SOLVER][calculateSolutionAsync] Stored callback: %s", callback);

        GameState state = currentState.getValue();
        if (state == null) {
            Timber.d("[SOLUTION_SOLVER][calculateSolutionAsync] Current state is null");
            onSolutionCalculationFailed("No game state available");
            return;
        }

        // Log the current game state details
        ArrayList<GridElement> elements = state.getGridElements();
        Timber.d("[SOLUTION_SOLVER][calculateSolutionAsync] Current GameState hash: %d", state.hashCode());
        Timber.d("[SOLUTION_SOLVER][calculateSolutionAsync] Current map has %d elements", elements.size());

        // Log robot positions
        List<GameElement> robots = state.getRobots();
        for (GameElement robot : robots) {
            Timber.d("[SOLUTION_SOLVER][calculateSolutionAsync] Robot ID %d (color %d) at position (%d, %d)",
                    robot.getColor(), robot.getColor(), robot.getX(), robot.getY());
        }

        // Log target position
        GameElement target = state.getTarget();
        if (target != null) {
            Timber.d("[SOLUTION_SOLVER][calculateSolutionAsync] Target for robot Color %d (color %d) at position (%d, %d)",
                    target.getColor(), target.getColor(), target.getX(), target.getY());
        } else {
            Timber.d("[SOLUTION_SOLVER][calculateSolutionAsync] No target found in current game state");
        }

        // Set solver running state
        isSolverRunning.setValue(true);

        // Signal that calculation has started
        onSolutionCalculationStarted();

        try {
            Timber.d("[SOLUTION_SOLVER][calculateSolutionAsync] Initializing solver with current game state");
            getSolverManager().initialize(elements);

            // Run the solver on a background thread
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    Timber.d("[SOLUTION_SOLVER][calculateSolutionAsync] Running solver on background thread");
                    // Get the current solver ID for tracing
                    SolverManager manager = getSolverManager();
                    // Ensure we're incrementing the counter before running
                    SolverManager.ensureUniqueInvocationId();
                    Timber.d("[SOLUTION_SOLVER][calculateSolutionAsync] Using solver manager with counter: %d",
                            SolverManager.getCurrentSolverInvocationId());
                    manager.run();
                    // Note: The solver will call the listener methods (onSolverFinished)
                    // when it completes, so we don't need to do anything more here
                } catch (Exception e) {
                    Timber.e(e, "[SOLUTION_SOLVER] Error running solver");
                    // Handle on main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        onSolutionCalculationFailed("Error: " + e.getMessage());
                    });
                }
            });
            executor.shutdown();
        } catch (Exception e) {
            Timber.e(e, "[SOLUTION_SOLVER] Error initializing solver");
            onSolutionCalculationFailed("Error: " + e.getMessage());
        }
    }

    /**
     * Called when the solution calculation starts
     */
    private void onSolutionCalculationStarted() {
        Timber.d("[SOLUTION_SOLVER] onSolutionCalculationStarted");
        currentSolution = null;
        currentSolutionStep = 0;

        // Notify callback if provided
        if (solutionCallback != null) {
            Timber.d("[SOLUTION_SOLVER] onSolutionCalculationStarted: Notifying callback: %s", solutionCallback);
            solutionCallback.onSolutionCalculationStarted();
        } else {
            Timber.w("[SOLUTION_SOLVER] onSolutionCalculationStarted: No callback to notify");
        }
    }

    /**
     * Called when the solution calculation completes successfully
     * This is the SolverManager.Listener implementation that's called by SolverManager when the solver finishes.
     * This method stores the solution and notifies any registered callback.
     *
     * @param solution The calculated solution
     */
    private void onSolutionCalculationCompleted(GameSolution solution) {
        Timber.d("[SOLUTION_SOLVER] onSolutionCalculationCompleted: solution=%s", solution);

        // Add more detailed logging about the solution
        int moveCount = solution != null && solution.getMoves() != null ? solution.getMoves().size() : 0;
        if (moveCount > 0) {
            Timber.d("[SOLUTION_SOLVER][MOVES] onSolutionCalculationCompleted: Found solution with %d moves", solution.getMoves().size());
            // If solution requires fewer moves than required minimum and we're not in level mode,
            // automatically start a new game because this one is too easy
            GameState state = currentState.getValue();
            boolean isLevelMode = (state != null && state.getLevelId() > 0);
            int minRequiredMoves = getMinimumRequiredMoves();
            int maxRequiredMoves = getMaximumRequiredMoves();

            // BEGINNER MODE: Check if solution is too easy (below minimum) or too hard (above maximum)
            if (!isLevelMode && regenerationCount < MAX_AUTO_REGENERATIONS) {
                boolean isTooEasy = moveCount < minRequiredMoves;
                boolean isTooHard = moveCount > maxRequiredMoves;

                if (isTooEasy) {
                    // Regenerate if puzzle is too easy
                    Timber.d("[SOLUTION_SOLVER][MOVES] Solution has only %d moves (minimum required: %d), regenerating (attempt %d/%d)",
                            moveCount, minRequiredMoves, regenerationCount + 1, MAX_AUTO_REGENERATIONS);
                    regenerationCount++;

                    // Force reset the solver state before starting a new game
                    SolverManager solverManager = getSolverManager();
                    solverManager.resetInitialization();
                    solverManager.cancelSolver(); // Cancel any running solver process

                    // Create a new game after a short delay to ensure the solver is fully reset
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        createValidGame(Preferences.boardSizeWidth, Preferences.boardSizeHeight);
                    }, 100);
                    return;
                } else if (isTooHard) {
                    // Regenerate if puzzle is too hard for current difficulty mode
                    Timber.w("[DIFFICULTY ENFORCER] %s mode - Solution has %d moves (maximum allowed: %d), regenerating (attempt %d/%d)",
                            getLocalizedDifficultyString(), moveCount, maxRequiredMoves, regenerationCount + 1, MAX_AUTO_REGENERATIONS);
                    regenerationCount++;

                    // Force reset the solver state before starting a new game
                    SolverManager solverManager = getSolverManager();
                    solverManager.resetInitialization();
                    solverManager.cancelSolver(); // Cancel any running solver process

                    // Create a new game after a short delay to ensure the solver is fully reset
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        validateDifficulty = true; // Make sure difficulty is checked
                        createValidGame(Preferences.boardSizeWidth, Preferences.boardSizeHeight);
                    }, 100);
                    return;
                }
            } else if (regenerationCount >= MAX_AUTO_REGENERATIONS) {
                Timber.d("[SOLUTION_SOLVER][MOVES] Reached maximum regeneration attempts (%d). Accepting current game.", MAX_AUTO_REGENERATIONS);
                regenerationCount = 0; // Reset for next time
            }
        } else {
            Timber.w("[SOLUTION_SOLVER][MOVES] onSolutionCalculationCompleted: Solution or moves is null!");
        }

        // Store the solution for later use with getHint()
        currentSolution = solution;
        currentSolutionStep = 0;

        // Update solver status
        isSolverRunning.setValue(false);

        // Notify the callback if provided
        if (solutionCallback != null) {
            Timber.d("[SOLUTION_SOLVER] onSolutionCalculationCompleted: Notifying callback: %s", solutionCallback);
            solutionCallback.onSolutionCalculationCompleted(solution);
            solutionCallback = null; // Clear callback after use
        } else {
            Timber.d("[SOLUTION_SOLVER] onSolutionCalculationCompleted: No callback provided");
        }

        // Store the minimum moves from this solution for display
        lastSolutionMinMoves = solution != null && solution.getMoves() != null ? solution.getMoves().size() : 0;
        Timber.d("[SOLUTION_SOLVER][MOVES] onSolutionCalculationCompleted: Found solution with %d moves after %d regeneration(s)", lastSolutionMinMoves, regenerationCount);
    }

    /**
     * Called when the solution calculation fails
     *
     * @param errorMessage The error message
     */
    private void onSolutionCalculationFailed(String errorMessage) {
        Timber.d("[SOLUTION_SOLVER] onSolutionCalculationFailed: %s", errorMessage);

        // Clear any partial solution
        currentSolution = null;
        currentSolutionStep = 0;

        // Update solver status
        isSolverRunning.setValue(false);

        // Notify the callback if provided
        if (solutionCallback != null) {
            Timber.d("[SOLUTION_SOLVER] onSolutionCalculationFailed: Notifying callback: %s", solutionCallback);
            solutionCallback.onSolutionCalculationFailed(errorMessage);
            solutionCallback = null; // Clear callback after use
        } else {
            Timber.d("[SOLUTION_SOLVER] onSolutionCalculationFailed: No callback provided");
        }
    }

    /**
     * Cancel any running solver operation
     */
    public void cancelSolver() {
        Timber.d("[SOLUTION_SOLVER] cancelSolver called");
        if (Boolean.TRUE.equals(isSolverRunning.getValue())) {
            getSolverManager().cancelSolver();
            // The solver will call onSolverCancelled() via the listener
        }
    }

    /**
     * Get the current solution
     *
     * @return The current solution or null if none is available
     */
    public GameSolution getCurrentSolution() {
        return currentSolution;
    }

    /**
     * Get the current solution step (hint number)
     *
     * @return The current solution step (0-indexed)
     */
    public int getCurrentSolutionStep() {
        return currentSolutionStep;
    }

    /**
     * Increment the solution step counter
     */
    public void incrementSolutionStep() {
        currentSolutionStep++;
    }

    /**
     * Reset the solution step counter to show hints from the beginning
     */
    public void resetSolutionStep() {
        currentSolutionStep = 0;
    }

    // Field to store the current solution callback
    private SolutionCallback solutionCallback;

    /**
     * Callback interface for solution calculation
     */
    public interface SolutionCallback {
        /**
         * Called when the solution calculation starts
         */
        void onSolutionCalculationStarted();

        /**
         * Called when the solution calculation completes successfully
         *
         * @param solution The calculated solution
         */
        void onSolutionCalculationCompleted(GameSolution solution);

        /**
         * Called when the solution calculation fails
         *
         * @param errorMessage The error message
         */
        void onSolutionCalculationFailed(String errorMessage);
    }

    /**
     * Shows a spinner animation while a solution is being calculated.
     * This method can be used by any UI implementation to show a standardized
     * loading indicator during solver processing.
     *
     * @param callback A callback that will receive the Braille spinner character
     *                 updates for display
     * @param show     True to show the spinner, false to hide it
     * @return The BrailleSpinner instance (so the caller can store and stop it later)
     */
    public BrailleSpinner showSpinner(BrailleSpinner.SpinnerListener callback, boolean show) {
        if (show) {
            BrailleSpinner spinner = new BrailleSpinner();
            spinner.setSpinnerListener(callback);
            spinner.start();
            return spinner;
        } else if (callback != null) {
            // Just send an empty character to hide it
            callback.onSpinnerUpdate("");
        }
        return null;
    }

    /**
     * Animate a solution by moving robots according to the solution steps.
     * This implementation uses the utility SolutionAnimator and provides
     * a standard way to display solution animations in any UI.
     *
     * @param solution The solution to animate
     * @param listener The listener to receive animation events
     * @return The SolutionAnimator instance (so the caller can store and control it)
     */
    public SolutionAnimator animateSolution(GameSolution solution, SolutionAnimator.AnimationListener listener) {
        if (solution == null || solution.getMoves().isEmpty()) {
            Timber.w("Cannot animate null or empty solution");
            if (listener != null) {
                listener.onAnimationComplete();
            }
            return null;
        }

        Timber.d("Animating solution with %d moves", solution.getMoves().size());

        // Create and configure animator
        SolutionAnimator animator = new SolutionAnimator();
        animator.setAnimationListener(listener);
        animator.animateSolution(solution);
        return animator;
    }

    /**
     * Reset statistics for a new game
     */
    private void resetStatistics() {
        hintsShown = 0;
        robotsUsed.clear();
        startTime = System.currentTimeMillis();
        moveCount.setValue(0);
        squaresMoved.setValue(0);
        isGameComplete.setValue(false);
    }

    /**
     * Gets the minimum required moves based on current difficulty setting
     *
     * @return minimum number of moves required for current difficulty
     */
    private int getMinimumRequiredMoves() {
        int difficulty = Preferences.difficulty;
        int minMoves = 0;
        switch (difficulty) {
            case Constants.DIFFICULTY_ADVANCED:
                minMoves = MIN_MOVES_ADVANCED;
                break;
            case Constants.DIFFICULTY_INSANE:
                minMoves = MIN_MOVES_INSANE;
                break;
            case Constants.DIFFICULTY_IMPOSSIBLE:
                minMoves = MIN_MOVES_IMPOSSIBLE;
                break;
            default:
                minMoves = MIN_MOVES_BEGINNER; // Default to beginner if unknown difficulty
        }
        Timber.d("[SOLUTION_SOLVER][MOVES] Minimum required moves for difficulty %d: %d", difficulty, minMoves);
        return minMoves;
    }

    private int getMaximumRequiredMoves() {
        int difficulty = Preferences.difficulty;
        int maxMoves = 9999;
        switch (difficulty) {
            case Constants.DIFFICULTY_ADVANCED:
                maxMoves = MAX_MOVES_ADVANCED;
                break;
            case Constants.DIFFICULTY_INSANE:
            case Constants.DIFFICULTY_IMPOSSIBLE:
                break;
            default:
                maxMoves = MAX_MOVES_BEGINNER;
        }
        Timber.d("[SOLUTION_SOLVER][MOVES] Maximum required moves for difficulty %d: %d", difficulty, maxMoves);
        return maxMoves;
    }

    /**
     * Creates a valid game with at least MIN_REQUIRED_MOVES difficulty
     *
     * @param width  Width of the board
     * @param height Height of the board
     */
    private void createValidGame(int width, int height) {
        Timber.d("GameStateManager: createValidGame() called");

        // Update WallStorage with current board size to ensure we're using the right storage
        WallStorage wallStorage = WallStorage.getInstance();
        wallStorage.updateCurrentBoardSize();

        // Create a new random game state using static Preferences
        GameState newState = GameState.createRandom();
        Timber.d("GameStateManager: Created new random GameState with robotCount=%d, targetColors=%d",
                Preferences.robotCount,
                Preferences.targetColors);

        // DEBUG: Analyze all game elements in the newly created state
        Timber.d("[DEBUG_ROBOTS] Starting debug of newly created GameState (createValidGame)");
        int robotCount = 0;
        for (GameElement element : newState.getGameElements()) {
            if (element.isRobot()) {
                robotCount++;
                Timber.d("[DEBUG_ROBOTS] Robot #%d at (%d,%d) with color %d (colorName: %s)",
                        robotCount, element.getX(), element.getY(), element.getColor(), 
                        GameLogic.getColorName(element.getColor(), true));
            }
        }
        Timber.d("[DEBUG_ROBOTS] Total robots in new GameState: %d (should be %d)", 
                robotCount, Constants.NUM_ROBOTS);

        // Set the game state
        currentState.setValue(newState);
        moveCount.setValue(0);
        isGameComplete.setValue(false);

        // Store walls for future use if we're not generating new maps each time
        if (!Preferences.generateNewMapEachTime) {
            wallStorage.storeWalls(newState.getGridElements());
            Timber.d("[WALL STORAGE] Stored walls for future use after creating new game");
        }

        // Clear the state history
        stateHistory.clear();
        squaresMovedHistory.clear();

        // Initialize the solver with grid elements from the new state
        ArrayList<GridElement> gridElements = newState.getGridElements();
        getSolverManager().resetInitialization();
        getSolverManager().initialize(gridElements);

        startTime = System.currentTimeMillis();

        // Start calculating the solution, but use our internal validation callback
        if (validateDifficulty) {
            // Temporarily set validateDifficulty to false to prevent infinite recursion

            // Calculate solution with our own callback to validate difficulty
            Timber.d("[calculateSolutionAsync] GameStateManager: Validating puzzle difficulty...");
            calculateSolutionAsync(new DifficultyValidationCallback(width, height));
        } else {
            // only debug
            Timber.d("[calculateSolutionAsync] GameStateManager: Not validating puzzle difficulty");
            // Regular game initialization, don't validate difficulty
            validateDifficulty = true; // Reset for next time
            calculateSolutionAsync(null);
        }
    }

    /**
     * Callback to validate puzzle difficulty and regenerate if needed
     */
    private class DifficultyValidationCallback implements SolutionCallback {
        private final int width;
        private final int height;
        private int attemptCount = 0;
        private static final int MAX_ATTEMPTS = 999;

        public DifficultyValidationCallback(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void onSolutionCalculationStarted() {
            Timber.d("DifficultyValidationCallback: Calculation started, attempt %d", attemptCount + 1);
        }

        @Override
        public void onSolutionCalculationCompleted(GameSolution solution) {
            attemptCount++;
            int moveCount = solution != null && solution.getMoves() != null ? solution.getMoves().size() : 0;
            int requiredMoves = getMinimumRequiredMoves();
            int maxMoves = getMaximumRequiredMoves();

            // if only one move is needed, then the puzzle is too easy
            if (getSolverManager().isSolution01()) {
                Timber.d("[DifficultyValidationCallback]: Puzzle too easy (1 move), generating new one");
                createValidGame(width, height);
                return;
            }

            Timber.d("[DifficultyValidationCallback]: Found solution with %d moves (minimum required: %d, maximum required: %d)",
                    moveCount, requiredMoves, maxMoves);

            if (moveCount < requiredMoves && attemptCount < MAX_ATTEMPTS) {
                // Puzzle too easy, generate a new one
                Timber.d("[DifficultyValidationCallback]: Puzzle too easy (%d moves), generating new one", moveCount);
                createValidGame(width, height);
            } else if (moveCount > maxMoves && attemptCount < MAX_ATTEMPTS) {
                // Puzzle too hard, generate a new one
                Timber.d("[DifficultyValidationCallback]: Puzzle too hard (%d moves), generating new one", moveCount);
                createValidGame(width, height);
            } else {
                // Puzzle is good enough or we've tried too many times
                Timber.d("[DifficultyValidationCallback][calculateSolutionAsync] Accepted puzzle with %d moves after %d attempts",
                        moveCount, attemptCount);
                validateDifficulty = true; // Reset validation flag
                // Store the solution
                currentSolution = solution;
                currentSolutionStep = 0;
                isSolverRunning.setValue(false);
                // Signal to UI that solution was accepted and hint container should be hidden
                Timber.d("[SOLUTION][ACCEPTED][calculateSolutionAsync] Solution accepted, notifying UI to hide hint container");

                // TODO: stop all other solver threads:

            }
        }

        @Override
        public void onSolutionCalculationFailed(String errorMessage) {
            Timber.w("DifficultyValidationCallback: Solution calculation failed: %s", errorMessage);
            // Just accept the current puzzle even if we couldn't solve it
            validateDifficulty = true;
            isSolverRunning.setValue(false);
        }
    }

    @Override
    public void onSolverFinished(boolean success, int solutionMoves, int numSolutions) {
        Timber.d("[SOLUTION_SOLVER][DIAGNOSTIC] GameStateManager.onSolverFinished called: success=%b, moves=%d, solutions=%d",
                success, solutionMoves, numSolutions);

        // Process on main thread to ensure thread safety with UI updates
        new Handler(Looper.getMainLooper()).post(() -> {
            if (success && numSolutions > 0) {
                // Get the solution from the solver manager
                try {
                    GameSolution solution = getSolverManager().getCurrentSolution();
                    if (solution != null) {
                        Timber.d("[SOLUTION_SOLVER][DIAGNOSTIC] GameStateManager found solution with %d moves",
                                solution.getMoves() != null ? solution.getMoves().size() : 0);
                        // Forward to the regular solution handling
                        onSolutionCalculationCompleted(solution);
                    } else {
                        Timber.e("[SOLUTION_SOLVER][DIAGNOSTIC] GameStateManager received null solution despite success=true");
                        onSolutionCalculationFailed("No valid solution found");
                    }
                } catch (Exception e) {
                    Timber.e(e, "[SOLUTION_SOLVER][DIAGNOSTIC] Error getting solution from solver: %s", e.getMessage());
                    onSolutionCalculationFailed("Error: " + e.getMessage());
                }
            } else {
                Timber.d("[SOLUTION_SOLVER][DIAGNOSTIC] GameStateManager - No solution found");
                onSolutionCalculationFailed("No solution found");
            }
        });
    }

    @Override
    public void onSolverCancelled() {
        Timber.d("[SOLUTION_SOLVER][DIAGNOSTIC] GameStateManager.onSolverCancelled called");

        // Process on main thread to ensure thread safety with UI updates
        new Handler(Looper.getMainLooper()).post(() -> {
            onSolutionCalculationFailed("Solver was cancelled");
        });
    }

    /**
     * Get the robot animation manager
     *
     * @return The robot animation manager
     */
    public RobotAnimationManager getRobotAnimationManager() {
        return robotAnimationManager;
    }

    /**
     * Get the current animation frame delay in milliseconds
     *
     * @return Current animation frame delay
     */
    public long getAnimationFrameDelay() {
        return animationFrameDelay;
    }

    /**
     * Get acceleration duration for animations
     *
     * @return Acceleration duration in milliseconds
     */
    public float getAccelerationDuration() {
        return accelerationDuration;
    }

    /**
     * Get maximum animation speed
     *
     * @return Maximum speed for animations
     */
    public float getMaxSpeed() {
        return maxSpeed;
    }

    /**
     * Get deceleration duration for animations
     *
     * @return Deceleration duration in milliseconds
     */
    public float getDecelerationDuration() {
        return decelerationDuration;
    }

    /**
     * Get overshoot percentage for animations
     *
     * @return Overshoot percentage (0.0-1.0)
     */
    public float getOvershootPercentage() {
        return overshootPercentage;
    }

    /**
     * Get spring back duration for animations
     *
     * @return Spring back duration in milliseconds
     */
    public float getSpringBackDuration() {
        return springBackDuration;
    }

    /**
     * Reset the game to its initial state (Soft Reset)
     * - Preserves the current board/map layout
     * - Resets robot positions to their starting positions
     * - Clears move counters and selection states
     * - Keeps the same target and wall configurations
     * - Perfect for when a player wants to try the same puzzle again
     */
    public void resetGame() {
        isResetting = true;

        // Cancel all animations first
        if (robotAnimationManager != null) {
            robotAnimationManager.cancelAllAnimations();
        }

        // Get the current game state
        GameState currentGameState = currentState.getValue();

        if (currentGameState != null) {
            // Reset robot positions
            currentGameState.resetRobotPositions();

            // Reset game statistics
            moveCount.setValue(0);
            squaresMoved.setValue(0);
            isGameComplete.setValue(false);

            // Reset robot selection
            for (GameElement element : currentGameState.getGameElements()) {
                if (element.isRobot()) {
                    element.setSelected(false);
                }
            }

            // REMOVED: resetRobots(); - This was causing infinite recursion

            // Reset the robotsUsed tracking for statistics
            robotsUsed.clear();

            // Notify that the game state has changed
            currentState.setValue(currentGameState);
        }

        isResetting = false;
    }

    /**
     * Check if animations are enabled
     *
     * @return True if animations are enabled
     */
    public boolean areAnimationsEnabled() {
        return animationsEnabled;
    }

    /**
     * Set the GameGridView for rendering
     *
     * @param gameGridView The game grid view
     */
    public void setGameGridView(GameGridView gameGridView) {
        this.gameGridView = gameGridView;

        // Connect the grid view to the animation manager
        if (robotAnimationManager != null) {
            robotAnimationManager.setGameGridView(gameGridView);
        }
    }

    /**
     * Reset all move counts and game history
     * This resets the move counter, squares moved counter, game completion status,
     * and clears both state history and squares moved history.
     */
    public void resetMoveCountsAndHistory() {
        Timber.d("[RESET_GAME] Resetting all move counts and game history");
        // Reset move counts and history
        setMoveCount(0);
        resetSquaresMoved(); // reset squares moved count
        setGameComplete(false);
        stateHistory.clear();
        squaresMovedHistory.clear();
    }

    /**
     * Gets the current solver restart count
     *
     * @return The number of times the solver has been restarted
     */
    public int getSolverRestartCount() {
        return solverRestartCount;
    }

    /**
     * reset the solver restart count
     */
    public void resetSolverRestartCount() {
        solverRestartCount = 0;
    }

    /**
     * reset last solution min moves
     */
    public void resetLastSolutionMinMoves() {
        lastSolutionMinMoves = 0;
    }

    /**
     * Gets the minimum moves from the last found solution
     *
     * @return The minimum moves from the last solution, or 0 if no solution found yet
     */
    public int getLastSolutionMinMoves() {
        return lastSolutionMinMoves;
    }

    private void resetGameTimer() {
        gameStartTime = System.currentTimeMillis();
        totalPlayTime = 0;
        isHistorySaved = false;
    }

    /**
     * Get the current activity
     *
     * @return The current activity or null if none is available
     */
    private Activity getActivity() {
        if (activityRef != null) {
            return activityRef.get();
        }
        return null;
    }

    /**
     * Set the current activity reference
     *
     * @param activity Current activity
     */
    public void setActivity(Activity activity) {
        if (activity != null) {
            this.activityRef = new WeakReference<>(activity);
            Timber.d("[HISTORY] Activity reference updated in GameStateManager");
        }
    }

    /**
     * Set the current game state
     * Used by deep link functionality to load a state from external data
     *
     * @param state The game state to set
     */
    public void setGameState(GameState state) {
        if (state == null) {
            Timber.e("[DEEPLINK] Cannot set null game state");
            return;
        }

        Timber.d("[DEEPLINK] Setting game state from deep link");

        // --- VALIDATION LOGGING [RANDOM_STATE_VALIDATION] ---
        // Check if the state is a random state (i.e., not from a level, save, or deep link)
        // If so, log a warning. This helps ensure validation is not bypassed.
        if (state.getLevelId() == -1 && (state.getLevelName() == null || state.getLevelName().equals("XXXXX"))) {
            Timber.w("[RANDOM_STATE_VALIDATION] setGameState called with a random state! This may bypass difficulty validation. State: levelId=%d, levelName=%s", state.getLevelId(), state.getLevelName());
        }
        // --- END VALIDATION LOGGING ---

        // Clear history and reset counters
        resetMoveCountsAndHistory();

        // Set the current map name
        this.currentMapName = state.getLevelName();
        Timber.d("[MAPNAME] GameStateManager.setGameState - Set currentMapName to: %s", this.currentMapName);

        // Set the connection back to this manager
        state.setGameStateManager(this);

        // Update the current state
        currentState.setValue(state);

        // Update the move count
        moveCount.setValue(state.getMoveCount());

        // Reset game timer
        resetGameTimer();
        startGameTimer();

        // Start the solver in the background
        calculateSolutionAsync(null);
    }
}
