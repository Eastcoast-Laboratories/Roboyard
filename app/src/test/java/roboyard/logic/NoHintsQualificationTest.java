package roboyard.logic;

import org.junit.Test;
import roboyard.logic.core.GameHistoryEntry;

import static org.junit.Assert.*;

/**
 * Unit tests for the "qualifies for no-hints achievement" logic in GameHistoryEntry.
 *
 * Bug: After solving a map optimally without hints, then re-playing with hints,
 * qualifiesForNoHintsAchievement() returned false because it checked !everUsedHints.
 *
 * Fix: qualifiesForNoHintsAchievement() now returns true if lastSolvedWithoutHints > 0,
 * regardless of later hint usage. The timestamp is set by recordSolvedWithoutHints()
 * and never cleared.
 *
 * Tags: unit-test, no-hints, achievement, history, qualification, regression
 * Run with: ./gradlew testDebugUnitTest --tests "roboyard.logic.NoHintsQualificationTest"
 */
public class NoHintsQualificationTest {

    private GameHistoryEntry makeEntry() {
        return new GameHistoryEntry(
                "history_1.txt", "TestMap", System.currentTimeMillis(),
                60, 5, 5, "16x16", null);
    }

    // --- recordSolvedWithoutHints ---

    @Test
    public void testInitiallyDoesNotQualify() {
        GameHistoryEntry entry = makeEntry();
        assertFalse("Fresh entry should NOT qualify", entry.qualifiesForNoHintsAchievement());
        assertFalse("Fresh entry should NOT qualify for perfect", entry.qualifiesForPerfectNoHintsAchievement());
        assertEquals("lastSolvedWithoutHints should be 0", 0L, entry.getLastSolvedWithoutHints());
        assertEquals("lastPerfectlySolvedWithoutHints should be 0", 0L, entry.getLastPerfectlySolvedWithoutHints());
    }

    @Test
    public void testSolvedWithoutHintsNonOptimalQualifies() {
        GameHistoryEntry entry = makeEntry();
        entry.recordSolvedWithoutHints(false);
        assertTrue("Should qualify after no-hints solve", entry.qualifiesForNoHintsAchievement());
        assertFalse("Should NOT qualify for perfect (non-optimal)", entry.qualifiesForPerfectNoHintsAchievement());
        assertTrue("lastSolvedWithoutHints should be set", entry.getLastSolvedWithoutHints() > 0);
        assertEquals("lastPerfectlySolvedWithoutHints should still be 0", 0L, entry.getLastPerfectlySolvedWithoutHints());
    }

    @Test
    public void testSolvedWithoutHintsOptimalQualifiesBoth() {
        GameHistoryEntry entry = makeEntry();
        entry.recordSolvedWithoutHints(true);
        assertTrue("Should qualify after optimal no-hints solve", entry.qualifiesForNoHintsAchievement());
        assertTrue("Should qualify for perfect after optimal no-hints solve", entry.qualifiesForPerfectNoHintsAchievement());
        assertTrue("lastSolvedWithoutHints should be set", entry.getLastSolvedWithoutHints() > 0);
        assertTrue("lastPerfectlySolvedWithoutHints should be set", entry.getLastPerfectlySolvedWithoutHints() > 0);
    }

    // --- Core regression test: later hint usage must NOT revoke qualification ---

    @Test
    public void testQualificationNotRevokedByLaterHintUsage() {
        GameHistoryEntry entry = makeEntry();

        // First: solve without hints (optimal)
        entry.recordSolvedWithoutHints(true);
        assertTrue("Should qualify before hint usage", entry.qualifiesForNoHintsAchievement());
        assertTrue("Should qualify for perfect before hint usage", entry.qualifiesForPerfectNoHintsAchievement());

        // Later: use hints (re-play with hints)
        entry.markEverUsedHints();
        entry.recordHintUsed(0);

        // Qualification must NOT be revoked
        assertTrue("qualifiesForNoHintsAchievement must survive later hint usage", entry.qualifiesForNoHintsAchievement());
        assertTrue("qualifiesForPerfectNoHintsAchievement must survive later hint usage", entry.qualifiesForPerfectNoHintsAchievement());
        assertTrue("everUsedHints is true", entry.isEverUsedHints());
        assertTrue("lastSolvedWithoutHints still set", entry.getLastSolvedWithoutHints() > 0);
    }

