package roboyard.eclabs;

import android.graphics.Color;
import android.graphics.Paint;

import timber.log.Timber;

/**
 * Created by Pierre on 04/02/2015.
 */
public class SettingsGameScreen extends GameScreen {

    private GameButtonGeneral buttonBeginner = null;
    private GameButtonGeneral buttonAdvanced = null;
    private GameButtonGeneral buttonInsane = null;
    private GameButtonGeneral buttonImpossible = null;
    private GameButtonGeneral buttonSoundOff = null;
    private GameButtonGeneral buttonSoundOn = null;
    private GameButtonGeneral buttonNewMapEachTimeOn = null;
    private GameButtonGeneral buttonNewMapEachTimeOff = null;
    private GameDropdown boardSizeDropdown;
    private int hs2;
    private int ws2;
    private String levelDifficulty="Beginner";
    private Preferences preferences;
    // UI Layout
    private ScreenLayout layout;
    // Flag to track if we've initialized map generation setting
    private boolean mapGenerationSettingInitialized = false;

    public SettingsGameScreen(GameManager gameManager){
        super(gameManager);
        preferences = new Preferences();
    }

    @Override
    public void create() {
        // Initialize screen layout
        layout = new ScreenLayout(
            gameManager.getScreenWidth(),
            gameManager.getScreenHeight(),
            gameManager.getActivity().getResources().getDisplayMetrics().density
        );

        // Remove map generation initialization from here to avoid NullPointerException
        // We'll initialize it the first time the settings screen is shown instead

        float displayRatio = gameManager.getScreenHeight() / gameManager.getScreenWidth();

        // Define button dimensions
        int buttonWidth = layout.x(160);
        int buttonHeight = layout.y(128);
        
        // Calculate x positions for buttons
        int x1 = layout.x(40);   // First column
        int x2 = layout.x(300);  // Second column
        int x3 = layout.x(560);  // Third column
        int x4 = layout.x(820);  // Fourth column

        int posY = 88;

        // Board size dropdown
        Timber.d("Settings: displayRatio: %f", displayRatio);

        // Create board size dropdown
        // Board size dropdown
        int boardSizeY = layout.y(50);
        boardSizeDropdown = new GameDropdown(x3, boardSizeY, buttonWidth * 2, layout.y(posY));
        boardSizeDropdown.setAccessibleContentDescription(gameManager.getActivity(), "Board size selection dropdown");

        // Define available board sizes
        int[][] boardSizes = {
                {12, 12}, {12, 14}, {12, 16}, {12, 18},
                {14, 14}, {14, 16}, {14, 18},
                {16, 16}, {16, 18}, {16, 20}, {16, 22},
                {18, 18}, {18, 20}, {18, 22}
        };

        float maxBoardRatio = calculateMaxBoardRatio(displayRatio);
        Timber.d("Settings: Display ratio: %.2f -> Max board ratio: %.2f", displayRatio, maxBoardRatio);

        int currentBoardSizeX = MainActivity.getBoardWidth();
        int currentBoardSizeY = MainActivity.getBoardHeight();
        String currentBoardSize = currentBoardSizeX + "x" + currentBoardSizeY;
        int selectedIndex = -1;
        int index = 0;

        for (int[] size : boardSizes) {
            float boardRatio = (float)size[1] / size[0];
            Timber.d("Settings: Checking board size %dx%d (ratio: %.2f)", size[0], size[1], boardRatio);

            if (boardRatio <= maxBoardRatio) {
                String option = size[0] + "x" + size[1];
                boardSizeDropdown.addOption(option, new setBoardSize(size[0], size[1]));
                Timber.d("Settings: Added board size option: %s", option);

                // Check if this is the current board size
                if (size[0] == currentBoardSizeX && size[1] == currentBoardSizeY) {
                    selectedIndex = index;
                }
                index++;
            }
        }

        // Set the selected option
        if (selectedIndex >= 0) {
            boardSizeDropdown.setSelectedIndex(selectedIndex);
        }

        // Add dropdown to instances
        this.instances.add(boardSizeDropdown);

        // Difficulty buttons - first row
        posY += 255;
        int difficultyY = layout.y(posY);
        buttonBeginner = new GameButtonGeneral(
            x1, difficultyY, buttonWidth, buttonHeight, 
            R.drawable.bt_up, R.drawable.bt_down, new setBeginnner());
        buttonBeginner.setAccessibleContentDescription(gameManager.getActivity(), "Set difficulty to Beginner");
        
        buttonAdvanced = new GameButtonGeneral(
            x2, difficultyY, buttonWidth, buttonHeight, 
            R.drawable.bt_up, R.drawable.bt_down, new setAdvanced());
        buttonAdvanced.setAccessibleContentDescription(gameManager.getActivity(), "Set difficulty to Advanced");

        // Difficulty buttons - second row
        posY += 244;
        int difficultyRow2Y = layout.y(posY);
        buttonInsane = new GameButtonGeneral(
            x1, difficultyRow2Y, buttonWidth, buttonHeight, 
            R.drawable.bt_up, R.drawable.bt_down, new setInsane());
        buttonInsane.setAccessibleContentDescription(gameManager.getActivity(), "Set difficulty to Insane");
        
        buttonImpossible = new GameButtonGeneral(
            x2, difficultyRow2Y, buttonWidth, buttonHeight, 
            R.drawable.bt_up, R.drawable.bt_down, new setImpossible());
        buttonImpossible.setAccessibleContentDescription(gameManager.getActivity(), "Set difficulty to Impossible");
        
        // New Map Each Time buttons
        posY += 353;
        int newMapY = layout.y(posY);
        buttonNewMapEachTimeOn = new GameButtonGeneral(
            x1, newMapY, buttonWidth, buttonHeight, 
            R.drawable.bt_up, R.drawable.bt_down, new setNewMapEachTimeOn());
        buttonNewMapEachTimeOn.setAccessibleContentDescription(gameManager.getActivity(), "Set to generate a new map each game");
        
        buttonNewMapEachTimeOff = new GameButtonGeneral(
            x2, newMapY, buttonWidth, buttonHeight, 
            R.drawable.bt_up, R.drawable.bt_down, new setNewMapEachTimeOff());
        buttonNewMapEachTimeOff.setAccessibleContentDescription(gameManager.getActivity(), "Set to use the same map for each new game");

        // Sound buttons
        posY += 333;
        // Make sound buttons circular
        int soundButtonSize = layout.x(222);
        int soundY = layout.y(posY);

        buttonSoundOn = new GameButtonGeneral(
            layout.x(40),
            soundY,
            soundButtonSize,
            soundButtonSize,
            R.drawable.bt_sound_on_up,
            R.drawable.bt_sound_on_down,
            new setSoundon());
        buttonSoundOn.setAccessibleContentDescription(gameManager.getActivity(), "Enable sound");

        buttonSoundOff = new GameButtonGeneral(
            layout.x(340),
            soundY,
            soundButtonSize,
            soundButtonSize,
            R.drawable.bt_sound_off_up,
            R.drawable.bt_sound_off_down,
            new setSoundoff());
        buttonSoundOff.setAccessibleContentDescription(gameManager.getActivity(), "Disable sound");

        // Add Button to set Beginner/Advanced/Insane
        this.instances.add(buttonBeginner);
        this.instances.add(buttonAdvanced);
        this.instances.add(buttonInsane);
        this.instances.add(buttonImpossible);
        this.instances.add(buttonSoundOff);
        this.instances.add(buttonSoundOn);
        this.instances.add(buttonNewMapEachTimeOn);
        this.instances.add(buttonNewMapEachTimeOff);

        // Add Button back to main screen
        int backButtonSize = layout.x(222);
        GameButtonGotoBack backButton = new GameButtonGotoBack(
            layout.x(90), 
            layout.y(-255), 
            backButtonSize, 
            backButtonSize, 
            R.drawable.bt_back_up, 
            R.drawable.bt_back_down);
        backButton.setAccessibleContentDescription(gameManager.getActivity(), "Back to main menu");
        this.instances.add(backButton);

    }

