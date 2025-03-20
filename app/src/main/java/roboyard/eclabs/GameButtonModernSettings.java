package roboyard.eclabs;

/**
 * Button to open the modern settings screen via FragmentHostActivity
 */
public class GameButtonModernSettings extends GameButtonModernUI {
    
    /**
     * @param x x-position
     * @param y y-position
     * @param w width
     * @param h height
     * @param imageUp image when not pressed
     * @param imageDown image when pressed
     */
    public GameButtonModernSettings(int x, int y, int w, int h, int imageUp, int imageDown) {
        super(x, y, w, h, imageUp, imageDown, "settings");
        setContentDescription("Settings");
    }
}
