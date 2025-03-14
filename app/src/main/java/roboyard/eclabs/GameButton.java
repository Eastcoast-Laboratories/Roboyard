package roboyard.eclabs;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.core.content.res.ResourcesCompat;

/**
 * Created by Pierre on 21/01/2015.
 */
public abstract class GameButton implements IGameObject {

    int x;
    int y;
    private final int w;
    private final int h;
    private int zIndex = Constants.UI_ELEMENT; // Default z-index for UI elements

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
    }


    @Override
    public void update(GameManager gameManager) {
        if(!this.enabled){
            return;
        }
        InputManager inputManager = gameManager.getInputManager();
        if(inputManager.eventHasOccurred()){
            float x = inputManager.getTouchX(), y = inputManager.getTouchY();
            if(inputManager.downOccurred()){
                this.btDown = (x > this.x  && x < this.x+this.w && y > this.y  && y < this.y+this.h);
            }
            if(inputManager.moveOccurred()){
                this.btDown = this.btDown & (x > this.x  && x < this.x+this.w && y > this.y  && y < this.y+this.h);
            }
            if(inputManager.upOccurred()){
                if(this.btDown){
                    this.btDown = false;
                    this.onClick(gameManager);
                }
            }
        }
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
