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
        // Default is BEGINNER (0)
        assertEquals(roboyard.logic.core.Constants.DIFFICULTY_BEGINNER, entry.getDifficulty());

        entry.setDifficulty(roboyard.logic.core.Constants.DIFFICULTY_IMPOSSIBLE);
        assertEquals(roboyard.logic.core.Constants.DIFFICULTY_IMPOSSIBLE, entry.getDifficulty());

        // Test all difficulty levels
        entry.setDifficulty(roboyard.logic.core.Constants.DIFFICULTY_ADVANCED);
        assertEquals(roboyard.logic.core.Constants.DIFFICULTY_ADVANCED, entry.getDifficulty());
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
        // Must use recordSolvedWithoutHints() to set the timestamp
        entry.recordSolvedWithoutHints(false);
        assertTrue(entry.qualifiesForNoHintsAchievement());
    }
    
    @Test
    public void testDoesNotQualifyForNoHintsAchievementWhenHintsUsedBeforeAnySolve() {
        // Hints used without ever solving without hints => does NOT qualify
        entry.recordHintUsed(0);
        assertFalse(entry.qualifiesForNoHintsAchievement());
    }

    @Test
    public void testStillQualifiesForNoHintsAfterLaterHintUsage() {
        // Solved without hints first, then hints used later => STILL qualifies
        entry.recordSolvedWithoutHints(false);
        entry.recordHintUsed(0);
        assertTrue("Qualification must survive later hint usage", entry.qualifiesForNoHintsAchievement());
    }
    
    @Test
    public void testDoesNotQualifyForNoHintsAchievementWhenNotSolvedWithoutHints() {
        // solvedWithoutHints is false by default
        assertFalse(entry.qualifiesForNoHintsAchievement());
    }
    
    @Test
    public void testHintTrackingPersistsThroughMultipleCompletions() {
        // First completion without hints
        entry.recordSolvedWithoutHints(false);
        assertTrue(entry.qualifiesForNoHintsAchievement());
        
        // Second completion - record completion doesn't change hint status
        entry.recordCompletion(100, 12);
        assertTrue(entry.qualifiesForNoHintsAchievement());
        
        // If hints are used later, qualification is NOT revoked (new behavior)
        entry.recordHintUsed(0);
        assertTrue("Qualification must NOT be revoked by later hint usage", entry.qualifiesForNoHintsAchievement());
        
        // Even more completions don't change it
        entry.recordCompletion(80, 10);
        assertTrue(entry.qualifiesForNoHintsAchievement());
    }

    // ========== OptimalMoves Persistence Tests ==========

    @Test
    public void testOptimalMovesSetInConstructor() {
        // Constructor sets optimalMoves = 10
        assertEquals(10, entry.getOptimalMoves());
    }

    @Test
    public void testOptimalMovesSetterGetter() {
        entry.setOptimalMoves(7);
        assertEquals(7, entry.getOptimalMoves());
    }

    @Test
    public void testOptimalMovesSurvivesRecordCompletion() {
        // optimalMoves should not be changed by recordCompletion
        assertEquals(10, entry.getOptimalMoves());
        entry.recordCompletion(100, 12);
        assertEquals(10, entry.getOptimalMoves());
    }

    @Test
    public void testBestMovesUpdatedByRecordCompletion() {
        // First completion sets bestMoves
        entry.recordCompletion(100, 12);
        assertEquals(12, entry.getBestMoves());

        // Better completion updates bestMoves
        entry.recordCompletion(80, 8);
        assertEquals(8, entry.getBestMoves());

        // Worse completion does NOT update bestMoves
        entry.recordCompletion(120, 15);
        assertEquals(8, entry.getBestMoves());
    }

    @Test
    public void testMovesMadeUpdatedByRecordCompletion() {
        // movesMade in constructor = 15
        assertEquals(15, entry.getMovesMade());

        // recordCompletion always updates movesMade to latest
        entry.recordCompletion(100, 12);
        assertEquals(12, entry.getMovesMade());

        entry.recordCompletion(80, 20);
        assertEquals(20, entry.getMovesMade());
    }

    @Test
    public void testOptimalMovesCanBeUpdatedAfterIntermediateSave() {
        // Simulate: entry created with optimalMoves=0 (intermediate save, solution not yet available)
        GameHistoryEntry intermediateEntry = new GameHistoryEntry(
                "history_2.txt", "Test Map 2", 2000000L, 60, 0, 0, "12x12", "preview.txt");
        assertEquals(0, intermediateEntry.getOptimalMoves());

        // Later, when game completes via updateHintTracking path, optimalMoves is set
        intermediateEntry.setOptimalMoves(5);
        assertEquals(5, intermediateEntry.getOptimalMoves());

        // recordCompletion updates bestMoves and movesMade but not optimalMoves
        intermediateEntry.recordCompletion(90, 7);
        assertEquals(5, intermediateEntry.getOptimalMoves());
        assertEquals(7, intermediateEntry.getBestMoves());
        assertEquals(7, intermediateEntry.getMovesMade());
    }

    @Test
    public void testQualifiesForNoHintsWithOptimalMoves() {
        // Solve optimally without hints
        entry.recordSolvedWithoutHints(true); // isOptimal=true
        assertTrue(entry.qualifiesForNoHintsAchievement());
        assertTrue(entry.getLastPerfectlySolvedWithoutHints() > 0);
    }
}
