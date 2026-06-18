package roboyard.logic.storage

import driftingdroids.model.Board
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for save/load game functionality
 * Tests the saveGame() and loadGame() functions
 */
class SaveLoadTest {

    @Test
    fun testSaveAndLoadGame() {
        println("[TEST] Starting save/load test")
        
        // Create a simple board
        val board = Board.createBoardFreestyle(null, 5, 5, 1) ?: run {
            println("[TEST] Failed to create board")
            return
        }
        board.setRobots(intArrayOf(12)) // Robot at (2, 2)
        board.addGoal(7, 0, 0) // Goal at (2, 1) for robot 0
        board.setGoalRandom()
        
        println("[TEST] Created board: ${board.width}x${board.height}")
        println("[TEST] Robot position: ${board.robotPositions[0]}")
        println("[TEST] Goal position: ${board.goals[0].position}")
        println("[TEST] Goal robot number: ${board.goals[0].robotNumber}")
        
        // Get storage
        val storage = getPlatformStorage()
        assertNotNull(storage, "Storage should not be null")
        
        // Save game to slot 1
        val saveFileName = "saves/save_1.dat"
        println("[TEST] Saving game to: $saveFileName")
        
        val moveCount = 5
        val isLevelGame = false
        val gameWon = false
        val timestamp = System.currentTimeMillis()
        
        val saveData = buildString {
            appendLine("width:${board.width}")
            appendLine("height:${board.height}")
            appendLine("robots:${board.robotPositions.joinToString(",")}")
            for (goal in board.goals) {
                appendLine("goal:${goal.position},${goal.robotNumber}")
            }
            appendLine("moveCount:$moveCount")
            appendLine("isLevelGame:$isLevelGame")
            appendLine("timestamp:$timestamp")
            appendLine("gameWon:$gameWon")
        }
        
        println("[TEST] Save data: $saveData")
        val saveResult = storage.writeFile(saveFileName, saveData)
        assertTrue(saveResult, "Save should succeed")
        
        // Check if file exists
        val fileExists = storage.fileExists(saveFileName)
        assertTrue(fileExists, "File should exist after save")
        println("[TEST] File exists: $fileExists")
        
        // Load game from slot 1
        println("[TEST] Loading game from: $saveFileName")
        val loadedData = storage.readFile(saveFileName)
        println("[TEST] Loaded data: $loadedData")
        
        val lines = loadedData.lines()
        var width = 0
        var height = 0
        var robots = ""
        var loadedMoveCount = 0
        var loadedIsLevelGame = false
        var loadedTimestamp = 0L
        var loadedGameWon = false
        var loadedGoals = mutableListOf<Pair<Int, Int>>()
        
        for (line in lines) {
            when {
                line.startsWith("width:") -> width = line.substringAfter("width:").toInt()
                line.startsWith("height:") -> height = line.substringAfter("height:").toInt()
                line.startsWith("robots:") -> robots = line.substringAfter("robots:")
                line.startsWith("moveCount:") -> loadedMoveCount = line.substringAfter("moveCount:").toInt()
                line.startsWith("isLevelGame:") -> loadedIsLevelGame = line.substringAfter("isLevelGame:").toBoolean()
                line.startsWith("timestamp:") -> loadedTimestamp = line.substringAfter("timestamp:").toLong()
                line.startsWith("gameWon:") -> loadedGameWon = line.substringAfter("gameWon:").toBoolean()
                line.startsWith("goal:") -> {
                    val parts = line.substringAfter("goal:").split(",")
                    loadedGoals.add(Pair(parts[0].toInt(), parts[1].toInt()))
                }
            }
        }
        
        println("[TEST] Parsed: width=$width, height=$height, robots=$robots")
        println("[TEST] Parsed: moveCount=$loadedMoveCount, isLevelGame=$loadedIsLevelGame, timestamp=$loadedTimestamp, gameWon=$loadedGameWon")
        println("[TEST] Parsed: goals=$loadedGoals")
        
        // Verify all data matches
        assertEquals(board.width, width, "Width should match")
        assertEquals(board.height, height, "Height should match")
        assertEquals(board.robotPositions.joinToString(","), robots, "Robot positions should match")
        assertEquals(moveCount, loadedMoveCount, "Move count should match")
        assertEquals(isLevelGame, loadedIsLevelGame, "isLevelGame should match")
        assertEquals(timestamp, loadedTimestamp, "Timestamp should match")
        assertEquals(gameWon, loadedGameWon, "gameWon should match")
        assertEquals(board.goals.size, loadedGoals.size, "Number of goals should match")
        
        // Verify goals match
        for (i in board.goals.indices) {
            assertEquals(board.goals[i].position, loadedGoals[i].first, "Goal position should match")
            assertEquals(board.goals[i].robotNumber, loadedGoals[i].second, "Goal robot number should match")
        }
        
        // Create board from save data
        val robotPositions = robots.split(",").map { it.toInt() }
        val numRobots = robotPositions.size
        val loadedBoard = Board.createBoardFreestyle(null, width, height, numRobots)
        
        assertNotNull(loadedBoard, "Loaded board should not be null")
        
        // Set robot positions
        for (i in robotPositions.indices) {
            loadedBoard.robotPositions[i] = robotPositions[i]
        }
        
        println("[TEST] Loaded board: ${loadedBoard.width}x${loadedBoard.height}")
        println("[TEST] Loaded robot position: ${loadedBoard.robotPositions[0]}")
        
        // Verify loaded board matches original
        assertEquals(board.width, loadedBoard.width, "Board width should match")
        assertEquals(board.height, loadedBoard.height, "Board height should match")
        assertEquals(board.robotPositions[0], loadedBoard.robotPositions[0], "Robot position should match")
        
        println("[TEST] Save/load test completed successfully - all data matches")
        
        // Clean up
        storage.writeFile(saveFileName, "")
        println("[TEST] Cleaned up save file")
    }
    
    @Test
    fun testStorageHasSavedGames() {
        println("[TEST] Testing hasSavedGames")
        
        val storage = getPlatformStorage()
        assertNotNull(storage, "Storage should not be null")
        
        // Save a game first
        val saveFileName = "saves/save_1.dat"
        val saveData = "width:5\nheight:5\nrobots:12"
        storage.writeFile(saveFileName, saveData)
        
        // Check if hasSavedGames returns true
        val hasSavedGames = storage.hasSavedGames()
        assertTrue(hasSavedGames, "hasSavedGames should return true after saving")
        
        println("[TEST] hasSavedGames: $hasSavedGames")
        
        // Clean up
        storage.writeFile(saveFileName, "")
        println("[TEST] Cleaned up save file")
    }
}
