package roboyard.eclabs;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import roboyard.eclabs.solver.ISolver;
import roboyard.eclabs.solver.SolverDD;
import roboyard.pm.ia.GameSolution;
import roboyard.pm.ia.IGameMove;
import roboyard.pm.ia.ricochet.RRGameMove;
import timber.log.Timber;

// import static android.content.Context.MODE_PRIVATE;

/**
 * This class represents the game screen where the grid is displayed.
 */
public class GridGameScreen extends GameScreen {
    private Canvas canvasGrid;
    private boolean isSolved = false;
    int solutionMoves = 0; // store the optimal number of moves for the solution globally
    private int NumDifferentSolutionsFound = 0; // store the number of different solution globally
    private int numSolutionClicks = 0; // count how often you clicked on the solution button, each time the shown count goes down by one
    private int numDifferentSolutionClicks = 0; // count how often you clicked on the solution button again to show a different solution
    private int showSolutionAtHint; // interval between the first hint and the current optimal solution (will be set to random 3..5 later
    private int initialSolutionAtHints = 5; // initial value for showSolutionAtHint

    private static int goodPuzzleMinMoves = 8; // below this number of moves there is a special hint shown from the start
    private static int simplePuzzleMinMoves = 6; // below this threshold a new puzzle is generated

    private static String requestToast = ""; // this can be set from outside to set the text for a popup

    private ISolver solver;

    private boolean autoSaved = false;

    private ArrayList<GridElement> gridElements;
    private int imageGridID;
    private boolean imageLoaded = false;

    private String mapPath = "";

    private int currentHistoryIndex = -1; // -1 means "no current history entry"

    private int xGrid;
    private int yGrid;
    private float gridSpace;
    private int gridBottom;
    private int topMargin = 16;
    
    // UI Layout
    private ScreenLayout layout;
    private int buttonSize;        // Size of buttons (width=height)
    private int buttonPosY;        // Y position of bottom button row
    private int nextButtonPosY;    // Y position of next button
    private int boardNamePosY;     // Y position of level name text
    private int solutionTextPosY;  // Y position of solution text area
    private int lineHeight;        // text line height
    private int lineHeightSmall;   // Small text line height
    private int textMarginLeft;    // Left margin for text

    // Colors moved to UIConstants
    private final RenderManager currentRenderManager;

    private int timeCpt = 0;
    private int nbCoups = 0;
    private boolean isGameWon = false;
    private long prevTime;
    private boolean isHistorySaved = false;
    private static final int HISTORY_SAVE_THRESHOLD = 3; // 3 seconds (reduced from 60 seconds)

    private int IAMovesNumber = 0;

    private boolean mustStartNext = false;

    private ArrayList<IGameMove> moves = null;

    private Thread t = null;

    private GameMovementInterface gmi;
    private Bitmap bitmapGrid;
    final Map<String, Drawable> drawables = new HashMap<>();
    final Map<String, Integer> colors = new HashMap<>();
    private final ArrayList<Move> allMoves= new ArrayList<>();

    private GameButtonGeneral buttonSolve;
    private GameButtonGoto buttonSave;

    private final Preferences preferences = new Preferences();

    private boolean isRandomGame = false;

    private int textColorHighlight = Color.parseColor("#aaaaaa");
    private int textColorNormal = Color.GRAY;
    
    String mapName = "";
    GameSolution solution;

    private static ArrayList<GridElement> map = new ArrayList<>();
    private static int levelDifficulty = 0;
    private static ArrayList<GridElement> currentMap = new ArrayList<>();

    // Variables for tracking moved squares
    private int currentMovedSquares = 0;
    private int numSquares = 0;

    /**
     * Constructor for GridGameScreen - creates and initializes the game grid screen
     * 
     * @param gameManager The game manager instance that handles game state and resources
     */
    public GridGameScreen(GameManager gameManager){
        super(gameManager);
        
        // Load difficulty setting from preferences, default to "Beginner" if not set
        String ld = preferences.getPreferenceValue(gameManager.getActivity(), "difficulty");
        if(ld.equals("")){
            // default difficulty
			ld = "Beginner";
            preferences.setPreferences(gameManager.getActivity(), "difficulty", ld);
        }
        setDifficulty(ld);
        
        // Initialize with default state
        isRandomGame = false;  // Start in non-random mode
        
        // Calculate grid dimensions based on screen size and board width
        gridSpace = (float)gameManager.getScreenWidth() / (float)MainActivity.getBoardWidth();
        xGrid = 0;
        yGrid = topMargin;  // Add a small margin at the top
        gridBottom = yGrid + (int)((MainActivity.getBoardHeight() + 1) * gridSpace);

        // Create bitmap for the game grid
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;  // Use ARGB_8888 for high quality graphics
        bitmapGrid = Bitmap.createBitmap(
            (int)(MainActivity.getBoardWidth() * gridSpace), 
            (int)(MainActivity.getBoardHeight() * gridSpace), 
            conf
        );
        canvasGrid = new Canvas(bitmapGrid);
        currentRenderManager = gameManager.getRenderManager();

        prevTime = System.currentTimeMillis();

        // Load robot images for all directions
        // Right-facing robots
        gameManager.getRenderManager().loadImage(R.drawable.robot_yellow_right);
        gameManager.getRenderManager().loadImage(R.drawable.robot_blue_right);
        gameManager.getRenderManager().loadImage(R.drawable.robot_green_right);
        gameManager.getRenderManager().loadImage(R.drawable.robot_red_right);

        // Left-facing robots
        gameManager.getRenderManager().loadImage(R.drawable.robot_yellow_left);
        gameManager.getRenderManager().loadImage(R.drawable.robot_blue_left);
        gameManager.getRenderManager().loadImage(R.drawable.robot_green_left);
        gameManager.getRenderManager().loadImage(R.drawable.robot_red_left);
    }

    public static int getLevel() {
        return GridGameScreen.levelDifficulty;
    }

    public static void setDifficulty(String levelDifficulty) {
        int difficulty = 0;
        if(levelDifficulty.equals("Beginner")) {
            difficulty = 0;
            GridGameScreen.goodPuzzleMinMoves = 6;
            GridGameScreen.simplePuzzleMinMoves = 4;
        } else if(levelDifficulty.equals("Advanced")) {
            difficulty = 1;
            GridGameScreen.goodPuzzleMinMoves = 8;
            GridGameScreen.simplePuzzleMinMoves = 6;
        } else if(levelDifficulty.equals("Insane")) {
            difficulty = 2;
            GridGameScreen.goodPuzzleMinMoves = 10;
            GridGameScreen.simplePuzzleMinMoves = 10;
        } else if(levelDifficulty.equals("Impossible")) {
            difficulty = 3;
            GridGameScreen.goodPuzzleMinMoves = 17;
            GridGameScreen.simplePuzzleMinMoves = 17;
            for (int i = 0; i < 3; i++) {
                // repeat three times to get a long toast
                GridGameScreen.requestToast = "Level Impossible will generate a fitting puzzle. This can take a while. In case the solver gets stuck, press >>";
            }
        }
        GridGameScreen.levelDifficulty = difficulty;
        Timber.d("Level difficulty set to: " + levelDifficulty + " goodPuzzleMinMoves: " + GridGameScreen.goodPuzzleMinMoves + " simplePuzzleMinMoves: " + GridGameScreen.simplePuzzleMinMoves);
    }

