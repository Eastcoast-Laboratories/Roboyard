package roboyard.eclabs;

import android.util.SparseArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

public class GameButtonGotoSavedGame extends GameButtonGoto {

    private String mapPath = null;

    public GameButtonGotoSavedGame(float x, float y, float w, float h, int imageUp, int imageDown, int target, String filePath) {
        super((int)x, (int)y, (int)w, (int)h, imageUp, imageDown, target);
        mapPath = filePath;

    }

    @Override
    public void onClick(GameManager gameManager) {

        if(gameManager.getPreviousScreenKey() == 4) {
            // Screen to save or overwrite a savegame

            ArrayList gridElements = ((GridGameScreen)gameManager.getScreens().get(gameManager.getPreviousScreenKey())).getGridElements();

            FileReadWrite.clearPrivateData(gameManager.getActivity(), mapPath);
            FileReadWrite.writePrivateData(gameManager.getActivity(), mapPath, MapObjects.createStringFromList(gridElements, false));

            addMapsSaved(gameManager);

            SparseArray<GameScreen> screens = gameManager.getScreens();
            for(int i = 0; i < screens.size(); i++)
            {
                if(screens.get(i).getClass() == SaveGameScreen.class)
                {
                    // redraw save buttons with new state of the saved game
                    ((SaveGameScreen)screens.get(i)).createButtons();
                }
            }

        } else {
            // Screen to select a savegame
            SaveManager saver = new SaveManager(gameManager.getActivity());

            if(saver.getMapsStateSaved(mapPath, "mapsSaved.txt"))
            {
                super.onClick(gameManager);

                ((GridGameScreen)(gameManager.getScreens().get(4))).setSavedGame(mapPath);
                // disable the savegame button in the gamescreen
                ((GridGameScreen)(gameManager.getScreens().get(4))).buttonSaveSetEnabled(false);
            }
        }

    }

    public void addMapsSaved(GameManager gameManager)
    {
        if(mapPath.length() > 0)
        {
            SaveManager saver = new SaveManager(gameManager.getActivity());

            if(!saver.getMapsStateSaved(mapPath, "mapsSaved.txt"))
            {
                FileReadWrite.writePrivateData(gameManager.getActivity(), "mapsSaved.txt", mapPath+"\n");
            }
        }
    }

}