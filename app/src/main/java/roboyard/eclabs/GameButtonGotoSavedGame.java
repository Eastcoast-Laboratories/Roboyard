package roboyard.eclabs;

import android.util.SparseArray;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Represents a button to save or load a game.
 */
public class GameButtonGotoSavedGame extends GameButtonGoto {

    private String mapPath = null;

    /**
     * Constructor for GameButtonGotoSavedGame.
     * @param x The x-coordinate of the button.
     * @param y The y-coordinate of the button.
     * @param w The width of the button.
     * @param h The height of the button.
     * @param imageUp The image when the button is not pressed.
     * @param imageDown The image when the button is pressed.
     * @param target The target screen number.
     * @param filePath The file path associated with the button.
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
            ArrayList gridElements = ((GridGameScreen) gameManager.getScreens().get(gameManager.getPreviousScreenKey())).getGridElements();
            FileReadWrite.clearPrivateData(gameManager.getActivity(), mapPath);
            FileReadWrite.writePrivateData(gameManager.getActivity(), mapPath, MapObjects.createStringFromList(gridElements, false));
            addMapsSaved(gameManager);

            // redraw save buttons with new state of the saved game
            SaveGameScreen saveGameScreen = (SaveGameScreen) gameManager.getScreens().get(Constants.SCREEN_SAVE_GAMES);
            saveGameScreen.createButtons();
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
