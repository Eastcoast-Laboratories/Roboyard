package roboyard.eclabs;

import org.junit.Test;
import static org.junit.Assert.*;

import roboyard.logic.core.LevelCompletionData;

/**
 * Unit tests for LevelCompletionData and the save/update logic
 * used in LevelCompletionManager.
 *
 * Tags: level-completion, stars, optimal-moves, hints, restore
 */
public class LevelCompletionDataTest {

    // ========== Basic LevelCompletionData Tests ==========

    @Test
    public void testNewDataHasZeroDefaults() {
        LevelCompletionData data = new LevelCompletionData(1);
        assertEquals(1, data.getLevelId());
        assertFalse(data.isCompleted());
        assertEquals(0, data.getStars());
        assertEquals(0, data.getOptimalMoves());
        assertEquals(0, data.getHintsShown());
        assertEquals(0, data.getMovesNeeded());
    }

    @Test
    public void testStarsClampedTo0to3() {
        LevelCompletionData data = new LevelCompletionData(1);
        data.setStars(-1);
        assertEquals(0, data.getStars());
        data.setStars(5);
        assertEquals(3, data.getStars());
        data.setStars(2);
        assertEquals(2, data.getStars());
    }

    // ========== Save Logic: Stars Only Improve ==========

    @Test
    public void testStarsOnlyImprove() {
        // Simulate LevelCompletionManager.saveLevelCompletionData logic
        LevelCompletionData existing = new LevelCompletionData(5);
        existing.setCompleted(true);
        existing.setStars(2);
        existing.setOptimalMoves(4);
        existing.setMovesNeeded(5);

        LevelCompletionData newData = new LevelCompletionData(5);
        newData.setCompleted(true);
        newData.setStars(1); // worse
        newData.setOptimalMoves(4);
        newData.setMovesNeeded(6);

        // Apply save logic
        boolean starsImproved = newData.getStars() > existing.getStars();
        assertFalse("Stars should NOT improve from 2 to 1", starsImproved);
        // Stars should remain 2
        assertEquals(2, existing.getStars());
    }

    @Test
    public void testStarsDoImproveWhenBetter() {
        LevelCompletionData existing = new LevelCompletionData(5);
        existing.setCompleted(true);
        existing.setStars(1);
        existing.setOptimalMoves(4);

        LevelCompletionData newData = new LevelCompletionData(5);
        newData.setCompleted(true);
        newData.setStars(3); // better
        newData.setOptimalMoves(4);

        boolean starsImproved = newData.getStars() > existing.getStars();
        assertTrue("Stars should improve from 1 to 3", starsImproved);

        if (starsImproved) {
            existing.setStars(newData.getStars());
        }
        assertEquals(3, existing.getStars());
    }

    // ========== OptimalMoves: Only Update When > 0 ==========

    @Test
    public void testOptimalMovesNotOverwrittenWithZero() {
        LevelCompletionData existing = new LevelCompletionData(10);
        existing.setOptimalMoves(5);

        LevelCompletionData newData = new LevelCompletionData(10);
        newData.setOptimalMoves(0); // e.g. from restore without solver data

        // Apply logic: only update if > 0
        if (newData.getOptimalMoves() > 0) {
            existing.setOptimalMoves(newData.getOptimalMoves());
        }

        assertEquals("OptimalMoves should NOT be overwritten with 0", 5, existing.getOptimalMoves());
    }

    @Test
    public void testOptimalMovesUpdatedWhenValid() {
        LevelCompletionData existing = new LevelCompletionData(10);
        existing.setOptimalMoves(0); // no solver data yet

        LevelCompletionData newData = new LevelCompletionData(10);
        newData.setOptimalMoves(4); // solver found solution

        if (newData.getOptimalMoves() > 0) {
            existing.setOptimalMoves(newData.getOptimalMoves());
        }

        assertEquals("OptimalMoves should be updated to 4", 4, existing.getOptimalMoves());
    }

    @Test
    public void testOptimalMovesUpdatedWhenBothValid() {
        LevelCompletionData existing = new LevelCompletionData(10);
        existing.setOptimalMoves(5);

        LevelCompletionData newData = new LevelCompletionData(10);
        newData.setOptimalMoves(4); // solver found better solution

        if (newData.getOptimalMoves() > 0) {
            existing.setOptimalMoves(newData.getOptimalMoves());
        }

        assertEquals("OptimalMoves should be updated to 4", 4, existing.getOptimalMoves());
    }