    @Override
    public void load(RenderManager renderManager) {
        super.load(renderManager);
    }

    @Override
    public void draw(RenderManager renderManager) {

        renderManager.setColor(Color.YELLOW);
        renderManager.setColor(Color.parseColor("#FFFDAE"));
        renderManager.paintScreen();

        // Set up text sizes
        int textSize = layout.getTextSize(89);
        int smallTextSize = layout.getTextSize(45);
        int mediumTextSize = layout.getTextSize(60);

        renderManager.setColor(Color.BLACK);
        renderManager.setTextSize(textSize);

        // Calculate button dimensions for layout
        int buttonWidth = layout.x(160);
        
        // Calculate x positions for buttons
        int x1 = layout.x(40);   // First column
        int x2 = layout.x(300);  // Second column
        int x3 = layout.x(560);  // Third column
        int x4 = layout.x(820);  // Fourth column

        // Board Size label
        Paint paint = new Paint();
        paint.setTextSize(mediumTextSize);
        renderManager.setTextSize(mediumTextSize);
        String text = "      Board Size:";
        float textWidth2 = paint.measureText(text);
        renderManager.drawText(
            x1 + buttonWidth - (int)(textWidth2/2), 
            layout.y(122),
            text);
        
        // Difficulty
        // current level difficulty
        levelDifficulty=preferences.getPreferenceValue(gameManager.getActivity(), "difficulty");
        renderManager.setTextSize(textSize);
        renderManager.drawText(layout.x(10), layout.y(300), "Difficulty: " + levelDifficulty);
        
        // difficulty text below Buttons (set in create() ):
        renderManager.setTextSize(smallTextSize);

        int posY = 522;
        // Center text under each button
        text = "Beginner";
        float textWidth = paint.measureText(text);
        renderManager.drawText(
            x1, 
            layout.y(posY),
            text);
        
        text = "Advanced";
        textWidth = paint.measureText(text);
        renderManager.drawText(
            x2, 
            layout.y(posY),
            text);

        posY += 244;
        renderManager.drawText(
            x1, 
            layout.y(posY),
            "Insane");
        
        renderManager.drawText(
            x2, 
            layout.y(posY),
            "Impossible");

        // New Map Each Time Section
        posY += 144;
        renderManager.setTextSize((int) (textSize * 0.7));
        String newMapSetting = preferences.getPreferenceValue(gameManager.getActivity(), "newMapEachTime");
        if (newMapSetting == null) {
            newMapSetting = "true"; // Default value
            preferences.setPreferences(gameManager.getActivity(), "newMapEachTime", newMapSetting);
        }
        String newMapText = newMapSetting.equals("true") ? "New Map Each Time: Yes" : "New Map Each Time: No";
        renderManager.drawText(layout.x(10), layout.y(posY), newMapText);

        // Button labels for New Map Each Time
        posY += 200;
        renderManager.setTextSize(smallTextSize);
        renderManager.drawText(x1, layout.y(posY), "Yes");
        renderManager.drawText(x2, layout.y(posY), "No");

        // Sound Settings
        posY += 111;
        renderManager.setTextSize(textSize);
        String soundSetting=preferences.getPreferenceValue(gameManager.getActivity(), "sound");
        if(soundSetting.equals("off") == false) {
            soundSetting = "Sound: On";
        } else {
            soundSetting = "Sound: Off";
        }
        renderManager.drawText(layout.x(90), layout.y(posY), soundSetting);
        
        renderManager.setTextSize(textSize/2); // this will be the text-size for the dropdown in create()

        if (!mapGenerationSettingInitialized) {
            // Initialize map generation setting
            newMapSetting = preferences.getPreferenceValue(gameManager.getActivity(), "newMapEachTime");
            Timber.d("newMapSetting: %s", newMapSetting);
            if (newMapSetting == null) {
                newMapSetting = "true"; // Default value
                preferences.setPreferences(gameManager.getActivity(), "newMapEachTime", newMapSetting);
            }
            MapGenerator.generateNewMapEachTime = newMapSetting.equals("true");
            mapGenerationSettingInitialized = true;
        }

        super.draw(renderManager);
    }

