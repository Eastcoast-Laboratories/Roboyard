package roboyard.eclabs.ui;

import static org.junit.Assert.*;

import android.util.Log;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.MainActivity;

/**
 * Test to verify that stars are correctly synced to the server after level completion.
 * This test uses logcat analysis to verify the sync process.
 */
@RunWith(AndroidJUnit4.class)
public class HistorySyncStarsTest {
    private static final String TAG = "HistorySyncStarsTest";

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setup() {
        TestHelper.clearLogcat();
        Log.d(TAG, "=== Test started ===");
    }

    @Test
    public void testStarsAreSyncedAfterLevelCompletion() throws InterruptedException {
        Log.d(TAG, "Starting level completion and sync test");
        
        // Start a level (Level 1)
        TestHelper.startLevelGame(activityRule, 1);
        Thread.sleep(2000);
        
        // Clear logcat before completing level
        TestHelper.clearLogcat();
        Log.d(TAG, "Logcat cleared, ready to complete level");
        
        // TODO: Complete the level programmatically
        // For now, this is a manual test - complete the level in the UI
        Log.d(TAG, "===========================================");
        Log.d(TAG, "MANUAL ACTION REQUIRED:");
        Log.d(TAG, "1. Complete Level 1 with 3 stars");
        Log.d(TAG, "2. Wait for sync to complete");
        Log.d(TAG, "===========================================");
        
        // Wait for level completion and sync
        Thread.sleep(15000);
        
        // Collect and analyze logs
        TestHelper.dumpLogcat("After level completion", "GameStateManager", "HISTORY_SYNC", 100);
        TestHelper.dumpLogcat("After level completion", "SyncManager", "HISTORY_SYNC", 100);
        TestHelper.dumpLogcat("After level completion", "RoboyardApi", "HISTORY_SYNC", 100);
        
        // Analyze logs for the sync process
        java.util.List<String> gameStateLogs = TestHelper.collectLogcatLines("GameStateManager", "HISTORY_SYNC", 100);
        java.util.List<String> syncManagerLogs = TestHelper.collectLogcatLines("SyncManager", "HISTORY_SYNC", 100);
        java.util.List<String> apiLogs = TestHelper.collectLogcatLines("RoboyardApi", "HISTORY_SYNC", 100);
        
        Log.d(TAG, "=== ANALYSIS ===");
        Log.d(TAG, "GameStateManager logs: " + gameStateLogs.size());
        Log.d(TAG, "SyncManager logs: " + syncManagerLogs.size());
        Log.d(TAG, "RoboyardApi logs: " + apiLogs.size());
        
        // Check if stars were set
        boolean starsSet = false;
        boolean starsPersisted = false;
        for (String log : gameStateLogs) {
            if (log.contains("Set stars=") && log.contains("for history entry")) {
                starsSet = true;
                Log.d(TAG, "✓ Stars were set: " + log);
            }
            if (log.contains("Updated and persisted") && log.contains("history entries with stars")) {
                starsPersisted = true;
                Log.d(TAG, "✓ Stars were persisted: " + log);
            }
        }
        
        // Check if stars were uploaded correctly
        boolean starsUploadedCorrectly = false;
        boolean starsUploadedAsZero = false;
        for (String log : syncManagerLogs) {
            if (log.contains("Uploading: Level 1")) {
                if (log.contains("stars=3") || log.contains("stars=2") || log.contains("stars=1")) {
                    starsUploadedCorrectly = true;
                    Log.d(TAG, "✓ Stars uploaded correctly: " + log);
                } else if (log.contains("stars=0")) {
                    starsUploadedAsZero = true;
                    Log.e(TAG, "✗ Stars uploaded as ZERO: " + log);
                }
            }
        }
        
        // Check server response
        boolean serverAccepted = false;
        boolean serverSkipped = false;
        for (String log : apiLogs) {
            if (log.contains("✓ Updated 'Level 1'") || log.contains("✓ Created 'Level 1'")) {
                serverAccepted = true;
                Log.d(TAG, "✓ Server accepted sync: " + log);
            }
            if (log.contains("⊘ Skipped 'Level 1'")) {
                serverSkipped = true;
                Log.e(TAG, "✗ Server skipped sync: " + log);
            }
        }
        
        // Report findings
        Log.d(TAG, "=== RESULTS ===");
        Log.d(TAG, "Stars set in memory: " + starsSet);
        Log.d(TAG, "Stars persisted to disk: " + starsPersisted);
        Log.d(TAG, "Stars uploaded correctly: " + starsUploadedCorrectly);
        Log.d(TAG, "Stars uploaded as zero: " + starsUploadedAsZero);
        Log.d(TAG, "Server accepted: " + serverAccepted);
        Log.d(TAG, "Server skipped: " + serverSkipped);
        
        // Assertions
        assertTrue("Stars should be set in memory", starsSet);
        assertTrue("Stars should be persisted to disk", starsPersisted);
        assertFalse("Stars should NOT be uploaded as zero", starsUploadedAsZero);
        assertTrue("Stars should be uploaded correctly", starsUploadedCorrectly);
        assertTrue("Server should accept the sync", serverAccepted);
        assertFalse("Server should NOT skip the sync", serverSkipped);
        
        Log.d(TAG, "=== Test completed ===");
    }
}