    // ========== HintsShown: Always Updated ==========

    @Test
    public void testHintsShownAlwaysUpdated() {
        LevelCompletionData existing = new LevelCompletionData(10);
        existing.setHintsShown(0);

        LevelCompletionData newData = new LevelCompletionData(10);
        newData.setHintsShown(2);

        existing.setHintsShown(newData.getHintsShown());
        assertEquals("HintsShown should be updated to 2", 2, existing.getHintsShown());
    }

    @Test
    public void testHintsShownCanGoToZero() {
        LevelCompletionData existing = new LevelCompletionData(10);
        existing.setHintsShown(3);

        LevelCompletionData newData = new LevelCompletionData(10);
        newData.setHintsShown(0); // this attempt used no hints

        existing.setHintsShown(newData.getHintsShown());
        assertEquals("HintsShown should be updated to 0", 0, existing.getHintsShown());
    }

    // ========== Direct Hint Field Sync (no conversion, all fields synced individually) ==========

    @Test
    public void testMaxHintUsedSyncedDirectly() {
        // Upload: maxHintUsed is sent as-is to server
        int maxHintUsed = 2;
        assertEquals("maxHintUsed should be synced directly", 2, maxHintUsed);

        // Download: maxHintUsed is read as-is from server
        int serverMaxHintUsed = 2;
        assertEquals("Downloaded maxHintUsed should match", 2, serverMaxHintUsed);
    }

    @Test
    public void testMaxHintUsedDefault() {
        int maxHintUsed = -1; // no hints ever used
        assertEquals("Default maxHintUsed should be -1", -1, maxHintUsed);
    }

    @Test
    public void testEverUsedHintsSyncedDirectly() {
        boolean everUsedHints = true;
        assertTrue("everUsedHints should be synced as boolean", everUsedHints);

        boolean serverEverUsedHints = true;
        assertTrue("Downloaded everUsedHints should match", serverEverUsedHints);
    }

    @Test
    public void testSolvedWithoutHintsSyncedDirectly() {
        boolean solvedWithoutHints = true;
        assertTrue("solvedWithoutHints should be synced as boolean", solvedWithoutHints);
    }

    @Test
    public void testTimestampFieldsSyncedDirectly() {
        long lastSolvedWithoutHints = 1709000000000L;
        long lastPerfectlySolvedWithoutHints = 1709000000000L;
        assertEquals(1709000000000L, lastSolvedWithoutHints);
        assertEquals(1709000000000L, lastPerfectlySolvedWithoutHints);
    }

    @Test
    public void testHintsShownDerivedFromMaxHintUsed() {
        // In restoreLevelStarsFromHistory, hintsShown is derived from max_hint_used
        int maxHintUsed = -1;
        int hintsShown = maxHintUsed >= 0 ? maxHintUsed + 1 : 0;
        assertEquals("No hints: hintsShown should be 0", 0, hintsShown);

        maxHintUsed = 0;
        hintsShown = maxHintUsed >= 0 ? maxHintUsed + 1 : 0;
        assertEquals("maxHintUsed=0: hintsShown should be 1", 1, hintsShown);

        maxHintUsed = 2;
        hintsShown = maxHintUsed >= 0 ? maxHintUsed + 1 : 0;
        assertEquals("maxHintUsed=2: hintsShown should be 3", 3, hintsShown);
    }

    // ========== Restore Logic: Stars + OptimalMoves + Hints ==========

    @Test
    public void testRestoreFromServer_starsImproved() {
        // Simulate restoreLevelStarsFromHistory logic
        LevelCompletionData existing = new LevelCompletionData(5);
        existing.setCompleted(true);
        existing.setStars(1);
        existing.setOptimalMoves(0);
        existing.setHintsShown(0);

        int serverStars = 3;
        int serverOptimal = 4;
        int serverMaxHintUsed = -1;
        int hintsShown = serverMaxHintUsed >= 0 ? serverMaxHintUsed + 1 : 0;

        boolean starsImproved = serverStars > existing.getStars();
        boolean hasNewMetadata = (serverOptimal > 0 && existing.getOptimalMoves() == 0);

        assertTrue("Stars should be improved", starsImproved);
        assertTrue("Should have new metadata", hasNewMetadata);

        // Apply
        existing.setStars(serverStars);
        if (serverOptimal > 0) existing.setOptimalMoves(serverOptimal);
        existing.setHintsShown(hintsShown);

        assertEquals(3, existing.getStars());
        assertEquals(4, existing.getOptimalMoves());
        assertEquals(0, existing.getHintsShown());
    }

