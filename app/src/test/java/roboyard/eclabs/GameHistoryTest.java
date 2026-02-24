package roboyard.eclabs;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import roboyard.logic.core.GameHistoryEntry;

/**
 * Unit tests for GameHistoryEntry unique map tracking functionality.
 * Tests the new fields and methods for tracking map completions.
 *
 * Tags: game-history, unique-map-tracking, map-completions, history-entry
 */
public class GameHistoryTest {

    private GameHistoryEntry entry;

    @Before
    public void setUp() {
        entry = new GameHistoryEntry(
                "history_1.txt",
                "Test Map",
                1000000L,
                120,  // playDuration
                15,   // movesMade
                10,   // optimalMoves
                "16x16",
                "history_1.txt_preview.txt"
        );
    }

    @Test
    public void testNewEntryHasCorrectInitialValues() {
        // completionCount starts at 0 - not completed until recordCompletion() is called
        assertEquals(0, entry.getCompletionCount());
        assertEquals(0L, entry.getLastCompletionTimestamp());
        assertEquals(0, entry.getBestTime());
        assertEquals(0, entry.getBestMoves());
        assertNotNull(entry.getCompletionTimestamps());
        assertEquals(0, entry.getCompletionTimestamps().size());
    }

    @Test
    public void testIsFirstCompletionForNewEntry() {
        // New entry is NOT a first completion yet - it hasn't been completed
        assertFalse(entry.isFirstCompletion());
    }

    @Test
    public void testIsFirstCompletionAfterOneCompletion() {
        entry.recordCompletion(120, 15);
        assertTrue(entry.isFirstCompletion());
    }

    @Test
    public void testRecordCompletionIncrementsCount() {
        entry.recordCompletion(100, 12);
        assertEquals(1, entry.getCompletionCount());
        assertTrue(entry.isFirstCompletion());
        assertEquals(1, entry.getCompletionTimestamps().size());

        entry.recordCompletion(90, 11);
        assertEquals(2, entry.getCompletionCount());
        assertFalse(entry.isFirstCompletion());
        assertEquals(2, entry.getCompletionTimestamps().size());
    }

    @Test
    public void testRecordCompletionUpdatesBestTime() {
        // First completion sets best time
        entry.recordCompletion(120, 15);
        assertEquals(120, entry.getBestTime());

        // Record faster completion
        boolean newBest = entry.recordCompletion(80, 20);
        assertTrue(newBest);
        assertEquals(80, entry.getBestTime());
        // Best moves should still be 15 (first completion)
        assertEquals(15, entry.getBestMoves());
    }

    @Test
    public void testRecordCompletionUpdatesBestMoves() {
        // First completion
        entry.recordCompletion(200, 15);
        assertEquals(15, entry.getBestMoves());

        // Record completion with fewer moves
        boolean newBest = entry.recordCompletion(200, 10);
        assertTrue(newBest);
        assertEquals(10, entry.getBestMoves());
    }

    @Test
    public void testRecordCompletionNoNewBest() {
        // First completion
        entry.recordCompletion(120, 15);

        // Record worse completion
        boolean newBest = entry.recordCompletion(200, 20);
        assertFalse(newBest);
        assertEquals(120, entry.getBestTime());
        assertEquals(15, entry.getBestMoves());
    }

    @Test
    public void testMultipleCompletionsTracked() {
        entry.recordCompletion(100, 12);
        entry.recordCompletion(90, 11);
        entry.recordCompletion(85, 10);

        assertEquals(3, entry.getCompletionCount());
        assertEquals(3, entry.getCompletionTimestamps().size());
        assertEquals(85, entry.getBestTime());
        assertEquals(10, entry.getBestMoves());
    }

    @Test
    public void testSignatureSettersAndGetters() {
        assertNull(entry.getWallSignature());
        assertNull(entry.getPositionSignature());
        assertNull(entry.getMapSignature());
        
        entry.setWallSignature("16x16|H1,2;V3,4;");
        entry.setPositionSignature("R0@5,5;|T0@10,10;");
        entry.setMapSignature("16x16|H1,2;V3,4;||R0@5,5;|T0@10,10;");
        
        assertEquals("16x16|H1,2;V3,4;", entry.getWallSignature());
        assertEquals("R0@5,5;|T0@10,10;", entry.getPositionSignature());
        assertEquals("16x16|H1,2;V3,4;||R0@5,5;|T0@10,10;", entry.getMapSignature());
    }

    @Test
    public void testCompletionTimestampsSetterHandlesNull() {
        entry.setCompletionTimestamps(null);
        assertNotNull(entry.getCompletionTimestamps());
        assertTrue(entry.getCompletionTimestamps().isEmpty());
    }

