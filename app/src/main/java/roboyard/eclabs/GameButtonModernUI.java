package roboyard.eclabs;

import android.content.Intent;

/**
 * Base class for buttons that launch the modern UI components via FragmentHostActivity
 * instead of using the old screen navigation system.
 */
public class GameButtonModernUI extends GameButton {
    protected String screenType;
    
    /**
     * @param x x-position
     * @param y y-position
     * @param w width
     * @param h height
     * @param imageUp image when not pressed
     * @param imageDown image when pressed
     * @param screenType the type of screen to show in FragmentHostActivity ("settings", "save", "help")
     */
    public GameButtonModernUI(int x, int y, int w, int h, int imageUp, int imageDown, String screenType) {
        super(x, y, w, h, imageUp, imageDown);
        this.screenType = screenType;
    }
    
    @Override
    public void onClick(GameManager gameManager) {
        if (!this.enabled) return;
        
        if (gameManager.getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) gameManager.getActivity();
            
            switch (screenType) {
                case "settings":
                    activity.openSettingsScreen();
                    break;
                case "save":
                    activity.openSaveScreen();
                    break;
                case "load":
                    activity.openLoadScreen();
                    break;
                case "help":
                    activity.openHelpScreen();
                    break;
            }
        }
    }
}