    public static ArrayList<GridElement> getMap() {
        return GridGameScreen.currentMap;
    }

    public static void setMap(ArrayList<GridElement> data){
        GridGameScreen.currentMap = data;
    }

    @Override
    public void create() {
        Timber.d("GridGameScreen.create()");
        gridElements = new ArrayList<>();
        prevTime = System.currentTimeMillis();
        isHistorySaved = false;
        numSolutionClicks = 0;
        gmi = new GameMovementInterface(gameManager);

        // Initialize screen layout
        layout = new ScreenLayout(
            gameManager.getScreenWidth(),
            gameManager.getScreenHeight(),
            gameManager.getActivity().getResources().getDisplayMetrics().density
        );

        // Calculate button and text positions
        buttonSize = layout.getButtonSize();
        buttonPosY = layout.y(UIConstants.BOTTOM_BUTTONS_Y);
        nextButtonPosY = layout.y(UIConstants.BOTTOM_BUTTONS_Y - UIConstants.BUTTON_GAP) - buttonSize;
        boardNamePosY = nextButtonPosY + buttonSize - layout.y(10);
        lineHeight = layout.getTextSize(UIConstants.TEXT_SIZE_NORMAL);
        lineHeightSmall = layout.getTextSize(UIConstants.TEXT_SIZE_SMALL);
        solutionTextPosY = layout.y(UIConstants.BOTTOM_BUTTONS_Y) - buttonSize + layout.y(77);
        textMarginLeft = layout.x(22);

        // Initialize grid
        xGrid = 0;
        yGrid = topMargin;
        gridSpace = (float)layout.getScreenWidth() / (float)MainActivity.getBoardWidth();
        gridBottom = yGrid + (int)((MainActivity.getBoardHeight() + 1) * gridSpace);

        // ----------------- Create buttons

        // Next Button
        int currentLevel = extractLevelNumber(mapPath);
        if (currentLevel < 140) { // Show next button for random games or levels below 140
            this.instances.add(new GameButtonGeneral(
                layout.x(UIConstants.NEXT_BUTTON_X), 
                nextButtonPosY,
                buttonSize, 
                buttonSize,
                R.drawable.bt_next_up,
                R.drawable.bt_next_down,
                new ButtonNext()
            ));
        }

        // Bottom row buttons
        // Save button
        buttonSave = new GameButtonGoto(
            0, 
            buttonPosY,
            buttonSize,
            buttonSize,
            R.drawable.bt_jeu_save_up,
            R.drawable.bt_jeu_save_down,
            9
        );
        this.instances.add(buttonSave);

        // one Step Back button
        this.instances.add(new GameButtonGeneral(
            buttonSize,
            buttonPosY,
            buttonSize,
            buttonSize,
            R.drawable.bt_jeu_retour_up,
            R.drawable.bt_jeu_retour_down,
            new ButtonBack()
        ));

        // Restart button
        this.instances.add(new GameButtonGeneral(
            buttonSize * 2,
            buttonPosY,
            buttonSize,
            buttonSize,
            R.drawable.bt_jeu_reset_up,
            R.drawable.bt_jeu_reset_down,
            new ButtonResetRobots()
        ));

        // Solve button
        buttonSolve = new GameButtonGeneral(
            buttonSize * 3,
            buttonPosY,
            buttonSize,
            buttonSize,
            R.drawable.bt_jeu_resolution_up,
            R.drawable.bt_jeu_resolution_down,
            new ButtonSolution()
        );
        buttonSolve.setImageDisabled(R.drawable.bt_jeu_resolution_disabled);
        buttonSolve.setEnabled(false);
        this.instances.add(buttonSolve);

        // Load images
        gameManager.getRenderManager().loadImage(R.drawable.transparent);
        gameManager.getRenderManager().loadImage(R.drawable.bt_jeu_resolution_disabled);

        this.solver = new SolverDD();
    }

    @Override
    public void load(RenderManager renderManager) {
        super.load(renderManager);
        this.gmi.load(renderManager);
    }

