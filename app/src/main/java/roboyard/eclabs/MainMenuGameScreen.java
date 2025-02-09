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

        int buttonSize = 440;
        float ratio = ((float)gameManager.getScreenWidth()) /((float)1080);
        int relativeButtonWidth = (int)(ratio*(float)buttonSize);
        int ws2 = (int)(((float)this.gameManager.getScreenWidth()-relativeButtonWidth)/2);

        // Random Game (large button)
        this.instances.add(new GameButtonGotoRandomGame(ws2, (int)(ratio*200), relativeButtonWidth, (int)(ratio*buttonSize), R.drawable.bt_start_up_random, R.drawable.bt_start_down_random, Constants.SCREEN_RANDOM_GAME));

        // Level Selection and Load Saved Game (medium buttons side by side)
        int mediumButtonSize = 330; // 75% of the regular size
        int mediumButtonWidth = (int)(ratio*(float)mediumButtonSize);
        int spacing = (int)(ratio*40); // Space between medium buttons
        int totalMediumWidth = mediumButtonWidth * 2 + spacing;
        int startXMedium = (gameManager.getScreenWidth() - totalMediumWidth) / 2;
        int mediumY = (int)(ratio*800); // Position below the random game button

        // Level Selection and Load Saved Game buttons
        this.instances.add(new GameButtonGoto(startXMedium, mediumY, mediumButtonWidth, (int)(ratio*mediumButtonSize), R.drawable.bt_start_up, R.drawable.bt_start_down, Constants.SCREEN_LEVEL_GAME_START));
        this.instances.add(new GameButtonGoto(startXMedium + mediumButtonWidth + spacing, mediumY, mediumButtonWidth, (int)(ratio*mediumButtonSize), R.drawable.bt_start_up_saved, R.drawable.bt_start_down_saved, Constants.SCREEN_SAVE_GAMES));

        // Small buttons at the bottom
        int smallButtonSize = 220; // Half the size of regular buttons
        int smallButtonWidth = (int)(ratio*(float)smallButtonSize);
        int smallSpacing = (int)(ratio*20); // Space between small buttons
        int totalSmallWidth = smallButtonWidth * 2 + smallSpacing;
        int startXSmall = (gameManager.getScreenWidth() - totalSmallWidth) / 2;
        int bottomY = (int)(ratio*1400); // Moved up from 1800 to 1400 to be fully visible

        // Settings and Credits as small buttons at the bottom
        this.instances.add(new GameButtonGoto(startXSmall, bottomY, smallButtonWidth, (int)(ratio*smallButtonSize), R.drawable.bt_settings_up, R.drawable.bt_settings_down, Constants.SCREEN_SETTINGS));
        this.instances.add(new GameButtonGoto(startXSmall + smallButtonWidth + smallSpacing, bottomY, smallButtonWidth, (int)(ratio*smallButtonSize), R.drawable.bt_credits_up, R.drawable.bt_credits_down, Constants.SCREEN_CREDITS));
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
