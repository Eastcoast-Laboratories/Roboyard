package roboyard.eclabs;

import android.graphics.Color;
import java.util.ArrayList;

/**
 * Screen for saving and loading games (screen 9)
 */
public class SaveGameScreen extends GameScreen {
    private float ratioW;
    private float ratioH;
    private int ts;
    private int iconSize;
    private int[] buttonPositionsX;
    private int[] buttonPositionsY;
    private String[] mapUniqueString;
    private int autosaveButtonX;
    private int autosaveButtonY;
    private int backButtonX;
    private int backButtonY;

    public SaveGameScreen(GameManager gameManager) {
        super(gameManager);
    }

    @Override
    public void create() {
        // Load button images and initialize
        gameManager.getRenderManager().loadImage(R.drawable.bt_start_down_saved_used);
        gameManager.getRenderManager().loadImage(R.drawable.bt_start_up_saved_used);
        gameManager.getRenderManager().loadImage(R.drawable.bt_start_up_saved);
        gameManager.getRenderManager().loadImage(R.drawable.bt_start_down_saved);
        init();
        createButtons();
    }

    /**
     * calculate Button Positions and load all saved maps to create a unique string for each from the mapElements
     */
    public void init() {
        // Button positions and dimensions
        ratioW = ((float) gameManager.getScreenWidth()) / ((float) 1080);
        ratioH = ((float) gameManager.getScreenHeight()) / ((float) 1920);
        int hs2 = this.gameManager.getScreenHeight() / 2;
        ts = hs2 / 10;
        iconSize = 144;

        // Calculate button positions for horizontal and vertical layout
        int stepX = 211; // Horizontal step
        int stepY = 222; // Vertical step
        int cols = 5; // Number of columns
        int rows = 7; // Number of rows

        buttonPositionsX = new int[cols * rows];
        buttonPositionsY = new int[cols * rows];

        for (int i = 0; i < cols * rows; i++) {
            int col = i % cols;
            int row = (i / cols) % rows;
            buttonPositionsX[i] = (int) ((55 + (stepX * col)) * ratioW);
            buttonPositionsY[i] = (int) ((45 + ts + (stepY * row)) * ratioH);
        }

        // Calculate positions for autosave button and back button
        autosaveButtonX = (int) (55 * ratioW);
        autosaveButtonY = (int) ((45 + ts) * ratioH);
        backButtonX = (int) (814 * ratioW);
        backButtonY = (int) (1650 * ratioH);
        ArrayList gridElements;

        // load all saved maps to create a unique string from the mapElements
        mapUniqueString = new String[cols * rows];
        for (int i = 0; i < cols * rows; i++) {
            String mapPath = getMapPath(i);
            SaveManager saver = new SaveManager(gameManager.getActivity());
            if(saver.getMapsStateSaved(mapPath, "mapsSaved.txt")) {
                String saveData = FileReadWrite.readPrivateData(gameManager.getActivity(), mapPath);
                gridElements = MapObjects.extractDataFromString(saveData);
                mapUniqueString[i] = MapObjects.createStringFromList(gridElements, true);
            } else {
                mapUniqueString[i] = "";
            }
        }
    }

    /**
     * Create buttons for saving and loading games.
     */
    public void createButtons() {
        init();
        ArrayList<GameButtonGotoSavedGame> aRemove = new ArrayList<>();
        for (Object currentObject : this.instances) {
            if (currentObject.getClass() == GameButtonGotoSavedGame.class) {
                aRemove.add((GameButtonGotoSavedGame) currentObject);
            }
        }
        for (GameButtonGotoSavedGame p : aRemove) {
            this.instances.remove(p);
        }

        String mapPath = "";
        SaveManager saver = new SaveManager(gameManager.getActivity());

        // Create buttons for each save slot
        for (int i = 0; i < buttonPositionsX.length; i++) {
            mapPath = getMapPath(i);
            if (i == 0) {
                this.instances.add(new GameButtonGotoSavedGame(autosaveButtonX, autosaveButtonY, iconSize * ratioH, iconSize * ratioW, saver.getButtonAutoSaved(mapPath, true), saver.getButtonAutoSaved(mapPath, false), 4, mapPath));
            } else {
                this.instances.add(new GameButtonGotoSavedGame(buttonPositionsX[i], buttonPositionsY[i], iconSize * ratioH, iconSize * ratioW, saver.getButtonSaved(mapPath, true), saver.getButtonSaved(mapPath, false), 4, mapPath));
            }
        }

        // Add back button
        this.instances.add(new GameButtonGotoBack(backButtonX, backButtonY, (int) (222 * ratioH), (int) (222 * ratioW), R.drawable.bt_back_up, R.drawable.bt_back_down));
    }

    /**
     * Get the file path for a specific level in the screen.
     *
     * @param levelInScreen The level index in the screen.
     * @return The file path for the level.
     */
    public static String getMapPath(int levelInScreen) {
        return "map_" + levelInScreen + ".txt";
    }

    @Override
    public void load(RenderManager renderManager) {
        super.load(renderManager);
    }

    @Override
    public void draw(RenderManager renderManager) {
        // Draw background and text
        renderManager.setColor(Color.parseColor("#cccccc"));
        renderManager.paintScreen();
        renderManager.setColor(Color.BLACK);

        renderManager.setTextSize((int) (0.4 * ts));
        renderManager.drawText((int) (20 * ratioW), (int) (55 * ratioH), "Select Savegame");

        // Draw save slots
        for (int i = 0; i < buttonPositionsX.length; i++) {
            if (i == 0) {
                renderManager.drawText((int) (20 * ratioW), (int) ((42 + ts) * ratioH)-5, "Autosave");
            } else {
                renderManager.setColor(Color.BLACK);
                renderManager.drawText(buttonPositionsX[i], buttonPositionsY[i]-5, (i<10?" ":"") + i + ".");
                renderManager.setColor(Color.parseColor("#222222"));
                renderManager.drawText(buttonPositionsX[i]+33, buttonPositionsY[i]-5, mapUniqueString[i]);
            }
        }

        super.draw(renderManager);
    }

    @Override
    public void update(GameManager gameManager) {
        super.update(gameManager);
        if (gameManager.getInputManager().backOccurred()) {
            gameManager.setGameScreen(gameManager.getPreviousScreenKey());
        }
    }

    @Override
    public void destroy() {
        super.destroy();
    }
}
