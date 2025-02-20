package roboyard.eclabs;

import android.graphics.Color;
import android.graphics.Paint;

import static android.os.SystemClock.sleep;

import timber.log.Timber;

/**
 * Created by Pierre on 04/02/2015.
 */
public class SettingsGameScreen extends GameScreen {

    private GameButtonGeneral buttonBeginner = null;
    private GameButtonGeneral buttonAdvanced = null;
    private GameButtonGeneral buttonInsane = null;
    private GameButtonGeneral buttonImpossible = null;
    private GameButtonGeneral buttonSoundOff = null;
    private GameButtonGeneral buttonSoundOn = null;
    private GameDropdown boardSizeDropdown;
    private int hs2;
    private int ws2;
    private String levelDifficulty="Beginner";
    private final Preferences preferences = new Preferences();

    private final float ratioW = ((float)gameManager.getScreenWidth()) /((float)1080);
    private final float ratioH = ((float)gameManager.getScreenHeight()) /((float)1920);

    public SettingsGameScreen(GameManager gameManager){
        super(gameManager);
    }

    @Override
    public void create() {
        // Set screen textSize in draw() at the end of the method
        ws2 = this.gameManager.getScreenWidth()/2;
        hs2 = this.gameManager.getScreenHeight()/2;

        float ratioW = ((float)gameManager.getScreenWidth()) /((float)1080);
        float ratioH = ((float)gameManager.getScreenHeight()) /((float)1920);

        float displayRatio = gameManager.getScreenHeight() / gameManager.getScreenWidth();

        // set levelDifficulty
        int posY = 340;
        int buttonWidth = (int)(160*ratioW);
        int buttonHeight = (int)(128*ratioH);
        
        // Calculate x positions for buttons
        int x1 = (int)(40*ratioW);   // First column
        int x2 = (int)(300*ratioW);  // Second column
        int x3 = (int)(560*ratioW);  // Third column
        int x4 = (int)(820*ratioW);  // Fourth column

        buttonBeginner = new GameButtonGeneral(x1, (int)(posY*ratioH), buttonWidth, buttonHeight, R.drawable.bt_up, R.drawable.bt_down, new setBeginnner());
        buttonAdvanced = new GameButtonGeneral(x2, (int)(posY*ratioH), buttonWidth, buttonHeight, R.drawable.bt_up, R.drawable.bt_down, new setAdvanced());

        posY += 270;

        buttonInsane = new GameButtonGeneral(x1, (int)(posY*ratioH), buttonWidth, buttonHeight, R.drawable.bt_up, R.drawable.bt_down, new setInsane());
        buttonImpossible = new GameButtonGeneral(x2, (int)(posY*ratioH), buttonWidth, buttonHeight, R.drawable.bt_up, R.drawable.bt_down, new setImpossible());

        // set Board Size
        posY = 44;
        Timber.d("ratioW: %f, ratioH: %f, displayRatio: %f", ratioW, ratioH, displayRatio);
        boardSizeDropdown = new GameDropdown(x3, (int)(posY*ratioH), buttonWidth * 2, buttonHeight);
        boardSizeDropdown.addOption("12x12", new setBoardSize(12, 12));
        boardSizeDropdown.addOption("12x14", new setBoardSize(12, 14));
        if(displayRatio > 1.8) {
            boardSizeDropdown.addOption("12x16", new setBoardSize(12, 16));
            boardSizeDropdown.addOption("12x18", new setBoardSize(12, 18));
        }
        boardSizeDropdown.addOption("14x14", new setBoardSize(14, 14));
        boardSizeDropdown.addOption("14x16", new setBoardSize(14, 16));
        if(displayRatio > 1.8) {
            boardSizeDropdown.addOption("14x18", new setBoardSize(14, 18));
        }
        boardSizeDropdown.addOption("16x16", new setBoardSize(16, 16));
        boardSizeDropdown.addOption("16x18", new setBoardSize(16, 18));
        boardSizeDropdown.addOption("16x20", new setBoardSize(16, 20));
        if(displayRatio > 1.8) {
            boardSizeDropdown.addOption("16x22", new setBoardSize(16, 22));
        }
        boardSizeDropdown.addOption("18x18", new setBoardSize(18, 18));
        boardSizeDropdown.addOption("18x20", new setBoardSize(18, 20));
        boardSizeDropdown.addOption("18x22", new setBoardSize(18, 22));
        this.instances.add(boardSizeDropdown);

        // icons from freeiconspng [1](https://www.freeiconspng.com/img/40963), [2](https://www.freeiconspng.com/img/40944)
        // Make sound buttons circular using the minimum of width/height ratio
        float buttonRatio = Math.min(ratioW, ratioH);
        int soundButtonSize = (int)(222 * buttonRatio);
        posY=1080;
        buttonSoundOn = new GameButtonGeneral(
            (int)(40*ratioW),
            (int)(posY*ratioH),
            soundButtonSize, 
            soundButtonSize, 
            R.drawable.bt_sound_on_up, 
            R.drawable.bt_sound_on_down, 
            new setSoundon());
        buttonSoundOff = new GameButtonGeneral(
            (int)(340*ratioW),
            (int)(posY*ratioH),
            soundButtonSize, 
            soundButtonSize, 
            R.drawable.bt_sound_off_up, 
            R.drawable.bt_sound_off_down, 
            new setSoundoff());

        // Add Button to set Beginner/Advanced/Insane
        this.instances.add(buttonBeginner);
        this.instances.add(buttonAdvanced);
        this.instances.add(buttonInsane);
        this.instances.add(buttonImpossible);
        this.instances.add(buttonSoundOff);
        this.instances.add(buttonSoundOn);

        // Add Button back to main screen
        this.instances.add(new GameButtonGoto(90, 9*hs2/5-128, (int)(222*ratioW), (int)(222*ratioW), R.drawable.bt_back_up, R.drawable.bt_back_down, 0));

    }