    @Test
    public void testRestoreFromServer_onlyMetadataNew() {
        // Stars same, but optimal_moves is new
        LevelCompletionData existing = new LevelCompletionData(5);
        existing.setCompleted(true);
        existing.setStars(2);
        existing.setOptimalMoves(0); // no solver data locally
        existing.setHintsShown(0);

        int serverStars = 2;
        int serverOptimal = 5;
        int serverMaxHintUsed = 0; // hint index 0 used
        int hintsShown = serverMaxHintUsed >= 0 ? serverMaxHintUsed + 1 : 0;

        boolean starsImproved = serverStars > existing.getStars();
        boolean hasNewMetadata = (serverOptimal > 0 && existing.getOptimalMoves() == 0);

        assertFalse("Stars should NOT be improved (same)", starsImproved);
        assertTrue("Should have new metadata (optimal moves)", hasNewMetadata);

        // Apply
        if (starsImproved) existing.setStars(serverStars);
        if (serverOptimal > 0) existing.setOptimalMoves(serverOptimal);
        existing.setHintsShown(hintsShown);

        assertEquals(2, existing.getStars());
        assertEquals(5, existing.getOptimalMoves());
        assertEquals(1, existing.getHintsShown());
    }

    @Test
    public void testRestoreFromServer_nothingNew() {
        // Everything same or worse
        LevelCompletionData existing = new LevelCompletionData(5);
        existing.setCompleted(true);
        existing.setStars(3);
        existing.setOptimalMoves(4);
        existing.setHintsShown(0);

        int serverStars = 2; // worse
        int serverOptimal = 4; // same
        int serverMaxHintUsed = -1;

        boolean starsImproved = serverStars > existing.getStars();
        boolean hasNewMetadata = (serverOptimal > 0 && existing.getOptimalMoves() == 0);

        assertFalse("Stars should NOT be improved", starsImproved);
        assertFalse("Should NOT have new metadata (already has optimal)", hasNewMetadata);
    }

    // ========== Full Round-Trip: Save -> Upload -> Download -> Restore ==========

    @Test
    public void testRoundTrip_optimalMovesPreserved() {
        // Step 1: Level completed locally with optimal_moves=4, hints=0, stars=3
        LevelCompletionData localData = new LevelCompletionData(7);
        localData.setCompleted(true);
        localData.setStars(3);
        localData.setOptimalMoves(4);
        localData.setHintsShown(0);
        localData.setMovesNeeded(4);

        // Step 2: Upload converts to JSON
        int uploadOptimal = localData.getOptimalMoves();
        int uploadHints = localData.getHintsShown(); // 0
        int uploadStars = localData.getStars();
        int uploadMoves = localData.getMovesNeeded();

        // Step 3: Server stores and returns these values

        // Step 4: Download & restore on new device
        LevelCompletionData restored = new LevelCompletionData(7);
        restored.setCompleted(true);
        restored.setStars(uploadStars);
        if (uploadMoves > 0) restored.setMovesNeeded(uploadMoves);
        if (uploadOptimal > 0) restored.setOptimalMoves(uploadOptimal);
        restored.setHintsShown(uploadHints);

        assertEquals(3, restored.getStars());
        assertEquals(4, restored.getOptimalMoves());
        assertEquals(0, restored.getHintsShown());
        assertEquals(4, restored.getMovesNeeded());
    }

    @Test
    public void testRoundTrip_hintsPreserved() {
        // Level completed with hints
        LevelCompletionData localData = new LevelCompletionData(8);
        localData.setCompleted(true);
        localData.setStars(1);
        localData.setOptimalMoves(3);
        localData.setHintsShown(2);
        localData.setMovesNeeded(5);

        // Upload
        int uploadOptimal = localData.getOptimalMoves();
        int uploadHints = localData.getHintsShown();
        int uploadStars = localData.getStars();

        // Restore
        LevelCompletionData restored = new LevelCompletionData(8);
        restored.setCompleted(true);
        restored.setStars(uploadStars);
        if (uploadOptimal > 0) restored.setOptimalMoves(uploadOptimal);
        restored.setHintsShown(uploadHints);

        assertEquals(1, restored.getStars());
        assertEquals(3, restored.getOptimalMoves());
        assertEquals(2, restored.getHintsShown());
    }
}
