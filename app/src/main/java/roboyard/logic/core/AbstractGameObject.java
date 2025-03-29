package roboyard.eclabs;

/**
 * Abstract implementation of IGameObject that provides default z-index functionality
 */
public abstract class AbstractGameObject implements IGameObject {
    private int zIndex = 0;
    
    @Override
    public int getZIndex() {
        return zIndex;
    }
    
    @Override
    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
    }
    
    // Other IGameObject methods must be implemented by subclasses
}