    @Override
    public void draw(RenderManager renderManager) {
        int levelNum = -1;
        if (mapPath != null && !mapPath.isEmpty()) {
            levelNum = extractLevelNumber(mapPath);
        }
        //renderManager.setColor(Color.argb(255, 255, 228, 0));
        renderManager.setColor(Color.BLACK);
        // ffe400
        // ff7c24
        renderManager.paintScreen();

        //renderManager.setColor(Color.BLACK);
        renderManager.setColor(textColorNormal);
        float ratio = ((float) gameManager.getScreenWidth()) / ((float) 1080); // bei 720x1280:0.6667 bei 1440x2580:1.333

        // Text Display underneath the Board
        int textPosY = (int)(gridBottom + (ratio * 13)); // Add margin below the game grid
        int textPosYSmall = textPosY + lineHeight;

        // Set text size
        renderManager.setTextSize(lineHeight);
        if (gameManager.getScreenWidth() <= 480) {
            renderManager.setTextSize(lineHeightSmall);
        }

        if (levelNum >= 1 && numSolutionClicks == 0) {
            // In Level game always show the first solution
            numSolutionClicks = 1;
        }

        // Draw moves and solution information
        renderManager.setColor(UIConstants.TEXT_COLOR_NORMAL);
        int posY = solutionTextPosY;
        renderManager.setTextSize(layout.getTextSize(UIConstants.TEXT_SIZE_NORMAL));
        if (nbCoups > 0) {
            // At least one move was made by hand or by AI
            renderManager.setColor(UIConstants.TEXT_COLOR_HIGHLIGHT);
            renderManager.drawText(textMarginLeft, posY, "Moves: " + nbCoups);
            renderManager.setColor(UIConstants.TEXT_COLOR_NORMAL);
            renderManager.setTextSize(layout.getTextSize(UIConstants.TEXT_SIZE_SMALL));
            renderManager.drawText(textMarginLeft, posY + lineHeight, "Squares: " + numSquares);
        } else if (isSolved && numSolutionClicks > 0) {
            // Show solution
            renderManager.setColor(UIConstants.TEXT_COLOR_HIGHLIGHT);
            if (numSolutionClicks - showSolutionAtHint >= 0) {
                renderManager.drawText(textMarginLeft, posY, "AI solution: ");
                
                // Draw number in larger font
                renderManager.setColor(Color.WHITE);
                renderManager.setTextSize(layout.getTextSize(UIConstants.TEXT_SIZE_LARGE));
                int larger9 = solutionMoves <= 9 ? 0 : 56;
                renderManager.drawText(layout.x(404), posY + 5, String.valueOf(solutionMoves));

                renderManager.setColor(UIConstants.TEXT_COLOR_HIGHLIGHT);
                renderManager.setTextSize(layout.getTextSize(UIConstants.TEXT_SIZE_NORMAL));
                renderManager.drawText(layout.x(452 + larger9), posY, " moves");
            } else {
                renderManager.drawText(textMarginLeft, posY,
                    "AI Hint " + numSolutionClicks + ": < " + 
                    (solutionMoves + showSolutionAtHint - numSolutionClicks) + " moves");
            }
        } else if (nbCoups == 0 && isSolved && solutionMoves < simplePuzzleMinMoves) {
            // too simple ... restart
            renderManager.setColor(textColorHighlight);
            renderManager.drawText(textMarginLeft, posY, "AI solution: " + solutionMoves + " moves");
            Timber.d("too simple: solutionMoves = %d", solutionMoves);
            renderManager.setColor(textColorNormal);
            renderManager.setTextSize(lineHeightSmall);
            renderManager.drawText(textMarginLeft, posY + lineHeightSmall, "... restarting!");
            if (timeCpt > 5) {
                // show a popup on restart if it took very long to solve but found a too simple solution
                requestToast = "Finally solved in " + solutionMoves + " moves. Restarting...";
            }
            mustStartNext = true;
        } else if (nbCoups == 0 && isSolved && solutionMoves < goodPuzzleMinMoves && getLevel() != 0) {
            // still simple, show a hint that this is solved with less than ... moves
            // TODO: change font (still crashes):
            //  renderManager.drawText(textMarginLeft, posY, "Number of moves < " + goodPuzzleMinMoves, "FiraMono-Bold", gameManager.getActivity());
            renderManager.setColor(textColorHighlight);
            renderManager.drawText(textMarginLeft, posY, "Number of moves < " + goodPuzzleMinMoves);
            showSolutionAtHint = goodPuzzleMinMoves - solutionMoves; // this sets the num clicks to the difference of the best solution (e.g. 5) and goodPuzzleMinMoves (e.g. 6 on Beginner)
        } else if (!isSolved) {
            if (timeCpt < 1) {
                // The first second it pretends to generate the map
                renderManager.setColor(UIConstants.TEXT_COLOR_NORMAL);
                if (levelNum >= 1) {
                    renderManager.drawText(textMarginLeft, posY, "Loading map...");
                } else {
                    renderManager.drawText(textMarginLeft, posY, "Generating map...");
                }
            } else {
                renderManager.setColor(UIConstants.TEXT_COLOR_NORMAL);
                if (getLevel() == 0 && levelNum < 0) {
                    renderManager.drawText(textMarginLeft, posY, "Too complicated");
                    renderManager.drawText(textMarginLeft, posY + lineHeight, "... restarting!");
                    mustStartNext = true;
                } else {
                    String solvingText;
                    // create a ascii spinner with thesse chars: . o O o
                    if (timeCpt % 4 == 0) {
                        solvingText = "AI solving /";
                    } else if (timeCpt % 4 == 1 || timeCpt % 4 == 3) {
                        solvingText = "AI solving –";
                    } else {
                        solvingText = "AI solving \\";
                    }
                    renderManager.drawText(textMarginLeft, posY, solvingText);
                }
            }
        }

        // Timber.d("showSolutionAtHint = %d", showSolutionAtHint);

        // Draw solution information
        posY += (lineHeight);
        if (isSolved && nbCoups == 0 && NumDifferentSolutionsFound > 1) {
            // Show number of different solutions found
            renderManager.setTextSize(layout.getTextSize(UIConstants.TEXT_SIZE_SMALL));
            renderManager.setColor(UIConstants.TEXT_COLOR_NORMAL);
            renderManager.drawText(textMarginLeft, posY, NumDifferentSolutionsFound + " solutions found");
            renderManager.setTextSize(lineHeight);
        }

        // Draw time
        posY += (lineHeight);
        int seconds = timeCpt % 60;
        String secondsS = Integer.toString(seconds);
        if (seconds < 10) {
            secondsS = "0" + secondsS;
        }
        renderManager.setTextSize(lineHeightSmall);
        renderManager.setColor(UIConstants.TEXT_COLOR_NORMAL);
        renderManager.drawText(textMarginLeft, posY, "Time: " + timeCpt / 60 + ":" + secondsS);

        // Draw level name or unique string
        renderManager.setColor(UIConstants.TEXT_COLOR_NORMAL);
        renderManager.setTextSize(lineHeight / 2);
        
        int boardNamePosX = layout.x(UIConstants.BOARD_NAME_POS_X);
        if (mapPath != null && mapPath.startsWith("Maps/")) {
            // Level number underneath the next button
            mapName = "Level " + levelNum;
            renderManager.drawText(boardNamePosX, boardNamePosY, mapName);
        } else {
            // Show the unique string for the current map
            mapName = MapObjects.createStringFromList(gridElements, true);
            renderManager.drawText(boardNamePosX - layout.x(2), boardNamePosY, mapName);
        }

        // Draw the grid
        if (imageLoaded) {
            renderManager.drawImage(xGrid, yGrid, 
                (int)(MainActivity.getBoardWidth() * gridSpace) + xGrid,
                (int)(MainActivity.getBoardHeight() * gridSpace) + yGrid, 
                imageGridID);
        }
        super.draw(renderManager);
        this.gmi.draw(renderManager);

        if (!requestToast.equals("")) {
            // show double toast to last longer
            gameManager.requestToast(requestToast, true);
            gameManager.requestToast(requestToast, true);
            requestToast = "";
        }
    }

    private void initializeSolutionsAtHints() {
        showSolutionAtHint = 2 + (int)(Math.random() * ((initialSolutionAtHints - 2) + 1));
    }

