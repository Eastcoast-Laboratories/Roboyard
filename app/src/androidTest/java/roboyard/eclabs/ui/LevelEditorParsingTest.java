package roboyard.eclabs.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.util.Scanner;

import roboyard.logic.core.Constants;
import roboyard.logic.core.GameElement;
import roboyard.logic.core.GameState;
import timber.log.Timber;

/**
 * Instrumented test for Level Editor parsing and wall positioning.
 * Verifies:
 * - Level files in board:W,H; mhX,Y; mvX,Y; format are parsed correctly
 * - Outer walls are at correct boundary positions (x=width for right, y=height for bottom)
 * - Level 1 loads with correct dimensions and element count
 */
@RunWith(AndroidJUnit4.class)
public class LevelEditorParsingTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    /**
     * Test that level_1.txt can be loaded from assets without errors.
     */
    @Test
    public void testLevel1FileExists() throws Exception {
        Timber.d("[TEST_LEVEL_PARSE] Testing level_1.txt exists in assets");
        InputStream is = context.getAssets().open("Maps/level_1.txt");
        assertNotNull("level_1.txt should exist in assets", is);
        is.close();
        Timber.d("[TEST_LEVEL_PARSE] level_1.txt exists");
    }

    /**
     * Test that GameState.parseLevel correctly parses the board:W,H; format.
     */
    @Test
    public void testParseLevelBoardDimensions() {
        Timber.d("[TEST_LEVEL_PARSE] Testing board dimension parsing");
        String levelContent = "board:12,14;\nmh0,0;\nmh1,0;\nmv0,0;\nmv12,0;\n";
        GameState state = GameState.parseLevel(context, levelContent, 999);

        assertNotNull("Parsed state should not be null", state);
        assertEquals("Board width should be 12", 12, state.getWidth());
        assertEquals("Board height should be 14", 14, state.getHeight());
        Timber.d("[TEST_LEVEL_PARSE] Board dimensions parsed correctly: %dx%d", state.getWidth(), state.getHeight());
    }

    /**
     * Test that horizontal walls (mh) are parsed correctly.
     */
    @Test
    public void testParseHorizontalWalls() {
        Timber.d("[TEST_LEVEL_PARSE] Testing horizontal wall parsing");
        String levelContent = "board:12,14;\nmh0,0;\nmh5,7;\nmh11,14;\n";
        GameState state = GameState.parseLevel(context, levelContent, 999);

        int hWallCount = 0;
        boolean foundTopLeft = false;
        boolean foundMiddle = false;
        boolean foundBottomRight = false;

        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_HORIZONTAL_WALL) {
                hWallCount++;
                if (element.getX() == 0 && element.getY() == 0) foundTopLeft = true;
                if (element.getX() == 5 && element.getY() == 7) foundMiddle = true;
                if (element.getX() == 11 && element.getY() == 14) foundBottomRight = true;
            }
        }

        assertEquals("Should have 3 horizontal walls", 3, hWallCount);
        assertTrue("Should have wall at (0,0)", foundTopLeft);
        assertTrue("Should have wall at (5,7)", foundMiddle);
        assertTrue("Should have wall at (11,14) - bottom boundary", foundBottomRight);
        Timber.d("[TEST_LEVEL_PARSE] Horizontal walls parsed correctly");
    }

    /**
     * Test that vertical walls (mv) are parsed correctly, including boundary walls.
     */
    @Test
    public void testParseVerticalWalls() {
        Timber.d("[TEST_LEVEL_PARSE] Testing vertical wall parsing");
        String levelContent = "board:12,14;\nmv0,0;\nmv6,5;\nmv12,13;\n";
        GameState state = GameState.parseLevel(context, levelContent, 999);

        int vWallCount = 0;
        boolean foundLeft = false;
        boolean foundMiddle = false;
        boolean foundRight = false;

        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_VERTICAL_WALL) {
                vWallCount++;
                if (element.getX() == 0 && element.getY() == 0) foundLeft = true;
                if (element.getX() == 6 && element.getY() == 5) foundMiddle = true;
                if (element.getX() == 12 && element.getY() == 13) foundRight = true;
            }
        }

        assertEquals("Should have 3 vertical walls", 3, vWallCount);
        assertTrue("Should have wall at (0,0) - left boundary", foundLeft);
        assertTrue("Should have wall at (6,5)", foundMiddle);
        assertTrue("Should have wall at (12,13) - right boundary", foundRight);
        Timber.d("[TEST_LEVEL_PARSE] Vertical walls parsed correctly");
    }

    /**
     * Test that level 1 loads from assets with correct dimensions and has walls.
     */
    @Test
    public void testLoadLevel1FromAssets() throws Exception {
        Timber.d("[TEST_LEVEL_PARSE] Testing full level 1 loading from assets");

        InputStream is = context.getAssets().open("Maps/level_1.txt");
        Scanner scanner = new Scanner(is);
        StringBuilder content = new StringBuilder();
        while (scanner.hasNextLine()) {
            content.append(scanner.nextLine()).append("\n");
        }
        scanner.close();
        is.close();

        GameState state = GameState.parseLevel(context, content.toString(), 1);

        assertNotNull("Parsed level 1 should not be null", state);
        assertEquals("Level 1 width should be 12", 12, state.getWidth());
        assertEquals("Level 1 height should be 14", 14, state.getHeight());

        // Count walls, robots, targets
        int hWalls = 0, vWalls = 0, robots = 0, targets = 0;
        boolean hasBottomWall = false;
        boolean hasRightWall = false;

        for (GameElement element : state.getGameElements()) {
            switch (element.getType()) {
                case GameElement.TYPE_HORIZONTAL_WALL:
                    hWalls++;
                    if (element.getY() == 14) hasBottomWall = true;
                    break;
                case GameElement.TYPE_VERTICAL_WALL:
                    vWalls++;
                    if (element.getX() == 12) hasRightWall = true;
                    break;
                case GameElement.TYPE_ROBOT:
                    robots++;
                    break;
                case GameElement.TYPE_TARGET:
                    targets++;
                    break;
            }
        }

        assertTrue("Level 1 should have horizontal walls", hWalls > 0);
        assertTrue("Level 1 should have vertical walls", vWalls > 0);
        assertTrue("Level 1 should have bottom boundary walls at y=14", hasBottomWall);
        assertTrue("Level 1 should have right boundary walls at x=12", hasRightWall);
        assertTrue("Level 1 should have robots", robots > 0);
        assertTrue("Level 1 should have targets", targets > 0);

        Timber.d("[TEST_LEVEL_PARSE] Level 1 loaded: %d hWalls, %d vWalls, %d robots, %d targets",
                hWalls, vWalls, robots, targets);
        Timber.d("[TEST_LEVEL_PARSE] LEVEL 1 LOADING TEST PASSED");
    }

    /**
     * Test that robots and targets are parsed from level format.
     */
    @Test
    public void testParseRobotsAndTargets() {
        Timber.d("[TEST_LEVEL_PARSE] Testing robot and target parsing");
        String levelContent = "board:12,14;\n" +
                "robot_red1,9;\n" +
                "robot_blue3,12;\n" +
                "robot_yellow10,2;\n" +
                "robot_green9,9;\n" +
                "target_blue8,11;\n";

        GameState state = GameState.parseLevel(context, levelContent, 999);

        int robots = 0, targets = 0;
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_ROBOT) robots++;
            if (element.getType() == GameElement.TYPE_TARGET) targets++;
        }

        assertEquals("Should have 4 robots", 4, robots);
        assertEquals("Should have 1 target", 1, targets);
        Timber.d("[TEST_LEVEL_PARSE] Robots and targets parsed correctly");
    }

    /**
     * Test that outer walls for a new level are at correct boundary positions.
     * For a 12x14 board: bottom walls at y=14, right walls at x=12.
     */
    @Test
    public void testNewLevelOuterWallPositions() {
        Timber.d("[TEST_LEVEL_PARSE] Testing outer wall positions for new level");

        // Simulate what createBorderWalls does
        int boardWidth = 12;
        int boardHeight = 14;

        // Build level content with outer walls matching createBorderWalls logic
        StringBuilder sb = new StringBuilder();
        sb.append("board:").append(boardWidth).append(",").append(boardHeight).append(";\n");

        // Top walls (y=0)
        for (int x = 0; x < boardWidth; x++) {
            sb.append("mh").append(x).append(",0;\n");
        }
        // Bottom walls (y=height)
        for (int x = 0; x < boardWidth; x++) {
            sb.append("mh").append(x).append(",").append(boardHeight).append(";\n");
        }
        // Left walls (x=0)
        for (int y = 0; y < boardHeight; y++) {
            sb.append("mv0,").append(y).append(";\n");
        }
        // Right walls (x=width)
        for (int y = 0; y < boardHeight; y++) {
            sb.append("mv").append(boardWidth).append(",").append(y).append(";\n");
        }

        GameState state = GameState.parseLevel(context, sb.toString(), 999);

        int topWalls = 0, bottomWalls = 0, leftWalls = 0, rightWalls = 0;
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_HORIZONTAL_WALL) {
                if (element.getY() == 0) topWalls++;
                if (element.getY() == boardHeight) bottomWalls++;
            } else if (element.getType() == GameElement.TYPE_VERTICAL_WALL) {
                if (element.getX() == 0) leftWalls++;
                if (element.getX() == boardWidth) rightWalls++;
            }
        }

        assertEquals("Top walls should span full width", boardWidth, topWalls);
        assertEquals("Bottom walls at y=" + boardHeight + " should span full width", boardWidth, bottomWalls);
        assertEquals("Left walls should span full height", boardHeight, leftWalls);
        assertEquals("Right walls at x=" + boardWidth + " should span full height", boardHeight, rightWalls);

        Timber.d("[TEST_LEVEL_PARSE] Outer wall positions correct: top=%d, bottom=%d, left=%d, right=%d",
                topWalls, bottomWalls, leftWalls, rightWalls);
        Timber.d("[TEST_LEVEL_PARSE] OUTER WALL POSITIONS TEST PASSED");
    }
}
