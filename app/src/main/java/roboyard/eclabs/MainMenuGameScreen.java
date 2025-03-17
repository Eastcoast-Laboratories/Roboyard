package roboyard.eclabs;

import android.graphics.Color;

/**
 * Created by Pierre on 21/01/2015.
 */
public class MainMenuGameScreen extends GameScreen {

    public MainMenuGameScreen(GameManager gameManager){
        super(gameManager);
    }

    private long prevBack;

    @Override
    public void create() {
        this.prevBack = System.currentTimeMillis();

        int buttonSize = 440;
        float ratio = ((float)gameManager.getScreenWidth()) /((float)1080);
        int relativeButtonWidth = (int)(ratio*(float)buttonSize);
        int ws2 = (int)(((float)this.gameManager.getScreenWidth()-relativeButtonWidth)/2);

        // Random Game (large button)
        GameButtonGotoRandomGame randomGameButton = new GameButtonGotoRandomGame(ws2, (int)(ratio*200), relativeButtonWidth, (int)(ratio*buttonSize), R.drawable.bt_start_up_random, R.drawable.bt_start_down_random, Constants.SCREEN_GAME);
        randomGameButton.setAccessibleContentDescription(gameManager.getActivity(), "Start new random game");
        this.instances.add(randomGameButton);

        // Level Selection and Load Saved Game (medium buttons side by side)
        int mediumButtonSize = 330; // 75% of the regular size
        int mediumButtonWidth = (int)(ratio*(float)mediumButtonSize);
        int spacing = (int)(ratio*40); // Space between medium buttons
        int totalMediumWidth = mediumButtonWidth * 2 + spacing;
        int startXMedium = (gameManager.getScreenWidth() - totalMediumWidth) / 2;
        int mediumY = (int)(ratio*800); // Position below the random game button

        // Level Selection and Load Saved Game buttons
        GameButtonGoto levelSelectionButton = new GameButtonGoto(startXMedium, mediumY, mediumButtonWidth, (int)(ratio*mediumButtonSize), R.drawable.bt_start_up, R.drawable.bt_start_down, Constants.SCREEN_LEVEL_BEGINNER);
        levelSelectionButton.setAccessibleContentDescription(gameManager.getActivity(), "Select level to play");
        this.instances.add(levelSelectionButton);
        
        GameButtonGoto loadSavedGameButton = new GameButtonGoto(startXMedium + mediumButtonWidth + spacing, mediumY, mediumButtonWidth, (int)(ratio*mediumButtonSize), R.drawable.bt_start_up_saved, R.drawable.bt_start_down_saved, Constants.SCREEN_SAVE_GAMES);
        loadSavedGameButton.setAccessibleContentDescription(gameManager.getActivity(), "Load saved game");
        this.instances.add(loadSavedGameButton);

        // Small buttons at the bottom
        int smallButtonSize = 220; // Half the size of regular buttons
        int smallButtonWidth = (int)(ratio*(float)smallButtonSize);
        int smallSpacing = (int)(ratio*20); // Space between small buttons
        int totalSmallWidth = smallButtonWidth * 2 + smallSpacing;
        int startXSmall = (gameManager.getScreenWidth() - totalSmallWidth) / 2;
        int bottomY = (int)(ratio*1400); // Moved up from 1800 to 1400 to be fully visible

        // Settings and Credits as small buttons at the bottom
        GameButtonGoto settingsButton = new GameButtonGoto(startXSmall, bottomY, smallButtonWidth, (int)(ratio*smallButtonSize), R.drawable.bt_settings_up, R.drawable.bt_settings_down, Constants.SCREEN_SETTINGS);
        settingsButton.setAccessibleContentDescription(gameManager.getActivity(), "Game settings");
        this.instances.add(settingsButton);
        
        GameButtonGoto creditsButton = new GameButtonGoto(startXSmall + smallButtonWidth + smallSpacing, bottomY, smallButtonWidth, (int)(ratio*smallButtonSize), R.drawable.bt_credits_up, R.drawable.bt_credits_down, Constants.SCREEN_CREDITS);
        creditsButton.setAccessibleContentDescription(gameManager.getActivity(), "View credits");
        this.instances.add(creditsButton);
    }

    @Override
    public void draw(RenderManager renderManager) {
        renderManager.setColor(Color.WHITE);
        renderManager.paintScreen();
        
        // Draw all game instances (buttons, etc.)
        for (IGameObject instance : instances) {
            instance.draw(renderManager);
        }
        
        // Draw accessibility debug info separately if needed
        if (renderManager.getContext() != null && 
            AccessibilityUtil.isScreenReaderActive(renderManager.getContext())) {
            // Only show debug info in development, not in production
            // This ensures the help texts don't interfere with button clicks
            boolean showDebugInfo = false; // Set to true only during development testing
            
            if (showDebugInfo) {
                for (IGameObject instance : instances) {
                    if (instance instanceof GameButton) {
                        GameButton button = (GameButton) instance;
                        drawContentDescriptionDebug(renderManager, button);
                    }
                }
            }
        }
    }
    
    /**
     * Draws content description debug info for a button
     * @param renderManager The render manager
     * @param button The button to draw debug info for
     */
    private void drawContentDescriptionDebug(RenderManager renderManager, GameButton button) {
        String contentDescription = button.getContentDescription();
        if (contentDescription != null && !contentDescription.isEmpty()) {
            // Save current drawing state
            renderManager.save();
            
            // Draw a semi-transparent background for better readability
            renderManager.setColor(Color.argb(120, 0, 0, 0));
            renderManager.fillRect(button.getPositionX(), 
                                  button.getPositionY() + button.getHeight(), 
                                  button.getPositionX() + button.getWidth(), 
                                  button.getPositionY() + button.getHeight() + 30);
            
            // Draw the content description text
            renderManager.setColor(Color.WHITE);
            renderManager.setTextSize(16);
            renderManager.drawText((int)(button.getPositionX() + 5), 
                                 (int)(button.getPositionY() + button.getHeight() + 20), 
                                 contentDescription);
            
            // Restore previous drawing state
            renderManager.restore();
        }
    }

    @Override
    public void update(GameManager gameManager) {
        super.update(gameManager);
        if(gameManager.getInputManager().eventHasOccurred() && gameManager.getInputManager().backOccurred()){
            long dt = System.currentTimeMillis() - this.prevBack;
            this.prevBack = System.currentTimeMillis();
            if(dt<2000){
                gameManager.requestEnd();
            }else{
                gameManager.requestToast("Press again to exit.", false);
            }
        }
    }
}
