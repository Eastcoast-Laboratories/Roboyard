package roboyard.eclabs;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.content.Context;

import roboyard.ui.components.InputManager;

/**
 * Created by Pierre on 21/01/2015.
 */
public abstract class GameButton implements IGameObject {

    int x;
    int y;
    private final int w;
    private final int h;
    private int zIndex = Constants.UI_ELEMENT; // Default z-index for UI elements
    private String contentDescription; // For accessibility

    // For accessibility - whether button can receive focus
    private boolean focusable = true;
    // For accessibility - whether button is currently focused
    private boolean focused = false;

    /** enable or disable the button
     * @param enabled boolean
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setImageUp(Drawable imageUp) {
        this.imageUpDrawable = imageUp;
        this.imageUp = 0; // Clear the resource ID when setting drawable
    }

    public void setImageDown(Drawable imageDown) {
        this.imageDownDrawable = imageDown;
        this.imageDown = 0; // Clear the resource ID when setting drawable
    }

    /**
     * Set the content description for accessibility
     * @param description The description to be read by screen readers
     */
    public void setContentDescription(String description) {
        this.contentDescription = description;
    }

    /**
     * Get the content description for accessibility
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

    private int imageUp;
    private int imageDown;
    private Drawable imageUpDrawable;
    private Drawable imageDownDrawable;
    private int imageDisabled;
    protected boolean btDown, enabled = true;

    public GameButton(int x, int y, int w, int h, int imageUp, int imageDown){
        this.create();
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.imageDown = imageDown;
        this.imageUp = imageUp;
        this.imageDisabled = imageUp;
        this.imageUpDrawable = null;
        this.imageDownDrawable = null;
        this.contentDescription = null; // Initialize content description
    }

    public void setImageDisabled(int imageDisabled){
        this.imageDisabled = imageDisabled;
    }

    @Override
    public void create() {
        this.btDown = false;
    }

    @Override
    public void load(RenderManager renderManager) {
        if (imageUpDrawable == null) {
            // Only load images if they have valid resource IDs
            if (this.imageUp != 0) {
                renderManager.loadImage(this.imageUp);
            }
            if (this.imageDown != 0) {
                renderManager.loadImage(this.imageDown);
            }
        }
    }

    @Override
    public void draw(RenderManager renderManager) {
        renderManager.setColor(Color.WHITE);
        if(!enabled){
            if (this.imageDisabled != 0) {
                renderManager.drawImage(this.x, this.y, this.x + this.w, this.y + this.h, this.imageDisabled);
            }
            return;
        }
        if(btDown){
            if (imageDownDrawable != null) {
                renderManager.drawDrawable(this.x, this.y, this.x + this.w, this.y + this.h, this.imageDownDrawable);
            } else if (this.imageDown != 0) {
                renderManager.drawImage(this.x, this.y, this.x + this.w, this.y + this.h, this.imageDown);
            }
        } else {
            if (imageUpDrawable != null) {
                renderManager.drawDrawable(this.x, this.y, this.x + this.w, this.y + this.h, this.imageUpDrawable);
            } else if (this.imageUp != 0) {
                renderManager.drawImage(this.x, this.y, this.x + this.w, this.y + this.h, this.imageUp);
            }
        }
        
        // Draw focus indicator for accessibility when focused
        if (focused && renderManager.getContext() != null && 
            AccessibilityUtil.isScreenReaderActive(renderManager.getContext())) {
            // Save current paint settings
            renderManager.save();
            
            // Draw a highlighted border around the focused button
            renderManager.setColor(Color.YELLOW);
            renderManager.setStrokeWidth(4);
            renderManager.drawRect(this.x - 2, this.y - 2, this.x + this.w + 4, this.y + this.h + 4);
            
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
            renderManager.setColor(Color.argb(120, 0, 0, 0));
            renderManager.fillRect(this.x, this.y + this.h, this.x + this.w, this.y + this.h + 30);
            
            // Draw the content description text
            renderManager.setColor(Color.WHITE);
            renderManager.setTextSize(16);
            renderManager.drawText(this.x + 5, this.y + this.h + 20, contentDescription);
            
            // Restore previous drawing state
            renderManager.restore();
        }
    }

    @Override
    public void update(GameManager gameManager) {
        if(!this.enabled){
            return;
        }
        
        InputManager inputManager = gameManager.getInputManager();
        
        if(inputManager.eventHasOccurred()){
            float x = inputManager.getTouchX(), y = inputManager.getTouchY();
            
            // Check if TalkBack is active
            boolean talkbackActive = false;
            if (gameManager.getActivity() != null) {
                talkbackActive = AccessibilityUtil.isScreenReaderActive(gameManager.getActivity());
            }
            
            // Handle TalkBack explore by touch - set focus when touched
            if (talkbackActive && inputManager.downOccurred()) {
                boolean isTouched = (x > this.x && x < this.x+this.w && y > this.y && y < this.y+this.h);
                if (isTouched) {
                    // Set focus on this button
                    setFocused(true);
                    
                    // Announce the content description
                    if (contentDescription != null && !contentDescription.isEmpty()) {
                        announceForAccessibility(gameManager, contentDescription);
                    }
                    
                    // Prevent other buttons from processing this event
                    inputManager.consumeEvent();
                    return;
                } else {
                    // Remove focus if touched elsewhere
                    setFocused(false);
                }
            }
            
            // Regular touch handling (for non-TalkBack or when TalkBack is in touch mode)
            if(inputManager.downOccurred()){
                this.btDown = (x > this.x && x < this.x+this.w && y > this.y && y < this.y+this.h);
            }
            
            if(inputManager.moveOccurred()){
                this.btDown = this.btDown & (x > this.x && x < this.x+this.w && y > this.y && y < this.y+this.h);
            }
            
            if(inputManager.upOccurred()){
                if(this.btDown){
                    this.btDown = false;
                    
                    // For TalkBack, only trigger click if this button has focus
                    if (!talkbackActive || (talkbackActive && focused)) {
                        // Perform the click action
                        this.onClick(gameManager);
                        
                        // If TalkBack is active, announce that the button was activated
                        if (talkbackActive && contentDescription != null && !contentDescription.isEmpty()) {
                            announceForAccessibility(gameManager, contentDescription + " activated");
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Announce a message for accessibility services like screen readers
     * @param gameManager The game manager instance
     * @param message The message to announce
     */
    protected void announceForAccessibility(GameManager gameManager, String message) {
        if (gameManager != null && gameManager.getActivity() != null && message != null) {
            AccessibilityUtil.announceForAccessibility(gameManager.getActivity(), message);
        }
    }

    /**
     * Set whether this button is focusable by TalkBack
     * @param focusable true if button should be focusable, false otherwise
     */
    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }
    
    /**
     * Check if this button is focusable
     * @return true if focusable, false otherwise
     */
    public boolean isFocusable() {
        return focusable;
    }
    
    /**
     * Set the focused state of this button (for TalkBack)
     * @param focused True if focused, false otherwise
     */
    public void setFocused(boolean focused) {
        this.focused = focused;
    }
    
    /**
     * Get the focused state of this button (for TalkBack)
     * @return True if focused, false otherwise
     */
    public boolean isFocused() {
        return focused;
    }

    public abstract void onClick(GameManager gameManager);

    @Override
    public void destroy(){

    }

    /**
     * Get the width of the button
     * @return The width
     */
    public int getWidth() {
        return w;
    }
    
    /**
     * Get the height of the button
     * @return The height
     */
    public int getHeight() {
        return h;
    }
    
    /**
     * Get the X position of the button
     * @return The X position
     */
    public int getPositionX() {
        return x;
    }
    
    /**
     * Get the Y position of the button
     * @return The Y position
     */
    public int getPositionY() {
        return y;
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
}
