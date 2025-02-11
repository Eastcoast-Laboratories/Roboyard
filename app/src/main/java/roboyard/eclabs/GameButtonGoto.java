package roboyard.eclabs;

/**
 * Created by Pierre on 21/01/2015.
 */
public class GameButtonGoto extends GameButton {
    private final int targetScreen;

    /**
     * big button to start another screen
     * @param x x-position
     * @param y y-position
     * @param w width
     * @param h height
     * @param imageUp   image, visible without touch
     * @param imageDown image, visible when touched
     * @param target    number of the screen defined in GameManager
         * screen 1: goto GameOptionsGameScreen
         * screen 2: settings (SettingsGameScreen)
         * screen 3: credits (CreditsGameScreen)
         * screen 4: start random game (setRandomGame(), then GridGameScreen)
         * screen 5-8: start a level game (LevelChoiceGameScreen)
         * screen 9: save games (SaveGameScreen)
     */
    public GameButtonGoto(int x, int y, int w, int h, int imageUp, int imageDown, int target){
        super(x, y, w, h, imageUp, imageDown);
        this.targetScreen = target;
    }

    @Override
    public void onClick(GameManager gameManager) {
        gameManager.setGameScreen(this.targetScreen);
    }
}