    @Test
    public void testCompletionTimestampsSetter() {
        List<Long> timestamps = new ArrayList<>();
        timestamps.add(1000L);
        timestamps.add(2000L);
        timestamps.add(3000L);
        
        entry.setCompletionTimestamps(timestamps);
        
        assertEquals(3, entry.getCompletionTimestamps().size());
        assertEquals(Long.valueOf(1000L), entry.getCompletionTimestamps().get(0));
        assertEquals(Long.valueOf(3000L), entry.getCompletionTimestamps().get(2));
    }

    @Test
    public void testDefaultConstructorInitializesCompletionTimestamps() {
        GameHistoryEntry emptyEntry = new GameHistoryEntry();
        assertNotNull(emptyEntry.getCompletionTimestamps());
        assertTrue(emptyEntry.getCompletionTimestamps().isEmpty());
    }

    @Test
    public void testLastCompletionTimestampUpdatedOnRecordCompletion() {
        // Initially 0 (not completed)
        assertEquals(0L, entry.getLastCompletionTimestamp());

        entry.recordCompletion(100, 12);
        assertTrue(entry.getLastCompletionTimestamp() > 0);

        long afterFirst = entry.getLastCompletionTimestamp();
        try { Thread.sleep(10); } catch (InterruptedException e) { }
        entry.recordCompletion(90, 11);
        assertTrue(entry.getLastCompletionTimestamp() > afterFirst);
    }

    @Test
    public void testDifficultyField() {
        // Default is empty
        assertEquals("", entry.getDifficulty());

        entry.setDifficulty("Expert");
        assertEquals("Expert", entry.getDifficulty());

        // Null should be treated as empty
        entry.setDifficulty(null);
        assertEquals("", entry.getDifficulty());
    }

    @Test
    public void testCompletedStatusNotSetOnCreation() {
        // A new entry should NOT be marked as completed
        assertEquals(0, entry.getCompletionCount());
        assertFalse(entry.isFirstCompletion());

        // After first completion it should be marked
        entry.recordCompletion(120, 15);
        assertEquals(1, entry.getCompletionCount());
        assertTrue(entry.isFirstCompletion());
    }
    
    // ========== Hint Tracking Tests ==========
    
    @Test
    public void testNewEntryHasNoHintsUsed() {
        assertEquals(-1, entry.getMaxHintUsed());
        assertFalse(entry.hasUsedHints());
    }
    
    @Test
    public void testRecordHintUsed() {
        entry.recordHintUsed(0);
        assertEquals(0, entry.getMaxHintUsed());
        assertTrue(entry.hasUsedHints());
    }
    
    @Test
    public void testRecordHintUsedTracksMaximum() {
        entry.recordHintUsed(2);
        entry.recordHintUsed(1);  // Lower hint, should not update
        entry.recordHintUsed(5);  // Higher hint, should update
        entry.recordHintUsed(3);  // Lower hint, should not update
        
        assertEquals(5, entry.getMaxHintUsed());
    }
    
    @Test
    public void testSolvedWithoutHintsDefault() {
        assertFalse(entry.isSolvedWithoutHints());
    }
    
    @Test
    public void testSolvedWithoutHintsSetter() {
        entry.setSolvedWithoutHints(true);
        assertTrue(entry.isSolvedWithoutHints());
    }
    
    @Test
    public void testQualifiesForNoHintsAchievementWhenNoHintsUsed() {
        entry.setSolvedWithoutHints(true);
        // maxHintUsed is -1 by default
        assertTrue(entry.qualifiesForNoHintsAchievement());
    }
    
    @Test
    public void testDoesNotQualifyForNoHintsAchievementWhenHintsUsed() {
        entry.setSolvedWithoutHints(true);
        entry.recordHintUsed(0);
        assertFalse(entry.qualifiesForNoHintsAchievement());
    }
    
    @Test
    public void testDoesNotQualifyForNoHintsAchievementWhenNotSolvedWithoutHints() {
        // solvedWithoutHints is false by default
        assertFalse(entry.qualifiesForNoHintsAchievement());
    }
    
    @Test
    public void testHintTrackingPersistsThroughMultipleCompletions() {
        // First completion without hints
        entry.setSolvedWithoutHints(true);
        assertTrue(entry.qualifiesForNoHintsAchievement());
        
        // Second completion - record completion doesn't change hint status
        entry.recordCompletion(100, 12);
        assertTrue(entry.qualifiesForNoHintsAchievement());
        
        // If hints are used later, map is permanently marked
        entry.recordHintUsed(0);
        assertFalse(entry.qualifiesForNoHintsAchievement());
        
        // Even more completions don't fix it
        entry.recordCompletion(80, 10);
        assertFalse(entry.qualifiesForNoHintsAchievement());
    }
}