    @Override
    public void load(RenderManager renderManager) {
        super.load(renderManager);
    }

    @Override
    public void draw(RenderManager renderManager) {

        renderManager.setColor(Color.YELLOW);
        renderManager.setColor(Color.parseColor("#FFFDAE"));
        renderManager.paintScreen();

        int textSize = hs2/10; // =89
        int marginL = 10;
        int posY;

        renderManager.setColor(Color.BLACK);
        renderManager.setTextSize(textSize);

        // Calculate button dimensions and positions
        int buttonWidth = (int)(160*ratioW);
        int buttonHeight = (int)(128*ratioH);
        
        // Calculate x positions for buttons
        int x1 = (int)(40*ratioW);   // First column
        int x2 = (int)(300*ratioW);  // Second column
        int x3 = (int)(560*ratioW);  // Third column
        int x4 = (int)(820*ratioW);  // Fourth column

        // Board Size
        Paint paint = new Paint();
        paint.setTextSize(textSize/2);
        renderManager.setTextSize((int) (textSize/1.5));
        paint.setTextSize(textSize/1.5f);
        posY = 144;
        String text = "      Board Size:";
        float textWidth2 = paint.measureText(text);
        renderManager.drawText((int)(x1 + buttonWidth - textWidth2/2), (int)(posY*ratioH), text);
        
        // Difficulty
        // current level difficulty
        levelDifficulty=preferences.getPreferenceValue(gameManager.getActivity(), "difficulty");
        posY = 300;
        if(!levelDifficulty.equals("")){
            renderManager.drawText(marginL, (int)((posY)*ratioH), "Difficulty: " + levelDifficulty);
        }
        
        // difficulty text below Buttons (set in create() ):
        posY = 550;
        renderManager.setTextSize(textSize/2);
        
        // Center text under each button
        text = "Beginner";
        float textWidth = paint.measureText(text)-66;
        renderManager.drawText((int)(x1 + buttonWidth/2 - textWidth/2), (int)(posY*ratioH), text);
        
        text = "Advanced";
        textWidth = paint.measureText(text)-55;
        renderManager.drawText((int)(x2 + buttonWidth/2 - textWidth/2), (int)(posY*ratioH), text);

        posY += 277;

        text = "Insane";
        textWidth = paint.measureText(text)-33;
        renderManager.drawText((int)(x1 + buttonWidth/2 - textWidth/2), (int)(posY*ratioH), text);
        
        text = "Impossible";
        textWidth = paint.measureText(text)-77;
        renderManager.drawText((int)(x2 + buttonWidth/2 - textWidth/2), (int)(posY*ratioH), text);

        // Sound Settings
        posY = 1055;
        renderManager.setTextSize((int) (textSize*0.8));
        String soundSetting=preferences.getPreferenceValue(gameManager.getActivity(), "sound");
        if(soundSetting.equals("off") == false) {
            soundSetting = "Sound: On";
        } else {
            soundSetting = "Sound: Off";
        }
        renderManager.drawText((int)(90*ratioW), (int)((posY)*ratioH), soundSetting);

        renderManager.setTextSize(textSize/2); // this will be the text-size for the dropdown in create()

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

    private class setBeginnner implements IExecutor{
        public void execute() {
            preferences.setPreferences(gameManager.getActivity(), "difficulty", "Beginner");
            GridGameScreen.setDifficulty("Beginner");
            levelDifficulty="Beginner";
        }
    }
    private class setAdvanced implements IExecutor{
        public void execute() {
            preferences.setPreferences(gameManager.getActivity(),"difficulty", "Advanced");
            GridGameScreen.setDifficulty("Advanced");
            levelDifficulty="Advanced";
        }
    }
    private class setInsane implements IExecutor{
        public void execute() {
            preferences.setPreferences(gameManager.getActivity(),"difficulty", "Insane");
            GridGameScreen.setDifficulty("Insane");
            levelDifficulty="Insane";
        }
    }
    private class setImpossible implements IExecutor{
        public void execute() {
            preferences.setPreferences(gameManager.getActivity(),"difficulty", "Impossible");
            GridGameScreen.setDifficulty("Impossible");
            levelDifficulty="Impossible";
        }
    }

    private class setSoundoff implements IExecutor{
        public void execute() {
            preferences.setPreferences(gameManager.getActivity(),"sound", "off");
            gameManager.toggleSound(false);
            gameManager.requestToast("Sound disabled", true);
        }
    }

    private class setSoundon implements IExecutor{
        public void execute() {
            preferences.setPreferences(gameManager.getActivity(),"sound", "on");
            gameManager.toggleSound(true);
            gameManager.requestToast("Sound enabled", true);
        }
    }

    private class setBoardSize implements Runnable {
        private final int width;
        private final int height;

        public setBoardSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void run() {
            MainActivity activity = gameManager.getActivity();
            activity.setBoardSize(activity, width, height);
            preferences.setPreferences(gameManager.getActivity(), "width", String.valueOf(width));
            preferences.setPreferences(gameManager.getActivity(), "height", String.valueOf(height));
            MapGenerator.generateNewMapEachTime = true;
            gameManager.getInputManager().resetEvents();
        }
    }

}
