package roboyard.eclabs;

public class GameButtonGotoRandomGame extends GameButtonGoto{
    public GameButtonGotoRandomGame(int x, int y, int w, int h, int imageUp, int imageDown, int target)
    {
        super(x,y,w,h,imageUp,imageDown,target);
    }

    @Override
    public void onClick(GameManager gameManager) {
        super.onClick(gameManager);
        // target 4: start game
        ((GridGameScreen)(gameManager.getScreens().get(4))).setRandomGame();


    }
}
