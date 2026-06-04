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
        assertEquals(0, entry.completionCount);
        assertEquals(0L, entry.lastCompletionTimestamp);
        assertEquals(0, entry.bestTime);
        assertEquals(0, entry.bestMoves);
        assertNotNull(entry.getCompletionTimestamps());
        assertEquals(0, entry.getCompletionTimestamps().size());
        assertNotNull(entry.getCompletionMoves());
        assertEquals(0, entry.getCompletionMoves().size());
        assertNotNull(entry.getCompletionStars());
        assertEquals(0, entry.getCompletionStars().size());
    }

    @Test
    public void testIsFirstCompletionForNewEntry() {
        // New entry is NOT a first completion yet - it hasn't been completed
        assertFalse(entry.isFirstCompletion());
    }

    @Test
    public void testIsFirstCompletionAfterOneCompletion() {
        entry.recordCompletion(120, 15, 3);
        assertTrue(entry.isFirstCompletion());
    }

    @Test
    public void testRecordCompletionIncrementsCount() {
        entry.recordCompletion(100, 12, 3);
        assertEquals(1, entry.completionCount);
        assertTrue(entry.isFirstCompletion());
        assertEquals(1, entry.getCompletionTimestamps().size());
        assertEquals(1, entry.getCompletionMoves().size());
        assertEquals(Integer.valueOf(12), entry.getCompletionMoves().get(0));

        entry.recordCompletion(90, 11, 3);
        assertEquals(2, entry.completionCount);
        assertFalse(entry.isFirstCompletion());
        assertEquals(2, entry.getCompletionTimestamps().size());
        assertEquals(2, entry.getCompletionMoves().size());
        assertEquals(Integer.valueOf(11), entry.getCompletionMoves().get(1));
    }

    @Test
    public void testRecordCompletionTracksStarsPerCompletion() {
        entry.recordCompletion(100, 12, 2);
        entry.recordCompletion(90, 11, 3);

        assertEquals(2, entry.completionCount);
        assertEquals(2, entry.getCompletionStars().size());
        assertEquals(Integer.valueOf(2), entry.getCompletionStars().get(0));
        assertEquals(Integer.valueOf(3), entry.getCompletionStars().get(1));
    }

    @Test
    public void testRecordCompletionUpdatesBestTime() {
        // First completion sets best time
        entry.recordCompletion(120, 15, 3);
        assertEquals(120, entry.bestTime);

        // Record faster completion
        boolean newBest = entry.recordCompletion(80, 20, 2);
        assertTrue(newBest);
        assertEquals(80, entry.bestTime);
        // Best moves should still be 15 (first completion)
        assertEquals(15, entry.bestMoves);
    }

    @Test
    public void testRecordCompletionUpdatesBestMoves() {
        // First completion
        entry.recordCompletion(200, 15, 3);
        assertEquals(15, entry.bestMoves);

        // Record completion with fewer moves
        boolean newBest = entry.recordCompletion(200, 10, 3);
        assertTrue(newBest);
        assertEquals(10, entry.bestMoves);
    }

    @Test
    public void testRecordCompletionNoNewBest() {
        // First completion
        entry.recordCompletion(120, 15, 3);

        // Record worse completion
        boolean newBest = entry.recordCompletion(200, 20, 2);
        assertFalse(newBest);
        assertEquals(120, entry.bestTime);
        assertEquals(15, entry.bestMoves);
    }

    @Test
    public void testMultipleCompletionsTracked() {
        entry.recordCompletion(100, 12, 3);
        entry.recordCompletion(90, 11, 3);
        entry.recordCompletion(85, 10, 3);

        assertEquals(3, entry.completionCount);
        assertEquals(3, entry.getCompletionTimestamps().size());
        assertEquals(85, entry.bestTime);
        assertEquals(10, entry.bestMoves);
    }

    @Test
    public void testSignatureSettersAndGetters() {
        assertNull(entry.wallSignature);
        assertNull(entry.positionSignature);
        assertNull(entry.mapSignature);
        
        entry.wallSignature = "16x16|H1,2;V3,4;";
        entry.positionSignature = "R0@5,5;|T0@10,10;";
        entry.mapSignature = "16x16|H1,2;V3,4;||R0@5,5;|T0@10,10;";
        
        assertEquals("16x16|H1,2;V3,4;", entry.wallSignature);
        assertEquals("R0@5,5;|T0@10,10;", entry.positionSignature);
        assertEquals("16x16|H1,2;V3,4;||R0@5,5;|T0@10,10;", entry.mapSignature);
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
        assertEquals(0L, entry.lastCompletionTimestamp);

        entry.recordCompletion(100, 12, 3);
        assertTrue(entry.lastCompletionTimestamp > 0);

        long afterFirst = entry.lastCompletionTimestamp;
        try { Thread.sleep(10); } catch (InterruptedException e) { }
        entry.recordCompletion(90, 11, 3);
        assertTrue(entry.lastCompletionTimestamp > afterFirst);
    }

    @Test
    public void testDifficultyField() {
        // Default is BEGINNER (0)
        assertEquals(roboyard.logic.core.Constants.DIFFICULTY_BEGINNER, entry.difficulty);

        entry.difficulty = roboyard.logic.core.Constants.DIFFICULTY_IMPOSSIBLE;
        assertEquals(roboyard.logic.core.Constants.DIFFICULTY_IMPOSSIBLE, entry.difficulty);

        // Test all difficulty levels
        entry.difficulty = roboyard.logic.core.Constants.DIFFICULTY_ADVANCED;
        assertEquals(roboyard.logic.core.Constants.DIFFICULTY_ADVANCED, entry.difficulty);
    }

    @Test
    public void testCompletedStatusNotSetOnCreation() {
        // A new entry should NOT be marked as completed
        assertEquals(0, entry.completionCount);
        assertFalse(entry.isFirstCompletion());

        // After first completion it should be marked
        entry.recordCompletion(120, 15, 3);
        assertEquals(1, entry.completionCount);
        assertTrue(entry.isFirstCompletion());
    }
    
    // ========== Hint Tracking Tests ==========
    
    @Test
    public void testNewEntryHasNoHintsUsed() {
        assertEquals(-1, entry.maxHintUsed);
        assertFalse(entry.hasUsedHints());
    }
    
    @Test
    public void testRecordHintUsed() {
        entry.recordHintUsed(0);
        assertEquals(0, entry.maxHintUsed);
        assertTrue(entry.hasUsedHints());
    }
    
    @Test
    public void testRecordHintUsedTracksMaximum() {
        entry.recordHintUsed(2);
        entry.recordHintUsed(1);  // Lower hint, should not update
        entry.recordHintUsed(5);  // Higher hint, should update
        entry.recordHintUsed(3);  // Lower hint, should not update
        
        assertEquals(5, entry.maxHintUsed);
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
        entry.recordCompletion(100, 12, 3);
        assertTrue(entry.qualifiesForNoHintsAchievement());
        
        // If hints are used later, qualification is NOT revoked (new behavior)
        entry.recordHintUsed(0);
        assertTrue("Qualification must survive later hint usage", entry.qualifiesForNoHintsAchievement());
        
        // Even more completions don't change it
        entry.recordCompletion(80, 10, 3);
        assertTrue(entry.qualifiesForNoHintsAchievement());
    }

    // ========== OptimalMoves Persistence Tests ==========

    @Test
    public void testOptimalMovesSetInConstructor() {
        // Constructor sets optimalMoves = 10
        assertEquals(10, entry.optimalMoves);
    }

    @Test
    public void testOptimalMovesSetterGetter() {
        entry.optimalMoves = 7;
        assertEquals(7, entry.optimalMoves);
    }

    @Test
    public void testOptimalMovesSurvivesRecordCompletion() {
        // optimalMoves should not be changed by recordCompletion
        assertEquals(10, entry.optimalMoves);
        entry.recordCompletion(100, 12, 3);
        assertEquals(10, entry.optimalMoves);
    }

    @Test
    public void testBestMovesUpdatedByRecordCompletion() {
        // First completion sets bestMoves
        entry.recordCompletion(100, 12, 2);
        assertEquals(12, entry.bestMoves);

        // Better completion updates bestMoves
        entry.recordCompletion(80, 8, 3);
        assertEquals(8, entry.bestMoves);

        // Worse completion does NOT update bestMoves
        entry.recordCompletion(120, 15, 1);
        assertEquals(8, entry.bestMoves);
    }

    @Test
    public void testMovesMadeUpdatedByRecordCompletion() {
        // movesMade in constructor = 15
        assertEquals(15, entry.movesMade);

        // recordCompletion always updates movesMade to latest
        entry.recordCompletion(100, 12, 3);
        assertEquals(12, entry.movesMade);

        entry.recordCompletion(80, 20, 1);
        assertEquals(20, entry.movesMade);
    }

    @Test
    public void testOptimalMovesCanBeUpdatedAfterIntermediateSave() {
        // Simulate: entry created with optimalMoves=0 (intermediate save, solution not yet available)
        GameHistoryEntry intermediateEntry = new GameHistoryEntry(
                "history_2.txt", "Test Map 2", 2000000L, 60, 0, 0, "12x12", "preview.txt");
        assertEquals(0, intermediateEntry.optimalMoves);

        // Later, when game completes via updateHintTracking path, optimalMoves is set
        intermediateEntry.optimalMoves = 5;
        assertEquals(5, intermediateEntry.optimalMoves);

        // recordCompletion updates bestMoves and movesMade but not optimalMoves
        intermediateEntry.recordCompletion(90, 7, 2);
        assertEquals(5, intermediateEntry.optimalMoves);
        assertEquals(7, intermediateEntry.bestMoves);
        assertEquals(7, intermediateEntry.movesMade);
    }

    @Test
    public void testQualifiesForNoHintsWithOptimalMoves() {
        // Solve optimally without hints
        entry.recordSolvedWithoutHints(true); // isOptimal=true
        assertTrue(entry.qualifiesForNoHintsAchievement());
        assertTrue(entry.lastPerfectlySolvedWithoutHints > 0);
    }
}
