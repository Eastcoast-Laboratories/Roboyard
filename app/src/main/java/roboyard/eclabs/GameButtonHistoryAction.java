package roboyard.eclabs;

import android.app.Activity;
import android.graphics.Color;
import timber.log.Timber;

/**
 * Button for history entry actions (delete, promote, etc.)
 */
public class GameButtonHistoryAction extends GameButton {
    private final int actionType;
    private final int historyIndex;
    private final GameHistoryEntry historyEntry;
    
    // Action types
    public static final int ACTION_DELETE = 0;
    public static final int ACTION_PROMOTE = 1;
    public static final int ACTION_SHARE = 2;
    
    private static final int[] ACTION_COLORS = {
        Color.parseColor("#F44336"),  // Red for delete
        Color.parseColor("#2196F3"),  // Blue for promote
        Color.parseColor("#4CAF50")   // Green for share
    };
    
    private static final String[] ACTION_LABELS = {
        "✕",  // Delete
        "⭐",  // Promote
        "⇪"   // Share
    };
    
    /**
     * Create a history action button
     * 
     * @param x X position
     * @param y Y position
     * @param size Size of the button (square)
     * @param actionType Type of action (use ACTION_* constants)
     * @param historyIndex Index of the history entry
     * @param historyEntry The history entry this button acts on
     */
    public GameButtonHistoryAction(int x, int y, int size, int actionType, int historyIndex, GameHistoryEntry historyEntry) {
        super(x, y, size, size, 0, 0); // No images for this button
        this.actionType = actionType;
        this.historyIndex = historyIndex;
        this.historyEntry = historyEntry;
    }
    
    @Override
    public void onClick(GameManager gameManager) {
        Timber.d("History action button clicked: %s for entry %s", 
                ACTION_LABELS[actionType], historyEntry.getFormattedDateTime());
        
        Activity activity = gameManager.getActivity();
        
        switch (actionType) {
            case ACTION_DELETE:
                // Delete history entry
                GameHistoryManager.deleteHistoryEntry(activity, historyEntry);
                // Refresh the screen
                SaveGameScreen saveScreen = (SaveGameScreen) gameManager.getCurrentScreen();
                saveScreen.createButtons();
                break;
                
            case ACTION_PROMOTE:
                // Promote history entry to save
                // TODO: check if already in savegames, then show a message toast
                int saveSlot = GameHistoryManager.promoteHistoryEntryToSave(activity, historyIndex);
                if(saveSlot >= 0) {
                    gameManager.requestToast("Map saved in Savegames: " + saveSlot , true);
                }else {
                    gameManager.requestToast("Failed to save map", true);
                }
                break;
                
            case ACTION_SHARE:
                // Share history entry
                String historyPath = historyEntry.getMapPath();
                String saveData = FileReadWrite.readPrivateData(activity, historyPath);
                if (saveData != null && !saveData.isEmpty()) {
                    // Use existing share functionality if available
                    // For now, just show a toast message
                    gameManager.requestToast("Sharing not implemented yet", true);
                }
                break;
        }
    }
    
    @Override
    public void draw(RenderManager renderManager) {
        // Draw button background
        renderManager.setColor(ACTION_COLORS[actionType]);
        renderManager.fillRect(x, y, x + getWidth(), y + getHeight());
        
        // Draw button border
        renderManager.setColor(Color.BLACK);
        renderManager.drawRect(x, y, x + getWidth(), y + getHeight());
        
        // Draw action symbol
        renderManager.setColor(Color.WHITE);
        renderManager.setTextSize((int)(getHeight() * 0.6f));
        
        // Center text
        float textWidth = renderManager.measureText(ACTION_LABELS[actionType]);
        float textX = x + (getWidth() - textWidth) / 2;
        float textY = y + getHeight() * 0.7f; // Approximate vertical centering
        
        renderManager.drawText((int)textX, (int)textY, ACTION_LABELS[actionType]);
    }
    
    // Getter methods for width and height
    public int getWidth() {
        return super.getWidth();
    }
    
    public int getHeight() {
        return super.getHeight();
    }
}
