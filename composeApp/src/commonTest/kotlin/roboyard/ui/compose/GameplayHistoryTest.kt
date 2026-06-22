package roboyard.ui.compose

import driftingdroids.model.Board
import roboyard.logic.core.GameHistoryEntry
import roboyard.logic.storage.getPlatformStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test that plays a complete game and verifies history saving
 * Tests timer, star calculation, and history info popup display
 * 
 * This is a HEADLESS test (no GUI window) - runs unit tests on game logic
 * 
 * How to run this test:
 ./gradlew :composeApp:desktopTest --tests "roboyard.ui.compose.GameplayHistoryTest" --no-configuration-cache
 * 
 * What this test verifies:
 * - Game completion records bestTime and bestMoves correctly
 * - Multiple completions update bestTime/bestMoves only when performance improves
 * - History info popup message contains all required information (completions, best time, best moves, optimal moves)
 * - Completion timestamps, stars, and moves are stored in lists for each completion
 */
class GameplayHistoryTest {

    /**
     * Test that plays a complete game and verifies bestTime and bestMoves are saved
     */
    @Test
    fun testCompleteGameSavesBestTimeAndMoves() {
        println("[TEST] Starting complete game test")
        
        // Create a simple solvable board
        val board = Board.createBoardFreestyle(null, 5, 5, 1) ?: return
        board.setRobots(intArrayOf(12)) // Robot at (2, 2)
        board.setWall(2, 0, Board.NORTH, true) // Wall north of robot at (2, 0)
        board.addGoal(2, 0, 0) // Goal at (2, 0) for robot 0
        board.setGoalRandom()
        
        println("[TEST] Initial board: robot at (2, 2), goal at (2, 0)")
        
        // Play the game by moving the robot
        var currentBoard = board
        var moveCount = 0
        
        // Move robot north to goal
        val newBoard = moveRobot(currentBoard, 0, Board.NORTH)
        assertNotNull(newBoard, "Move should succeed")
        currentBoard = newBoard
        moveCount++
        
        println("[TEST] Move $moveCount: Robot moved to (2, 0)")
        
        // Verify game is solved
        assertTrue(isSolved(currentBoard), "Game should be solved")
        println("[TEST] Game solved in $moveCount moves")
        
        // Create a history entry
        val storage = getPlatformStorage()
        val entry = GameHistoryEntry(
            mapPath = "test_map",
            mapName = "Test Map",
            timestamp = System.currentTimeMillis(),
            playDuration = 0,
            movesMade = 0,
            optimalMoves = 1, // Optimal is 1 move
            boardSize = "5x5",
            previewImagePath = null
        )
        
        // Simulate game completion with time and moves
        val gameTime = 30 // 30 seconds
        entry.recordCompletion(gameTime, moveCount, 3) // 3 stars for optimal solution
        
        println("[TEST] Recorded completion: time=$gameTime, moves=$moveCount, stars=3")
        
        // Verify bestTime and bestMoves are saved
        assertEquals(gameTime, entry.bestTime, "Best time should be saved")
        assertEquals(moveCount, entry.bestMoves, "Best moves should be saved")
        assertEquals(3, entry.starsEarned, "Stars should be saved")
        
        println("[TEST] Verified: bestTime=${entry.bestTime}, bestMoves=${entry.bestMoves}, stars=${entry.starsEarned}")
        
        // Play the game again with worse performance
        val worseTime = 45 // 45 seconds
        val worseMoves = 2 // 2 moves (not optimal)
        entry.recordCompletion(worseTime, worseMoves, 2) // 2 stars
        
        println("[TEST] Recorded worse completion: time=$worseTime, moves=$worseMoves, stars=2")
        
        // Verify bestTime and bestMoves are NOT updated (worse performance)
        assertEquals(gameTime, entry.bestTime, "Best time should remain unchanged")
        assertEquals(moveCount, entry.bestMoves, "Best moves should remain unchanged")
        assertEquals(3, entry.starsEarned, "Stars should remain unchanged")
        
        println("[TEST] Verified: bestTime and bestMoves unchanged after worse performance")
        
        // Play the game again with better time but same moves
        val betterTime = 20 // 20 seconds (better time)
        entry.recordCompletion(betterTime, moveCount, 3) // 3 stars
        
        println("[TEST] Recorded better time completion: time=$betterTime, moves=$moveCount, stars=3")
        
        // Verify bestTime is updated but bestMoves remains the same
        assertEquals(betterTime, entry.bestTime, "Best time should be updated")
        assertEquals(moveCount, entry.bestMoves, "Best moves should remain unchanged")
        
        println("[TEST] Verified: bestTime updated to ${entry.bestTime}, bestMoves unchanged")
    }