    @Test
    public void testNoHintsQualificationNotRevokedBySubsequentNonOptimalSolve() {
        GameHistoryEntry entry = makeEntry();

        // First: solve optimally without hints
        entry.recordSolvedWithoutHints(true);
        long firstTimestamp = entry.getLastPerfectlySolvedWithoutHints();
        assertTrue(firstTimestamp > 0);

        // Later: solve again without hints but non-optimally
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        entry.recordSolvedWithoutHints(false);

        // No-hints still qualifies
        assertTrue(entry.qualifiesForNoHintsAchievement());
        // Perfect still qualifies (from first solve)
        assertTrue(entry.qualifiesForPerfectNoHintsAchievement());
        // lastSolvedWithoutHints updated to newer timestamp
        assertTrue(entry.getLastSolvedWithoutHints() >= firstTimestamp);
        // lastPerfectlySolvedWithoutHints still from first solve (not overwritten by non-optimal)
        assertEquals("Perfect timestamp should remain from first optimal solve",
                firstTimestamp, entry.getLastPerfectlySolvedWithoutHints());
    }

    @Test
    public void testHintsUsedFirstNeverQualifies() {
        GameHistoryEntry entry = makeEntry();

        // Hints used first, never solved without hints
        entry.markEverUsedHints();
        entry.recordHintUsed(0);

        assertFalse("Should NOT qualify if only solved with hints", entry.qualifiesForNoHintsAchievement());
        assertFalse("Should NOT qualify for perfect if only solved with hints", entry.qualifiesForPerfectNoHintsAchievement());
    }

    @Test
    public void testHintsThenNoHintsSolveStillDisqualified() {
        GameHistoryEntry entry = makeEntry();

        // First attempt: with hints - permanently disqualifies
        entry.markEverUsedHints();
        entry.recordHintUsed(1);

        // Second attempt: without hints - but still disqualified because everUsedHints=true
        entry.recordSolvedWithoutHints(false);

        assertFalse("Once disqualified by hints, cannot be re-qualified", entry.qualifiesForNoHintsAchievement());
        assertEquals("lastSolvedWithoutHints must NOT be set after disqualification",
                0L, entry.getLastSolvedWithoutHints());
    }

    // --- Getters/Setters round-trip ---

    @Test
    public void testSettersGettersRoundTrip() {
        GameHistoryEntry entry = makeEntry();
        long ts1 = 1_700_000_000_000L;
        long ts2 = 1_800_000_000_000L;

        entry.setLastSolvedWithoutHints(ts1);
        entry.setLastPerfectlySolvedWithoutHints(ts2);

        assertEquals(ts1, entry.getLastSolvedWithoutHints());
        assertEquals(ts2, entry.getLastPerfectlySolvedWithoutHints());
        assertTrue(entry.qualifiesForNoHintsAchievement());
        assertTrue(entry.qualifiesForPerfectNoHintsAchievement());
    }

    @Test
    public void testZeroTimestampsDoNotQualify() {
        GameHistoryEntry entry = makeEntry();
        entry.setLastSolvedWithoutHints(0);
        entry.setLastPerfectlySolvedWithoutHints(0);

        assertFalse(entry.qualifiesForNoHintsAchievement());
        assertFalse(entry.qualifiesForPerfectNoHintsAchievement());
    }

    @Test
    public void testOptimalTimestampUpdatedOnSubsequentOptimalSolve() {
        GameHistoryEntry entry = makeEntry();

        entry.recordSolvedWithoutHints(true);
        long first = entry.getLastPerfectlySolvedWithoutHints();

        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        entry.recordSolvedWithoutHints(true);
        long second = entry.getLastPerfectlySolvedWithoutHints();

        assertTrue("Second optimal timestamp should be >= first", second >= first);
    }
}
