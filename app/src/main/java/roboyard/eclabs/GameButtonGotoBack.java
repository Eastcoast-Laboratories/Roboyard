package roboyard.eclabs;

/**
 * Created by Alain on 31/03/2015.
 */
public class GameButtonGotoBack extends GameButton {

    public GameButtonGotoBack(int x, int y, int w, int h, int imageUp, int imageDown){
        super(x, y, w, h, imageUp, imageDown);
    }

    @Override
    public void onClick(GameManager gameManager) {
        // Reset to load mode when going back to main menu
        if (gameManager.getScreens().get(Constants.SCREEN_GAME) instanceof GridGameScreen) {
            ((GridGameScreen) gameManager.getScreens().get(Constants.SCREEN_GAME)).isRandomGame = false;
        }
        gameManager.setGameScreen(gameManager.getPreviousScreenKey());
    }
}
