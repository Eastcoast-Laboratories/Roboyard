package roboyard.eclabs;

import timber.log.Timber;

/**
 * Created by Pierre on 21/01/2015.
 */
public class GameButtonGeneral extends GameButton {
    private final IExecutor executor;

    public GameButtonGeneral(int x, int y, int w, int h, int imageUp, int imageDown, IExecutor executor){
        super(x, y, w, h, imageUp, imageDown);
        this.executor = executor;
    }

    @Override
    public void onClick(GameManager gameManager) {
            Timber.d("[RESTART] GameButtonGeneral.onClick() called for button at (%d,%d)", getPositionX(), getPositionY());
            executor.execute();
    }
}
