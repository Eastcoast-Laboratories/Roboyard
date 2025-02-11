package roboyard.eclabs;

import android.graphics.Color;

import java.util.ArrayList;

/**
 * Created by Pierre on 21/01/2015.
 */
public class LevelChoiceGameScreen extends GameScreen {

    private int firstLevel = 0;
    private int leftScreen = -1;
    private int rightScreen = -1;

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
            this.rightScreen = Constants.SCREEN_LEVEL_GAME_INTERMEDIATE;  // Go to intermediate
        } else if (firstLevel == 36) {  // Intermediate page (36-70)
            this.leftScreen = Constants.SCREEN_LEVEL_GAME_START;   // Back to beginner
            this.rightScreen = Constants.SCREEN_LEVEL_GAME_ADVANCED; // Go to advanced
        } else if (firstLevel == 71) {  // Advanced page (71-105)
            this.leftScreen = Constants.SCREEN_LEVEL_GAME_INTERMEDIATE;  // Back to intermediate
            this.rightScreen = Constants.SCREEN_LEVEL_GAME_EXPERT; // Go to expert
        } else if (firstLevel == 106) {  // Expert page (106-140)
            this.leftScreen = Constants.SCREEN_LEVEL_GAME_ADVANCED;  // Back to advanced
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
            this.instances.add(new GameButtonGotoLevelGame((55+(stepX*col))*ratioW, (45+ts+(stepY*row))*ratioH, iconsize*ratioH, iconsize*ratioW, saver.getButtonLevels(mapPath, true), saver.getButtonLevels(mapPath, false), 4, mapPath));
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

    @Override
    public void draw(RenderManager renderManager) {
        int stepX = 211;
        int stepY = 222;
        int cols = 5;
        int rows = 7;

        renderManager.setColor(Color.parseColor("#77ABD6"));
        renderManager.paintScreen();

        renderManager.setColor(Color.BLACK);

        int hs2 = this.gameManager.getScreenHeight()/2;
        int ts = hs2/10;
        renderManager.setTextSize((int)(0.5*ts));

        float ratioW = ((float)gameManager.getScreenWidth()) / ((float)1080);
        float ratioH = ((float)gameManager.getScreenHeight()) / ((float)1920);

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
        for (int i = 0; i < cols*rows; i++) {
            col = i % cols;
            row = (i / cols) % rows;
            int levelNum = firstLevel + i;
            // write the number of the level:
            renderManager.drawText((int)((55+(stepX*col))*ratioW)-10, (int)((45+ts+(stepY*row))*ratioH), levelNum + ".");
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
