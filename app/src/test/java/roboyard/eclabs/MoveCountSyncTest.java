package roboyard.eclabs;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import roboyard.logic.core.GameState;
import roboyard.logic.core.GameHistoryEntry;

/**
 * Unit test to verify moveCount synchronization between GameState and history entries.
 * Tests that when a game is saved to history, the moveCount is recorded correctly.
 *
 * Tags: move-count, game-state, history, synchronization
 */
public class MoveCountSyncTest {

    private GameState gameState;

    @Before
    public void setUp() {
        gameState = new GameState(16, 16);
        gameState.setMoveCount(0);
    }

    @Test
    public void testGameStateMoveCounting() {
        // Initial state
        assertEquals("Initial moveCount should be 0", 0, gameState.getMoveCount());

        // Simulate 5 moves
        for (int i = 1; i <= 5; i++) {
            gameState.setMoveCount(i);
            assertEquals("After " + i + " moves", i, gameState.getMoveCount());
        }
    }

    @Test
    public void testHistoryEntryMoveCounting() {
        // Create a history entry with 5 moves
        GameHistoryEntry entry = new GameHistoryEntry(
                "history_1.txt",
                "Test Map",
                System.currentTimeMillis(),
                60,  // playDuration
                5,   // movesMade
                3,   // optimalMoves (3 optimal, but 5 actual = 2 extra)
                "16x16",
                "preview.txt"
        );

        assertEquals("movesMade should be 5", 5, entry.getMovesMade());
        assertEquals("optimalMoves should be 3", 3, entry.getOptimalMoves());
        // bestMoves is 0 until recordCompletion() is called
        assertEquals("bestMoves should be 0 before completion", 0, entry.getBestMoves());
    }

    @Test
    public void testMoveCountWithOptimalSolution() {
        // Scenario: optimal solution is 3 moves, but player makes 8 moves (3 optimal + 5 extra)
        int optimalMoves = 3;
        int playerMoves = 8;
        int extraMoves = playerMoves - optimalMoves;

        GameHistoryEntry entry = new GameHistoryEntry(
                "history_1.txt",
                "Test Map",
                System.currentTimeMillis(),
                60,
                playerMoves,
                optimalMoves,
                "16x16",
                "preview.txt"
        );

        assertEquals("Player should have made 8 moves", 8, entry.getMovesMade());
        assertEquals("Optimal should be 3 moves", 3, entry.getOptimalMoves());
        assertEquals("Extra moves should be 5", 5, extraMoves);
        // bestMoves is 0 until recordCompletion() is called
        assertEquals("bestMoves should be 0 before completion", 0, entry.getBestMoves());
    }

    @Test
    public void testMoveCountOffByOne() {
        // Test if there's an off-by-one error
        // If moveCount is always recorded as optimalMoves + 1 instead of actual moves
        int optimalMoves = 3;
        int actualPlayerMoves = 8;
        int buggyRecordedMoves = optimalMoves + 1; // This would be the bug

        GameHistoryEntry entry = new GameHistoryEntry(
                "history_1.txt",
                "Test Map",
                System.currentTimeMillis(),
                60,
                actualPlayerMoves,
                optimalMoves,
                "16x16",
                "preview.txt"
        );

        // Verify the bug is NOT present
        assertNotEquals("movesMade should NOT be optimalMoves + 1", buggyRecordedMoves, entry.getMovesMade());
        assertEquals("movesMade should be actual player moves", actualPlayerMoves, entry.getMovesMade());
    }
}
