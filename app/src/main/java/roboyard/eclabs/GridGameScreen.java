package roboyard.eclabs;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;

import java.util.*;
import java.util.HashMap;
import java.util.Map;

import roboyard.eclabs.solver.ISolver;
import roboyard.eclabs.solver.SolverDD;
import roboyard.pm.ia.GameSolution;
import roboyard.pm.ia.IGameMove;
import roboyard.pm.ia.ricochet.RRGameMove;

// import static android.content.Context.MODE_PRIVATE;

/**
 * This class represents the game screen where the grid is displayed.
 */
public class GridGameScreen extends GameScreen {
    private final Canvas canvasGrid;
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

    private float gridSpace; // gamescreen width / boardSizeX

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
    private final Bitmap bitmapGrid;
    final RenderManager currentRenderManager;
    final Map<String, Drawable> drawables = new HashMap<>();
    final Map<String, Integer> colors = new HashMap<>();
    private final ArrayList<Move> allMoves= new ArrayList<>();

    private GameButtonGeneral buttonSolve;
    private GameButtonGoto buttonSave;

    private final int boardSizeX = MainActivity.boardSizeX;
    private final int boardSizeY = MainActivity.boardSizeY;
    private final Preferences preferences = new Preferences();

    public GridGameScreen(GameManager gameManager){
        super(gameManager);
        String ld=preferences.getPreferenceValue(gameManager.getActivity(), "difficulty");
        if(ld.equals("")){
            // default difficulty
            ld="Beginner";
            preferences.setPreferences(gameManager.getActivity(),"difficulty", ld);
        }
        setDifficulty(ld);
        gridSpace = (float)gameManager.getScreenWidth() / (float)boardSizeX;
        xGrid = 0;
        yGrid = gameManager.getScreenHeight()/7; // distance of the grid from the top

        Bitmap.Config conf = Bitmap.Config.ARGB_8888;

        bitmapGrid = Bitmap.createBitmap((int)(boardSizeX * gridSpace), (int) (boardSizeY * gridSpace), conf);
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
    public void create()
    {
        gmi = new GameMovementInterface();

        xGrid = 0;
        yGrid = gameManager.getScreenHeight()/7;

        int visibleScreenHeight=gameManager.getScreenHeight(); // bei 720x1280:1184px

        int y = yGrid+gameManager.getScreenWidth();
        int dy = visibleScreenHeight-y; // 248
        int buttonW = gameManager.getScreenWidth()/4;

        // TODO: make this depending on the screen size:
        float ratioW = ((float)gameManager.getScreenWidth()) /((float)1080); // bei 720x1280:0.6667 bei 1440x2580:1.333
        float ratioH = ((float)visibleScreenHeight) /((float)1920); // bei 720x1280:0.61667 bei 1440x2580:2.45
        //int buttonPosY = (int)(6.5*dy * ratioH);
        int buttonPosY = y+10*dy/20; // 1060
        int nextButtonDim=(int)(160*ratioH);

        if(visibleScreenHeight<=1280){
            // on very low res screens
            nextButtonDim=(int)(220*ratioH);
            buttonPosY = -50 +y+10*dy/20;
        }

        // Button Next game (top right)
        this.instances.add(new GameButtonGeneral((int)(870*ratioW), 0, nextButtonDim, nextButtonDim, R.drawable.bt_next_up, R.drawable.bt_next_down, new ButtonNext()));

        // Button Save
        gameManager.getRenderManager().loadImage(R.drawable.transparent);
        buttonSave = new GameButtonGoto(0, buttonPosY, buttonW,buttonW, R.drawable.bt_jeu_save_up, R.drawable.bt_jeu_save_down, 9);
        buttonSave.setImageDisabled(R.drawable.transparent);
        // save button will be disabled when playing a saved game
        buttonSave.setEnabled(true);
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
    public void draw(RenderManager renderManager)
    {
        //renderManager.setColor(Color.argb(255, 255, 228, 0));
        renderManager.setColor(Color.BLACK);
        // ffe400
        // ff7c24
        renderManager.paintScreen();

        //renderManager.setColor(Color.BLACK);
        renderManager.setColor(Color.GRAY);
        float ratio = ((float)gameManager.getScreenWidth()) /((float)1080); // bei 720x1280:0.6667 bei 1440x2580:1.333
        int lineHeight = (int)(ratio*55);
        int lineHeightSmall = (int)(lineHeight*0.8);
        int textPosY = lineHeight;
        int textPosYSmall = 2*lineHeight-(int)(ratio*4);
        int textPosYTime = 2*lineHeight+lineHeightSmall+(int)(8/ratio);
        renderManager.setTextSize(lineHeight);
        if(gameManager.getScreenWidth() <=480){
            renderManager.setTextSize(lineHeightSmall);
        }
        if(isSolved && nbCoups == 0 && NumDifferentSolutionsFound > 1){
            // show number of different solutions found
            renderManager.setTextSize(lineHeightSmall);
            renderManager.drawText(10, textPosYSmall, NumDifferentSolutionsFound + " solutions found");
            renderManager.setTextSize(lineHeight);
        }
        if(nbCoups>0){
            // at least one move was made by hand or by AI
            renderManager.drawText(10, textPosY, "Moves: " + nbCoups);
            renderManager.setTextSize(lineHeightSmall);
            renderManager.drawText(10, textPosYSmall, "Squares: " + numSquares);
        } else if(isSolved && numSolutionClicks>0){
            // show solution
            if(numSolutionClicks-showSolutionAtHint >= 0) {
                renderManager.drawText(10, textPosY, "AI solution: " + solutionMoves + " moves");
            } else {
                renderManager.drawText(10, textPosY, "AI Hint " + numSolutionClicks + ": < " + (solutionMoves+showSolutionAtHint-numSolutionClicks) + " moves");
            }
        } else if(nbCoups==0 && isSolved && solutionMoves < simplePuzzleMinMoves){
            // too simple ... restart
            renderManager.drawText(10, textPosY, "AI solution: " + solutionMoves + " moves");
            renderManager.setTextSize(lineHeightSmall);
            renderManager.drawText(10, textPosYSmall, "... restarting!");
            if(timeCpt>5){
                // show a popup on restart if it took very long to solve but found a too simple solution
                requestToast = "Finally solved in " + solutionMoves + " moves. Restarting...";
            }
            mustStartNext = true;
        } else if(nbCoups==0 && isSolved && solutionMoves < goodPuzzleMinMoves){
            // still simple, show a hint that this is solved with less than ... moves
            // TODO: change font (still crashes):
            //  renderManager.drawText(10, textPosY, "Number of moves < " + goodPuzzleMinMoves, "FiraMono-Bold", gameManager.getActivity());
            renderManager.drawText(10, textPosY, "Number of moves < " + goodPuzzleMinMoves);
            showSolutionAtHint = goodPuzzleMinMoves - solutionMoves;
        } else if(!isSolved){
            if (timeCpt<1){
                // the first second it pretends to generate the map :)
                // in real it is still calculating the solution
                renderManager.drawText(10, textPosY, "Generating map...");
            }else{
                // in Beginner mode it will create a new puzzle, if it is not solvable within one second
                if(getLevel().equals("Beginner")){
                    renderManager.drawText(10, textPosY, "Too complicated");
                    renderManager.drawText(10, textPosYSmall, "... restarting!");
                    mustStartNext = true;
                }else {
                    renderManager.drawText(10, textPosY, "AI solving...");
                }
            }
        }
        int seconds = timeCpt%60;
        String secondsS = Integer.toString(seconds);
        if(seconds < 10){
            secondsS="0" + secondsS;
        }
        renderManager.setTextSize(lineHeightSmall);
        renderManager.drawText(10, textPosYTime, "Time: " + timeCpt / 60 + ":" + secondsS);

        if(timeCpt>=40 && autoSaved == false && mustStartNext==false){
            // save autosave in slot 0
            ArrayList<GridElement> gridElements = getGridElements();
            String autosaveMapPath=SaveGameScreen.getMapPath(0);
            FileReadWrite.clearPrivateData(gameManager.getActivity(), autosaveMapPath);
            FileReadWrite.writePrivateData(gameManager.getActivity(), autosaveMapPath, MapObjects.createStringFromList(gridElements, false));
            gameManager.requestToast("Autosaving...", false);
            autoSaved = true;
        }


        if(imageLoaded)
        {
            gameManager.getRenderManager().drawImage(xGrid, yGrid, (int)(boardSizeX*gridSpace) + xGrid, (int)(boardSizeY*gridSpace) + yGrid,  imageGridID);
        }
        super.draw(renderManager);
        this.gmi.draw(renderManager);

        if(!requestToast.equals("")){
            // show double toast to last longer
            gameManager.requestToast(requestToast, true);
            gameManager.requestToast(requestToast, true);
            requestToast="";
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

            buttonSave.setEnabled(true);

            buttonSolve.setEnabled(false);
            if(t != null){
                t.interrupt();
                moves = null;
                t = null;
            }
            int integer = -1;

            if(!mapPath.equals(""))
            {

//                int value = 0;
//                Scanner s = new Scanner(mapPath);

                Scanner in = new Scanner(mapPath).useDelimiter("[^0-9]+");
                integer = in.nextInt();

                //value = s.nextInt();
                System.out.println("Value mappath:"+integer);
            }

            System.out.println("B");
            if(integer >=0 && integer < 60)
            {
                mapPath = "Maps/generatedMap_"+(integer+1)+".txt";
                setLevelGame(mapPath);
            } else {
                // start a game in screen 4
                setRandomGame();
            }
            System.out.println("C");

            mustStartNext = false;
        }
        if(System.currentTimeMillis() - prevTime > 1000L){
            timeCpt++;
            prevTime = System.currentTimeMillis();
        }
        this.gmi.update(gameManager);
        if(gameManager.getInputManager().backOccurred()){
            if(t != null){
                t.interrupt();
                moves = null;
                t = null;
            }
            gameManager.setGameScreen(1);
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
        System.out.println("store "+currentMovedSquares+" moved squares in last Move");
        allMoves.get(allMoves.size()-1).setSquaresMoved(currentMovedSquares);
    }

    public void setSavedGame(String mapPath)
    {
        this.mapPath = "";

        String saveData = FileReadWrite.readPrivateData(gameManager.getActivity(), mapPath);
        gridElements = MapObjects.extractDataFromString(saveData);
        GridGameScreen.setMap(gridElements);

        createGrid();
    }

    public void setLevelGame(String mapPath)
    {
        this.mapPath = mapPath;

        System.out.println("SetLevelGame");
        String saveData = FileReadWrite.readAssets(gameManager.getActivity(), mapPath);
        gridElements = MapObjects.extractDataFromString(saveData);
        System.out.println("SetLevelGame, gridElements :"+gridElements.size());
        GridGameScreen.setMap(gridElements);
        numSolutionClicks = 0;
        createGrid();
    }

    /**
     * creates the grid in screen 4 (game screen)
     */
    public void setRandomGame() {
        this.mapPath = "";  //La carte étant générée, elle n'a pas de chemin d'accès
        MapGenerator generatedMap = new MapGenerator();
        gridElements = generatedMap.getGeneratedGameMap();

        createGrid();
    }

    public void createGrid() {
        this.solver = new SolverDD();

        IAMovesNumber = 0;
        isSolved = false;

        nbCoups = 0;
        numSquares = 0;
        timeCpt = 0;
        prevTime = System.currentTimeMillis();

        currentRenderManager.setTarget(canvasGrid);

        drawables.put("grid", currentRenderManager.getResources().getDrawable(R.drawable.grid)); // white background
        drawables.put("grid_tiles", currentRenderManager.getResources().getDrawable(R.drawable.grid_tiles)); // white background for 14x16 boards
        drawables.put("grid_14x16", currentRenderManager.getResources().getDrawable(R.drawable.grid_14x16)); // white background for other than 16x16 boards
        drawables.put("roboyard", currentRenderManager.getResources().getDrawable(R.drawable.roboyard)); // center roboyard in carré
        drawables.put("mh", currentRenderManager.getResources().getDrawable(R.drawable.mh)); // horizontal lines (hedge)
        drawables.put("mv", currentRenderManager.getResources().getDrawable(R.drawable.mv)); // vertical lines (hedge)

        drawables.put("robot_green", currentRenderManager.getResources().getDrawable(R.drawable.robot_green_right)); // green robot
        drawables.put("robot_red", currentRenderManager.getResources().getDrawable(R.drawable.robot_red_right)); // red
        drawables.put("robot_yellow", currentRenderManager.getResources().getDrawable(R.drawable.robot_yellow_right)); // yellow
        drawables.put("robot_blue", currentRenderManager.getResources().getDrawable(R.drawable.robot_blue_right)); // blue

        drawables.put("target_red", currentRenderManager.getResources().getDrawable(R.drawable.cr)); // ...
        drawables.put("target_blue", currentRenderManager.getResources().getDrawable(R.drawable.cb)); // ...
        drawables.put("target_green", currentRenderManager.getResources().getDrawable(R.drawable.cv)); // ...
        drawables.put("target_yellow", currentRenderManager.getResources().getDrawable(R.drawable.cj)); // ...
        drawables.put("target_multi", currentRenderManager.getResources().getDrawable(R.drawable.cm)); // ...

        // white background of grid
        if(boardSizeX == 16 && boardSizeY<=16){
            drawables.get("grid").setBounds(0, 0,(int)( boardSizeX * gridSpace),(int)( boardSizeY * gridSpace));
            drawables.get("grid").draw(canvasGrid);
        }else if(boardSizeX == 14 && boardSizeY==16){
            drawables.get("grid_14x16").setBounds(0, 0,(int)( boardSizeX * gridSpace),(int)( boardSizeY * gridSpace));
            drawables.get("grid_14x16").draw(canvasGrid);
        }else{
            // grid with fine lines that gives other sizes some orientation
            drawables.get("grid_tiles").setBounds(0, 0,(int)( boardSizeX * gridSpace),(int)( boardSizeY * gridSpace));
            drawables.get("grid_tiles").draw(canvasGrid);
        }

        // grid with fine lines that gibes other sizes some orientation
        drawables.get("roboyard").setBounds((int)((boardSizeX/2 - 1)*gridSpace),(int)((boardSizeY/2 - 1)*gridSpace),(int)((boardSizeX/2 + 1)*gridSpace),(int)((boardSizeY/2 + 1)*gridSpace));
        drawables.get("roboyard").draw(canvasGrid);

        // draw targets
        for (Object element : gridElements) {
            GridElement myp = (GridElement) element;

            if (myp.getType().equals("target_red") || myp.getType().equals("target_green") || myp.getType().equals("target_yellow") || myp.getType().equals("target_blue") || myp.getType().equals("target_multi")) {
                drawables.get(myp.getType()).setBounds((int)(myp.getX() * gridSpace),(int)( myp.getY() * gridSpace),(int)( (myp.getX() + 1) * gridSpace),(int)( (myp.getY()+1) * gridSpace));
                drawables.get(myp.getType()).draw(canvasGrid);
            }
        }

        int pixel = (int)(gridSpace / 45); // equivalent to a pixel on a 720x1280 screen
        int stretchWall = 12 * pixel; // strech all walls
        int offsetWall = -2 * pixel;
        int wallThickness = 16 * pixel; // thickness of walls

        // draw horizontal lines
        for (Object element : gridElements) {
            GridElement myp = (GridElement) element;

            if (myp.getType().equals("mh")) {
                drawables.get("mh").setBounds((int)(myp.getX() * gridSpace - stretchWall), // left x
                        (int)(myp.getY() * gridSpace - stretchWall + offsetWall), // left y
                        (int)((myp.getX() + 1) * gridSpace + stretchWall), // right x
                        (int)(myp.getY() * gridSpace + wallThickness + offsetWall)); // right y
                drawables.get("mh").draw(canvasGrid);
            }

            if (myp.getType().equals("mv")) {
                // vertical lines
                drawables.get("mv").setBounds((int)(myp.getX() * gridSpace - stretchWall + offsetWall), // left x
                        (int)(myp.getY() * gridSpace - stretchWall), // left y
                        (int)(myp.getX() * gridSpace + wallThickness + offsetWall), // right x
                        (int)((myp.getY() + 1) * gridSpace + stretchWall) // right y
                );
                drawables.get("mv").draw(canvasGrid);
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

            if (myp.getType().equals("robot_red") || myp.getType().equals("robot_green") || myp.getType().equals("robot_yellow") || myp.getType().equals("robot_blue")) {

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
        for (Object element : gridElements) {
            GridElement myp = (GridElement) element;
            {
                 if (myp.getType().equals("target_multi") && myp.getX() == p.getX() && myp.getY() == p.getY())
                {
                    sayWon();

                    return true;
                }
                else if((myp.getX() == p.getX()) && (myp.getY() == p.getY()) && (myp.getType().equals("target_red") || myp.getType().equals("target_green") || myp.getType().equals("target_blue") || myp.getType().equals("target_yellow")))
                {
                    if(p.getColor() == colors.get((myp.getType())))
                    {
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
        if(IAMovesNumber > 0)
        {
            gameManager.requestToast("The AI found a solution in "+IAMovesNumber+" moves.", true);
        }
        else
        {
            gameManager.requestToast("You won in "+nbCoups+" moves, "+numSquares+" squares", true);
        }
        updatePlayedMaps();
    }

    private void updatePlayedMaps()
    {
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


    public void addMapsPlayed()
    {
        if(mapPath.length() > 0)
        {
            SaveManager saver = new SaveManager(gameManager.getActivity());

            if(!saver.getMapsStateSaved(mapPath, "mapsPlayed.txt"))
            {
                FileReadWrite.writePrivateData(gameManager.getActivity(), "mapsPlayed.txt", mapPath.substring(5)+"\n");
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
            ButtonBack bb = new ButtonBack();
            while(allMoves.size()>0)
            {
                bb.execute();
            }
            nbCoups = 0;
            numSquares = 0;
        }
    }

    private class ButtonNext implements IExecutor{

        public void execute(){
            mustStartNext = true;

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

            if (numSolutionClicks >= showSolutionAtHint) {
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
            } else {
                numSolutionClicks++;
                if (numSolutionClicks < showSolutionAtHint) {
                    gameManager.requestToast("Press again to see the next hint.", false);
                } else {
                    gameManager.requestToast("Press again to see the solution.", false);
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
            if(allMoves.size() > 0)
            {
                int last=allMoves.size()-1;
                Move lastMove=allMoves.get(last);
                numSquares-=lastMove.getSquaresMoved();
                System.out.println("substract "+lastMove.getSquaresMoved());
                lastMove.goBack();
                System.out.println("remove move nr. "+(allMoves.size()-1)+" "+lastMove._x+"/"+lastMove._y);
                allMoves.remove(last);
                nbCoups--;
            }
        }
    }
}
