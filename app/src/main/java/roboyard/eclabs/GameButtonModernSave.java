package roboyard.eclabs;

import timber.log.Timber;

/**
 * Button to open the modern save screen via FragmentHostActivity
 * Updates history entry before opening the save screen
 */
public class GameButtonModernSave extends GameButtonModernUI {
    
    /**
     * @param x x-position
     * @param y y-position
     * @param w width
     * @param h height
     * @param imageUp image when not pressed
     * @param imageDown image when pressed
     */
    public GameButtonModernSave(int x, int y, int w, int h, int imageUp, int imageDown) {
        super(x, y, w, h, imageUp, imageDown, "save");
        setContentDescription("Save Game");
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
        
        // Continue with normal button behavior (open modern UI)
        super.onClick(gameManager);
    }
}