    public void update(GameManager gameManager){
        super.update(gameManager);

        if(mustStartNext){
            // Reset the game state
            timeCpt = 0;
            nbCoups = 0;
            numSquares = 0;
            IAMovesNumber = 0;
            isHistorySaved = false;

            allMoves.clear();
            autoSaved = false;

            buttonSolve.setEnabled(false);
            if(t != null){
                t.interrupt();
                moves = null;
                t = null;
            }

            int levelNum = extractLevelNumber(mapPath);
            if(levelNum >=0 && levelNum < 60)
            {
                // Next level in sequence
                String nextMapPath = "Maps/level_" + (levelNum + 1) + ".txt";
                setLevelGame(nextMapPath);
            } else {
                // start a game in screen 4
                setRandomGame();
            }

            mustStartNext = false;
        }
        
        // Only update time if the game is not won
        if(System.currentTimeMillis() - prevTime > 1000L && !isGameWon){
            // update time every second
            timeCpt++;
            prevTime = System.currentTimeMillis();
            
            // Check if we need to save to history
            if (!isHistorySaved && timeCpt >= HISTORY_SAVE_THRESHOLD && !isGameWon) {
                gameManager.requestToast("Game saved to history", true);
                Timber.d("Time threshold reached (%d seconds), saving to history", timeCpt);
                boolean saveSuccess = saveToHistory();
                isHistorySaved = true;
                Timber.d("History saved, currentHistoryIndex=%d, success=%b", currentHistoryIndex, saveSuccess);
            }
        }
        this.gmi.update(gameManager);
        if(gameManager.getInputManager().backOccurred()){
            // Update history entry if it exists before exiting
            Timber.d("Back detected, currentHistoryIndex=%d, isHistorySaved=%b", currentHistoryIndex, isHistorySaved);
            if (isHistorySaved) {
                updateHistoryEntry();
            }
            
            // If movement interface is active, trigger the movement
            if (this.gmi.display) {
                // if robot is on the left side of the screen, trigger EAST
                if (this.gmi.getTargetX() == 0) { // column 0
                    this.gmi.triggerMovement(Constants.EAST);
                } else {
                    this.gmi.triggerMovement(Constants.WEST);
                }
                // Consume the back event to prevent it from being processed again
                gameManager.getInputManager().resetEvents();
                return; // Exit the update method to prevent further processing
            }
            
            // Only reach here if gmi is not displayed - handle normal back behavior
            if(t != null){
                t.interrupt();
                moves = null;
                t = null;
            }
            gameManager.setGameScreen(Constants.SCREEN_START);
            gameManager.getInputManager().resetEvents();
        }

        if(!isSolved && solver.getSolverStatus().isFinished())
        {
            isSolved = true;
            buttonSolve.setEnabled(true);
            NumDifferentSolutionsFound=solver.getSolutionList().size();
            solution = solver.getSolution(numDifferentSolutionClicks);
            solutionMoves=0;
            for(IGameMove m : solution.getMoves()){
                solutionMoves++;
            }
            if(solver.isSolution01()){
                solutionMoves=1;
            }
            // DEBUG: save solutions directly as if played:
            // SaveManager saveManager = new SaveManager(gameManager.getActivity());
            // saveManager.saveMapCompletion(mapPath, solutionMoves, 99, 9999, 99999);
            // FileReadWrite.appendPrivateData(gameManager.getActivity(), "mapsPlayed.txt", mapPath.substring(5)+"\n");

            /*if(solutionMoves > simplePuzzleMinMoves && solutionMoves < goodPuzzleMinMoves) {
                // very simple puzzle with max 6 moves
                gameManager.requestToast("AI sais: this is a simple puzzle.", true);
            }*/
        }
    }

    @Override
    public void destroy(){
        if(t != null){
            t.interrupt();
            moves = null;
            t = null;
        }
    }

    public ArrayList<GridElement> getGridElements() {
        return gridElements;
    }

    public void setCurrentMovedSquares(int movedSquares){
        currentMovedSquares=movedSquares;
        Timber.d("store %d moved squares in last Move", currentMovedSquares);
        allMoves.get(allMoves.size()-1).setSquaresMoved(currentMovedSquares);
    }

    public void setSavedGame(String mapPath) {
        Timber.d("Loading saved game from %s", mapPath);
        this.mapPath = mapPath;  // Keep the mapPath to identify it as a saved game
        this.isGameWon = false;  // Reset game won flag
        this.isRandomGame = false;  // Loading a saved game is not a random game
        
        if (mapPath == null) {
            // If mapPath is null, start a new random game instead
            Timber.d("No saved game path provided, starting random game instead");
            setRandomGame();
            return;
        }
        
        try {
            String saveData = FileReadWrite.readPrivateData(gameManager.getActivity(), mapPath);
            if (saveData == null || saveData.isEmpty()) {
                Timber.d("Empty save data, starting random game instead");
                setRandomGame();
                return;
            }
            
            // Use true for applyBoardSize parameter to apply the board size when actually loading a game
            gridElements = MapObjects.extractDataFromString(saveData, true);
            Timber.d("Extracted gridElements size=%d", gridElements.size());
            
            if (gridElements.isEmpty()) {
                Timber.d("No grid elements extracted, starting random game instead");
                setRandomGame();
                return;
            }
            
            GridGameScreen.setMap(gridElements);
            createGrid();
            Timber.d("Saved game loaded successfully, save button disabled");
        } catch (Exception e) {
            Timber.d("Error loading saved game: %s", e.getMessage());
            e.printStackTrace();
            setRandomGame();  // Fall back to random game on error
        }
    }

