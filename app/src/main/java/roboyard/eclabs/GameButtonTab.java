package roboyard.eclabs;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import timber.log.Timber;

/**
 * Button used for tabs in the SaveGameScreen
 */
public class GameButtonTab extends GameButton {
    private final String text;
    private final boolean isActive;
    private final Runnable onClickAction;


    private static final int ACTIVE_COLOR = Color.BLACK;
    private static final int INACTIVE_COLOR = Color.WHITE;
    private static final int ACTIVE_TEXT_COLOR = Color.WHITE;
    private static final int INACTIVE_TEXT_COLOR = Color.BLACK;
    private static final int CORNER_RADIUS = 10;
    
    /**
     * Create a tab button
     * 
     * @param x X position
     * @param y Y position
     * @param width Width of the button
     * @param height Height of the button
     * @param text Text to display on the button
     * @param isActive Whether this tab is currently active
     * @param onClickAction Action to perform when clicked
     */
    public GameButtonTab(int x, int y, int width, int height, String text, boolean isActive, Runnable onClickAction) {
        super(x, y, width, height, 0, 0); // No images for this button
        this.text = text;
        this.isActive = isActive;
        this.onClickAction = onClickAction;
        
        // Set content description for accessibility
        String state = isActive ? "active" : "inactive";
        setContentDescription(text + " tab, currently " + state);
    }
    
    @Override
    public void onClick(GameManager gameManager) {
        Timber.d("Tab button clicked: %s", text);
        if (onClickAction != null) {
            onClickAction.run();
        }
    }
    
    @Override
    public void draw(RenderManager renderManager) {
        // Draw button background
        renderManager.setColor(isActive ? ACTIVE_COLOR : INACTIVE_COLOR);
        renderManager.drawRoundRect(x, y, x + this.getWidth(), y + this.getHeight(), CORNER_RADIUS);
        
        // Draw text
        renderManager.setColor(isActive ? ACTIVE_TEXT_COLOR : INACTIVE_TEXT_COLOR);
        renderManager.setTextSize(this.getHeight() / 2);
        
        // Center text
        float textWidth = renderManager.measureText(text);
        float textX = x + (this.getWidth() - textWidth) / 2;
        float textY = y + this.getHeight() * 0.65f; // Approximate vertical centering
        
        renderManager.drawText((int)textX, (int)textY, text);
    }
    
    // Getter methods for width and height
    public int getWidth() {
        return super.getWidth();
    }
    
    public int getHeight() {
        return super.getHeight();
    }
}
