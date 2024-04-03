package roboyard.eclabs;

import android.util.SparseArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Represents a button that navigates to a saved game.
 */
public class GameButtonGotoSavedGame extends GameButtonGoto {

    private String mapPath = null;

    /**
     * Constructs a GameButtonGotoSavedGame object.
     * @param x The x-coordinate of the button.
     * @param y The y-coordinate of the button.
     * @param w The width of the button.
     * @param h The height of the button.
     * @param imageUp The image resource ID when the button is not pressed.
     * @param imageDown The image resource ID when the button is pressed.
     * @param target The target screen to navigate to.
     * @param filePath The file path of the saved game.
     */
    public GameButtonGotoSavedGame(float x, float y, float w, float h, int imageUp, int imageDown, int target, String filePath) {
        super((int)x, (int)y, (int)w, (int)h, imageUp, imageDown, target);
        mapPath = filePath;
    }

    /**
     * Handles the click event of the button.
     * @param gameManager The GameManager instance.
     */
    @Override
    public void onClick(GameManager gameManager) {
        if (gameManager.getPreviousScreenKey() == 4) {
            // Screen to save or overwrite a savegame

            ArrayList gridElements = ((GridGameScreen)gameManager.getScreens().get(gameManager.getPreviousScreenKey())).getGridElements();

            FileReadWrite.clearPrivateData(gameManager.getActivity(), mapPath);
            FileReadWrite.writePrivateData(gameManager.getActivity(), mapPath, MapObjects.createStringFromList(gridElements, false));
            addMapsSaved(gameManager);
            SparseArray<GameScreen> screens = gameManager.getScreens();
            for (int i = 0; i < screens.size(); i++) {
                if (screens.get(i).getClass() == SaveGameScreen.class) {
                    // redraw save buttons with new state of the saved game
                    ((SaveGameScreen) screens.get(i)).createButtons();
                }
            }
        } else {
            // Screen to select a savegame
            SaveManager saver = new SaveManager(gameManager.getActivity());
            if (saver.getMapsStateSaved(mapPath, "mapsSaved.txt")) {
                super.onClick(gameManager);
                ((GridGameScreen) (gameManager.getScreens().get(4))).setSavedGame(mapPath);
                // disable the savegame button in the gamescreen
                ((GridGameScreen) (gameManager.getScreens().get(4))).buttonSaveSetEnabled(false);
            }
        }
    }

    /**
     * Adds the saved game to the list of maps saved.
     * @param gameManager The GameManager instance.
     */
    public void addMapsSaved(GameManager gameManager) {
        if (mapPath.length() > 0) {
            SaveManager saver = new SaveManager(gameManager.getActivity());
            if (!saver.getMapsStateSaved(mapPath, "mapsSaved.txt")) {
                FileReadWrite.writePrivateData(gameManager.getActivity(), "mapsSaved.txt", mapPath + "\n");
            }
        }
    }
}
