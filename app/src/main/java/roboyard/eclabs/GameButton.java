package roboyard.eclabs;

import android.graphics.Color;

/**
 * Created by Pierre on 21/01/2015.
 */
public abstract class GameButton implements IGameObject {

    int x;
    int y;
    private final int w;
    private final int h;

    /** enable or disable the button
     * @param enabled boolean
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setImageUp(int imageUp) {
        this.imageUp = imageUp;
    }

    public void setImageDown(int imageDown) {
        this.imageDown = imageDown;
    }

    private int imageUp, imageDown, imageDisabled;
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
        renderManager.loadImage(this.imageDown);
        renderManager.loadImage(this.imageUp);
    }

    @Override
    public void draw(RenderManager renderManager) {
        renderManager.setColor(Color.WHITE);
        if(!this.enabled){
            renderManager.drawImage(this.x, this.y, this.x + this.w, this.y + this.h, this.imageDisabled);
        }else if(this.btDown){
            renderManager.drawImage(this.x, this.y, this.x + this.w, this.y + this.h, this.imageDown);
        }else{
            renderManager.drawImage(this.x, this.y, this.x + this.w, this.y + this.h, this.imageUp);
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
}
