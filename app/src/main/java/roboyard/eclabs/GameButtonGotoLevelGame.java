package roboyard.eclabs;

/**
 * Created by Alain on 28/03/2015.
 */
public class GameButtonGotoLevelGame extends GameButtonGoto {

    private String mapPath = null;
    private int levelNumber;
    private String levelName = null;

    public GameButtonGotoLevelGame(float x, float y, float w, float h, int imageUp, int imageDown, int target, String filePath, int levelNumber, String levelName) {
        super((int)x, (int)y, (int)w, (int)h, imageUp, imageDown, target);
        mapPath = filePath;
        this.levelNumber = levelNumber;
        this.levelName = levelName;
    }

    @Override
    public void create() {
        super.create();
        // Set content description for accessibility
        String description = "Start new game: Level " + levelNumber;
        if (levelName != null && !levelName.isEmpty()) {
            description += ", " + levelName;
        }
        setContentDescription(description);
    }

    @Override
    public void onClick(GameManager gameManager) {
        super.onClick(gameManager);

        LevelChoiceGameScreen.setLastButtonUsed(this);
        GridGameScreen.setDifficulty("Beginner");
        ((GridGameScreen)(gameManager.getScreens().get(4))).setLevelGame(mapPath);
    }
}