    @Override
    public void update(GameManager gameManager) {
        super.update(gameManager);
        if(gameManager.getInputManager().backOccurred()){
            gameManager.setGameScreen(Constants.SCREEN_START);

        }

    }

    @Override
    public void destroy() {
        super.destroy();
    }

    private class setBeginnner implements IExecutor{
        public void execute() {
            preferences.setPreferences(gameManager.getActivity(), "difficulty", "Beginner");
            GridGameScreen.setDifficulty("Beginner");
            levelDifficulty="Beginner";
        }
    }
    private class setAdvanced implements IExecutor{
        public void execute() {
            preferences.setPreferences(gameManager.getActivity(),"difficulty", "Advanced");
            GridGameScreen.setDifficulty("Advanced");
            levelDifficulty="Advanced";
        }
    }
    private class setInsane implements IExecutor{
        public void execute() {
            preferences.setPreferences(gameManager.getActivity(),"difficulty", "Insane");
            GridGameScreen.setDifficulty("Insane");
            levelDifficulty="Insane";
        }
    }
    private class setImpossible implements IExecutor{
        public void execute() {
            preferences.setPreferences(gameManager.getActivity(),"difficulty", "Impossible");
            GridGameScreen.setDifficulty("Impossible");
            levelDifficulty="Impossible";
            
            // Check if board size is small for impossible difficulty
            int boardWidth = MainActivity.getBoardWidth();
            int boardHeight = MainActivity.getBoardHeight();
            int boardArea = boardWidth * boardHeight;
            
            if (boardArea < 256) {
                gameManager.requestToast(" 'Impossible' mode + small board\n    = very long generation time", true);
            }
        }
    }

