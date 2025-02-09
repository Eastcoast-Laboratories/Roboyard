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
     * @param firstLevel : number of the first map, 0 -> generatedMap_0.txt
     * @param leftScreen : reference to the previous level selection game screen (-1 if none)
     * @param rightScreen : reference to the next level selection game screen (-1 if none)
     */
    public LevelChoiceGameScreen(GameManager gameManager, int firstLev, int lScreen, int rScreen){

        super(gameManager);
        this.firstLevel = firstLev;
        this.leftScreen = lScreen;
        this.rightScreen = rScreen;

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
        //int ws2 = this.gameManager.getScreenWidth()/2;
        //int hs2 = this.gameManager.getScreenHeight()/2;

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
        for (int i = 0;  i < cols*rows;  i++) {
            col = i % cols;
            row = (i / cols) % rows;
            mapPath = getMapPath(i);
            this.instances.add(new GameButtonGotoLevelGame((55+(stepX*col))*ratioW, (45+ts+(stepY*row))*ratioH, iconsize*ratioH, iconsize*ratioW, saver.getButtonLevels(mapPath, true), saver.getButtonLevels(mapPath, false), 4, mapPath));
        }

        if(this.leftScreen > 0)
            this.instances.add(new GameButtonGoto((int)(77*ratioW), (int)((1600+ts)*ratioH), (int)(432*ratioH), (int)(200*ratioW), R.drawable.bt_page_gauche_up, R.drawable.bt_page_gauche_down, this.leftScreen));

        if(this.rightScreen > 0)
            this.instances.add(new GameButtonGoto((int)(611*ratioW), (int)((1600+ts)*ratioH), (int)(432*ratioH), (int)(200*ratioW), R.drawable.bt_page_droite_up, R.drawable.bt_page_droite_down, this.rightScreen));

    }

    private String getMapPath(int levelInScreen)
    {
        levelInScreen+=firstLevel;
        return "Maps/generatedMap_"+levelInScreen+".txt";
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

        //renderManager.setColor(Color.GRAY);
        renderManager.setColor(Color.parseColor("#77ABD6"));
        renderManager.paintScreen();

        renderManager.setColor(Color.BLACK);

        int hs2 = this.gameManager.getScreenHeight()/2;
        int ts = hs2/10;
        renderManager.setTextSize((int)(0.5*ts));

        float ratioW = ((float)gameManager.getScreenWidth()) / ((float)1080);
        float ratioH = ((float)gameManager.getScreenHeight()) / ((float)1920);

        renderManager.drawText((int)(55*ratioW)-10, (int)(55*ratioH), "Select Level");

        int col, row;
        for (int i = 0;  i < cols*rows;  i++) {
            col = i % cols;
            row = (i / cols) % rows;
            // write the number of the level:
            renderManager.drawText((int)((55+(stepX*col))*ratioW)-10, (int)((45+ts+(stepY*row))*ratioH), (i+1) + ".");
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
