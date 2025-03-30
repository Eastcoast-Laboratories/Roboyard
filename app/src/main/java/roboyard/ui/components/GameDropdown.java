package roboyard.ui.components;
import roboyard.eclabs.Constants;
import roboyard.eclabs.GameManager;
import roboyard.ui.components.RenderManager;
import roboyard.eclabs.IGameObject;
import roboyard.eclabs.util.AccessibilityUtil;

import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;
import android.content.Context; // Import Context

public class GameDropdown implements IGameObject {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final List<String> options;
    private final List<Runnable> actions;
    private boolean isOpen;
    private int selectedIndex;
    private final Paint paint;
    private final int backgroundColor = 0xFF404040;
    private final int textColor = 0xFFFFFFFF;
    private final int highlightColor = 0xFF606060;
    private int zIndex = Constants.UI_ELEMENT; // Default z-index for UI elements
    private String contentDescription; // Add contentDescription field
    
    // For accessibility - whether dropdown can receive focus
    private boolean focusable = true;
    // For accessibility - whether dropdown is currently focused
    private boolean focused = false;

    public GameDropdown(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.options = new ArrayList<>();
        this.actions = new ArrayList<>();
        this.isOpen = false;
        this.selectedIndex = 0;
        
        this.paint = new Paint();
        this.paint.setTextSize(height * 0.6f);
        this.paint.setColor(textColor);
        this.paint.setAntiAlias(true);
    }

    public void addOption(String option, Runnable action) {
        options.add(option);
        actions.add(action);
    }
    
    /**
     * Sets the selected index of the dropdown
     * @param index The index to select
     */
    public void setSelectedIndex(int index) {
        if (index >= 0 && index < options.size()) {
            selectedIndex = index;
        }
    }

    @Override
    public void create() {
    }

    @Override
    public void destroy() {
        // Nothing to clean up
    }

    @Override
    public void load(RenderManager renderManager) {
    }

    @Override
    public void update(GameManager gameManager) {
        InputManager input = gameManager.getInputManager();
        if (input.eventHasOccurred() && input.downOccurred()) {
            float touchX = input.getTouchX();
            float touchY = input.getTouchY();

            // Check if click is in dropdown area
            if (touchX >= x && touchX <= x + width) {
                if (!isOpen) {
                    // Check header click
                    if (touchY >= y && touchY <= y + height) {
                        isOpen = true;
                        input.resetEvents();
                    }
                } else {
                    // Check option clicks
                    int totalHeight = (options.size() + 1) * height;
                    if (touchY >= y && touchY <= y + totalHeight) {
                        int clickedIndex = (int)((touchY - y) / height - 1);
                        if (clickedIndex >= 0 && clickedIndex < options.size()) {
                            selectedIndex = clickedIndex;
                            actions.get(clickedIndex).run();
                        }
                        isOpen = false;
                        input.resetEvents();
                    } else {
                        isOpen = false;
                    }
                }
            } else if (isOpen) {
                isOpen = false;
            }
        }
    }

