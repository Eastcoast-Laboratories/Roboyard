package roboyard.eclabs;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Pierre on 21/01/2015.
 */
public class LevelChoiceGameScreen extends GameScreen {

    private int firstLevel = 0;
    private int leftScreen = -1;
    private int rightScreen = -1;
    private int totalStars = 0;
    boolean LevelCompleteToastShown = false;
    private GameButton nextButton;

    public static GameButtonGotoLevelGame getLastButtonUsed() {
        return lastButtonUsed;
    }

    public static void setLastButtonUsed(GameButtonGotoLevelGame lastButtonUsed) {
        LevelChoiceGameScreen.lastButtonUsed = lastButtonUsed;
    }

    private static GameButtonGotoLevelGame lastButtonUsed = null;
    private static Set<String> mapsNeedingUpdate = new HashSet<>(); // Track which maps need updates

    private SaveManager saver;
    private HashMap<String, HashMap<String, Integer>> mapDataCache; // Cache for map data

    /*
     * Game screen for level selection
     * @param firstLevel : number of the first map, 1 -> level_1.txt
     * @param leftScreen : reference to the previous level selection game screen (-1 if none)
     * @param rightScreen : reference to the next level selection game screen (-1 if none)
     */
    public LevelChoiceGameScreen(GameManager gameManager, int firstLevel, int leftScreen, int rightScreen){
        super(gameManager);
        this.firstLevel = firstLevel;
        this.leftScreen = leftScreen;
        this.rightScreen = rightScreen;
        saver = new SaveManager(gameManager.getActivity());
        mapDataCache = new HashMap<>(); // Initialize cache
        
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


        float ratioW = ((float)gameManager.getScreenWidth()) /((float)1080);
        float ratioH = ((float)gameManager.getScreenHeight()) /((float)1920);

        int iconSize = (int)(125 * ratioH/ratioW);

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

        int hs2 = this.gameManager.getScreenHeight()/2;
        int ts = hs2/10;

        // Use minimum of width/height ratio to maintain circular shape
        float buttonRatio = Math.min(ratioW, ratioH);
        int buttonSize = (int)(iconSize * buttonRatio);

        int col, row;
        for (int i = 0; i < cols*rows; i++) {
            col = i % cols;
            row = (i / cols) % rows;
            int levelNum = firstLevel + i;
            mapPath = getMapPath(i);
            Boolean played=saver.getMapsStateLevel(mapPath, "mapsPlayed.txt");
            this.instances.add(new GameButtonGotoLevelGame(
                (int)((55+(stepX*col))*ratioW), 
                (int)((45+ts+(stepY*row))*ratioH), 
                buttonSize,  // Use same size for both width and height
                buttonSize,
                saver.getButtonLevels(played, true), 
                saver.getButtonLevels(played, false), 
                4, 
                mapPath));
        }

        // Add navigation buttons at the bottom
        int screenHeight = this.gameManager.getScreenHeight();
        
        // Calculate navigation button dimensions - scale with screen width
        int navButtonWidth = (int)(300 * ratioW);  // Reduced from 432 to 300
        int navButtonHeight = (int)(144 * ratioH);  // Using same height as level buttons
        int marginbottom = (int)(22 * ratioH);

        if (firstLevel == 1) {
            // In beginner screen, only show small back button and forward button
            int backButtonSize = (int)(199 * ratioH);
            GameButtonGoto backButton = new GameButtonGoto((int)(30 * ratioW), screenHeight - backButtonSize - marginbottom, backButtonSize, backButtonSize, R.drawable.bt_back_up, R.drawable.bt_back_down, Constants.SCREEN_START);
            this.instances.add(backButton);
            
            // Show forward button to intermediate (right side only)
            nextButton = new GameButtonGoto(
                    (int)(611*ratioW),
                    (int)((1600+ts)*ratioH),
                    navButtonWidth,
                    navButtonHeight,
                    R.drawable.bt_page_droite_up,
                    R.drawable.bt_page_droite_down,
                    rightScreen);
            this.instances.add(nextButton);
            return;  // Skip adding any other navigation buttons
        }
        
        // For all other screens
        if (leftScreen >= 0 && firstLevel >= 36){
            // Show back button to left screen
            this.instances.add(new GameButtonGoto(
                (int)(77*ratioW), 
                (int)((1600+ts)*ratioH), 
                navButtonWidth, 
                navButtonHeight, 
                R.drawable.bt_page_gauche_up, 
                R.drawable.bt_page_gauche_down, 
                leftScreen));
        }
        
        if (firstLevel == 106) {
            // In expert screen, show small back button to menu
            // TODO: this.instances.add(new GameButtonGoto((int)(611*ratioW), screenHeight - iconsize - 33, iconsize, iconsize, R.drawable.bt_back_up, R.drawable.bt_back_down, Constants.SCREEN_START));
        } else if (rightScreen >= 0 && firstLevel < 106) {
            // For intermediate and advanced screens, show forward button
            nextButton = new GameButtonGoto(
                (int)(611*ratioW), 
                (int)((1600+ts)*ratioH), 
                navButtonWidth, 
                navButtonHeight, 
                R.drawable.bt_page_droite_up, 
                R.drawable.bt_page_droite_down, 
                rightScreen);
            this.instances.add(nextButton);
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
    
    private void drawStarsAroundButton(RenderManager renderManager, int centerX, int centerY, int buttonSize, int numstars, int levelNum) {
        float ratioW = ((float)gameManager.getScreenWidth()) / ((float)1080);
        float ratioH = ((float)gameManager.getScreenHeight()) / ((float)1920);
        float buttonRatio = Math.min(ratioW, ratioH);
        
        // Make stars slightly smaller (1/4 of button size)
        int starSize = (int)(buttonSize * buttonRatio / 4);
        
        // Increase radius to place stars further from button
        float radius = buttonSize * buttonRatio * 0.7f;
        
        // Draw stars in a circle around the button
        int startAngle = -90;
        if(levelNum>99) {
            startAngle = -77;
        }
        for (int i = 0; i < numstars; i++) {
            // Calculate angle for each star (30 degrees apart, starting from top)
            double angle = Math.toRadians(startAngle + (i * 30));
            
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
        
        // For lower resolutions (e.g. 720x1280), we need a larger base size
        float baseIconSize = 99;
        if (gameManager.getScreenWidth() <= 720) {
            baseIconSize = 198; // Double the size for lower resolutions
        }
        int iconsize = (int)(baseIconSize * ratioH);

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
        renderManager.drawText((int)(55*ratioW), (int)(55*ratioH), difficulty + " Levels");

        int col, row;
        int moves;
        int minMoves;
        totalStars = 0;
        // Draw level buttons and numbers
		for (int i = 0; i < cols*rows; i++) {
            col = i % cols;
            row = (i / cols) % rows;
            int levelNum = firstLevel + i;
            
            // Calculate button center position
            float buttonRatio = Math.min(ratioW, ratioH);
            int buttonSize = (int)(iconsize * buttonRatio);
            int centerX = (int)((55+(stepX*col))*ratioW) + buttonSize/2;
            int centerY = (int)((45+ts+(stepY*row))*ratioH) + buttonSize/2;
            
            // Draw level number
            renderManager.setColor(Color.BLACK);
            renderManager.setTextSize((int)(0.5*ts));
            renderManager.drawText((int)((22+(stepX*col))*ratioW), (int)((40+ts+(stepY*row))*ratioH), levelNum + ".");

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
	            HashMap<String, Integer> mapData = null;
	            if (!mapsNeedingUpdate.contains(mapPath)) {
	                mapData = mapDataCache.get(mapPath);
	            }
	            if (mapData == null) {
	                mapData = saver.getMapLevelData(mapPath, 1);
	                if (mapData != null) {
	                    mapDataCache.put(mapPath, mapData);
	                    mapsNeedingUpdate.remove(mapPath); // Remove from update list after refresh
	                }
	            }
				if (mapData != null) {
                    // completion data: minMoves, moves, squares, time
	                moves = mapData.get("moves");
	                minMoves = mapData.get("minMoves");
                    // not used for now solveTime = mapData.get("time");
                    // not used for now int squares = mapData.get("squares");

	                // write the number in the center of the button
	                renderManager.setColor(Color.WHITE);
	                renderManager.setTextSize((int)(0.3*ts));
	                renderManager.drawText((int)((150+(stepX*col))*ratioW), (int)((188+ts+(stepY*row))*ratioH), moves + "/" + minMoves);
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
	            drawStarsAroundButton(renderManager, centerX, centerY, iconsize, numStars, levelNum);
	            // TODO: show moves on the button
	            // TODO: show best moves on the button for DEBUG
	            // the levelselectio screen reads al files in a loop, fix that, only read level files once every time, you enter the levelselection screen 
	        }
		}
        // show number of total stars in the level selection screen at the top right:
        renderManager.setColor(Color.YELLOW);
        renderManager.setTextSize((int)(0.5*ts));
        int of = (cols * rows); // one star for each level

        if(totalStars >= of && totalStars < of + 3 && LevelCompleteToastShown == false) {
            gameManager.requestToast("Congratulations, you have collected enough stars to unlock the next level", true);
            LevelCompleteToastShown = true;
        }
        if(totalStars>of) {
            of=of*3; // three stars on each level
        }
        int marginRight = 30;
        renderManager.drawText((int)((gameManager.getScreenWidth() - (166 + marginRight) * ratioW)), (int)(55*ratioH), totalStars + "/" + of);
        // draw a star
        int starSize = (int) (50*ratioH);
        int starX = (int)((gameManager.getScreenWidth() - (222 + marginRight) * ratioW));
        int starY = (int)(11*ratioH);
        renderManager.drawImage(starX, starY, starX + starSize, starY + starSize, R.drawable.star);

        // Enable/disable forward button based on total stars
        if (totalStars < 35) {
            nextButton.setEnabled(false);
        } else {
            nextButton.setEnabled(true);
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

    public static void invalidateMapCache(String mapPath) {
        mapsNeedingUpdate.add(mapPath);
    }
}
