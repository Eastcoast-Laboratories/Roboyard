package roboyard.eclabs;

import android.content.Intent;
import android.graphics.Color;
import java.util.ArrayList;
import android.graphics.Rect;
import android.net.Uri;

import java.util.List;

/**
 * Screen for saving and loading games (screen 9)
 */
public class SaveGameScreen extends GameScreen {
    private static List<GameButtonLink> links = new ArrayList<>();
    private float ratioW;
    private float ratioH;
    private int ts;
    private int iconSize;
    private int[] buttonPositionsX;
    private int[] buttonPositionsY;
    private String[] mapUniqueString;
    private String[] mapUniqueColor;
    private int autosaveButtonX;
    private int autosaveButtonY;
    private int backButtonX;
    private int backButtonY;

    int hs2; // Half the screen height

    public SaveGameScreen(GameManager gameManager) {
        super(gameManager);
    }

    @Override
    public void create() {
        hs2 = this.gameManager.getScreenHeight() / 2;
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
        mapUniqueColor = new String[cols * rows];
        for (int i = 0; i < cols * rows; i++) {
            String mapPath = getMapPath(i);
            SaveManager saver = new SaveManager(gameManager.getActivity());
            if(saver.getMapsStateSaved(mapPath, "mapsSaved.txt")) {
                String saveData = FileReadWrite.readPrivateData(gameManager.getActivity(), mapPath);
                gridElements = MapObjects.extractDataFromString(saveData);
                mapUniqueString[i] = MapObjects.createStringFromList(gridElements, true);
                mapUniqueColor[i] = MapObjects.generateHexColorFromString(mapUniqueString[i]);
            } else {
                mapUniqueString[i] = "";
                mapUniqueColor[i] = "#000000";
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

        links.clear();

        // Draw save slots
        for (int i = 0; i < buttonPositionsX.length; i++) {
            if (i == 0) {
                renderManager.drawText((int) (20 * ratioW), (int) ((42 + ts) * ratioH)-5, "Autosave");
            } else {
                renderManager.setColor(Color.parseColor(mapUniqueColor[i]));
                int moveleft=16;

                if (mapUniqueString[i].length() > 0){
                    // unicode string with 11 filled squares
                    String bar = "\u2588\u2588\u2588\u2588\u2588\u2588\u2588";
                    for (int j = 0; j < 8; j++) {
                        renderManager.drawText(buttonPositionsX[i] - moveleft, buttonPositionsY[i] - 5 + j * 15, bar);
                    }

                }

                renderManager.setColor(Color.parseColor("#000000"));
                renderManager.drawText(buttonPositionsX[i] - moveleft + 1 + (i<10? 8 : 0), buttonPositionsY[i] - 5, i + ". " + mapUniqueString[i]);

                drawShareSymbol(buttonPositionsX[i] - 15, buttonPositionsY[i] - 15, renderManager);
            }
        }

        super.draw(renderManager);
    }

    // Function to draw the Share symbol
    public static void drawShareSymbol(int x, int y, RenderManager renderManager) {
        int hs2 = 444; // TODO: get the screen height from the game manager
        int leftPadding = 1;
        int topPadding = (int)(hs2 * 0.27);
        drawClickableText(renderManager, x - 22 + leftPadding, y + topPadding, "\u2B24", "https://eclabs.de"); // Circle middle
        drawClickableText(renderManager, x + leftPadding + 5, y + topPadding + 11, "\u2B24", "https://eclabs.de"); // Circle top right
        drawClickableText(renderManager, x + leftPadding + 5, y + topPadding - 11, "\u2B24", "https://eclabs.de"); // Circle bottom right
        drawClickableText(renderManager, x + leftPadding - 10, y + topPadding - 7, "\u27CB", "https://eclabs.de");  // Slash-like diagonal line
        drawClickableText(renderManager, x + leftPadding - 10 , y+ topPadding + 7, "\u27CD", "https://eclabs.de"); // Backslash-like diagonal line
    }
    /**
     * Draws a clickable link.
     *
     * @param renderManager The render manager
     * @param x             The x-coordinate of the link
     * @param y             The y-coordinate of the link
     * @param url           The URL of the link
     */
    private static void drawClickableText(RenderManager renderManager, int x, int y, String linkText, String url) {
        int hs2=444; // TODO: get the screen height from the game manager
        Rect rect = renderManager.drawLinkText(x, y, linkText, Color.BLACK, (int) (0.7 * (hs2 / 10)));
        links.add(new GameButtonLink(rect.left, rect.top, rect.right, rect.bottom, url));
    }

    /**
     * Opens a link in a web browser.
     *
     * @param url The URL to open
     */
    private void openLink(String url) {
        // Create an intent to open a web browser with the specified URL
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        // Start the activity (open the web browser)
        gameManager.getActivity().startActivity(browserIntent);
    }

    /**
     * Updates the Credits screen.
     *
     * @param gameManager The game manager
     */
    @Override
    public void update(GameManager gameManager) {
        super.update(gameManager);
        InputManager im = gameManager.getInputManager();
        // Handle back button press to return to the main menu
        if (im.backOccurred()) {
            gameManager.setGameScreen(gameManager.getPreviousScreenKey());
        } else if(im.eventHasOccurred()) {
            for (GameButtonLink link : links) {
                boolean linkTouched = (im.getTouchX() >= link.getX() && im.getTouchX() <= link.getW()) && (im.getTouchY() >= link.getY() && im.getTouchY() <= link.getH());
                if(linkTouched) {
                    openLink(link.getUrl());
                    break;
                }
            }
        }
    }

    /**
     * Cleans up resources used by the Credits screen.
     */
    @Override
    public void destroy() {
        super.destroy();
    }
}
