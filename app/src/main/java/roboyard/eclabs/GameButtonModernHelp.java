package roboyard.eclabs;

/**
 * Button to open the modern help/credits screen via FragmentHostActivity
 */
public class GameButtonModernHelp extends GameButtonModernUI {
    
    /**
     * @param x x-position
     * @param y y-position
     * @param w width
     * @param h height
     * @param imageUp image when not pressed
     * @param imageDown image when pressed
     */
    public GameButtonModernHelp(int x, int y, int w, int h, int imageUp, int imageDown) {
        super(x, y, w, h, imageUp, imageDown, "help");
        setContentDescription("Help and Credits");
    }
}
