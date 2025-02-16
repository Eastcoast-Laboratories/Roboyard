package roboyard.eclabs;

import android.graphics.Color;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Pierre on 21/01/2015.
 */
public class LevelChoiceGameScreen extends GameScreen {

    private int firstLevel = 0;
    private int leftScreen = -1;
    private int rightScreen = -1;
    private int totalStars = 0;

    public static GameButtonGotoLevelGame getLastButtonUsed() {
        return lastButtonUsed;
    }

    public static void setLastButtonUsed(GameButtonGotoLevelGame lastButtonUsed) {
        LevelChoiceGameScreen.lastButtonUsed = lastButtonUsed;
    }

    private static GameButtonGotoLevelGame lastButtonUsed = null;

    /*
     * Game screen for level selection
     * @param firstLevel : number of the first map, 1 -> level_1.txt
     * @param leftScreen : reference to the previous level selection game screen (-1 if none)
     * @param rightScreen : reference to the next level selection game screen (-1 if none)
     */
    public LevelChoiceGameScreen(GameManager gameManager, int firstLevel, int leftScreen, int rightScreen){
        super(gameManager);
        this.firstLevel = firstLevel;
        
        // Update navigation based on current page
        if (firstLevel == 1) {  // Beginner page (1-35)
            this.leftScreen = -1;  // No back navigation in beginner
            this.rightScreen = Constants.SCREEN_LEVEL_INTERMEDIATE;  // Go to intermediate
        } else if (firstLevel == 36) {  // Intermediate page (36-70)
            this.leftScreen = Constants.SCREEN_LEVEL_BEGINNER;   // Back to beginner
            this.rightScreen = Constants.SCREEN_LEVEL_ADVANCED; // Go to advanced
        } else if (firstLevel == 71) {  // Advanced page (71-105)
            this.leftScreen = Constants.SCREEN_LEVEL_INTERMEDIATE;  // Back to intermediate
            this.rightScreen = Constants.SCREEN_LEVEL_EXPERT; // Go to expert
        } else if (firstLevel == 106) {  // Expert page (106-140)
            this.leftScreen = Constants.SCREEN_LEVEL_ADVANCED;  // Back to advanced
            this.rightScreen = -1;  // No forward navigation in expert
        }

        createButtons();
    }



    public GameButtonGotoLevelGame  testButton= null;

    @Override
    public void create() {


        //These images must be loaded at the beginning of the application,
        gameManager.getRenderManager().loadImage(R.drawable.bt_start_up_played);
        gameManager.getRenderManager().loadImage(R.drawable.bt_start_down_played);

        gameManager.getRenderManager().loadImage(R.drawable.bt_page_droite_down);
        gameManager.getRenderManager().loadImage(R.drawable.bt_page_droite_up);
        gameManager.getRenderManager().loadImage(R.drawable.bt_page_gauche_down);
        gameManager.getRenderManager().loadImage(R.drawable.bt_page_gauche_up);

        gameManager.getRenderManager().loadImage(R.drawable.star);

        createButtons();

    }

    /**
     * Create buttons to load levels
     */
    public void createButtons()
    {
        int stepX = 211;
        int stepY = 222;
        int cols = 5;
        int rows = 7;
        int iconsize = 144;

        ArrayList<GameButtonGotoLevelGame> aRemove = new ArrayList<>();
        for(Object currentObject : this.instances)
        {
            if(currentObject.getClass() == GameButtonGotoLevelGame.class)
            {
                aRemove.add((GameButtonGotoLevelGame)currentObject);
            }
        }
        for(GameButtonGotoLevelGame p : aRemove)
        {
            this.instances.remove(p);
        }

        String mapPath = "";
        SaveManager saver = new SaveManager(gameManager.getActivity());

        float ratioW = ((float)gameManager.getScreenWidth()) /((float)1080);
        float ratioH = ((float)gameManager.getScreenHeight()) /((float)1920);

        int hs2 = this.gameManager.getScreenHeight()/2;
        int ts = hs2/10;

        int col, row;
        for (int i = 0; i < cols*rows; i++) {
            col = i % cols;
            row = (i / cols) % rows;
            int levelNum = firstLevel + i;
            mapPath = getMapPath(i);
            Boolean played=saver.getMapsStateLevel(mapPath, "mapsPlayed.txt");
            this.instances.add(new GameButtonGotoLevelGame((55+(stepX*col))*ratioW, (45+ts+(stepY*row))*ratioH, iconsize*ratioH, iconsize*ratioW, saver.getButtonLevels(played, true), saver.getButtonLevels(played, false), 4, mapPath));
        }

        // Add navigation buttons at the bottom
        int screenHeight = this.gameManager.getScreenHeight();
        
        if (firstLevel == 1) {
            // In beginner screen, only show small back button and forward button
            GameButtonGoto backButton = new GameButtonGoto(33, screenHeight - iconsize - 33, iconsize, iconsize, R.drawable.bt_back_up, R.drawable.bt_back_down, Constants.SCREEN_START);
            this.instances.add(backButton);
            
            // Show forward button to intermediate (right side only)
            this.instances.add(new GameButtonGoto((int)(611*ratioW), (int)((1600+ts)*ratioH), (int)(432*ratioH), (int)(200*ratioW), R.drawable.bt_page_droite_up, R.drawable.bt_page_droite_down, rightScreen));
            return;  // Skip adding any other navigation buttons
        }
        
        // For all other screens
        if (leftScreen >= 0 && firstLevel >= 36){
            // Show back button to left screen
            this.instances.add(new GameButtonGoto((int)(77*ratioW), (int)((1600+ts)*ratioH), (int)(432*ratioH), (int)(200*ratioW), R.drawable.bt_page_gauche_up, R.drawable.bt_page_gauche_down, leftScreen));
        }
        
        if (firstLevel == 106) {
            // In expert screen, show small back button to menu
            // TODO: this.instances.add(new GameButtonGoto((int)(611*ratioW), screenHeight - iconsize - 33, iconsize, iconsize, R.drawable.bt_back_up, R.drawable.bt_back_down, Constants.SCREEN_START));
        } else if (rightScreen >= 0 && firstLevel < 106) {
            // For intermediate and advanced screens, show forward button
            this.instances.add(new GameButtonGoto((int)(611*ratioW), (int)((1600+ts)*ratioH), (int)(432*ratioH), (int)(200*ratioW), R.drawable.bt_page_droite_up, R.drawable.bt_page_droite_down, rightScreen));
        }
    }

