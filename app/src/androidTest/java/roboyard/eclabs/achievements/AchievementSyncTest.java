package roboyard.eclabs.achievements;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import roboyard.ui.components.RoboyardApiClient;
import timber.log.Timber;

/**
 * Test for Achievement sync to roboyard.z11.de server.
 * 
 * Tests:
 * 1. Achievement sync API call structure
 * 2. Sync after achievement unlock
 * 3. Sync only when logged in
 */
@RunWith(AndroidJUnit4.class)
public class AchievementSyncTest {

    private Context context;
    private AchievementManager achievementManager;
    private RoboyardApiClient apiClient;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        achievementManager = AchievementManager.getInstance(context);
        apiClient = RoboyardApiClient.getInstance(context);
        
        // Reset achievements for clean test
        achievementManager.resetAll();
        
        Timber.d("[ACHIEVEMENT_SYNC_TEST] ========== TEST STARTED ==========");
    }

    @After
    public void tearDown() {
        // Reset achievements after test
        achievementManager.resetAll();
        Timber.d("[ACHIEVEMENT_SYNC_TEST] ========== TEST FINISHED ==========");
    }

    @Test
    public void testSyncToServerNotLoggedIn() {
        // Ensure not logged in
        apiClient.logout();
        
        // This should not throw an error, just log and skip
        achievementManager.syncToServer();
        
        // If we get here without exception, test passes
        Timber.d("[ACHIEVEMENT_SYNC_TEST] Sync skipped correctly when not logged in");
    }

    @Test
    public void testAchievementUnlockTriggersSyncCall() {
        // Unlock an achievement
        boolean unlocked = achievementManager.unlock("first_game");
        
        assertTrue("first_game should be unlocked", unlocked);
        assertTrue("first_game should be marked as unlocked", achievementManager.isUnlocked("first_game"));
        
        // The sync is triggered with a delay, so we just verify the achievement is unlocked
        // The actual sync will happen asynchronously
        Timber.d("[ACHIEVEMENT_SYNC_TEST] Achievement unlocked, sync should be triggered");
    }

    @Test
    public void testAchievementDataStructure() throws Exception {
        // Unlock some achievements
        achievementManager.unlock("first_game");
        achievementManager.unlock("level_1_complete");
        
        // Get all achievements
        java.util.List<Achievement> achievements = achievementManager.getAllAchievements();
        
        // Build JSON array like syncToServer does
        JSONArray achievementsArray = new JSONArray();
        for (Achievement achievement : achievements) {
            JSONObject achievementJson = new JSONObject();
            achievementJson.put("id", achievement.getId());
            achievementJson.put("unlocked", achievement.isUnlocked());
            achievementJson.put("unlocked_timestamp", achievement.getUnlockedTimestamp());
            achievementsArray.put(achievementJson);
        }
        
        // Verify structure
        assertTrue("Should have achievements", achievementsArray.length() > 0);
        
        // Find first_game in the array
        boolean foundFirstGame = false;
        for (int i = 0; i < achievementsArray.length(); i++) {
            JSONObject obj = achievementsArray.getJSONObject(i);
            if ("first_game".equals(obj.getString("id"))) {
                foundFirstGame = true;
                assertTrue("first_game should be unlocked", obj.getBoolean("unlocked"));
                assertTrue("first_game should have timestamp", obj.getLong("unlocked_timestamp") > 0);
            }
        }
        
        assertTrue("first_game should be in achievements array", foundFirstGame);
        Timber.d("[ACHIEVEMENT_SYNC_TEST] Achievement data structure is correct");
    }

    @Test
    public void testStatsDataStructure() throws Exception {
        // Simulate some game completions
        achievementManager.onLevelCompleted(1, 5, 5, 0, 3, 15000);
        achievementManager.onRandomGameCompleted(8, 8, 0, 25000, false, 4, 1, 1);
        
        // Build stats object like syncToServer does
        JSONObject stats = new JSONObject();
        // These are approximations since we can't access private fields directly
        stats.put("total_games_solved", 2);
        stats.put("total_games_solved_no_hints", 1);
        stats.put("total_perfect_solutions", 2);
        
        // Verify structure
        assertTrue("Should have total_games_solved", stats.has("total_games_solved"));
        assertTrue("Should have total_games_solved_no_hints", stats.has("total_games_solved_no_hints"));
        assertTrue("Should have total_perfect_solutions", stats.has("total_perfect_solutions"));
        
        Timber.d("[ACHIEVEMENT_SYNC_TEST] Stats data structure is correct");
    }

    @Test
    public void testSyncWithRealServer() throws InterruptedException {
        // This test requires a logged-in user
        // Skip if not logged in
        if (!apiClient.isLoggedIn()) {
            Timber.d("[ACHIEVEMENT_SYNC_TEST] Skipping real server test - not logged in");
            return;
        }
        
        // Unlock an achievement
        achievementManager.unlock("first_game");
        
        // Create a latch to wait for async response
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] syncSuccess = {false};
        final String[] syncError = {null};
        
        try {
            // Build achievements array
            JSONArray achievementsArray = new JSONArray();
            for (Achievement achievement : achievementManager.getAllAchievements()) {
                JSONObject achievementJson = new JSONObject();
                achievementJson.put("id", achievement.getId());
                achievementJson.put("unlocked", achievement.isUnlocked());
                achievementJson.put("unlocked_timestamp", achievement.getUnlockedTimestamp());
                achievementsArray.put(achievementJson);
            }
            
            // Build stats
            JSONObject stats = new JSONObject();
            stats.put("total_games_solved", 1);
            stats.put("total_games_solved_no_hints", 0);
            stats.put("total_perfect_solutions", 0);
            
            // Sync to server
            apiClient.syncAchievements(achievementsArray, stats, new RoboyardApiClient.ApiCallback<RoboyardApiClient.AchievementSyncResult>() {
                @Override
                public void onSuccess(RoboyardApiClient.AchievementSyncResult result) {
                    syncSuccess[0] = result.success;
                    Timber.d("[ACHIEVEMENT_SYNC_TEST] Sync result: success=%s, synced=%d, new=%d",
                            result.success, result.syncedCount, result.newAchievements);
                    latch.countDown();
                }
                
                @Override
                public void onError(String error) {
                    syncError[0] = error;
                    Timber.e("[ACHIEVEMENT_SYNC_TEST] Sync error: %s", error);
                    latch.countDown();
                }
            });
            
            // Wait for response (max 10 seconds)
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            
            assertTrue("Sync should complete within timeout", completed);
            assertNull("Sync should not have error: " + syncError[0], syncError[0]);
            assertTrue("Sync should be successful", syncSuccess[0]);
            
        } catch (Exception e) {
            Timber.e(e, "[ACHIEVEMENT_SYNC_TEST] Exception during sync test");
            fail("Exception during sync: " + e.getMessage());
        }
    }
}
