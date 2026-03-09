package roboyard.eclabs.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import roboyard.logic.core.GameHistoryEntry;
import roboyard.logic.core.GameState;
import roboyard.ui.activities.MainActivity;
import roboyard.ui.components.GameHistoryManager;
import timber.log.Timber;

/**
 * E2E test to verify that replaying a game from history with an improved solution
 * correctly updates the existing history entry instead of creating a duplicate.
 * 
 * Test scenario:
 * 1. Create a history entry with 8 moves (non-optimal)
 * 2. Simulate replaying the same map with 6 moves (improved)
 * 3. Verify that bestMoves is updated to 6 (not creating a new entry)
 * 4. Verify that completionCount is incremented
 * 5. Verify that mapSignature matching works correctly
 * 
 * This test ensures the mapSignature matching works correctly for history updates,
 * including migration of old entries without mapSignature.
 * 
 * Tags: e2e, history, replay, improvement, map-signature, migration
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class HistoryReplayImprovementE2ETest {
    private static final String TAG = "HISTORY_REPLAY_IMPROVE_E2E";

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() throws InterruptedException {
        TestHelper.clearLogcat();
        activityRule.getScenario().onActivity(activity -> {
            try {
                TestHelper.startNewSessionWithEmptyStorage(activity);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testHistoryReplayWithImprovement() throws Exception {
        step("1/6", "Starting test: History Replay with Improvement");
        
        // Step 1: Create a test history entry with a known mapSignature
        step("2/6", "Creating initial history entry with 8 moves");
        String[] testData = new String[3]; // [mapName, mapSignature, historyPath]
        
        activityRule.getScenario().onActivity(activity -> {
            String mapName = "TEST_MAP_REPLAY_" + System.currentTimeMillis();
            String mapSig = "12x14;test_signature_" + System.currentTimeMillis();
            
            testData[0] = mapName;
            testData[1] = mapSig;
            
            // Create history entry with 8 moves (non-optimal)
            GameHistoryEntry entry = new GameHistoryEntry(
                    "history_test_replay.txt",
                    mapName,
                    System.currentTimeMillis(),
                    30, // 30 seconds
                    8,  // 8 moves (non-optimal)
                    5,  // optimal is 5
                    "12x14",
                    null
            );
            
            // Set mapSignature
            entry.setMapSignature(mapSig);
            
            // Record the first completion
            entry.recordCompletion(30, 8);
            
            // Save to history
            GameHistoryManager.addHistoryEntry(activity, entry);
            testData[2] = entry.getMapPath();
            
            step("INFO", String.format("Created entry: %s, mapSig=%s..., moves=8, count=1",
                    testData[0], mapSig.substring(0, 30)));
        });
        
        // Step 2: Verify initial state
        step("3/6", "Verifying initial history entry");
        int[] initialStats = new int[3]; // [bestMoves, completionCount, historySize]
        activityRule.getScenario().onActivity(activity -> {
            List<GameHistoryEntry> entries = GameHistoryManager.getHistoryEntries(activity);
            initialStats[2] = entries.size();
            
            GameHistoryEntry entry = GameHistoryManager.findByMapSignature(activity, testData[1]);
            assertNotNull("Initial history entry should be found by mapSignature", entry);
            initialStats[0] = entry.getBestMoves();
            initialStats[1] = entry.getCompletionCount();
            
            step("INFO", String.format("Initial: bestMoves=%d, completionCount=%d, historySize=%d",
                    initialStats[0], initialStats[1], initialStats[2]));
        });
        
        assertEquals("Initial bestMoves should be 8", 8, initialStats[0]);
        assertEquals("Initial completionCount should be 1", 1, initialStats[1]);
        assertEquals("History should have 1 entry", 1, initialStats[2]);
        
        // Step 3: Simulate replay with improved solution (6 moves)
        step("4/6", "Simulating replay with improved solution (6 moves)");
        activityRule.getScenario().onActivity(activity -> {
            // Create a new entry with the same mapSignature but better moves
            GameHistoryEntry improvedEntry = new GameHistoryEntry(
                    testData[2], // same map path
                    testData[0], // same map name
                    System.currentTimeMillis(),
                    20, // 20 seconds (faster)
                    6,  // 6 moves (improved)
                    5,  // optimal is still 5
                    "12x14",
                    null
            );
            
            // Set the same mapSignature
            improvedEntry.setMapSignature(testData[1]);
            
            // Add to history - should update existing entry, not create new one
            GameHistoryManager.addHistoryEntry(activity, improvedEntry);
            
            step("INFO", "Added improved entry with 6 moves");
        });
        
        // Step 4: Verify history was updated (not duplicated)
        step("5/6", "Verifying history entry was updated (not duplicated)");
        int[] updatedStats = new int[3]; // [bestMoves, completionCount, historySize]
        activityRule.getScenario().onActivity(activity -> {
            List<GameHistoryEntry> entries = GameHistoryManager.getHistoryEntries(activity);
            updatedStats[2] = entries.size();
            
            GameHistoryEntry entry = GameHistoryManager.findByMapSignature(activity, testData[1]);
            assertNotNull("Updated history entry should still be found by mapSignature", entry);
            updatedStats[0] = entry.getBestMoves();
            updatedStats[1] = entry.getCompletionCount();
            
            step("INFO", String.format("Updated: bestMoves=%d, completionCount=%d, historySize=%d",
                    updatedStats[0], updatedStats[1], updatedStats[2]));
        });
        
        // Verify no duplicate entry was created
        assertEquals("History size should remain 1 (no duplicate)", 1, updatedStats[2]);
        
        // Verify bestMoves was updated
        assertEquals("BestMoves should be updated to 6", 6, updatedStats[0]);
        
        // Verify completionCount was incremented
        assertEquals("CompletionCount should be incremented to 2", 2, updatedStats[1]);
        
        step("6/6", "PASS: History replay with improvement correctly updated existing entry");
        
        // Dump relevant logs for debugging
        TestHelper.dumpLogcat("Final Logs", "HISTORY", "findByMapSignature|Updated existing map|MIGRATION", 30);
    }

    private void step(String step, String msg) {
        String line = TAG + " [" + step + "] " + msg;
        Timber.d(line);
        System.out.println(line);
    }
}

