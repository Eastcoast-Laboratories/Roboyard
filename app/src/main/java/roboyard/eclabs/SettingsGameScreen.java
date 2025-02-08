package roboyard.eclabs;

import android.graphics.Color;

import static android.os.SystemClock.sleep;

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
    private GameButtonGeneral buttonBoard1 = null;
    private GameButtonGeneral buttonBoard2 = null;
    private GameButtonGeneral buttonBoard3 = null;
    private GameButtonGeneral buttonBoard4 = null;
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
        ws2 = this.gameManager.getScreenWidth()/2;
        hs2 = this.gameManager.getScreenHeight()/2;

        float ratioW = ((float)gameManager.getScreenWidth()) /((float)1080);
        float ratioH = ((float)gameManager.getScreenHeight()) /((float)1920);

        // set levelDifficulty
        int posY = 340;
        buttonBeginner = new GameButtonGeneral((int)(40*ratioW), (int)(posY*ratioH),(int) (160*ratioW),(int) (128*ratioH), R.drawable.bt_up, R.drawable.bt_down, new setBeginnner());
        buttonAdvanced = new GameButtonGeneral((int)(300*ratioW), (int)(posY*ratioH),(int) (160*ratioW),(int) (128*ratioH), R.drawable.bt_up, R.drawable.bt_down, new setAdvanced());
        buttonInsane   = new GameButtonGeneral((int)(560*ratioW), (int)(posY*ratioH),(int) (160*ratioW),(int) (128*ratioH), R.drawable.bt_up, R.drawable.bt_down, new setInsane());
        buttonImpossible   = new GameButtonGeneral((int)(820*ratioW), (int)(posY*ratioH),(int) (160*ratioW),(int) (128*ratioH), R.drawable.bt_up, R.drawable.bt_down, new setImpossible());

        // set Board Size
        posY = 1200;
        buttonBoard1 = new GameButtonGeneral((int)(40*ratioW), (int)(posY*ratioH),(int) (160*ratioW),(int) (128*ratioH), R.drawable.bt_up, R.drawable.bt_down, new setBoardSize(12, 14));
        buttonBoard2 = new GameButtonGeneral((int)(300*ratioW), (int)(posY*ratioH),(int) (160*ratioW),(int) (128*ratioH), R.drawable.bt_up, R.drawable.bt_down, new setBoardSize(14, 14));
        buttonBoard3 = new GameButtonGeneral((int)(560*ratioW), (int)(posY*ratioH),(int) (160*ratioW),(int) (128*ratioH), R.drawable.bt_up, R.drawable.bt_down, new setBoardSize(14, 16));
        buttonBoard4 = new GameButtonGeneral((int)(820*ratioW), (int)(posY*ratioH),(int) (160*ratioW),(int) (128*ratioH), R.drawable.bt_up, R.drawable.bt_down, new setBoardSize(16, 16));

        // icons from freeiconspng [1](https://www.freeiconspng.com/img/40963), [2](https://www.freeiconspng.com/img/40944)
        buttonSoundOn = new GameButtonGeneral((int)(240*ratioW), (int)((780)*ratioH),(int) (222*ratioW),(int) (222*ratioH), R.drawable.bt_sound_on_up, R.drawable.bt_sound_on_down, new setSoundon());
        buttonSoundOff = new GameButtonGeneral((int)(540*ratioW), (int)((780)*ratioH),(int) (222*ratioW),(int) (222*ratioH), R.drawable.bt_sound_off_up, R.drawable.bt_sound_off_down, new setSoundoff());

        // Add Button to set Beginner/Advanced/Insane
        this.instances.add(buttonBeginner);
        this.instances.add(buttonAdvanced);
        this.instances.add(buttonInsane);
        this.instances.add(buttonImpossible);
        this.instances.add(buttonSoundOff);
        this.instances.add(buttonSoundOn);
        this.instances.add(buttonBoard1);
        this.instances.add(buttonBoard2);
        this.instances.add(buttonBoard3);
        this.instances.add(buttonBoard4);

        // Add Button back to main screen
        this.instances.add(new GameButtonGoto(6*ws2/4-128, 9*hs2/5-128, (int)(222*ratioW), (int)(222*ratioW), R.drawable.bt_back_up, R.drawable.bt_back_down, 0));

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

        // Difficulty
        // current level difficulty
        levelDifficulty=preferences.getPreferenceValue(gameManager.getActivity(), "difficulty");
        posY = 140;
        if(!levelDifficulty.equals("")){
            renderManager.drawText(marginL, (int)((posY)*ratioH), "Difficulty: " + levelDifficulty);
        }
        // difficulty text below Buttons (set in create() ):
        posY = 550;
        renderManager.setTextSize(textSize/2);
        renderManager.drawText((int)(45*ratioW), (int)((posY)*ratioH), "Beginner");
        renderManager.drawText((int)(290*ratioW), (int)((posY)*ratioH), "Advanced");
        renderManager.drawText((int)(575*ratioW), (int)((posY)*ratioH), "Insane");
        renderManager.drawText((int)(795*ratioW), (int)((posY)*ratioH), "Impossible");

        // Board Size
        renderManager.setTextSize((int) (textSize/1.5));
        String boardSize = MainActivity.getBoardWidth() + "x" + MainActivity.getBoardHeight();
        posY = 1160;
        renderManager.drawText(marginL, (int)((posY)*ratioH), "Board Size: " + boardSize);
        posY = 1370;
        renderManager.setTextSize(textSize/2);
        renderManager.drawText((int)(50*ratioW), (int)((posY)*ratioH), "12x14");
        renderManager.drawText((int)(314*ratioW), (int)((posY)*ratioH), "14x14");
        renderManager.drawText((int)(575*ratioW), (int)((posY)*ratioH), "14x16");
        renderManager.drawText((int)(835*ratioW), (int)((posY)*ratioH), "16x16");

        // Sound Settings
        posY = 755;
        renderManager.setTextSize(textSize);
        String soundSetting=preferences.getPreferenceValue(gameManager.getActivity(), "sound");
        if(soundSetting.equals("off") == false) {
            soundSetting = "Sound: On";
        } else {
            soundSetting = "Sound: Off";
        }
        renderManager.drawText((int)(290*ratioW), (int)((posY)*ratioH), soundSetting);

        super.draw(renderManager);
    }

    @Override
    public void update(GameManager gameManager) {
        super.update(gameManager);
        if(gameManager.getInputManager().backOccurred()){
            gameManager.setGameScreen(0);

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
            gameManager.requestToast("The app must restart to change the sound settings...", true);
            sleep(333);
            gameManager.requestRestart();
        }
    }


    private class setSoundon implements IExecutor{
        public void execute() {
            preferences.setPreferences(gameManager.getActivity(),"sound", "on");
            gameManager.requestToast("The app must restart to change the sound settings...", true);
            sleep(333);
            gameManager.requestRestart();
        }
    }

    private class setBoardSize implements IExecutor {
        private final int width;
        private final int height;

        public setBoardSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public void execute() {
            MainActivity activity = gameManager.getActivity();
            activity.setBoardSize(activity, width, height);
        }
    }

}
