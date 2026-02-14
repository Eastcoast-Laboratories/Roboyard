package roboyard.ui.components;
import roboyard.ui.components.RenderManager;
import roboyard.eclabs.IGameObject;
import roboyard.ui.components.GameManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Pierre on 21/01/2015.
 */
public abstract class GameScreen implements IGameObject {
    public static GameManager gameManager = null;
    protected List<IGameObject> instances;
    private int zIndex = 0;
    private boolean instancesSorted = false;

    // Queue for objects to add/remove after updates complete
    private final List<IGameObject> pendingAdditions = new ArrayList<>();
    private final List<IGameObject> pendingRemovals = new ArrayList<>();
    private boolean processingUpdates = false;

    public GameScreen(GameManager gameManager){
        this.instances = new CopyOnWriteArrayList<>();
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
        processingUpdates = true;
        for(IGameObject e : this.instances){
            e.update(gameManager);
        }
        processingUpdates = false;
        processPendingChanges();
    }

    @Override
    public void destroy() {
        for(IGameObject e : this.instances){
            e.destroy();
        }
    }

    
    
    
    private void processPendingChanges() {
        for (IGameObject object : pendingAdditions) {
            this.instances.add(object);
        }
        pendingAdditions.clear();
        
        for (IGameObject object : pendingRemovals) {
            this.instances.remove(object);
        }
        pendingRemovals.clear();
        
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
