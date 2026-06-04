package roboyard.eclabs;
import roboyard.ui.components.RenderManager;
import roboyard.ui.components.GameManager;

/**
 * Created by Pierre on 21/01/2015.
 */
public interface IGameObject {

    void create();
    void load(RenderManager renderManager);
    void draw(RenderManager renderManager);
    void update(GameManager gameManager);
    void destroy();
    
    /**
     * Get the z-index of this game object
     * @return The z-index value (higher values are drawn on top)
     */
    int getZIndex();
    
    /**
     * Set the z-index of this game object
     * @param zIndex The z-index value (higher values are drawn on top)
     */
    void setZIndex(int zIndex);

}
