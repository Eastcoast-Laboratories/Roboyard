package roboyard.eclabs;

import timber.log.Timber;

/**
 * Special button for going to the save screen
 * updates history entry before exiting
 */
public class GameButtonSaveScreen extends GameButtonGoto {

    /**
     * Button to save game and history before exiting game screen
     * 
     * @param x x-position
     * @param y y-position
     * @param w width
     * @param h height
     * @param imageUp image, visible without touch
     * @param imageDown image, visible when touched
     * @param target number of the screen
     */
    public GameButtonSaveScreen(int x, int y, int w, int h, int imageUp, int imageDown, int target) {
        super(x, y, w, h, imageUp, imageDown, target);
    }

    @Override
    public void onClick(GameManager gameManager) {
        // Get current screen
        if (gameManager.getCurrentScreen() instanceof GridGameScreen) {
            GridGameScreen gridGameScreen = (GridGameScreen) gameManager.getCurrentScreen();
            
            // If we have an active history entry, update it before exiting
            if (gridGameScreen.isHistorySaved()) {
                Timber.d("Updating history entry before exiting to save screen");
                gridGameScreen.updateHistoryEntry();
            }
        }
        
        // Continue with normal button behavior (switch screen)
        super.onClick(gameManager);
    }
}