    private String getMapPath(int levelInScreen) {
        int levelNum = firstLevel + levelInScreen;
        return "Maps/level_" + levelNum + ".txt";
    }

    @Override
    public void load(RenderManager renderManager) {
        super.load(renderManager);
    }
    
    private void drawStarsAroundButton(RenderManager renderManager, int centerX, int centerY, int buttonSize, int numstars) {
        // Make stars slightly smaller (1/5 of button size)
        int starSize = buttonSize / 4;
        
        // Increase radius to place stars further from button
        float radius = buttonSize * 0.64f;
        
        // Draw 3 stars in a circle around the button
        for (int i = 0; i < numstars; i++) {
            // Calculate angle for each star (120 degrees apart, offset to start from top)
            double angle = Math.toRadians(-90 + (i * 30));
            
            // Calculate position on the circle
            int starX = (int)(centerX + radius * Math.cos(angle));
            int starY = (int)(centerY + radius * Math.sin(angle));
            
            // Draw star at this position
            renderManager.drawImage(
                starX - starSize/2,
                starY - starSize/2,
                starX + starSize/2,
                starY + starSize/2,
                R.drawable.star
            );
        }
    }

    @Override
    public void draw(RenderManager renderManager) {

        float ratioW = ((float)gameManager.getScreenWidth()) / ((float)1080);
        float ratioH = ((float)gameManager.getScreenHeight()) / ((float)1920);

        // Calculate button layout
        int stepX = 211;
        int stepY = 222;
        int cols = 5;
        int rows = 7;
        int buttonSize = (int)(144 * ratioH);

        renderManager.setColor(Color.parseColor("#77ABD6"));
        renderManager.paintScreen();

        renderManager.setColor(Color.BLACK);

        int hs2 = this.gameManager.getScreenHeight()/2;
        int ts = hs2/10;
        renderManager.setTextSize((int)(0.5*ts));


        // Show current difficulty in the title
        String difficulty;
        if (firstLevel == 1) {
            difficulty = "Beginner";
        } else if (firstLevel == 36) {
            difficulty = "Intermediate";
        } else if (firstLevel == 71) {
            difficulty = "Advanced";
        } else {
            difficulty = "Expert";
        }
        renderManager.drawText((int)(55*ratioW)-10, (int)(55*ratioH), difficulty + " Levels");

        int col, row;
        int moves;
        int minMoves;
        // Draw level buttons and numbers
		for (int i = 0; i < cols*rows; i++) {
            col = i % cols;
            row = (i / cols) % rows;
            int levelNum = firstLevel + i;
            
            // Calculate button center position
            int centerX = (int)((55+(stepX*col))*ratioW) + buttonSize/2;
            int centerY = (int)((45+ts+(stepY*row))*ratioH) + buttonSize/2;
            
            // Draw level number
            renderManager.setColor(Color.BLACK);
            renderManager.drawText((int)((55+(stepX*col))*ratioW)-10, (int)((45+ts+(stepY*row))*ratioH), levelNum + ".");
            
            // Draw button
            instances.get(i).draw(renderManager);

            // Draw stars if level is completed
            String mapPath = "Maps/level_" + levelNum + ".txt";
            SaveManager saver = new SaveManager(gameManager.getActivity());
            Boolean played=saver.getMapsStateLevel(mapPath, "mapsPlayed.txt");
            int numStars=0;
            if (played) {
                moves = 999999;
                minMoves = -1;
                // int solveTime;
                // read data from file to see your minimum used moves with getMapsStateLevel
                HashMap<String, Integer> mapData = saver.getMapLevelData(mapPath, 1);
                if (mapData != null) {
                    // completion data: minMoves, moves, squares, time
                    moves = mapData.get("moves");
                    minMoves = mapData.get("minMoves");
                    // not used for now solveTime = mapData.get("time");
                    // not used for now int squares = mapData.get("squares");
                }
                if (moves < 999999) {
                    totalStars++;
                    //noinspection ReassignedVariable
                    numStars++;
                }
                if (moves <= minMoves + 1) {
                    totalStars++;
                    numStars++;
                }
                if (moves <= minMoves) {
                    totalStars++;
                    numStars++;
                }
                if (moves < minMoves) {
                    // should not happen
                    totalStars++;
                    numStars++;
                }
                drawStarsAroundButton(renderManager, centerX, centerY, buttonSize, numStars);
            }
        }

        super.draw(renderManager);
    }
 
    @Override
    public void update(GameManager gameManager) {
        super.update(gameManager);
        if(gameManager.getInputManager().backOccurred()){
            gameManager.setGameScreen(Constants.SCREEN_START);
        }

    }

    @Override
    public void destroy() {
        super.destroy();
    }
}
