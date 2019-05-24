package roboyard.eclabs;

import android.graphics.Color;

/**
 * Created by Pierre on 04/02/2015.
 */
public class SettingsGameScreen extends GameScreen {

    private GameButtonGeneral buttonBeginner = null;
    private GameButtonGeneral buttonAdvanced = null;
    private GameButtonGeneral buttonInsane = null;
    private GameButtonGeneral buttonSoundOff = null;
    private GameButtonGeneral buttonSoundOn = null;
    private int hs2;
    private int ws2;
    private String UserLevel="Advanced";
    private Preferences preferences = new Preferences();

    public SettingsGameScreen(GameManager gameManager){
        super(gameManager);
    }

    @Override
    public void create() {
        ws2 = this.gameManager.getScreenWidth()/2;
        hs2 = this.gameManager.getScreenHeight()/2;

        float ratioW = ((float)gameManager.getScreenWidth()) /((float)1080);
        float ratioH = ((float)gameManager.getScreenHeight()) /((float)1920);

        // set UserLevel
        buttonBeginner = new GameButtonGeneral((int)(40*ratioW), (int)((380)*ratioH),(int) (120*2*ratioW),(int) (128*ratioH), R.drawable.bt_up, R.drawable.bt_down, new setBeginnner());
        buttonAdvanced = new GameButtonGeneral((int)(390*ratioW), (int)((380)*ratioH),(int) (120*2*ratioW),(int) (128*ratioH), R.drawable.bt_up, R.drawable.bt_down, new setAdvanced());
        buttonInsane   = new GameButtonGeneral((int)(740*ratioW), (int)((380)*ratioH),(int) (120*2*ratioW),(int) (128*ratioH), R.drawable.bt_up, R.drawable.bt_down, new setInsane());

        buttonSoundOn = new GameButtonGeneral((int)(240*ratioW), (int)((780)*ratioH),(int) (222*ratioW),(int) (222*ratioH), R.drawable.bt_sound_on_up, R.drawable.bt_sound_on_down, new setSoundon());
        buttonSoundOff = new GameButtonGeneral((int)(540*ratioW), (int)((780)*ratioH),(int) (222*ratioW),(int) (222*ratioH), R.drawable.bt_sound_off_up, R.drawable.bt_sound_off_down, new setSoundoff());

        // Add Button to set Beginner/Advanced/Insane
        this.instances.add(buttonBeginner);
        this.instances.add(buttonAdvanced);
        this.instances.add(buttonInsane);
        this.instances.add(buttonSoundOff);
        this.instances.add(buttonSoundOn);

        // Add Button back to main screen
        this.instances.add(new GameButtonGoto(7*ws2/4-128, 9*hs2/5-128, 128, 128, R.drawable.bt_back_up, R.drawable.bt_back_down, 0));

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

        int ts = hs2/10; // =89

        renderManager.setColor(Color.BLACK);
        renderManager.setTextSize(ts);

        renderManager.drawText(10, 1*ts, "Difficulty:");
        UserLevel=preferences.getPreferenceValue(gameManager.getActivity(), "difficulty");
        if(UserLevel!=""){
            renderManager.drawText(10, 3*ts, UserLevel);
        }

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
            UserLevel="Beginner";
        }
    }
    private class setAdvanced implements IExecutor{
        public void execute() {
            preferences.setPreferences(gameManager.getActivity(),"difficulty", "Advanced");
            GridGameScreen.setLevel("Advanced");
            UserLevel="Advanced";
        }
    }
    private class setInsane implements IExecutor{
        public void execute() {
            preferences.setPreferences(gameManager.getActivity(),"difficulty", "Insane");
            GridGameScreen.setLevel("Insane");
            UserLevel="Insane";
        }
    }

    private class setSoundoff implements IExecutor{
        public void execute() {
            preferences.setPreferences(gameManager.getActivity(),"sound", "off");
            gameManager.requestRestart();
        }
    }


    private class setSoundon implements IExecutor{
        public void execute() {
            preferences.setPreferences(gameManager.getActivity(),"sound", "on");
            gameManager.requestRestart();
        }
    }

}