    @Override
    public void draw(RenderManager renderManager) {
        // Draw dropdown header background
        renderManager.setColor(backgroundColor);
        renderManager.drawRect(x, y, x + width, y + height);
        
        // Draw dropdown header text
        String selectedText = options.get(selectedIndex);
        float textWidth = paint.measureText(selectedText);
        float textX = x + (width - textWidth) / 2;
        float textY = y + height * 0.7f;
        renderManager.setColor(textColor);
        renderManager.drawText((int)textX, (int)textY, selectedText);
        
        // Draw dropdown arrow
        String arrow = isOpen ? "▲" : "▼";
        float arrowWidth = paint.measureText(arrow);
        renderManager.drawText((int)(x + width - arrowWidth - 10), (int)textY, arrow);

        // Draw options if open
        if (isOpen) {
            for (int i = 0; i < options.size(); i++) {
                int optionY = y + (i + 1) * height;
                renderManager.setColor(i == selectedIndex ? highlightColor : backgroundColor);
                renderManager.drawRect(x, optionY, x + width, optionY + height);
                
                String optionText = options.get(i);
                float optionWidth = paint.measureText(optionText);
                float optionX = x + (width - optionWidth) / 2;
                float optionTextY = optionY + height * 0.7f;
                renderManager.setColor(textColor);
                renderManager.drawText((int)optionX, (int)optionTextY, optionText);
            }
        }
        
        // Draw focus indicator for accessibility when focused
        if (focused && renderManager.getContext() != null && 
            AccessibilityUtil.isScreenReaderActive(renderManager.getContext())) {
            // Save current paint settings
            renderManager.save();
            
            // Draw a highlighted border around the focused dropdown
            renderManager.setColor(0xFFFFFF00); // Yellow
            renderManager.setStrokeWidth(4);
            renderManager.drawRect(this.x - 2, this.y - 2, this.x + this.width + 4, this.y + this.height + 4);
            
            // If expanded, also draw a border around the expanded options
            if (isOpen) {
                int totalHeight = (options.size() + 1) * height;
                renderManager.drawRect(this.x - 2, this.y - 2, 
                                     this.x + this.width + 4, this.y + totalHeight + 4);
            }
            
            // Restore previous paint settings
            renderManager.restore();
        }
        
        // Draw debug information for content descriptions
        drawContentDescriptionDebug(renderManager);
    }

    /**
     * Draw debug information for content descriptions
     * @param renderManager The render manager to use for drawing
     */
    private void drawContentDescriptionDebug(RenderManager renderManager) {
        if (contentDescription != null && !contentDescription.isEmpty() && 
            renderManager.getContext() != null && 
            AccessibilityUtil.isScreenReaderActive(renderManager.getContext())) {
            // Save current drawing state
            renderManager.save();
            
            // Draw a semi-transparent background for better readability
            renderManager.setColor(0x80000000); // Semi-transparent black
            renderManager.fillRect(this.x, this.y + this.height, this.x + this.width, this.y + this.height + 30);
            
            // Draw the content description text
            renderManager.setColor(0xFFFFFFFF); // White
            renderManager.setTextSize(16);
            renderManager.drawText(this.x + 5, this.y + this.height + 20, contentDescription);
            
            // Restore previous drawing state
            renderManager.restore();
        }
    }

    /**
     * Get the z-index of this game object
     * @return The z-index value
     */
    @Override
    public int getZIndex() {
        return zIndex;
    }
    
    /**
     * Set the z-index of this game object
     * @param zIndex The z-index value
     */
    @Override
    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
    }

    /**
     * Sets the content description for accessibility
     * @param description The description to be read by screen readers
     */
    public void setContentDescription(String description) {
        this.contentDescription = description;
    }
    
    /**
     * Gets the content description for accessibility
     * @return The description to be read by screen readers
     */
    public String getContentDescription() {
        return contentDescription;
    }
    
    /**
     * Sets the content description for accessibility only if a screen reader is active
     * @param context The context to check for screen reader
     * @param description The description to set
     */
    public void setAccessibleContentDescription(Context context, String description) {
        if (AccessibilityUtil.isScreenReaderActive(context)) {
            this.contentDescription = description;
        }
    }

    /**
     * Set whether this dropdown is focusable by TalkBack
     * @param focusable true if dropdown should be focusable, false otherwise
     */
    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }
    
    /**
     * Check if this dropdown is focusable
     * @return true if focusable, false otherwise
     */
    public boolean isFocusable() {
        return focusable;
    }
    
    /**
     * Set the focused state of this dropdown
     * @param focused true if focused, false otherwise
     */
    public void setFocused(boolean focused) {
        this.focused = focused;
    }
    
    /**
     * Check if this dropdown currently has focus
     * @return true if focused, false otherwise
     */
    public boolean isFocused() {
        return focused;
    }
}
