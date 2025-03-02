package roboyard.eclabs;

import android.app.Activity;
import android.graphics.Color;
import timber.log.Timber;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Button to load a game from history
 */
public class GameButtonGotoHistoryGame extends GameButton {
    private final GameHistoryEntry historyEntry;
    private final int historyIndex;
    
    private static final int BUTTON_COLOR = Color.parseColor("#E0E0E0");
    private static final int TEXT_COLOR = Color.BLACK;
    private static final int HIGHLIGHT_COLOR = Color.parseColor("#BBDEFB");
    
    /**
     * Create a button for a history entry
     * 
     * @param x X position
     * @param y Y position
     * @param width Width of the button
     * @param height Height of the button
     * @param historyEntry The history entry this button represents
     * @param historyIndex Index of this history entry
     */
    public GameButtonGotoHistoryGame(int x, int y, int width, int height, 
                                    GameHistoryEntry historyEntry, int historyIndex) {
        super(x, y, width, height, 0, 0); // No images for this button
        this.historyEntry = historyEntry;
        this.historyIndex = historyIndex;
    }
    
    @Override
    public void onClick(GameManager gameManager) {
        Timber.d("History game button clicked: %s", historyEntry.getMapName());
        
        Activity activity = gameManager.getActivity();
        String historyPath = "history/" + historyEntry.getMapPath();
        
        // Read the save data from the history entry
        String saveData = FileReadWrite.readPrivateData(activity, historyPath);
        
        if (saveData != null && !saveData.isEmpty()) {
            // Load the game from history
            GridGameScreen gameScreen = (GridGameScreen) gameManager.getScreens().get(Constants.SCREEN_GAME);
            gameScreen.setSavedGame(historyPath);
            
            // Switch to game screen
            gameManager.setGameScreen(Constants.SCREEN_GAME);
        } else {
            // Show error message
            gameManager.requestToast("Could not load history entry", true);
        }
    }
    
    @Override
    public void draw(RenderManager renderManager) {
        // Draw button background
        renderManager.setColor(BUTTON_COLOR);
        renderManager.drawRect(x, y, x + getWidth(), y + getHeight());
        
        // Draw button border
        renderManager.setColor(Color.BLACK);
        renderManager.drawRect(x, y, x + getWidth(), y + getHeight());
        
        // Draw history entry information
        renderManager.setColor(TEXT_COLOR);
        renderManager.setTextSize(getHeight() / 6);
        
        int textPadding = 10;
        int textY = y + textPadding + renderManager.getTextSize();
        
        // Format timestamp
        String formattedDate = historyEntry.getFormattedDateTime();
        
        // Draw map name
        renderManager.drawText(x + textPadding, textY, historyEntry.getMapName());
        textY += renderManager.getTextSize() + 5;
        
        // Draw date/time
        renderManager.drawText(x + textPadding, textY, "Date: " + formattedDate);
        textY += renderManager.getTextSize() + 5;
        
        // Draw duration
        renderManager.drawText(x + textPadding, textY, "Duration: " + historyEntry.getFormattedDuration());
        textY += renderManager.getTextSize() + 5;
        
        // Draw moves
        renderManager.drawText(x + textPadding, textY, "Moves: " + historyEntry.getMovesMade());
    }
    
    // Getter methods for width and height
    public int getWidth() {
        return super.getWidth();
    }
    
    public int getHeight() {
        return super.getHeight();
    }
}
