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
    private int solutionMoves = 0; // store the optimal number of moves for the solution globally
    private int NumDifferentSolutionsFound = 0; // store the number of different solution globally
    private int numSolutionClicks = 0; // count how often you clicked on the solution button, each time the shown count goes down by one
    private int numDifferentSolutionClicks = 0; // count how often you clicked on the solution button again to show a different solution
    private int showSolutionAtHint = 5; // interval between the first hint and the current optimal solution (will be set to random 3..5 later

    private static int goodPuzzleMinMoves = 8; // below this number of moves there is a special hint shown from the start
    private static int simplePuzzleMinMoves = 6; // below this threshold a new puzzle is generated

    private static String requestToast = ""; // this can be set from outside to set the text for a popup

    private ISolver solver;

    private boolean autoSaved = false;

    private ArrayList<GridElement> gridElements;
    private int imageGridID;
    private boolean imageLoaded = false;

    private String mapPath = "";

    private int xGrid;
    private int yGrid;
    private int boardHeight;

    private float gridSpace; // gamescreen width / boardSizeX
    private int gridBottom;
    private int topMargin = 16;
    private int timeCpt = 0;
    private int nbCoups = 0;
    private int numSquares = 0;
    private int currentMovedSquares = 0;
    private long prevTime;

    private static String levelDifficulty="Beginner";
    private static ArrayList<GridElement> currentMap;

    private int IAMovesNumber = 0;

    private boolean mustStartNext = false;

    private ArrayList<IGameMove> moves = null;

    private Thread t = null;

    private GameMovementInterface gmi;
    private Bitmap bitmapGrid;
    final RenderManager currentRenderManager;
    final Map<String, Drawable> drawables = new HashMap<>();
    final Map<String, Integer> colors = new HashMap<>();
    private final ArrayList<Move> allMoves= new ArrayList<>();

    private GameButtonGeneral buttonSolve;
    private GameButtonGoto buttonSave;

    private final Preferences preferences = new Preferences();

    private boolean isGameWon = false;

    boolean isRandomGame = false;

    private int textColorHighlight = Color.parseColor("#aaaaaa");
    private int textColorNormal = Color.GRAY;

    public boolean isRandomGame() {
        return isRandomGame;
    }

    public GridGameScreen(GameManager gameManager){
        super(gameManager);
        String ld=preferences.getPreferenceValue(gameManager.getActivity(), "difficulty");
        if(ld.equals("")){
            // default difficulty
            ld="Beginner";
            preferences.setPreferences(gameManager.getActivity(),"difficulty", ld);
        }
        setDifficulty(ld);
        
        // Initialize with default state
        isRandomGame = false;  // Start in non-random mode
        gridSpace = (float)gameManager.getScreenWidth() / (float)MainActivity.getBoardWidth();
        xGrid = 0;
        yGrid = topMargin; // 2px margin from top
        gridBottom = yGrid + (int)((MainActivity.getBoardHeight() + 1) * gridSpace);

        Bitmap.Config conf = Bitmap.Config.ARGB_8888;

        bitmapGrid = Bitmap.createBitmap((int)(MainActivity.getBoardWidth() * gridSpace), (int) (MainActivity.getBoardHeight() * gridSpace), conf);
        canvasGrid = new Canvas(bitmapGrid);
        currentRenderManager = gameManager.getRenderManager();

        prevTime = System.currentTimeMillis();

        gameManager.getRenderManager().loadImage(R.drawable.robot_yellow_right);
        gameManager.getRenderManager().loadImage(R.drawable.robot_blue_right);
        gameManager.getRenderManager().loadImage(R.drawable.robot_green_right);
        gameManager.getRenderManager().loadImage(R.drawable.robot_red_right);

        // robots facing left:
        gameManager.getRenderManager().loadImage(R.drawable.robot_yellow_left);
        gameManager.getRenderManager().loadImage(R.drawable.robot_blue_left);
        gameManager.getRenderManager().loadImage(R.drawable.robot_green_left);
        gameManager.getRenderManager().loadImage(R.drawable.robot_red_left);

    }

    public static String getLevel() {
        return GridGameScreen.levelDifficulty;
    }

    public static void setDifficulty(String levelDifficulty) {
        GridGameScreen.levelDifficulty = levelDifficulty;
        if(levelDifficulty.equals("Beginner")) {
            GridGameScreen.goodPuzzleMinMoves = 6;
            GridGameScreen.simplePuzzleMinMoves = 4;
        } else if(levelDifficulty.equals("Advanced")) {
            GridGameScreen.goodPuzzleMinMoves = 8;
            GridGameScreen.simplePuzzleMinMoves = 6;
        } else if(levelDifficulty.equals("Insane")) {
            GridGameScreen.goodPuzzleMinMoves = 10;
            GridGameScreen.simplePuzzleMinMoves = 10;
        } else if(levelDifficulty.equals("Impossible")) {
            GridGameScreen.goodPuzzleMinMoves = 17;
            GridGameScreen.simplePuzzleMinMoves = 17;
            for (int i = 0; i < 3; i++) {
                // repeat three times to get a long toast
                GridGameScreen.requestToast = "Level Impossible will generate a fitting puzzle. This can take a while. In case the solver gets stuck, press >>";
            }
        }
    }

    public static ArrayList<GridElement> getMap() {
        return GridGameScreen.currentMap;
    }

    public static void setMap(ArrayList<GridElement> data){
        GridGameScreen.currentMap = data;
    }

    @Override
    public void create() {
        gmi = new GameMovementInterface(gameManager);

        xGrid = 0;
        yGrid = 0;

        int visibleScreenHeight = gameManager.getScreenHeight(); // bei 720x1280:1184px

        int y = yGrid + gameManager.getScreenWidth();
        int dy = visibleScreenHeight - y; // 248
        int buttonW = gameManager.getScreenWidth() / 4;

        // TODO: make this depending on the screen size:
        float ratioW = ((float) gameManager.getScreenWidth()) / ((float) 1080); // at 720x1280:0.6667 at 1440x2580:1.333
        float ratioH = ((float) visibleScreenHeight) / ((float) 1920); // at 720x1280:0.61667 at 1440x2580:2.45
        // int buttonPosY = (int)(6.5*dy * ratioH);
        int buttonPosY = (int) (gameManager.getScreenHeight() * 0.85f); // 1060
        int nextButtonDim = (int) (220 * ratioH);

        // Button Next game (top right) (new randomgame) sets mustStartNext to true
        int currentLevel = extractLevelNumber(mapPath);
        if (currentLevel < 140) { // Show next button for random games or levels below 140
            boardHeight = (int) (visibleScreenHeight * 0.669);
            this.instances.add(new GameButtonGeneral((int) (870 * ratioW), boardHeight, nextButtonDim, nextButtonDim, R.drawable.bt_next_up, R.drawable.bt_next_down, new ButtonNext()));
        }

        // Button Save
        gameManager.getRenderManager().loadImage(R.drawable.transparent);
        buttonSave = new GameButtonGoto(0, buttonPosY, buttonW,buttonW, R.drawable.bt_jeu_save_up, R.drawable.bt_jeu_save_down, 9);
        buttonSave.setImageDisabled(R.drawable.transparent);
        // save button will be disabled when playing a saved game
        buttonSave.setEnabled(false);
        this.instances.add(buttonSave);

        // Button one step back
        this.instances.add(new GameButtonGeneral(buttonW, buttonPosY, buttonW,buttonW, R.drawable.bt_jeu_retour_up, R.drawable.bt_jeu_retour_down, new ButtonBack()));

        // Button restart
        this.instances.add(new GameButtonGeneral(buttonW*2, buttonPosY, buttonW,buttonW, R.drawable.bt_jeu_reset_up, R.drawable.bt_jeu_reset_down, new ButtonRestart()));

        // Button Solve
        gameManager.getRenderManager().loadImage(R.drawable.bt_jeu_resolution_disabled);
        buttonSolve = new GameButtonGeneral(buttonW*3, buttonPosY, buttonW,buttonW, R.drawable.bt_jeu_resolution_up, R.drawable.bt_jeu_resolution_down, new ButtonSolution());
        buttonSolve.setImageDisabled(R.drawable.bt_jeu_resolution_disabled);
        buttonSolve.setEnabled(false);
        this.instances.add(buttonSolve);

        this.solver = new SolverDD();
    }

    /**
     * possibility to disable the saveButton from outside in GameButtonGotoSavedGame.java
     * @param status set false to disable
     */
    public void buttonSaveSetEnabled(boolean status){
        buttonSave.setEnabled(status);
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
        int lineHeight = (int) (ratio * 65);
        int lineHeightSmall = (int) (lineHeight * 0.8);
        int textPosY = (int)(gridBottom + (ratio * 13)); // Add margin below the game grid
        int textPosYSmall = (int)(gridBottom + 2 * lineHeight - (ratio * 53f));
        int textPosYTime = (int)(textPosYSmall + lineHeightSmall + (ratio * 21f));
        renderManager.setTextSize(lineHeight);
        if (gameManager.getScreenWidth() <= 480) {
            renderManager.setTextSize(lineHeightSmall);
        }
        if (isSolved && nbCoups == 0 && NumDifferentSolutionsFound > 1) {
            // show number of different solutions found
            renderManager.setTextSize(lineHeightSmall);
            renderManager.setColor(textColorNormal);
            renderManager.drawText(10, textPosYSmall, NumDifferentSolutionsFound + " solutions found");
            renderManager.setTextSize(lineHeight);
        }
        if(levelNum >= 1 && numSolutionClicks == 0){
            // in Level game always show the first solution
            numSolutionClicks = 1;
        }
        renderManager.setColor(textColorNormal);
        if (nbCoups > 0) {
            // at least one move was made by hand or by AI
            renderManager.setColor(textColorHighlight);
            renderManager.drawText(10, textPosY, "Moves: " + nbCoups);
            renderManager.setColor(textColorNormal);
            renderManager.setTextSize(lineHeightSmall);
            renderManager.drawText(10, textPosYSmall, "Squares: " + numSquares);
        } else if (isSolved && numSolutionClicks > 0) {
            // show solution
            renderManager.setColor(textColorHighlight);
            if (numSolutionClicks - showSolutionAtHint >= 0) {
                renderManager.drawText(10, textPosY, "AI solution: ");
                
                // Draw number in larger font
                renderManager.setColor(Color.WHITE);
                renderManager.setTextSize((int) (lineHeight * 1.4f));
                int larger9 = solutionMoves <= 9?0:27;
                renderManager.drawText(10 + (int)(ratio * (350 - larger9 )), textPosY+5, String.valueOf(solutionMoves));

                renderManager.setColor(textColorHighlight);
                renderManager.setTextSize(lineHeight);
                renderManager.drawText(10 + (int)(ratio * 415), textPosY, " moves");
            } else {
                renderManager.drawText(10, textPosY, "AI Hint " + numSolutionClicks + ": < " + (solutionMoves + showSolutionAtHint - numSolutionClicks) + " moves");
            }
        } else if (nbCoups == 0 && isSolved && solutionMoves < simplePuzzleMinMoves) {
            // too simple ... restart
            renderManager.setColor(textColorHighlight);
            renderManager.drawText(10, textPosY, "AI solution: " + solutionMoves + " moves");
            renderManager.setColor(textColorNormal);
            renderManager.setTextSize(lineHeightSmall);
            renderManager.drawText(10, textPosYSmall, "... restarting!");
            if (timeCpt > 5) {
                // show a popup on restart if it took very long to solve but found a too simple solution
                requestToast = "Finally solved in " + solutionMoves + " moves. Restarting...";
            }
            mustStartNext = true;
        } else if (nbCoups == 0 && isSolved && solutionMoves < goodPuzzleMinMoves) {
            // still simple, show a hint that this is solved with less than ... moves
            // TODO: change font (still crashes):
            //  renderManager.drawText(10, textPosY, "Number of moves < " + goodPuzzleMinMoves, "FiraMono-Bold", gameManager.getActivity());
            renderManager.setColor(textColorHighlight);
            renderManager.drawText(10, textPosY, "Number of moves < " + goodPuzzleMinMoves);
            showSolutionAtHint = goodPuzzleMinMoves - solutionMoves;
        } else if (!isSolved) {
            if (timeCpt < 1) {
                // the first second it pretends to generate the map :)
                // in real it is still calculating the solution
                renderManager.setColor(textColorNormal);
                if (levelNum >= 1){
                    renderManager.drawText(10, textPosY, "Loading map...");
                }else{
                    renderManager.drawText(10, textPosY, "Generating map...");
                }
            } else {
                // in Beginner mode it will create a new puzzle, if it is not solvable within one second
                renderManager.setColor(textColorNormal);
                if (getLevel().equals("Beginner") && levelNum< 0) {
                    renderManager.drawText(10, textPosY, "Too complicated");
                    renderManager.drawText(10, textPosYSmall, "... restarting!");
                    mustStartNext = true;
                } else {
                    renderManager.drawText(10, textPosY, "AI solving...");
                }
            }
        }
        int seconds = timeCpt % 60;
        String secondsS = Integer.toString(seconds);
        if (seconds < 10) {
            secondsS = "0" + secondsS;
        }
        renderManager.setTextSize(lineHeightSmall);
        renderManager.setColor(textColorNormal);
        renderManager.drawText(10, textPosYTime, "Time: " + timeCpt / 60 + ":" + secondsS);

        if (timeCpt >= 40 && autoSaved == false && mustStartNext == false && mapPath != null && !mapPath.isEmpty()) {
            Timber.d("Starting autosave. mapPath=%s, timeCpt=%d, autoSaved=%b, mustStartNext=%b", mapPath, timeCpt, autoSaved, mustStartNext);
            // save autosave in slot 0
            ArrayList<GridElement> gridElements = getGridElements();
            String autosaveMapPath = SaveGameScreen.getMapPath(0);
            Timber.d("Autosave path=%s, gridElements size=%d", autosaveMapPath, gridElements.size());
            try {
                FileReadWrite.clearPrivateData(gameManager.getActivity(), autosaveMapPath);
                String saveData = MapObjects.createStringFromList(gridElements, false);
                Timber.d("Save data length=%d", saveData.length());
                FileReadWrite.appendPrivateData(gameManager.getActivity(), autosaveMapPath, saveData);
                // Also add to mapsSaved.txt if not already there
                SaveManager saver = new SaveManager(gameManager.getActivity());
                if (!saver.getMapsStateSaved(autosaveMapPath, "mapsSaved.txt")) {
                    Timber.d("Adding autosave to mapsSaved.txt");
                    FileReadWrite.appendPrivateData(gameManager.getActivity(), "mapsSaved.txt", autosaveMapPath + "\n");
                }
                gameManager.requestToast("Autosaving...", false);
                autoSaved = true;
                Timber.d("Autosave completed successfully");
            } catch (Exception e) {
                Timber.d("Error during autosave: %s", e.getMessage());
                e.printStackTrace();
            }
        }

        // Display level number if it's a level game
        renderManager.setColor(textColorNormal);
        renderManager.setTextSize(lineHeight / 2);
        int levelNamePos =  boardHeight + (int) (lineHeight * 3.5f);
        Boolean isLevelGame = false;
        if (mapPath != null && mapPath.startsWith("Maps/")) {
            isLevelGame = true;
            // Level number underneath the next button
            renderManager.drawText((int) (gameManager.getScreenWidth() - ratio * 165), levelNamePos, "Level " + levelNum);
        } else {
            // show the unique string for the current map like in the save game
            String uniqueString = MapObjects.createStringFromList(gridElements, true);
            renderManager.drawText((int) (gameManager.getScreenWidth() - ratio * 155), levelNamePos, uniqueString);
        }

        if (imageLoaded) {
            gameManager.getRenderManager().drawImage(xGrid, yGrid, (int) (MainActivity.getBoardWidth() * gridSpace) + xGrid, (int) (MainActivity.getBoardHeight() * gridSpace) + yGrid, imageGridID);
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

    public void update(GameManager gameManager){
        super.update(gameManager);

        if(mustStartNext) {

            numSolutionClicks = 0;
            currentMovedSquares = 0;

            // show solution as the 2nd to 5th hint
            showSolutionAtHint = 2 + (int)(Math.random() * ((5 - 2) + 1));
            allMoves.clear();
            autoSaved = false;

            buttonSave.setEnabled(false);

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
        if(System.currentTimeMillis() - prevTime > 1000L){
            // update time every second
            timeCpt++;
            prevTime = System.currentTimeMillis();
        }
        this.gmi.update(gameManager);
        if(gameManager.getInputManager().backOccurred()){
            // If movement interface is active, trigger the movement
            if (this.gmi.display) {
                // if robot is on the left side of the screen, trigger EAST
                if (this.gmi.getTargetX() == 0) { // column 0
                    this.gmi.triggerMovement(Constants.EAST);
                } else {
                    this.gmi.triggerMovement(Constants.WEST);
                }
            } else {
                // Otherwise handle normal back navigation
                if(t != null){
                    t.interrupt();
                    moves = null;
                    t = null;
                }
                gameManager.setGameScreen(Constants.SCREEN_START);
            }
        }

        if(!isSolved && solver.getSolverStatus().isFinished())
        {
            isSolved = true;
            buttonSolve.setEnabled(true);
            NumDifferentSolutionsFound=solver.getSolutionList().size();
            GameSolution solution = solver.getSolution(numDifferentSolutionClicks);
            solutionMoves=0;
            for(IGameMove m : solution.getMoves()){
                solutionMoves++;
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
        try {
            String saveData = FileReadWrite.readPrivateData(gameManager.getActivity(), mapPath);
            gridElements = MapObjects.extractDataFromString(saveData);
            Timber.d("Extracted gridElements size=%d", gridElements.size());
            GridGameScreen.setMap(gridElements);
            createGrid();
            buttonSaveSetEnabled(false);  // Disable save button for saved games
            Timber.d("Saved game loaded successfully, save button disabled");
        } catch (Exception e) {
            Timber.d("Error loading saved game: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    public void setLevelGame(String mapPath)
    {
        Timber.d("Loading level game from %s", mapPath);
        this.mapPath = mapPath;
        this.isGameWon = false;  // Reset game won flag
        try {
            String saveData = FileReadWrite.readAssets(gameManager.getActivity(), mapPath);
            Timber.d("Loaded level data length=%d", saveData.length());
            gridElements = MapObjects.extractDataFromString(saveData);
            Timber.d("Extracted gridElements size=%d", gridElements.size());
            GridGameScreen.setMap(gridElements);
            numSolutionClicks = 0;
            createGrid();
            buttonSaveSetEnabled(false);  // Disable save button for level games
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
            
            buttonSaveSetEnabled(true);  // Enable save button only for random games
            Timber.d("Random game created successfully, save button enabled");
        } catch (Exception e) {
            Timber.d("Error creating random game: %s", e.getMessage());
            e.printStackTrace();
        }
    }

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
        drawables.put("grid", ResourcesCompat.getDrawable(currentRenderManager.getResources(), R.drawable.grid, null)); // white background for 16x16
        drawables.put("grid_tiles", ResourcesCompat.getDrawable(currentRenderManager.getResources(), R.drawable.grid_tiles, null)); // white background for variable sizes
        drawables.put("grid_14x16", ResourcesCompat.getDrawable(currentRenderManager.getResources(), R.drawable.grid_14x16, null)); // white background for 14x16
        drawables.put("grid_14x14", ResourcesCompat.getDrawable(currentRenderManager.getResources(), R.drawable.grid_14x14, null)); // white background for 14x16
        drawables.put("grid_12x14", ResourcesCompat.getDrawable(currentRenderManager.getResources(), R.drawable.grid_12x14, null)); // white background for 14x16
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
        if (MainActivity.getBoardWidth() == 16 && MainActivity.getBoardHeight() <= 16) {
            gridBackground = drawables.get("grid"); // 16x16 grid
            // Scale the grid background to match our calculated dimensions
            gridBackground.setBounds(0, 0, totalWidth, totalHeight);
            gridBackground.draw(canvasGrid);
        } else if (MainActivity.getBoardWidth() == 14 && MainActivity.getBoardHeight() == 16) {
            gridBackground = drawables.get("grid_14x16"); // 14x16 grid
            // Scale the grid background to match our calculated dimensions
            gridBackground.setBounds(0, 0, totalWidth, totalHeight);
            gridBackground.draw(canvasGrid);
        } else if (MainActivity.getBoardWidth() == 14 && MainActivity.getBoardHeight() == 14) {
            gridBackground = drawables.get("grid_14x14"); // 14x16 grid
            // Scale the grid background to match our calculated dimensions
            gridBackground.setBounds(0, 0, totalWidth, totalHeight);
            gridBackground.draw(canvasGrid);
        } else if (MainActivity.getBoardWidth() == 12 && MainActivity.getBoardHeight() == 14) {
            gridBackground = drawables.get("grid_12x14"); // 14x16 grid
            // Scale the grid background to match our calculated dimensions
            gridBackground.setBounds(0, 0, totalWidth, totalHeight);
            gridBackground.draw(canvasGrid);
        } else {
            // Get the 16x16 grid as source
            Drawable fullGrid = drawables.get("grid");

            // Calculate size of one grid cell in the source image
            int sourceTotalWidth = fullGrid.getIntrinsicWidth();
            int sourceTotalHeight = fullGrid.getIntrinsicHeight();
            float sourceCellWidth = sourceTotalWidth / 16f;  // 16x16 grid
            float sourceCellHeight = sourceTotalHeight / 16f;

            // Calculate tile size based on board dimensions
            // We want the tile to be roughly half of the board size
            float tileSize = 91.5f / MainActivity.getBoardWidth();
            // Round to nearest 0.5 to ensure clean tiling
            tileSize = tileSize * 2 / 2f;

            int tileWidth = (int)(Math.round(sourceCellWidth * tileSize));
            int tileHeight = (int)(Math.round(sourceCellHeight * tileSize));

            // Set bounds for the full grid to show only the corner we want
            fullGrid.setBounds(0, 0, tileWidth, tileHeight);

            // Tile the grid background with our corner pattern
            for (int x = 0; x < MainActivity.getBoardWidth(); x += tileSize) {
                for (int y = 0; y < MainActivity.getBoardHeight(); y += tileSize) {
                    canvasGrid.save();
                    canvasGrid.translate(x * gridSpace, y * gridSpace);
                    fullGrid.draw(canvasGrid);
                    canvasGrid.restore();
                }
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

        //On supprime l'image de fond si elle existe et on sauvegarde celle que l'on vient de créer
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
            if(instance.getClass() == p.getClass() && p != instance && canMove)
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
        }
        LevelChoiceGameScreen.invalidateMapCache(mapPath); // Invalidate only this specific map in the cache
        updatePlayedMaps();
        // set mapCachHasToBeUpdated in Level
    }

    private void updatePlayedMaps()
    {
        // Timber.d(" updatePlayedMaps: " + mapPath);
        if(mapPath.length() > 0) {
            addMapsPlayed();
            SparseArray<GameScreen> screens = gameManager.getScreens();
            LevelChoiceGameScreen.getLastButtonUsed().setImageUp(R.drawable.bt_start_up_played);
            LevelChoiceGameScreen.getLastButtonUsed().setImageDown(R.drawable.bt_start_down_played);
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

    private class ButtonRestart implements IExecutor{

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
     * Button to Start a new random level
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
                if (currentLevel >= 1 && currentLevel < 140) {
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
        ButtonRestart br = new ButtonRestart();
        br.execute();

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
}