    public void setLevelGame(String mapPath) {
        Timber.d("Loading level game from %s", mapPath);
        this.mapPath = mapPath;
        this.isGameWon = false;  // Reset game won flag
        initializeSolutionsAtHints();
        try {
            String saveData = FileReadWrite.readAssets(gameManager.getActivity(), mapPath);
            Timber.d("Loaded level data length=%d", saveData.length());
            // Use true for applyBoardSize parameter to apply the board size when actually loading a level
            gridElements = MapObjects.extractDataFromString(saveData, true);
            Timber.d("Extracted gridElements size=%d", gridElements.size());
            GridGameScreen.setMap(gridElements);
            numSolutionClicks = 0;
            createGrid();
            Timber.d("Level game loaded successfully, save button disabled");
        } catch (Exception e) {
            Timber.d("Error loading level game: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    public void setRandomGame() {
        Timber.d("Starting new random game");
        this.mapPath = "";  //La carte étant générée, elle n'a pas de chemin d'accès
        this.autoSaved = false;  // Reset autosave flag for new random game
        this.isGameWon = false;  // Reset game won flag
        this.isRandomGame = true;  // Set random game flag
        numSolutionClicks = 0;
        numDifferentSolutionClicks = 0;
        isHistorySaved = false;
        initializeSolutionsAtHints();
        try {
            // Reset game state
            nbCoups = 0;
            numSquares = 0;
            currentMovedSquares = 0;
            allMoves.clear();
            
            MapGenerator generatedMap = new MapGenerator();
            gridElements = generatedMap.getGeneratedGameMap();
            Timber.d("Generated gridElements size=%d", gridElements.size());
            
            createGrid();
            createRobots();  // Make sure robots are created immediately
            
            Timber.d("Random game created successfully, save button enabled");
        } catch (Exception e) {
            Timber.d("Error creating random game: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /** Creates the grid and initializes variables */
    public void createGrid() {
        this.solver = new SolverDD();

        IAMovesNumber = 0;
        isSolved = false;

        nbCoups = 0;
        numSquares = 0;
        timeCpt = 0;
        prevTime = System.currentTimeMillis();

        // Keep fixed top margin, calculate remaining space
        int availableHeight = gameManager.getScreenHeight();
        int availableWidth = gameManager.getScreenWidth() - (2 * xGrid);   // Subtract side margins

        // Calculate grid space to maximize screen usage
        float gridSpaceX = (float)availableWidth / (float)MainActivity.getBoardWidth();
        float gridSpaceY = (float)availableHeight / (float)MainActivity.getBoardHeight();
        
        // Use the larger value that still fits both dimensions
        gridSpace = Math.min(gridSpaceX, gridSpaceY);

        // Calculate total board dimensions
        int totalWidth = (int)(MainActivity.getBoardWidth() * gridSpace);
        int totalHeight = (int)(MainActivity.getBoardHeight() * gridSpace);

        // Center the grid on screen
        xGrid = (gameManager.getScreenWidth() - totalWidth) / 2;
        yGrid = topMargin;

        // Clean up old bitmap if it exists
        if (bitmapGrid != null && !bitmapGrid.isRecycled()) {
            bitmapGrid.recycle();
        }

        // Create new bitmap with current board size
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        bitmapGrid = Bitmap.createBitmap(totalWidth, totalHeight, conf);
        canvasGrid = new Canvas(bitmapGrid);

        currentRenderManager.setTarget(canvasGrid);

        // Load all drawables
        drawables.put("grid_tiles", ResourcesCompat.getDrawable(currentRenderManager.getResources(), R.drawable.grid_tiles, null)); // white background for variable sizes
        drawables.put("roboyard", ResourcesCompat.getDrawable(currentRenderManager.getResources(), R.drawable.roboyard, null)); // center roboyard in carré
        drawables.put("mh", ResourcesCompat.getDrawable(currentRenderManager.getResources(), R.drawable.mh, null)); // horizontal lines (hedge)
        drawables.put("mv", ResourcesCompat.getDrawable(currentRenderManager.getResources(), R.drawable.mv, null)); // vertical lines (hedge)

        drawables.put("robot_green", ResourcesCompat.getDrawable(currentRenderManager.getResources(), R.drawable.robot_green_right, null)); // green robot
        drawables.put("robot_red", ResourcesCompat.getDrawable(currentRenderManager.getResources(), R.drawable.robot_red_right, null)); // red
        drawables.put("robot_yellow", ResourcesCompat.getDrawable(currentRenderManager.getResources(), R.drawable.robot_yellow_right, null)); // yellow
        drawables.put("robot_blue", ResourcesCompat.getDrawable(currentRenderManager.getResources(), R.drawable.robot_blue_right, null)); // blue

        drawables.put("target_red", ResourcesCompat.getDrawable(currentRenderManager.getResources(), R.drawable.cr, null)); // ...
        drawables.put("target_blue", ResourcesCompat.getDrawable(currentRenderManager.getResources(), R.drawable.cb, null)); // ...
        drawables.put("target_green", ResourcesCompat.getDrawable(currentRenderManager.getResources(), R.drawable.cv, null)); // ...
        drawables.put("target_yellow", ResourcesCompat.getDrawable(currentRenderManager.getResources(), R.drawable.cj, null)); // ...
        drawables.put("target_multi", ResourcesCompat.getDrawable(currentRenderManager.getResources(), R.drawable.cm, null)); // ...

        // Choose appropriate grid background based on board size
        Drawable gridBackground;
        
        // all other boards loop the single grid tile, which is for one field
        Drawable fullGrid = drawables.get("grid_tiles");
        // Loop through board and draw grid tiles
        for(int x = 0; x < MainActivity.getBoardWidth(); x++) {
            for(int y = 0; y < MainActivity.getBoardHeight(); y++) {
                fullGrid.setBounds(
                    (int)(x * gridSpace),
                    (int)(y * gridSpace),
                    (int)((x + 1) * gridSpace),
                    (int)((y + 1) * gridSpace)
                );
                fullGrid.draw(canvasGrid);
            }
        }

        // Draw Roboyard logo in center
        drawables.get("roboyard").setBounds(
            (int)((MainActivity.getBoardWidth()/2 - 1)*gridSpace),
            (int)((MainActivity.getBoardHeight()/2 - 1)*gridSpace),
            (int)((MainActivity.getBoardWidth()/2 + 1)*gridSpace),
            (int)((MainActivity.getBoardHeight()/2 + 1)*gridSpace)
        );
        drawables.get("roboyard").draw(canvasGrid);

        // draw targets
        for (Object element : gridElements) {
            GridElement myp = (GridElement) element;

            if (myp.getType().equals("target_red") || myp.getType().equals("target_green") || myp.getType().equals("target_yellow") || myp.getType().equals("target_blue") || myp.getType().equals("target_multi")) {
                drawables.get(myp.getType()).setBounds((int)(myp.getX() * gridSpace),(int)( myp.getY() * gridSpace),(int)( (myp.getX() + 1) * gridSpace),(int)( (myp.getY()+1) * gridSpace));
                drawables.get(myp.getType()).draw(canvasGrid);
            }
        }

        // Calculate wall dimensions based on grid size
        int pixel = Math.max(1, (int)(gridSpace / 45)); // ensure minimum thickness of 1 pixel (equivalent to a pixel on a 720x1280 screen)
        int stretchWall = 12 * pixel; // strech all walls
        int offsetWall = -2 * pixel;
        int wallThickness = 16 * pixel; // thickness of walls

        for (Object element : gridElements) {
            GridElement myp = (GridElement) element;

            // add horizontal lines
            if (myp.getType().equals("mh")) {
                drawables.get("mh").setBounds((int)(myp.getX() * gridSpace - stretchWall), // left x
                (int)(myp.getY() * gridSpace - stretchWall + offsetWall), // left y
                (int)((myp.getX() + 1) * gridSpace + stretchWall), // right x
                (int)(myp.getY() * gridSpace + wallThickness + offsetWall)); // right y
                drawables.get("mh").draw(canvasGrid);
            }
            
            // add vertical lines
            if (myp.getType().equals("mv")) {
                drawables.get("mv").setBounds((int)(myp.getX() * gridSpace - stretchWall + offsetWall), // left x
                (int)(myp.getY() * gridSpace - stretchWall), // left y
                (int)(myp.getX() * gridSpace + wallThickness + offsetWall), // right x
                (int)((myp.getY() + 1) * gridSpace + stretchWall) // right y
                );
                drawables.get("mv").draw(canvasGrid);
            }
            
            // add small robots underneath as marker for each start position
            if (myp.getType().startsWith("robot_")) {
                drawables.get(myp.getType()).setBounds(
                    (int)(myp.getX() * gridSpace),
                    (int)(myp.getY() * gridSpace),
                    (int)((myp.getX() + 1) * gridSpace),
                    (int)((myp.getY() + 1) * gridSpace)
                    );
                drawables.get(myp.getType()).draw(canvasGrid);
            }
        }

        currentRenderManager.resetTarget();

        // We delete the background image if it exists and save the one we just created
        if(imageLoaded == true)
        {
            currentRenderManager.unloadBitmap(imageGridID);
        }
        imageGridID = currentRenderManager.loadBitmap(bitmapGrid);
        imageLoaded = true;

        // add robots
        createRobots();

        this.solver.init(gridElements);

        buttonSolve.setEnabled(false);
        t = new Thread(solver, "solver");
        t.start();

    }

    public void createRobots()
    {
        colors.put("robot_red", Color.RED);
        colors.put("robot_blue", Color.BLUE);
        colors.put("robot_green", Color.GREEN);
        colors.put("robot_yellow", Color.YELLOW);
        
        colors.put("target_red", Color.RED);
        colors.put("target_blue", Color.BLUE);
        colors.put("target_green", Color.GREEN);
        colors.put("target_yellow", Color.YELLOW);
        colors.put("target_multi", 0);      // no color

        ArrayList<GamePiece> aRemove = new ArrayList<>();
        for(Object currentObject : this.instances)
        {
            if(currentObject.getClass() == GamePiece.class)
            {
                aRemove.add((GamePiece)currentObject);
            }
        }
        for(GamePiece p : aRemove)
        {
            this.instances.remove(p);
        }

        for (Object element : gridElements) {
            GridElement myp = (GridElement) element;

            // add robots
            if (myp.getType().startsWith("robot_")) {
                GamePiece currentPiece = new GamePiece(myp.getX(), myp.getY(), colors.get(myp.getType()));
                currentPiece.setGridDimensions(xGrid, yGrid, gridSpace);

                this.instances.add(currentPiece);
            }
        }

    }

    public void activateInterface(GamePiece p, int x, int y){
        gmi.enable(true);
        gmi.setPosition(x-1, y);
        gmi.setTarget(p);
    }

    public void editDestination(GamePiece p, int direction, Boolean moved)
    {
        int xDestination = p.getxObjective();
        int yDestination = p.getyObjective();

        boolean canMove = true;

        if(!moved) {
            Move currentMove = new Move(p, p.getxObjective(), p.getyObjective());
            allMoves.add(currentMove);
        }

        for(Object instance : this.instances)
        {
            if(instance.getClass() == GamePiece.class)
            {
                if(instance != p && canMove)
                {
                    switch(direction){
                        case 0:     // haut
                            canMove = collision((GamePiece) instance, xDestination, yDestination - 1, canMove);
                            break;
                        case 1:     // droite
                            canMove = collision((GamePiece) instance, xDestination+1, yDestination, canMove);
                            break;
                        case 2:     // bas
                            canMove = collision((GamePiece) instance, xDestination, yDestination + 1, canMove);
                            break;
                        case 3:     // gauche
                            canMove = collision((GamePiece) instance, xDestination-1, yDestination, canMove);
                            break;
                    }
                }
            }
        }

        for (Object element : gridElements) {
            GridElement myp = (GridElement) element;

            if ((myp.getType().equals("mv")) && (direction == 1)) {  // droite
                canMove = collision(p, myp.getX() - 1, myp.getY(), canMove);
            }
            if ((myp.getType().equals("mv")) && (direction == 3)) {  // gauche
                canMove = collision(p, myp.getX(), myp.getY(), canMove);
            }
            if ((myp.getType().equals("mh")) && (direction == 0)) {  // haut
                canMove = collision(p, myp.getX(), myp.getY(), canMove);
            }
            if ((myp.getType().equals("mh")) && (direction == 2)) {  // bas
                canMove = collision(p, myp.getX(), myp.getY() - 1, canMove);
            }
        }

        if(canMove){
            switch(direction){
                case 0:     // haut
                    yDestination -= 1;
                    break;
                case 1:     // droite
                    xDestination +=1;
                    break;
                case 2:     // bas
                    yDestination += 1;
                    break;
                case 3:     // gauche
                    xDestination -=1;
                    break;
            }
            p.setxObjective(xDestination);
            p.setyObjective(yDestination);

            p.setDirection(direction);

            editDestination(p, direction, true);
            numSquares++;
        }else{
            if(moved){
                nbCoups++;
                //boolean b = win(p);
            } else {
                allMoves.remove(allMoves.size()-1);
            }
        }
    }

    public boolean win(GamePiece p)
    {
        if(!p.testIfWon) {
            return false;  // Don't check win condition if we already won
        }

        for (Object element : gridElements) {
            GridElement myp = (GridElement) element;
            {
                if (myp.getType().equals("target_multi") && myp.getX() == p.getX() && myp.getY() == p.getY()) {
                    p.testIfWon = false;  // Prevent further win checks
                    sayWon();
                    return true;
                }
                else if((myp.getX() == p.getX()) && (myp.getY() == p.getY()) && (myp.getType().equals("target_red") || myp.getType().equals("target_green") || myp.getType().equals("target_blue") || myp.getType().equals("target_yellow"))) {
                    if(p.getColor() == colors.get((myp.getType())))
                    {
                        p.testIfWon = false;  // Prevent further win checks
                        sayWon();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void sayWon()
    {
        if(isGameWon) {
            return;  // Don't show win message again
        }
        isGameWon = true;

        SaveManager saveManager = new SaveManager(gameManager.getActivity());
        if(IAMovesNumber > 0)
        {
            gameManager.requestToast("The AI found a solution in " + IAMovesNumber + " moves.", true);
        }
        else
        {
            gameManager.requestToast("You won in "+nbCoups+" moves, "+numSquares+" squares", true);
            // Save completion data
            saveManager.saveMapCompletion(mapPath, solutionMoves, nbCoups, numSquares, timeCpt);

            // Save to history
            Timber.d("Game won, updating history entry");
            
            // If the game already has a history entry, update it
            // otherwise create a new entry
            if (isHistorySaved && currentHistoryIndex >= 0) {
                updateHistoryEntry();
            } else {
                saveToHistory();
                isHistorySaved = true;
            }
        }
        LevelChoiceGameScreen.invalidateMapCache(mapPath); // Invalidate only this specific map in the cache
        updatePlayedMaps();
    }

    private void updatePlayedMaps()
    {
        // Timber.d(" updatePlayedMaps: " + mapPath);
        if(mapPath.length() > 0) {
            addMapsPlayed();
            SparseArray<GameScreen> screens = gameManager.getScreens();
            GameButton lastButton = LevelChoiceGameScreen.getLastButtonUsed();
            if (lastButton != null) {
                lastButton.setImageUp(ResourcesCompat.getDrawable(gameManager.getActivity().getResources(), R.drawable.bt_start_up_played, null));
                lastButton.setImageDown(ResourcesCompat.getDrawable(gameManager.getActivity().getResources(), R.drawable.bt_start_down_played, null));
            }
            /*
            for (int i = 0; i < screens.size(); i++) {
                if (screens.get(i).getClass() == LevelChoiceGameScreen.class) {
                    ((LevelChoiceGameScreen) screens.get(i)).createButtons();
                }
            }*/
        }
    }


    /**
     * Adds the current map to the list of maps played.
     */
    public void addMapsPlayed()
    {
        // Timber.d(" addMapsPlayed: " + mapPath);
        if(mapPath.length() > 0)
        {
            SaveManager saver = new SaveManager(gameManager.getActivity());

            if(!saver.getMapsStateSaved(mapPath, "mapsPlayed.txt"))
            {
                FileReadWrite.appendPrivateData(gameManager.getActivity(), "mapsPlayed.txt", mapPath.substring(5)+"\n");
            }
        }
    }

    public Boolean collision(GamePiece p, int x, int y, boolean canMove)
    {
        if(p.getxObjective() == x && p.getyObjective() == y && canMove == true) {
            return false;
        } else return canMove != false;
    }

    public boolean getRobotsTouching() {
        for(Object currentObject : this.instances)
        {
            if(currentObject.getClass() == GamePiece.class)
            {
                for(Object currentObject2 : this.instances)
                {
                    if(currentObject2.getClass() == GamePiece.class)
                    {
                        if(currentObject != currentObject2)
                        {
                            // if the difference between currentObject x and currentObject2 x is equal to 1 or -1 or y
                            if(Math.abs(((GamePiece) currentObject).getX() - ((GamePiece) currentObject2).getX()) <= 1 && Math.abs(((GamePiece) currentObject).getY() - ((GamePiece) currentObject2).getY()) <= 1)
                            {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Button to reset all robots back to their 
     * original positions
     */
    private class ButtonResetRobots implements IExecutor{

        public void execute(){
            // Reset game state
            nbCoups = 0;
            numSquares = 0;
            currentMovedSquares = 0;
            allMoves.clear();
            
            // Recreate grid and robots with current settings
            createGrid();
            createRobots();
            
            Timber.d("Game restarted");
        }
    }

    /**
     * Button to Start a new random level (next button)
     * sets mustStartNext to true
     */
    private class ButtonNext implements IExecutor{
        private static final long NEXT_BUTTON_COOLDOWN = 500; // 500ms cooldown between next button clicks
        private long lastNextButtonClickTime = 0;

        public void execute() {
            // Check if enough time has passed since last click
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastNextButtonClickTime < NEXT_BUTTON_COOLDOWN) {
                return;
            }
            lastNextButtonClickTime = currentTime;

            // Only proceed to next level if we're in the Maps/ directory
            if (mapPath != null && mapPath.startsWith("Maps/")) {
                int currentLevel = extractLevelNumber(mapPath);
                if (currentLevel >= 1 && currentLevel < 140 && currentLevel != 35 && currentLevel != 70 && currentLevel != 105) {
                    // Next level in sequence
                    String nextMapPath = "Maps/level_" + (currentLevel + 1) + ".txt";
                    setLevelGame(nextMapPath);
                    isRandomGame = false;
                } else {
                    // For invalid level numbers, generate a new random game
                    setRandomGame();
                    isRandomGame = true;
                }
            } else {
                // For random games, saved games, or no map path
                setRandomGame();
                isRandomGame = true;
            }
        }
    }

    private class ButtonSolution implements IExecutor{
        public void execute(){
            // Prevent rapid clicking while toast is showing or solver is running
            if (!isSolved || t == null || t.isAlive()) {
                return;
            }
            
            // Check if we have valid solutions
            if (solver == null || solver.getSolutionList() == null || solver.getSolutionList().isEmpty()) {
                return;
            }

            Boolean isLevelGame = false;
            if (mapPath != null && mapPath.startsWith("Maps/")) {
                isLevelGame = true;
            }
            if (numSolutionClicks >= showSolutionAtHint) {
                // if levelgame, don't show the solution
                if (!isLevelGame) {
                    try {
                        GameSolution solution = solver.getSolution(numDifferentSolutionClicks);
                        if (solution != null) {
                            showSolution(solution);
                            if (NumDifferentSolutionsFound > 1) {
                                // if solution is shown and there are more than 1 solutions found:
                                if (numDifferentSolutionClicks >= NumDifferentSolutionsFound - 1) {
                                    numDifferentSolutionClicks = 0;
                                } else {
                                    numDifferentSolutionClicks++;
                                }
                                gameManager.requestToast("Press again to see solution " + (numDifferentSolutionClicks + 1), false);
                            }
                        }
                    } catch (Exception e) {
                        // Reset state if something goes wrong
                        numSolutionClicks = 0;
                        numDifferentSolutionClicks = 0;
                    }
                }
            } else {
                numSolutionClicks++;
                if (numSolutionClicks < showSolutionAtHint) {
                    gameManager.requestToast("Press again to see the next hint.", false);
                } else {
                    if (!isLevelGame) {
                        gameManager.requestToast("Press again to see the solution.", false);
                    }else{
                        gameManager.requestToast("In Level games you have to find the solution.", false);
                    }
                }
            }
        }
    }

    public void doMovesInMemory()
    {

        if(moves != null)
        {

            if(moves.size() > 0)
            {

                IGameMove move = moves.get(0);

                if(move.getClass() == RRGameMove.class)
                {

                    for (Object currentObject : this.instances)
                    {
                        if(currentObject.getClass() == GamePiece.class)
                        {
                            if(((GamePiece)currentObject).getColor() == ((RRGameMove) move).getColor())
                            {
                                editDestination(((GamePiece) currentObject), translateIADirectionToGameDirection(((RRGameMove) move).getDirection()), false);
                            }
                        }
                    }
                }
                moves.remove(0);
            }
        }
    }

    private void showSolution(GameSolution solution)
    {
        ButtonResetRobots br = new ButtonResetRobots();
        br.execute();

        nbCoups = 0;
        numSquares = 0;
        allMoves.clear();

        moves = solution.getMoves();
        IAMovesNumber = moves.size();

        doMovesInMemory();
    }

    private int translateIADirectionToGameDirection(int IADirection)
    {
        switch(IADirection){
            case 1:
                return 0;
            case 2:
                return 1;
            case 4:
                return 2;
            case 8:
                return 3;
            default:
                return -1;
        }
    }

    private class ButtonBack implements IExecutor{
        public void execute(){
            if(allMoves.size() > 0) {
                int last = allMoves.size()-1;
                Move lastMove = allMoves.get(last);
                numSquares -= lastMove.getSquaresMoved();
                Timber.d("substract %d", lastMove.getSquaresMoved());
                lastMove.goBack();
                Timber.d("remove move nr. %d %d/%d", (allMoves.size()-1), lastMove._x, lastMove._y);
                allMoves.remove(last);
                nbCoups--;
            }
        }
    }

    /**
     * Extracts the level number from a map path.
     * @param path The map path (format: level_X.txt)
     * @return The level number, or -1 if invalid
     */
    private int extractLevelNumber(String path) {
        if (path == null || path.isEmpty()) {
            return -1;
        }
        try {
            int start = path.lastIndexOf("_") + 1;
            int end = path.lastIndexOf(".");
            if (start > 0 && end > start) {
                return Integer.parseInt(path.substring(start, end));
            }
        } catch (Exception e) {
            // If parsing fails, return -1
        }
        return -1;
    }

    /**
     * Save the current game state to history
     * 
     * @return boolean True if save was successful
     */
    public boolean saveToHistory() {
        try {
            Timber.d("Saving game to history after %d seconds of play", timeCpt);
            
            int historyIndex = GameHistoryManager.getHighestHistoryIndex(gameManager.getActivity()) + 1;
            String historyFileName = GameHistoryManager.indexToPath(historyIndex);

            // save current history index for later updates
            currentHistoryIndex = historyIndex;
            
            // Create game name based on map name or generated index
            String gameName = mapName;
            if (gameName == null || gameName.isEmpty()) {
                gameName = "Game " + historyIndex;
            }
            
            // Build JSON data to save
            StringBuilder saveData = new StringBuilder();
            
            // Add board name
            saveData.append("name:").append(gameName).append(";");
            
            // Add timestamp
            saveData.append("timestamp:").append(System.currentTimeMillis()).append(";");
            
            // Add play duration
            saveData.append("duration:").append(timeCpt).append(";");
            
            // Add number of moves the player made
            saveData.append("moves:").append(nbCoups).append(";");
            
            // Add number of optimal moves if available
            if (solutionMoves > 0) {
                saveData.append("num_moves:").append(solutionMoves).append(";");
            }
            
            // Add board size
            saveData.append("board:").append(MainActivity.getBoardWidth()).append(",")
                   .append(MainActivity.getBoardHeight()).append(";");
            
            // Add the grid elements data using the existing method
            saveData.append(MapObjects.createStringFromList(gridElements, false));
            
            // Write save data - use just the filename, not the path
            FileReadWrite.writePrivateData(gameManager.getActivity(), historyFileName, saveData.toString());
            
            // Create preview image path
            String previewFileName = "history_" + historyIndex + "_preview.png";
            
            // Create and save history entry metadata
            GameHistoryEntry entry = new GameHistoryEntry(
                historyFileName,
                gameName,
                System.currentTimeMillis(),
                timeCpt,
                nbCoups,
                solutionMoves,
                MainActivity.getBoardWidth() + "x" + MainActivity.getBoardHeight(),
                previewFileName
            );
            
            // save original map path if available
            if (mapPath != null && !mapPath.isEmpty()) {
                entry.setOriginalMapPath(mapPath);
            }
            
            // Add entry to history index
            Boolean historySaved = GameHistoryManager.addHistoryEntry(gameManager.getActivity(), entry);

            Timber.d("Game saved to history: %s", historyFileName);
            return historySaved;
        } catch (Exception e) {
            Timber.e("Error saving game to history: %s", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update an existing history entry with the current game state
     */
    private void updateHistoryEntry() {
        try {
            // Only update if we have a current history entry
            if (currentHistoryIndex < 0) {
                Timber.d("No current history entry to update, currentHistoryIndex=%d", currentHistoryIndex);
                
                // We have isHistorySaved=true but no valid index, create a new entry instead
                if (isHistorySaved) {
                    Timber.d("History flag is true but index is invalid, creating new history entry");
                    saveToHistory();
                }
                return;
            }
            
            // Update the history entry
            GameHistoryEntry entry = GameHistoryManager.getHistoryEntry(
                gameManager.getActivity(), currentHistoryIndex);
            
            if (entry == null) {
                Timber.e("History entry is null for index: %d", currentHistoryIndex);
                // If entry is null, try to create a new history entry
                Timber.d("Attempting to create a new history entry instead");
                saveToHistory();
                return;
            }
            
            // Update the values
            entry.setMovesMade(nbCoups);
            entry.setOptimalMoves(solutionMoves);
            entry.setPlayDuration(timeCpt);

            // Save the updated entry
            GameHistoryManager.updateHistoryEntry(gameManager.getActivity(), entry);
            
            Timber.d("Updated history entry: history_%d.txt", currentHistoryIndex);
        } catch (Exception e) {
            Timber.e(e, "Error updating history entry");
        }
    }

    /**
     * Get the map data as a string
     * @return The map data string
     */
    public String getMapData() {
        // This method is used for testing
        // In a real implementation, it would build the map data string from the grid elements
        StringBuilder saveData = new StringBuilder();
        
        // Add board name
        String gameName = mapName != null ? mapName : "Game " + currentHistoryIndex;
        saveData.append("name:").append(gameName).append(";");
        
        // Add timestamp
        saveData.append("timestamp:").append(System.currentTimeMillis()).append(";");
        
        // Add number of optimal moves if available
        if (solutionMoves > 0) {
            saveData.append("num_moves:").append(solutionMoves).append(";");
        }
        
        // Add solution if available 
        if (solution != null) {
            StringBuilder solutionStr = new StringBuilder("solution:");
            saveData.append(solutionStr);
        }

        // Add the grid elements data
        saveData.append(MapObjects.createStringFromList(gridElements, false));
        
        return saveData.toString();
    }
    
    /**
     * Check if this is a random game
     * @return true if random game, false otherwise
     */
    public boolean isRandomGame() {
        return isRandomGame;
    }
    
    /**
     * Get the map name
     * @return The map name
     */
    public String getMapName() {
        return mapName;
    }
}