    private class setSoundoff implements IExecutor{
        public void execute() {
            preferences.setPreferences(gameManager.getActivity(),"sound", "off");
            gameManager.toggleSound(false);
            gameManager.requestToast("Sound disabled", true);
        }
    }

    private class setSoundon implements IExecutor{
        public void execute() {
            preferences.setPreferences(gameManager.getActivity(),"sound", "on");
            gameManager.toggleSound(true);
            gameManager.requestToast("Sound enabled", true);
        }
    }

    private class setNewMapEachTimeOn implements IExecutor {
        public void execute() {
            preferences.setPreferences(gameManager.getActivity(), "newMapEachTime", "true");
            MapGenerator.generateNewMapEachTime = true;
            GridGameScreen.setMap(null);
            gameManager.requestToast("A new map will be generated each game", true);
        }
    }
    
    private class setNewMapEachTimeOff implements IExecutor {
        public void execute() {
            preferences.setPreferences(gameManager.getActivity(), "newMapEachTime", "false");
            MapGenerator.generateNewMapEachTime = false;
            GridGameScreen.setMap(null);
            gameManager.requestToast("The same map will be used if you play a new random game", true);
        }
    }

    private class setBoardSize implements Runnable {
        private final int width;
        private final int height;

        public setBoardSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void run() {
            MainActivity activity = gameManager.getActivity();
            activity.setAndSaveBoardSizeToPreferences(activity, width, height);
            // Use consistent key names with loadBoardSizeFromPreferences
            preferences.setPreferences(gameManager.getActivity(), "boardSizeX", String.valueOf(width));
            preferences.setPreferences(gameManager.getActivity(), "boardSizeY", String.valueOf(height));
            MapGenerator.generateNewMapEachTime = true;
            gameManager.getInputManager().resetEvents();
        }
    }

    /**
     * Calculate maximum allowed board ratio based on display ratio.
     * For display ratio 1.5 -> max board ratio 1.2
     * For display ratio 2.0 -> max board ratio 1.8
     * Linear interpolation between these points
     */
    private float calculateMaxBoardRatio(float displayRatio) {
        float minDisplayRatio = 1.5f;
        float maxDisplayRatio = 2.0f;
        float minBoardRatio = 1.2f;
        float maxBoardRatio = 1.5f;
        
        // Clamp display ratio to valid range
        displayRatio = Math.max(minDisplayRatio, Math.min(maxDisplayRatio, displayRatio));
        
        // Linear interpolation
        float t = (displayRatio - minDisplayRatio) / (maxDisplayRatio - minDisplayRatio);
        float maxRatio = minBoardRatio + t * (maxBoardRatio - minBoardRatio);
        
        Timber.d("Settings: in functin Display ratio: %.2f -> Max board ratio: %.2f", displayRatio, maxRatio);
        return maxRatio;
    }

}
