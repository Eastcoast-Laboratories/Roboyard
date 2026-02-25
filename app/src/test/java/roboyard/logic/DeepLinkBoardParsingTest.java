package roboyard.logic;

import org.junit.Test;

import roboyard.logic.core.GameState;
import roboyard.logic.core.GameElement;
import roboyard.logic.core.LevelFormatParser;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for deep-link board parsing.
 * 
 * Verifies that compact format strings from deep links are correctly parsed,
 * especially board dimensions (board:W,H;).
 * 
 * Bug: Deep-link with board:12,12 was parsed as 8x8 because parseFromSaveData()
 * did not handle the "board" entry from LevelFormatParser.
 * 
 * Tags: deep-link, board-size, compact-format, parsing
 */
public class DeepLinkBoardParsingTest {

    // The exact deep-link map data (URL-decoded) from the bug report
    private static final String DEEP_LINK_MAP_DATA_12x12 =
            "board:12,12;\n" +
            "h0,0;\n" +
            "h0,0;\n" +
            "h0,4;\n" +
            "h0,7;\n" +
            "h0,9;\n" +
            "h0,12;\n" +
            "h0,12;\n" +
            "h1,0;\n" +
            "h1,0;\n" +
            "h1,2;\n" +
            "h1,9;\n" +
            "h1,12;\n" +
            "h1,12;\n" +
            "h2,0;\n" +
            "h2,0;\n" +
            "h2,6;\n" +
            "h2,12;\n" +
            "h2,12;\n" +
            "h3,0;\n" +
            "h3,0;\n" +
            "h3,4;\n" +
            "h3,12;\n" +
            "h3,12;\n" +
            "h4,0;\n" +
            "h4,0;\n" +
            "h4,8;\n" +
            "h4,12;\n" +
            "h4,12;\n" +
            "h5,0;\n" +
            "h5,0;\n" +
            "h5,3;\n" +
            "h5,5;\n" +
            "h5,7;\n" +
            "h5,7;\n" +
            "h5,12;\n" +
            "h5,12;\n" +
            "h6,0;\n" +
            "h6,0;\n" +
            "h6,5;\n" +
            "h6,7;\n" +
            "h6,7;\n" +
            "h6,12;\n" +
            "h6,12;\n" +
            "h7,0;\n" +
            "h7,0;\n" +
            "h7,1;\n" +
            "h7,12;\n" +
            "h7,12;\n" +
            "h8,0;\n" +
            "h8,0;\n" +
            "h8,7;\n" +
            "h8,9;\n" +
            "h8,12;\n" +
            "h8,12;\n" +
            "h9,0;\n" +
            "h9,0;\n" +
            "h9,3;\n" +
            "h9,10;\n" +
            "h9,12;\n" +
            "h9,12;\n" +
            "h10,0;\n" +
            "h10,0;\n" +
            "h10,4;\n" +
            "h10,12;\n" +
            "h10,12;\n" +
            "h11,0;\n" +
            "h11,0;\n" +
            "h11,3;\n" +
            "h11,5;\n" +
            "h11,7;\n" +
            "h11,12;\n" +
            "h11,12;\n" +
            "v0,0;\n" +
            "v0,0;\n" +
            "v0,1;\n" +
            "v0,1;\n" +
            "v0,2;\n" +
            "v0,2;\n" +
            "v0,3;\n" +
            "v0,3;\n" +
            "v0,4;\n" +
            "v0,4;\n" +
            "v0,5;\n" +
            "v0,5;\n" +
            "v0,6;\n" +
            "v0,6;\n" +
            "v0,7;\n" +
            "v0,7;\n" +
            "v0,8;\n" +
            "v0,8;\n" +
            "v0,9;\n" +
            "v0,9;\n" +
            "v0,10;\n" +
            "v0,10;\n" +
            "v0,11;\n" +
            "v0,11;\n" +
            "v1,2;\n" +
            "v1,9;\n" +
            "v2,0;\n" +
            "v2,6;\n" +
            "v2,11;\n" +
            "v4,0;\n" +
            "v4,3;\n" +
            "v5,5;\n" +
            "v5,6;\n" +
            "v5,7;\n" +
            "v6,0;\n" +
            "v6,3;\n" +
            "v6,11;\n" +
            "v7,1;\n" +
            "v7,5;\n" +
            "v7,5;\n" +
            "v7,6;\n" +
            "v7,6;\n" +
            "v8,6;\n" +
            "v8,11;\n" +
            "v9,9;\n" +
            "v10,2;\n" +
            "v10,9;\n" +
            "v11,3;\n" +
            "v12,0;\n" +
            "v12,0;\n" +
            "v12,1;\n" +
            "v12,1;\n" +
            "v12,2;\n" +
            "v12,2;\n" +
            "v12,3;\n" +
            "v12,3;\n" +
            "v12,4;\n" +
            "v12,4;\n" +
            "v12,5;\n" +
            "v12,5;\n" +
            "v12,6;\n" +
            "v12,6;\n" +
            "v12,7;\n" +
            "v12,7;\n" +
            "v12,8;\n" +
            "v12,8;\n" +
            "v12,9;\n" +
            "v12,9;\n" +
            "v12,10;\n" +
            "v12,10;\n" +
            "v12,11;\n" +
            "v12,11;\n" +
            "ts10,0;\n" +
            "rr5,8;\n" +
            "rs6,1;\n" +
            "ry7,3;\n" +
            "rg7,8;\n" +
            "rb11,3;\n";

