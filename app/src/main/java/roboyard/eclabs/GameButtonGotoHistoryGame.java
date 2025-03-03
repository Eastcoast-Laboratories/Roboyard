package roboyard.eclabs;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
    private Bitmap minimapBitmap;
    private Activity activity;
    
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
    
    /**
     * Create a button for a history entry with activity
     * 
     * @param x X position
     * @param y Y position
     * @param width Width of the button
     * @param height Height of the button
     * @param historyEntry The history entry this button represents
     * @param activity The activity reference
     */
    public GameButtonGotoHistoryGame(int x, int y, int width, int height, 
                                    GameHistoryEntry historyEntry, Activity activity) {
        super(x, y, width, height, 0, 0); // No images for this button
        this.historyEntry = historyEntry;
        this.historyIndex = 0;
        this.activity = activity;
    }
    
    @Override
    public void create() {
        super.create();
        
        // Try to load the minimap immediately if we have an activity
        if (activity != null) {
            loadMinimap();
        }
    }
    
    /**
     * Load the minimap for this history entry
     */
    public void loadMinimap() {
        if (minimapBitmap == null && activity != null) {
            String historyPath = "history/" + historyEntry.getMapPath();
            
            // Read the save data from the history entry
            String saveData = FileReadWrite.readPrivateData(activity, historyPath);
            
            if (saveData != null && !saveData.isEmpty()) {
                // Create minimap
                String minimapPath = GameButtonGotoSavedGame.createMiniMap(saveData, activity);
                if (minimapPath != null) {
                    try {
                        // Load minimap bitmap
                        minimapBitmap = BitmapFactory.decodeFile(minimapPath);
                        Timber.d("Loaded minimap for history entry: %s", historyEntry.getMapName());
                    } catch (Exception e) {
                        Timber.e(e, "Failed to load minimap for history entry");
                    }
                }
            }
        }
    }
    
    @Override
    public void update(GameManager gameManager) {
        super.update(gameManager);
        
        // Store activity reference
        if (activity == null) {
            activity = gameManager.getActivity();
        }
    }
    
    /**
     * Update the button state
     * @deprecated Use update(GameManager) instead
     */
    @Deprecated
    public void update() {
        // Deprecated method, use update(GameManager) instead
        // This is kept for backward compatibility
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
        renderManager.fillRect(x, y, x + getWidth(), y + getHeight());
        
        // Draw button border
        renderManager.setColor(Color.BLACK);
        renderManager.drawRect(x, y, x + getWidth(), y + getHeight());
        
        // Draw minimap if available
        if (minimapBitmap != null) {
            int minimapSize = getHeight() - 20;
            int minimapX = x + 10;
            int minimapY = y + 10;
            
            // Draw minimap
            renderManager.drawBitmap(minimapBitmap, minimapX, minimapY, minimapX + minimapSize, minimapY + minimapSize);
            
            // Draw history entry information to the right of the minimap
            renderManager.setColor(TEXT_COLOR);
            renderManager.setTextSize(getHeight() / 6);
            
            int textPadding = 10;
            int textX = minimapX + minimapSize + 20;
            int textY = y + textPadding + renderManager.getTextSize();
            
            // Format timestamp
            String formattedDate = historyEntry.getFormattedDateTime();
            
            // Draw map name
            renderManager.drawText(textX, textY, historyEntry.getMapName());
            textY += renderManager.getTextSize() + 5;
            
            // Draw date/time
            renderManager.drawText(textX, textY, "Date: " + formattedDate);
            textY += renderManager.getTextSize() + 5;
            
            // Draw duration
            renderManager.drawText(textX, textY, "Duration: " + historyEntry.getFormattedDuration());
            textY += renderManager.getTextSize() + 5;
            
            // Draw moves
            renderManager.drawText(textX, textY, "Moves: " + historyEntry.getMovesMade());
        } else {
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
    }
    
    /**
     * Get the history entry associated with this button
     * @return the history entry
     */
    public GameHistoryEntry getHistoryEntry() {
        return historyEntry;
    }
    
    /**
     * Set the minimap bitmap for this button
     * @param bitmap the minimap bitmap
     */
    public void setMinimapBitmap(Bitmap bitmap) {
        this.minimapBitmap = bitmap;
    }
    
    // Getter methods for width and height
    public int getWidth() {
        return super.getWidth();
    }
    
    public int getHeight() {
        return super.getHeight();
    }
}
