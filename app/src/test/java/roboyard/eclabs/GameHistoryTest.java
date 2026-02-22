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
        assertEquals(1, entry.getCompletionCount());
        assertEquals(1000000L, entry.getLastCompletionTimestamp());
        assertEquals(120, entry.getBestTime());
        assertEquals(15, entry.getBestMoves());
        assertNotNull(entry.getCompletionTimestamps());
        assertEquals(1, entry.getCompletionTimestamps().size());
        assertEquals(Long.valueOf(1000000L), entry.getCompletionTimestamps().get(0));
    }

    @Test
    public void testIsFirstCompletionForNewEntry() {
        assertTrue(entry.isFirstCompletion());
    }

    @Test
    public void testRecordCompletionIncrementsCount() {
        entry.recordCompletion(100, 12);
        
        assertEquals(2, entry.getCompletionCount());
        assertFalse(entry.isFirstCompletion());
        assertEquals(2, entry.getCompletionTimestamps().size());
    }

    @Test
    public void testRecordCompletionUpdatesBestTime() {
        // Initial best time is 120
        assertEquals(120, entry.getBestTime());
        
        // Record faster completion
        boolean newBest = entry.recordCompletion(80, 20);
        
        assertTrue(newBest);
        assertEquals(80, entry.getBestTime());
        // Best moves should still be 15 (original)
        assertEquals(15, entry.getBestMoves());
    }

    @Test
    public void testRecordCompletionUpdatesBestMoves() {
        // Initial best moves is 15
        assertEquals(15, entry.getBestMoves());
        
        // Record completion with fewer moves
        boolean newBest = entry.recordCompletion(200, 10);
        
        assertTrue(newBest);
        assertEquals(10, entry.getBestMoves());
    }

    @Test
    public void testRecordCompletionNoNewBest() {
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
        
        assertEquals(4, entry.getCompletionCount());
        assertEquals(4, entry.getCompletionTimestamps().size());
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
        long initialTimestamp = entry.getLastCompletionTimestamp();
        
        // Wait a tiny bit to ensure different timestamp
        try { Thread.sleep(10); } catch (InterruptedException e) { }
        
        entry.recordCompletion(100, 12);
        
        assertTrue(entry.getLastCompletionTimestamp() > initialTimestamp);
    }
}
