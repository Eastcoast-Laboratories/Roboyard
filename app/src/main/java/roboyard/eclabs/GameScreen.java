package roboyard.eclabs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Pierre on 21/01/2015.
 */
public abstract class GameScreen implements IGameObject {
    protected static GameManager gameManager = null;
    protected final ArrayList<IGameObject> instances;
    private int zIndex = 0;
    private boolean instancesSorted = false;

    public GameScreen(GameManager gameManager){
        this.instances = new ArrayList<IGameObject>();
        GameScreen.gameManager = gameManager;
        this.create();
        this.load(gameManager.getRenderManager());
    }

    @Override
    public abstract void create();

    @Override
    public void load(RenderManager renderManager) {
        for(IGameObject e : this.instances){
            e.load(renderManager);
        }
    }

    @Override
    public void draw(RenderManager renderManager) {
        // Sort instances by z-index before drawing if needed
        if (!instancesSorted) {
            sortInstancesByZIndex();
        }
        
        // Draw in sorted order
        for(IGameObject e : this.instances){
            e.draw(renderManager);
        }
    }
    
    /**
     * Sort the instances list by z-index
     */
    protected void sortInstancesByZIndex() {
        Collections.sort(this.instances, new Comparator<IGameObject>() {
            @Override
            public int compare(IGameObject o1, IGameObject o2) {
                return Integer.compare(o1.getZIndex(), o2.getZIndex());
            }
        });
        instancesSorted = true;
    }
    
    /**
     * Mark the instances list as needing to be sorted again
     * Call this after adding objects or changing z-indices
     */
    protected void markUnsorted() {
        instancesSorted = false;
    }

    @Override
    public void update(GameManager gameManager) {
        for(IGameObject e : this.instances){
            e.update(gameManager);
        }
    }

    @Override
    public void destroy() {
        for(IGameObject e : this.instances){
            e.destroy();
        }
    }

    /**
     * Get all game objects in this screen
     * @return List of game objects
     */
    public ArrayList<IGameObject> getGameObjects() {
        return instances;
    }
    
    /**
     * Add a game object to this screen
     * @param object The object to add
     */
    public void addGameObject(IGameObject object) {
        this.instances.add(object);
        markUnsorted(); // Mark for re-sorting
    }
    
    @Override
    public int getZIndex() {
        return zIndex;
    }
    
    @Override
    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
    }
}