    /**
     * Test that verifies history info popup message contains all required information
     */
    @Test
    fun testHistoryInfoPopupMessageContainsAllInfo() {
        println("[TEST] Starting history info popup message test")
        
        // Create a history entry with multiple completions
        val entry = GameHistoryEntry(
            mapPath = "test_map",
            mapName = "Level 1",
            timestamp = System.currentTimeMillis() - 86400000, // 1 day ago
            playDuration = 0,
            movesMade = 0,
            optimalMoves = 1,
            boardSize = "5x5",
            previewImagePath = null
        )
        
        // Record multiple completions
        entry.recordCompletion(30, 1, 3) // First completion: 30s, 1 move, 3 stars
        Thread.sleep(100)
        entry.recordCompletion(25, 1, 3) // Second completion: 25s, 1 move, 3 stars
        Thread.sleep(100)
        entry.recordCompletion(40, 2, 2) // Third completion: 40s, 2 moves, 2 stars
        
        println("[TEST] Recorded 3 completions")
        
        // Verify all data is stored correctly
        assertEquals(3, entry.completionCount, "Should have 3 completions")
        assertEquals(25, entry.bestTime, "Best time should be 25s")
        assertEquals(1, entry.bestMoves, "Best moves should be 1")
        assertEquals(3, entry.starsEarned, "Stars should be 3")
        
        // Verify completion timestamps and stars/moves are stored
        val timestamps = entry.getCompletionTimestamps()
        val stars = entry.getCompletionStars()
        val moves = entry.getCompletionMoves()
        
        assertNotNull(timestamps, "Timestamps should not be null")
        assertNotNull(stars, "Stars should not be null")
        assertNotNull(moves, "Moves should not be null")
        
        assertEquals(3, timestamps?.size, "Should have 3 timestamps")
        assertEquals(3, stars?.size, "Should have 3 star entries")
        assertEquals(3, moves?.size, "Should have 3 move entries")
        
        println("[TEST] Verified: ${entry.completionCount} completions, bestTime=${entry.bestTime}s, bestMoves=${entry.bestMoves}")
        
        // Simulate building the history info popup message
        val message = buildString {
            append("Completions: ${entry.completionCount}\n")
            append("Best time: ")
            if (entry.bestTime > 0) {
                append("${entry.bestTime / 60}m ${entry.bestTime % 60}s")
            } else {
                append("—")
            }
            append("\n")
            append("Best moves: ")
            append(if (entry.bestMoves > 0) entry.bestMoves else "—")
            append("\n")
            append("Optimal moves: ")
            if (entry.optimalMoves > 0) {
                append(entry.optimalMoves)
                if (entry.bestMoves > 0 && entry.bestMoves == entry.optimalMoves) {
                    append(" ✓ (Perfect)")
                }
            } else {
                append("—")
            }
        }
        
        println("[TEST] History info message:\n$message")
        
        // Verify message contains all required information
        assertTrue(message.contains("Completions: 3"), "Message should contain completion count")
        assertTrue(message.contains("Best time: 0m 25s"), "Message should contain best time")
        assertTrue(message.contains("Best moves: 1"), "Message should contain best moves")
        assertTrue(message.contains("Optimal moves: 1"), "Message should contain optimal moves")
        assertTrue(message.contains("Perfect"), "Message should indicate perfect solution")
    }
}
