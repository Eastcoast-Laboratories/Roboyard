package roboyard.eclabs.ui;

import static org.junit.Assert.*;

import android.app.Activity;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import roboyard.eclabs.R;
import roboyard.logic.core.Constants;
import roboyard.logic.core.GameState;
import roboyard.ui.activities.MainFragmentActivity;
import roboyard.ui.components.FileReadWrite;
import roboyard.ui.components.GameStateManager;
import roboyard.ui.fragments.SaveGameFragment;
import timber.log.Timber;

/**
 * Tests for the share URL parsing logic in SaveGameFragment.
 * Verifies that the compact metadata format (walls, robots, targets in # line)
 * is correctly parsed into the share URL data format.
 *
 * Tests:
 * 1. Compact format with ||R, |T, mv/mh items in metadata line
 * 2. SIZE: metadata field parsed correctly
 * 3. Real save file from a game is parsed with walls > 0, targets > 0, robots > 0
 *
 * Tags: share, parsing, save-data, compact-format, instrumented
 * Run with:
 * ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.eclabs.ui.ShareParsingTest
 */
@RunWith(AndroidJUnit4.class)
public class ShareParsingTest {

    private static final String TAG = "[SHARE_PARSING]";

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);

    private GameStateManager gameStateManager;

    @Before
    public void setUp() throws InterruptedException {
        step("setUp", "Initializing GameStateManager");
        activityRule.getScenario().onActivity(a ->
                gameStateManager = ((MainFragmentActivity) a).getGameStateManager());
        assertNotNull("GameStateManager must not be null", gameStateManager);
        Thread.sleep(2000);
    }

    /**
     * Test 1: Parse compact metadata format with robots, targets, walls in # line.
     * This simulates the exact format seen in the logs.
     */
    @Test
    public void testParseCompactMetadataFormat() {
        step("1/4", "Testing compact metadata format parsing");

        String saveData = "#MAPNAME:TestMap;mv9,10;mv9,6;mv9,8;||R0@3,6;R1@5,9;R2@5,12;R3@7,8;|T3@1,3;;MAX_HINT_USED:13;SOLVED:false;SIZE:12,14;DIFFICULTY:0;TIME:27805;MOVES:14";

        SaveGameFragment.ShareParseResult result = SaveGameFragment.parseSaveDataForShare(saveData);

        assertNotNull(TAG + " Parse result must not be null", result);
        assertEquals(TAG + " Map name", "TestMap", result.mapName);
        assertEquals(TAG + " Width", 12, result.width);
        assertEquals(TAG + " Height", 14, result.height);
        assertEquals(TAG + " Move count", 14, result.numMoves);

        step("1/4", "Walls=" + result.wallCount + " Targets=" + result.targetCount + " Robots=" + result.robotCount);

        assertTrue(TAG + " Must have walls (found " + result.wallCount + ")", result.wallCount > 0);
        assertTrue(TAG + " Must have targets (found " + result.targetCount + ")", result.targetCount > 0);
        assertTrue(TAG + " Must have robots (found " + result.robotCount + ")", result.robotCount > 0);

        assertEquals(TAG + " Wall count", 3, result.wallCount);
        assertEquals(TAG + " Target count", 1, result.targetCount);
        assertEquals(TAG + " Robot count", 4, result.robotCount);

        // Verify formatted data contains expected entries
        assertTrue(TAG + " Must contain robot_pink", result.formattedData.contains("robot_pink"));
        assertTrue(TAG + " Must contain robot_green", result.formattedData.contains("robot_green"));
        assertTrue(TAG + " Must contain robot_blue", result.formattedData.contains("robot_blue"));
        assertTrue(TAG + " Must contain robot_yellow", result.formattedData.contains("robot_yellow"));
        assertTrue(TAG + " Must contain target_yellow", result.formattedData.contains("target_yellow"));
        assertTrue(TAG + " Must contain mv", result.formattedData.contains("mv"));

        step("1/4", "PASSED - Compact metadata format parsed correctly");
    }

    /**
     * Test 2: Parse multicolor target (T-1@x,y).
     */
    @Test
    public void testParseMulticolorTarget() {
        step("2/4", "Testing multicolor target parsing");

        String saveData = "#MAPNAME:MultiTest;||R0@2,5;R1@2,8;R2@3,1;R3@3,11;|T-1@11,11;;SIZE:12,14;MOVES:0";

        SaveGameFragment.ShareParseResult result = SaveGameFragment.parseSaveDataForShare(saveData);

        assertNotNull(TAG + " Parse result must not be null", result);
        assertEquals(TAG + " Target count", 1, result.targetCount);
        assertEquals(TAG + " Robot count", 4, result.robotCount);
        assertTrue(TAG + " Must contain target_multi", result.formattedData.contains("target_multi"));

        step("2/4", "PASSED - Multicolor target parsed correctly");
    }

    /**
     * Test 3: Parse real save file from a game that was saved and verify 
     * walls, targets, and robots are correctly extracted.
     */
    @Test
    public void testParseRealSaveFile() throws InterruptedException {
        step("3/4", "Starting random game to generate real save data");
        TestHelper.startAndWait8sForPopupClose();
        TestHelper.closeAchievementPopupIfPresent();

        // Start a random game
        TestHelper.startRandomGame();
        Thread.sleep(3000);

        // Save the game to slot 1 (slot 0 is reserved for autosave)
        step("3/4", "Saving game to slot 1");
        AtomicReference<Boolean> savedRef = new AtomicReference<>(false);
        activityRule.getScenario().onActivity(a -> {
            boolean success = gameStateManager.saveGame(1);
            savedRef.set(success);
            Timber.d(TAG + " Game saved to slot 1: %b", success);
        });
        Thread.sleep(1000);
        assertTrue(TAG + " Game must be saved successfully", savedRef.get());

        // Load the save data back
        AtomicReference<String> saveDataRef = new AtomicReference<>();
        activityRule.getScenario().onActivity(a -> {
            File savesDir = new File(a.getFilesDir(), Constants.SAVE_DIRECTORY);
            String filename = Constants.SAVE_FILENAME_PREFIX + "1" + Constants.SAVE_FILENAME_EXTENSION;
            File saveFile = new File(savesDir, filename);
            if (saveFile.exists()) {
                String data = FileReadWrite.loadAbsoluteData(saveFile.getAbsolutePath());
                saveDataRef.set(data);
                Timber.d(TAG + " Loaded save data: %d chars", data != null ? data.length() : 0);
                Timber.d(TAG + " Save data first 200 chars: %s", data != null ? data.substring(0, Math.min(200, data.length())) : "null");
            }
        });
        Thread.sleep(1000);

        String saveData = saveDataRef.get();
        assertNotNull(TAG + " Save data must not be null", saveData);
        assertTrue(TAG + " Save data must not be empty", saveData.length() > 0);

        step("3/4", "Parsing save data (" + saveData.length() + " chars)");
        SaveGameFragment.ShareParseResult result = SaveGameFragment.parseSaveDataForShare(saveData);

        assertNotNull(TAG + " Parse result must not be null", result);
        step("3/4", "Result: walls=" + result.wallCount + " targets=" + result.targetCount 
                + " robots=" + result.robotCount + " size=" + result.width + "x" + result.height);

        assertTrue(TAG + " Must have walls (found " + result.wallCount + ")", result.wallCount > 0);
        assertTrue(TAG + " Must have targets (found " + result.targetCount + ")", result.targetCount > 0);
        assertTrue(TAG + " Must have robots (found " + result.robotCount + ")", result.robotCount > 0);
        assertTrue(TAG + " Width must be > 0", result.width > 0);
        assertTrue(TAG + " Height must be > 0", result.height > 0);
        assertTrue(TAG + " formattedData must contain board:", result.formattedData.contains("board:"));
        assertTrue(TAG + " formattedData must contain robot_", result.formattedData.contains("robot_"));
        assertTrue(TAG + " formattedData must contain target_", result.formattedData.contains("target_"));

        step("3/4", "PASSED - Real save file parsed correctly");
    }

    /**
     * Test 4: Verify null/empty input handling.
     */
    @Test
    public void testParseNullAndEmpty() {
        step("4/4", "Testing null and empty input");

        assertNull(TAG + " null input must return null", SaveGameFragment.parseSaveDataForShare(null));
        assertNull(TAG + " empty input must return null", SaveGameFragment.parseSaveDataForShare(""));

        step("4/4", "PASSED - Null/empty handled correctly");
    }

    private void step(String step, String msg) {
        String line = TAG + " [" + step + "] " + msg;
        Timber.d(line);
        System.out.println(line);
    }
}
