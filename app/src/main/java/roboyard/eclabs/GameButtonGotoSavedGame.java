package roboyard.eclabs;

import java.util.ArrayList;

/**
 * Represents a button to save or load a game.
 */
public class GameButtonGotoSavedGame extends GameButtonGoto {

    private String mapPath;

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
        GridGameScreen gameScreen = (GridGameScreen) gameManager.getScreens().get(Constants.SCREEN_GAME);
        boolean isSavemode = false;
        if (gameScreen != null && gameScreen.isRandomGame()) {
            isSavemode = true;
        }
        if (isSavemode) {
            // Screen to save or overwrite a savegame
            // System.out.println("DEBUG: Saving game to slot: " + mapPath);
            ArrayList<GridElement> gridElements = gameScreen.getGridElements();
            String saveData = MapObjects.createStringFromList(gridElements, false);
            
            try {
                // Write save data directly (will overwrite if file exists)
                FileReadWrite.writePrivateData(gameManager.getActivity(), mapPath, saveData);
                // System.out.println("DEBUG: wrote " + saveData.length() + " bytes to " + mapPath);
                
                // Add to saved games list if needed
                addMapsSaved(gameManager);

                // Refresh save game screen buttons
                SaveGameScreen saveGameScreen = (SaveGameScreen) gameManager.getScreens().get(Constants.SCREEN_SAVE_GAMES);
                if (saveGameScreen != null) {
                    SaveGameScreen.clearCachesForMap(mapPath);
                    saveGameScreen.createButtons();
                    // System.out.println("DEBUG: Refreshed save game screen buttons");
                }
                
                // Keep track of the game screen and explicitly set it as previous
                gameManager.setPreviousScreen(gameManager.getScreens().get(Constants.SCREEN_GAME));
                gameManager.setGameScreen(Constants.SCREEN_SAVE_GAMES);
            } catch (Exception e) {
                // System.out.println("DEBUG: Error saving game: " + e.getMessage());
            }
        } else {
            // Screen to select a savegame
            super.onClick(gameManager);
            gameScreen.setSavedGame(mapPath);
            // disable the savegame button in the gamescreen
            gameScreen.buttonSaveSetEnabled(false);
        }
    }

    /**
     * Adds the saved game to the list of maps saved only if not already in it
     * @param gameManager The GameManager instance.
     */
    public void addMapsSaved(GameManager gameManager) {
        if (mapPath.length() > 0) {
            SaveManager saver = new SaveManager(gameManager.getActivity());
            if (!saver.getMapsStateSaved(mapPath, "mapsSaved.txt")) {
                FileReadWrite.appendPrivateData(gameManager.getActivity(), "mapsSaved.txt", mapPath + "\n");
            }
        }
    }
}
