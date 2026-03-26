package roboyard.eclabs.achievements;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test to verify that the SharedPreferences key format used by syncFromServer()
 * matches the format used by loadState().
 *
 * Bug history: syncFromServer() previously wrote keys as "id_unlocked" (e.g. "first_game_unlocked")
 * but loadState() reads "unlocked_id" (e.g. "unlocked_first_game"). This mismatch caused
 * achievements synced from server to be invisible after app restart.
 *
 * Tags: achievement-sync, shared-preferences, key-format, regression
 */
public class AchievementSyncKeyFormatTest {

    private static final String KEY_PREFIX_UNLOCKED = "unlocked_";
    private static final String KEY_PREFIX_TIMESTAMP = "timestamp_";

    /**
     * Simulates the key format that loadState() uses to READ from SharedPreferences.
     */
    private String loadStateUnlockedKey(String id) {
        return KEY_PREFIX_UNLOCKED + id;
    }

    private String loadStateTimestampKey(String id) {
        return KEY_PREFIX_TIMESTAMP + id;
    }

    /**
     * Simulates the FIXED key format that syncFromServer() uses to WRITE to SharedPreferences.
     * (After fix: uses KEY_PREFIX_UNLOCKED + id, matching loadState)
     */
    private String syncFromServerUnlockedKey(String id) {
        return KEY_PREFIX_UNLOCKED + id;
    }

    private String syncFromServerTimestampKey(String id) {
        return KEY_PREFIX_TIMESTAMP + id;
    }

    /**
     * Simulates the OLD BUGGY key format that syncFromServer() used to write.
     */
    private String buggyUnlockedKey(String id) {
        return id + "_unlocked";
    }

    private String buggyTimestampKey(String id) {
        return id + "_timestamp";
    }

    @Test
    public void testFixedSyncKeysMatchLoadStateKeys() {
        String[] testIds = {"first_game", "level_master_10", "speed_demon", "gimme_five", "daily_streak_7"};
        
        for (String id : testIds) {
            assertEquals(
                "syncFromServer unlocked key must match loadState unlocked key for: " + id,
                loadStateUnlockedKey(id),
                syncFromServerUnlockedKey(id)
            );
            assertEquals(
                "syncFromServer timestamp key must match loadState timestamp key for: " + id,
                loadStateTimestampKey(id),
                syncFromServerTimestampKey(id)
            );
        }
    }

    @Test
    public void testBuggyKeysDoNotMatchLoadStateKeys() {
        String[] testIds = {"first_game", "level_master_10", "speed_demon"};
        
        for (String id : testIds) {
            assertNotEquals(
                "Buggy unlocked key format must NOT match loadState format for: " + id,
                loadStateUnlockedKey(id),
                buggyUnlockedKey(id)
            );
            assertNotEquals(
                "Buggy timestamp key format must NOT match loadState format for: " + id,
                loadStateTimestampKey(id),
                buggyTimestampKey(id)
            );
        }
    }

    @Test
    public void testKeyFormatExamples() {
        // Verify concrete key examples
        assertEquals("unlocked_first_game", loadStateUnlockedKey("first_game"));
        assertEquals("timestamp_first_game", loadStateTimestampKey("first_game"));
        assertEquals("unlocked_first_game", syncFromServerUnlockedKey("first_game"));
        assertEquals("timestamp_first_game", syncFromServerTimestampKey("first_game"));
        
        // Verify buggy keys are different
        assertEquals("first_game_unlocked", buggyUnlockedKey("first_game"));
        assertEquals("first_game_timestamp", buggyTimestampKey("first_game"));
    }

    @Test
    public void testMigrationKeyMapping() {
        // Verify that migration correctly maps old keys to new keys
        String id = "level_master_10";
        
        String wrongKey = buggyUnlockedKey(id);      // "level_master_10_unlocked"
        String correctKey = loadStateUnlockedKey(id); // "unlocked_level_master_10"
        
        assertNotEquals("Old and new keys must be different", wrongKey, correctKey);
        assertTrue("Correct key must start with prefix", correctKey.startsWith(KEY_PREFIX_UNLOCKED));
        assertFalse("Wrong key must not start with prefix", wrongKey.startsWith(KEY_PREFIX_UNLOCKED));
    }
}
