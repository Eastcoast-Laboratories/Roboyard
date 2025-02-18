package roboyard.eclabs;

/**
 * Created by Pierre on 21/01/2015.
 */
public interface IGameObject {

    void create();
    void load(RenderManager renderManager);
    void draw(RenderManager renderManager);
    void update(GameManager gameManager);
    void destroy();

}
