package roboyard.eclabs;

import android.util.SparseArray;
import android.widget.Toast;

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

        // List of all screens
        /* screen 0: start screen
         * screen 1: second start screen (deleted)
         * screen 2: settings
         * screen 3: credits
         * screen 4: start random game
         * screen 5-8: start level game
         * screen 9: save games
         */
        this.screens.append(Constants.SCREEN_START, new MainMenuGameScreen(this));
        this.screens.append(Constants.SCREEN_SETTINGS, new SettingsGameScreen(this));
        this.screens.append(Constants.SCREEN_CREDITS, new CreditsGameScreen(this, activity));
        this.screens.append(Constants.SCREEN_RANDOM_GAME, new GridGameScreen(this));
        this.screens.append(Constants.SCREEN_LEVEL_GAME_START,     new LevelChoiceGameScreen(this, 0, -1, 6));
        this.screens.append(Constants.SCREEN_LEVEL_GAME_START + 1, new LevelChoiceGameScreen(this, 15, 5, 7));
        this.screens.append(Constants.SCREEN_LEVEL_GAME_START + 2, new LevelChoiceGameScreen(this, 30, 6, 8));
        this.screens.append(Constants.SCREEN_LEVEL_GAME_END, new LevelChoiceGameScreen(this, 45, 7, -1));
        this.screens.append(Constants.SCREEN_SAVE_GAMES, new SaveGameScreen(this));
        // End of list of all screens

        this.currentScreen = this.screens.get(Constants.SCREEN_START);
        this.previousScreen = this.screens.get(Constants.SCREEN_START);
    }

    /**
     * Requests to end the application.
     */
    public void requestEnd() {
        this.activity.closeApp();
    }

    /**
     * Requests to restart the application.
     */
    public void requestRestart() {
        this.activity.restartApp();
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
     * Returns the width of the screen.
     *
     * @return Width of the screen.
     */
    public int getScreenWidth() {
        return this.sWidth;
    }

    /**
     * Returns the height of the screen.
     *
     * @return Height of the screen.
     */
    public int getScreenHeight() {
        return this.sHeight;
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
     * Returns the currently active game screen.
     *
     * @return The current GameScreen.
     */
    public GameScreen getCurrentScreen() {
        return this.currentScreen;
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

    /**
     * Destroys all existing game screens and objects.
     */
    public void destroy() {
        for (int i = 0; i < this.screens.size(); i++) {
            this.screens.get(this.screens.keyAt(i)).destroy();
        }
    }

    /**
     * Modifies the current game screen, allowing to switch to another screen.
     *
     * @param nextScreen Index of the new game screen.
     */
    public void setGameScreen(int nextScreen) {
        if (screens.indexOfValue(this.previousScreen) != nextScreen) {
            this.previousScreen = this.currentScreen;
        }
        this.currentScreen = this.screens.get(nextScreen);
    }

    /**
     * Returns the key of the previous game screen.
     *
     * @return Key of the previous game screen.
     */
    public int getPreviousScreenKey() {
        for (int i = 0; i < screens.size(); i++) {
            int key = screens.keyAt(i);
            if (screens.get(key) == this.previousScreen) {
                return key;
            }
        }
        return Constants.SCREEN_START; // Fallback to main menu
    }
}
