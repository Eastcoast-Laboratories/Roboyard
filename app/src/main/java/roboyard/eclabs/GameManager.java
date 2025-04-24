package roboyard.eclabs;
import roboyard.logic.core.Constants;
import roboyard.ui.components.RenderManager;
import roboyard.ui.activities.MainActivity;
import roboyard.ui.components.GameScreen;

import android.util.SparseArray;

import roboyard.ui.components.InputManager;

/**
 * Manages the game screens and provides methods to interact with them.
 * Created by Pierre on 04/02/2015.
 */
public class GameManager {
    private GameScreen currentScreen;
    private GameScreen previousScreen;
    private final SparseArray<GameScreen> screens;
    private final InputManager inputManager;
    private final RenderManager renderManager;
    private final int sWidth;
    private final int sHeight;
    private final MainActivity activity;

    /**
     * Returns the main activity instance associated with the game manager.
     * It allows other classes and components to interact with the main activity,
     * which is necessary for performing various tasks and accessing resources.
     *
     * @return The main activity instance.
     */
    public MainActivity getActivity() {
        return this.activity;
    }

    /**
     * Constructor for the GameManager class.
     * Initializes the GameManager with necessary components.
     *
     * @param inputManager  Reference to the input manager (InputManager).
     * @param renderManager Reference to the render manager (RenderManager).
     * @param sWidth        Width of the screen.
     * @param sHeight       Height of the screen.
     * @param activity      The main activity instance.
     */
    public GameManager(InputManager inputManager, RenderManager renderManager, int sWidth, int sHeight, MainActivity activity) {
        this.inputManager = inputManager;
        this.renderManager = renderManager;
        this.sWidth = sWidth;
        this.sHeight = sHeight;
        this.screens = new SparseArray<>();
        this.activity = activity;

        this.currentScreen = this.screens.get(Constants.SCREEN_START);
        this.previousScreen = this.screens.get(Constants.SCREEN_START);
    }

    /**
     * Requests to display a toast message.
     *
     * @param str The message to display.
     * @param big Flag indicating if the toast should be large.
     */
    public void requestToast(CharSequence str, boolean big) {
        this.activity.doToast(str, big);
    }

    /**
     * Returns the SparseArray containing all game screens.
     *
     * @return SparseArray of GameScreens.
     */
    public SparseArray<GameScreen> getScreens() {
        return this.screens;
    }

    /**
     * Returns the render manager instance.
     *
     * @return RenderManager instance.
     */
    public RenderManager getRenderManager() {
        return this.renderManager;
    }

    /**
     * Returns the input manager instance.
     *
     * @return InputManager instance.
     */
    public InputManager getInputManager() {
        return this.inputManager;
    }

    /**
     * Gets the current screen.
     *
     * @return The current screen.
     */
    public GameScreen getCurrentScreen() {
        return currentScreen;
    }

    /**
     * Updates the currently active game screen.
     * Updates all objects belonging to this screen.
     */
    public void update() {
        this.currentScreen.update(this);
    }

    /**
     * Draws the currently active game screen.
     * Draws all objects belonging to this screen.
     */
    public void draw() {
        this.currentScreen.draw(this.renderManager);
    }

    public void announce(String s) {
        requestToast(s, false);
    }
}