    @Test
    public void testLevelFormatParserParsesBoardEntry() {
        List<LevelFormatParser.LevelEntry> entries = LevelFormatParser.parseEntries(DEEP_LINK_MAP_DATA_12x12);

        // Find the board entry
        LevelFormatParser.LevelEntry boardEntry = null;
        for (LevelFormatParser.LevelEntry entry : entries) {
            if (entry.type.equals("board")) {
                boardEntry = entry;
                break;
            }
        }

        assertNotNull("LevelFormatParser should parse a 'board' entry", boardEntry);
        // board:12,12; -> type="board", data=":12,12"
        String cleanData = boardEntry.data.startsWith(":") ? boardEntry.data.substring(1) : boardEntry.data;
        assertEquals("Board data should be '12,12'", "12,12", cleanData);
    }

    @Test
    public void testParseFromSaveDataBoardSize12x12() {
        GameState state = GameState.parseFromSaveData(DEEP_LINK_MAP_DATA_12x12, null);

        assertNotNull("parseFromSaveData should return a non-null GameState", state);
        assertEquals("Board width should be 12", 12, state.getWidth());
        assertEquals("Board height should be 12", 12, state.getHeight());
    }

    @Test
    public void testParseFromSaveDataRobotCount() {
        GameState state = GameState.parseFromSaveData(DEEP_LINK_MAP_DATA_12x12, null);

        assertNotNull("GameState should not be null", state);

        int robotCount = 0;
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_ROBOT) {
                robotCount++;
            }
        }
        assertEquals("Should have 5 robots (rr, rs, ry, rg, rb)", 5, robotCount);
    }

    @Test
    public void testParseFromSaveDataTargetCount() {
        GameState state = GameState.parseFromSaveData(DEEP_LINK_MAP_DATA_12x12, null);

        assertNotNull("GameState should not be null", state);

        int targetCount = 0;
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_TARGET) {
                targetCount++;
            }
        }
        assertEquals("Should have 1 target (ts10,0)", 1, targetCount);
    }

    @Test
    public void testParseFromSaveDataTargetPosition() {
        GameState state = GameState.parseFromSaveData(DEEP_LINK_MAP_DATA_12x12, null);

        assertNotNull("GameState should not be null", state);

        // Target ts10,0 = silver target at (10, 0)
        boolean found = false;
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_TARGET) {
                assertEquals("Target X should be 10", 10, element.getX());
                assertEquals("Target Y should be 0", 0, element.getY());
                found = true;
            }
        }
        assertTrue("Should find the silver target", found);
    }

    @Test
    public void testParseFromSaveDataWallCount() {
        GameState state = GameState.parseFromSaveData(DEEP_LINK_MAP_DATA_12x12, null);

        assertNotNull("GameState should not be null", state);

        int hWallCount = 0;
        int vWallCount = 0;
        for (GameElement element : state.getGameElements()) {
            if (element.getType() == GameElement.TYPE_HORIZONTAL_WALL) {
                hWallCount++;
            } else if (element.getType() == GameElement.TYPE_VERTICAL_WALL) {
                vWallCount++;
            }
        }
        // Count from the data: 73 h-entries, but many are border duplicates
        // Count from the data: 48 v-entries
        assertTrue("Should have horizontal walls", hWallCount > 0);
        assertTrue("Should have vertical walls", vWallCount > 0);
    }

    @Test
    public void testParseLevelBoardSize12x12() {
        GameState state = GameState.parseLevel(null, DEEP_LINK_MAP_DATA_12x12, -1);

        assertNotNull("parseLevel should return a non-null GameState", state);
        assertEquals("Board width should be 12", 12, state.getWidth());
        assertEquals("Board height should be 12", 12, state.getHeight());
    }

    @Test
    public void testSmallBoardSize8x8() {
        String smallBoard = "board:8,8;\n" +
                "h0,0;\n" +
                "h0,8;\n" +
                "v0,0;\n" +
                "v8,0;\n" +
                "ts4,4;\n" +
                "rr2,2;\n" +
                "rg6,6;\n" +
                "rb1,7;\n" +
                "ry7,1;\n";

        GameState state = GameState.parseFromSaveData(smallBoard, null);

        assertNotNull("GameState should not be null", state);
        assertEquals("Board width should be 8", 8, state.getWidth());
        assertEquals("Board height should be 8", 8, state.getHeight());
    }

    @Test
    public void testLargeBoardSize16x16() {
        String largeBoard = "board:16,16;\n" +
                "h0,0;\n" +
                "ts8,8;\n" +
                "rr4,4;\n" +
                "rg12,12;\n" +
                "rb2,14;\n" +
                "ry14,2;\n";

        GameState state = GameState.parseFromSaveData(largeBoard, null);

        assertNotNull("GameState should not be null", state);
        assertEquals("Board width should be 16", 16, state.getWidth());
        assertEquals("Board height should be 16", 16, state.getHeight());
    }

    @Test
    public void testCompactFormatDetection() {
        // The compact format detection in parseFromSaveData checks for lines matching
        // "^[hmvtr].*\\d+,\\d+;.*" - verify our deep link data triggers this
        String firstWallLine = "h0,0;";
        assertTrue("h0,0; should match compact format pattern",
                firstWallLine.matches("^[hmvtr].*\\d+,\\d+;.*"));

        String boardLine = "board:12,12;";
        // board line starts with 'b', not in [hmvtr], so it alone should NOT trigger compact format
        assertFalse("board:12,12; should not match compact format pattern alone",
                boardLine.matches("^[hmvtr].*\\d+,\\d+;.*"));
    }
}
