package roboyard.eclabs;

import android.graphics.Color;

/**
 * Created by Pierre on 21/01/2015.
 */
public class GameOptionsGameScreen extends GameScreen {


    public GameOptionsGameScreen(GameManager gameManager){
        super(gameManager);
    }

    @Override
    public void create() {
        int ws2 = this.gameManager.getScreenWidth()/2;
        int hs2 = this.gameManager.getScreenHeight()/2;
        this.instances.add(new GameButtonGotoRandomGame(ws2 - 128, hs2 - 512, 256, 256, R.drawable.bt_start_up_random, R.drawable.bt_start_down_random, 4));
        this.instances.add(new GameButtonGoto(ws2-128, hs2-128, 256, 256, R.drawable.bt_start_up, R.drawable.bt_start_down, 5));
        this.instances.add(new GameButtonGoto(ws2-128, hs2+256, 256, 256, R.drawable.bt_start_up_saved, R.drawable.bt_start_down_saved, 9));
    }

    @Override
    public void load(RenderManager renderManager) {
        super.load(renderManager);
    }

    @Override
    public void draw(RenderManager renderManager) {
//        renderManager.setColor(Color.GREEN);
//        renderManager.setColor(Color.parseColor("#FFAE07"));
        renderManager.setColor(Color.parseColor("#E3C898"));
        renderManager.paintScreen();
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
}
