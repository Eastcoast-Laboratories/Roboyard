package roboyard.eclabs;

import android.content.Intent;
import roboyard.ui.activities.MainActivity;
import timber.log.Timber;

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
        
        Timber.d("GameButtonModernUI: onClick called for screenType: %s", screenType);
        
        if (gameManager == null) {
            Timber.e("GameButtonModernUI: gameManager is null!");
            return;
        }
        
        MainActivity activity = gameManager.getActivity();
        if (activity == null) {
            Timber.e("GameButtonModernUI: gameManager.getActivity() returned null!");
            return;
        }
        
        if (!(activity instanceof MainActivity)) {
            Timber.e("GameButtonModernUI: activity is not an instance of MainActivity! Class: %s", activity.getClass().getName());
            return;
        }
        
        Timber.d("GameButtonModernUI: got valid MainActivity instance");
        
        switch (screenType) {
            case "settings":
                Timber.d("GameButtonModernUI: opening settings screen");
                activity.openSettingsScreen();
                break;
            case "save":
                Timber.d("GameButtonModernUI: opening save screen");
                activity.openSaveScreen();
                break;
            case "load":
                Timber.d("GameButtonModernUI: opening load screen");
                activity.openLoadScreen();
                break;
            case "help":
                Timber.d("GameButtonModernUI: opening help screen");
                activity.openHelpScreen();
                break;
        }
    }
}
