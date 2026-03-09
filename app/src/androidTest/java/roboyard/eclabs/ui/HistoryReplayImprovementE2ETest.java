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
        step("1/9", "Starting test: History Replay with Improvement + Hints");
        
        // Step 1: Create a test history entry with 8 moves (non-optimal, no hints)
        step("2/9", "Creating initial history entry with 8 moves (non-optimal, no hints)");
        String[] testData = new String[3]; // [mapName, mapSignature, historyPath]
        
        activityRule.getScenario().onActivity(activity -> {
            String mapName = "TEST_MAP_REPLAY_" + System.currentTimeMillis();
            String mapSig = "12x14;test_signature_" + System.currentTimeMillis();
            
            testData[0] = mapName;
            testData[1] = mapSig;
            
            // Create history entry with 8 moves (non-optimal, but solved without hints)
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
            
            // Record the first completion WITHOUT hints
            entry.recordCompletion(30, 8);
            // Mark as solved without hints (maxHintUsed stays -1)
            entry.setSolvedWithoutHints(true);
            entry.setLastSolvedWithoutHints(System.currentTimeMillis());
            // NOT perfect (8 moves vs 5 optimal), so lastPerfectlySolvedWithoutHints stays 0
            
            // Save to history
            GameHistoryManager.addHistoryEntry(activity, entry);
            testData[2] = entry.getMapPath();
            
            step("INFO", String.format("Created entry: %s, moves=8, hints=-1, solvedNoHints=true, lastPerfect=0",
                    testData[0]));
        });
        
        // Step 2: Verify initial state
        step("3/9", "Verifying initial history entry");
        long[] initialHintStats = new long[5]; // [bestMoves, completionCount, historySize, lastSolvedNoHints, lastPerfectNoHints]
        activityRule.getScenario().onActivity(activity -> {
            List<GameHistoryEntry> entries = GameHistoryManager.getHistoryEntries(activity);
            initialHintStats[2] = entries.size();
            
            GameHistoryEntry entry = GameHistoryManager.findByMapSignature(activity, testData[1]);
            assertNotNull("Initial history entry should be found by mapSignature", entry);
            initialHintStats[0] = entry.getBestMoves();
            initialHintStats[1] = entry.getCompletionCount();
            initialHintStats[3] = entry.getLastSolvedWithoutHints();
            initialHintStats[4] = entry.getLastPerfectlySolvedWithoutHints();
            
            step("INFO", String.format("Initial: bestMoves=%d, count=%d, everHints=%b, lastNoHints=%d, lastPerfect=%d",
                    initialHintStats[0], initialHintStats[1], entry.isEverUsedHints(), 
                    initialHintStats[3], initialHintStats[4]));
        });
        
        assertEquals("Initial bestMoves should be 8", 8, initialHintStats[0]);
        assertEquals("Initial completionCount should be 1", 1, initialHintStats[1]);
        assertEquals("History should have 1 entry", 1, initialHintStats[2]);
        assertTrue("lastSolvedWithoutHints should be > 0", initialHintStats[3] > 0);
        assertEquals("lastPerfectlySolvedWithoutHints should be 0 (not perfect)", 0, initialHintStats[4]);
        
        // Step 3: Simulate viewing hints without completing the game
        step("4/9", "Simulating hint viewing (no completion, just browsing hints)");
        activityRule.getScenario().onActivity(activity -> {
            // Simulate viewing the game and clicking through hints without completing
            // This should mark everUsedHints=true but NOT change lastPerfectlySolvedWithoutHints
            GameHistoryEntry entry = GameHistoryManager.findByMapSignature(activity, testData[1]);
            if (entry != null) {
                // Update maxHintUsed to 2 (viewed hints 0, 1, 2)
                entry.setMaxHintUsed(2);
                entry.markEverUsedHints();
                // No completion, so we just save the updated entry
                GameHistoryManager.addHistoryEntry(activity, entry);
                step("INFO", "Updated entry: maxHintUsed=2, everUsedHints=true (no completion)");
            }
        });
        
        // Step 4: Verify hints were marked but lastPerfectlySolvedWithoutHints unchanged
        step("5/9", "Verifying hint usage marked correctly");
        boolean[] hintVerify = new boolean[3]; // [everUsedHints, qualifiesNoHints, qualifiesPerfectNoHints]
        long[] hintTimestamps = new long[2]; // [lastSolvedNoHints, lastPerfectNoHints]
        activityRule.getScenario().onActivity(activity -> {
            GameHistoryEntry entry = GameHistoryManager.findByMapSignature(activity, testData[1]);
            assertNotNull("Entry should still exist after hint viewing", entry);
            hintVerify[0] = entry.isEverUsedHints();
            hintVerify[1] = entry.qualifiesForNoHintsAchievement();
            hintVerify[2] = entry.qualifiesForPerfectNoHintsAchievement();
            hintTimestamps[0] = entry.getLastSolvedWithoutHints();
            hintTimestamps[1] = entry.getLastPerfectlySolvedWithoutHints();
            
            step("INFO", String.format("After hints: everHints=%b, qualifyNoHints=%b, qualifyPerfect=%b, lastNoHints=%d, lastPerfect=%d",
                    hintVerify[0], hintVerify[1], hintVerify[2], hintTimestamps[0], hintTimestamps[1]));
        });
        
        assertTrue("everUsedHints should be true after viewing hints", hintVerify[0]);
        assertTrue("Should still qualify for no-hints achievement (solved once without)", hintVerify[1]);
        assertFalse("Should NOT qualify for perfect no-hints (never solved perfectly without hints)", hintVerify[2]);
        assertEquals("lastSolvedWithoutHints should remain unchanged", initialHintStats[3], hintTimestamps[0]);
        assertEquals("lastPerfectlySolvedWithoutHints should still be 0", 0, hintTimestamps[1]);
        
        // Step 5: Simulate replay with improved solution (6 moves, still not perfect)
        step("6/9", "Simulating replay with improved solution (6 moves, still not perfect)");
        activityRule.getScenario().onActivity(activity -> {
            // Create a new entry with the same mapSignature but better moves
            GameHistoryEntry improvedEntry = new GameHistoryEntry(
                    testData[2], // same map path
                    testData[0], // same map name
                    System.currentTimeMillis(),
                    20, // 20 seconds (faster)
                    6,  // 6 moves (improved but still not optimal)
                    5,  // optimal is still 5
                    "12x14",
                    null
            );
            
            // Set the same mapSignature
            improvedEntry.setMapSignature(testData[1]);
            
            // Add to history - should update existing entry, not create new one
            GameHistoryManager.addHistoryEntry(activity, improvedEntry);
            
            step("INFO", "Added improved entry with 6 moves (still not perfect)");
        });
        
        // Step 6: Verify history was updated (not duplicated)
        step("7/9", "Verifying history entry was updated (not duplicated)");
        long[] updatedStats = new long[5]; // [bestMoves, completionCount, historySize, lastSolvedNoHints, lastPerfectNoHints]
        activityRule.getScenario().onActivity(activity -> {
            List<GameHistoryEntry> entries = GameHistoryManager.getHistoryEntries(activity);
            updatedStats[2] = entries.size();
            
            GameHistoryEntry entry = GameHistoryManager.findByMapSignature(activity, testData[1]);
            assertNotNull("Updated history entry should still be found by mapSignature", entry);
            updatedStats[0] = entry.getBestMoves();
            updatedStats[1] = entry.getCompletionCount();
            updatedStats[3] = entry.getLastSolvedWithoutHints();
            updatedStats[4] = entry.getLastPerfectlySolvedWithoutHints();
            
            step("INFO", String.format("Updated: bestMoves=%d, count=%d, size=%d, lastNoHints=%d, lastPerfect=%d",
                    updatedStats[0], updatedStats[1], updatedStats[2], updatedStats[3], updatedStats[4]));
        });
        
        // Verify no duplicate entry was created
        assertEquals("History size should remain 1 (no duplicate)", 1, updatedStats[2]);
        
        // Verify bestMoves was updated
        assertEquals("BestMoves should be updated to 6", 6, updatedStats[0]);
        
        // Verify completionCount was incremented
        assertEquals("CompletionCount should be incremented to 2", 2, updatedStats[1]);
        
        // Verify hint timestamps remain correct
        assertEquals("lastSolvedWithoutHints should remain unchanged", initialHintStats[3], updatedStats[3]);
        assertEquals("lastPerfectlySolvedWithoutHints should still be 0", 0, updatedStats[4]);
        
        step("8/9", "Verifying qualifiesForPerfectNoHintsAchievement returns false");
        boolean[] finalQualify = new boolean[1];
        activityRule.getScenario().onActivity(activity -> {
            GameHistoryEntry entry = GameHistoryManager.findByMapSignature(activity, testData[1]);
            finalQualify[0] = entry.qualifiesForPerfectNoHintsAchievement();
            step("INFO", "qualifiesForPerfectNoHintsAchievement = " + finalQualify[0]);
        });
        
        assertFalse("Should NOT qualify for perfect no-hints (never solved optimally without hints)", finalQualify[0]);
        
        step("9/9", "PASS: All verifications passed - hints marked, perfect flag stays false, no duplicates");
        
        // Dump relevant logs for debugging
        TestHelper.dumpLogcat("Final Logs", "HISTORY", "findByMapSignature|Updated existing map|MIGRATION|everUsedHints", 30);
    }

    private void step(String step, String msg) {
        String line = TAG + " [" + step + "] " + msg;
        Timber.d(line);
        System.out.println(line);
    }
}

