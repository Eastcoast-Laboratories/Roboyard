package roboyard.eclabs;

import android.graphics.Color;
import java.util.ArrayList;

/**
 * Created by Alain on 29/03/2015.
 */
public class SaveGameScreen extends GameScreen {
    private float ratioW;
    private float ratioH;
    private int ts;
    private int iconSize;
    private int[] buttonPositionsX;
    private int[] buttonPositionsY;
    private int autosaveButtonX;
    private int autosaveButtonY;
    private int backButtonX;
    private int backButtonY;

    public SaveGameScreen(GameManager gameManager) {
        super(gameManager);
    }

    @Override
    public void create() {
        gameManager.getRenderManager().loadImage(R.drawable.bt_start_down_saved_used);
        gameManager.getRenderManager().loadImage(R.drawable.bt_start_up_saved_used);
        gameManager.getRenderManager().loadImage(R.drawable.bt_start_up_saved);
        gameManager.getRenderManager().loadImage(R.drawable.bt_start_down_saved);
        init();
        createButtons();
    }

    /**
     * prepare Button Positions and load all saved maps to create a unique string from the mapElements
     */
    private void init() {
        // Button positions
        ratioW = ((float) gameManager.getScreenWidth()) / ((float) 1080);
        ratioH = ((float) gameManager.getScreenHeight()) / ((float) 1920);
        int hs2 = this.gameManager.getScreenHeight() / 2;
        ts = hs2 / 10;
        iconSize = 144;

        int stepX = 211;
        int stepY = 222;
        int cols = 5;
        int rows = 7;

        buttonPositionsX = new int[cols * rows];
        buttonPositionsY = new int[cols * rows];

        for (int i = 0; i < cols * rows; i++) {
            int col = i % cols;
            int row = (i / cols) % rows;
            buttonPositionsX[i] = (int) ((55 + (stepX * col)) * ratioW);
            buttonPositionsY[i] = (int) ((45 + ts + (stepY * row)) * ratioH);
        }

        autosaveButtonX = (int) (55 * ratioW);
        autosaveButtonY = (int) ((45 + ts) * ratioH);

        backButtonX = (int) (814 * ratioW);
        backButtonY = (int) (1650 * ratioH);

        // TODO: load all saved maps to create a unique string from the mapElements

    }

    public void createButtons() {
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

        int col, row;
        for (int i = 0; i < buttonPositionsX.length; i++) {
            col = i % 5;
            row = (i / 5) % 7;
            mapPath = getMapPath(i);
            if (i == 0) {
                this.instances.add(new GameButtonGotoSavedGame(autosaveButtonX, autosaveButtonY, iconSize * ratioH, iconSize * ratioW, saver.getButtonAutoSaved(mapPath, true), saver.getButtonAutoSaved(mapPath, false), 4, mapPath));
            } else {
                this.instances.add(new GameButtonGotoSavedGame(buttonPositionsX[i], buttonPositionsY[i], iconSize * ratioH, iconSize * ratioW, saver.getButtonSaved(mapPath, true), saver.getButtonSaved(mapPath, false), 4, mapPath));
            }
        }

        this.instances.add(new GameButtonGotoBack(backButtonX, backButtonY, (int) (222 * ratioH), (int) (222 * ratioW), R.drawable.bt_back_up, R.drawable.bt_back_down));
    }

    public static String getMapPath(int levelInScreen) {
        return "map_" + levelInScreen + ".txt";
    }

    @Override
    public void load(RenderManager renderManager) {
        super.load(renderManager);
    }

    @Override
    public void draw(RenderManager renderManager) {
        renderManager.setColor(Color.parseColor("#cccccc"));
        renderManager.paintScreen();
        renderManager.setColor(Color.BLACK);

        renderManager.setTextSize((int) (0.5 * ts));
        renderManager.drawText((int) (20 * ratioW), (int) (55 * ratioH), "Select Savegame");

        for (int i = 0; i < buttonPositionsX.length; i++) {
            if (i == 0) {
                renderManager.drawText((int) (20 * ratioW), (int) ((42 + ts) * ratioH), "Autosave");
            } else {
                renderManager.drawText(buttonPositionsX[i], buttonPositionsY[i], i + ".");
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
