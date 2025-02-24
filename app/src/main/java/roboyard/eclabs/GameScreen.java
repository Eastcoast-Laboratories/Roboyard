package roboyard.eclabs;

import java.util.ArrayList;

/**
 * Created by Pierre on 21/01/2015.
 */
public abstract class GameScreen implements IGameObject {
    protected static GameManager gameManager = null;
    protected final ArrayList<IGameObject> instances;

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
        for(IGameObject e : this.instances){
            e.draw(renderManager);
        }
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
}
