package roboyard.eclabs;

import timber.log.Timber;

/**
 * Created by Alain on 31/03/2015.
 */
public class GameButtonGotoBack extends GameButton {

    public GameButtonGotoBack(int x, int y, int w, int h, int imageUp, int imageDown){
        super(x, y, w, h, imageUp, imageDown);
    }

    @Override
    public void onClick(GameManager gameManager) {
        // Check if we're coming from a game screen
        int previousScreenKey = gameManager.getPreviousScreenKey();
        GameScreen previousScreen = gameManager.getScreens().get(previousScreenKey);
        
        if (previousScreen instanceof GridGameScreen) {
            Timber.d("Coming back from game screen, preserving game state");
            // Don't reset the game state when coming back from a game
        } else {
            // Reset to load mode when going back to main menu from other screens
            Timber.d("Coming back from non-game screen, resetting game state");
            if (gameManager.getScreens().get(Constants.SCREEN_GAME) instanceof GridGameScreen) {
                GridGameScreen gameScreen = (GridGameScreen) gameManager.getScreens().get(Constants.SCREEN_GAME);
                gameScreen.setSavedGame(null); // Reset to default state
            }
        }
        
        // If we're in the save game screen, make sure it's in load mode when returning
        if (gameManager.getCurrentScreen() instanceof SaveGameScreen) {
            SaveGameScreen saveScreen = (SaveGameScreen) gameManager.getCurrentScreen();
            saveScreen.setHistoryMode(false); // Ensure we're in save/load mode, not history mode
            saveScreen.setSaveMode(false); // Set to load mode
        }
        
        gameManager.setGameScreen(gameManager.getPreviousScreenKey());
    }
}
