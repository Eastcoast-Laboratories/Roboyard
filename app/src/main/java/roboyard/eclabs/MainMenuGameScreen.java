package roboyard.eclabs;

import android.graphics.Color;

/**
 * Created by Pierre on 21/01/2015.
 */
public class MainMenuGameScreen extends GameScreen {

    public MainMenuGameScreen(GameManager gameManager){
        super(gameManager);
    }

    private long prevBack;

    @Override
    public void create() {
        this.prevBack = System.currentTimeMillis();

        int buttonSize=440;
        float ratioW = ((float)gameManager.getScreenWidth()) /((float)1080);
        float ratioH = ((float)gameManager.getScreenHeight()) /((float)1920);
        int relativeButtonWidth=(int)(ratioW*(float)buttonSize);
        int ws2 = (int)(((float)this.gameManager.getScreenWidth()-relativeButtonWidth)/2);

        // screen 1: goto GameOptionsGameScreen
        this.instances.add(new GameButtonGoto(ws2, (int)(ratioH*200), relativeButtonWidth, (int)(ratioH*buttonSize), R.drawable.bt_start_up, R.drawable.bt_start_down, 1));
        // screen 2: settings
        this.instances.add(new GameButtonGoto(ws2, (int)(ratioH*750), relativeButtonWidth, (int)(ratioH*buttonSize), R.drawable.bt_settings_up, R.drawable.bt_settings_down, 2));
        // screen 3: credits
        this.instances.add(new GameButtonGoto(ws2, (int)(ratioH*1300), relativeButtonWidth, (int)(ratioH*buttonSize), R.drawable.bt_credits_up, R.drawable.bt_credits_down, 3));
    }

    @Override
    public void draw(RenderManager renderManager) {
        renderManager.setColor(Color.WHITE);
        renderManager.paintScreen();
        super.draw(renderManager);
    }

    @Override
    public void update(GameManager gameManager) {
        super.update(gameManager);
        if(gameManager.getInputManager().eventHasOccurred() && gameManager.getInputManager().backOccurred()){
            long dt = System.currentTimeMillis() - this.prevBack;
            this.prevBack = System.currentTimeMillis();
            if(dt<2000){
                gameManager.requestEnd();
            }else{
                gameManager.requestToast("Press again to exit.", false);
            }
        }
    }
}
