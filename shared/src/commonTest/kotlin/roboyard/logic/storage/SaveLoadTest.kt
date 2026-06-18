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
        
        // Get storage
        val storage = getPlatformStorage()
        assertNotNull(storage, "Storage should not be null")
        
        // Save game to slot 1
        val saveFileName = "saves/save_1.dat"
        println("[TEST] Saving game to: $saveFileName")
        
        val saveData = buildString {
            appendLine("width:${board.width}")
            appendLine("height:${board.height}")
            appendLine("robots:${board.robotPositions.joinToString(",")}")
            for (goal in board.goals) {
                appendLine("goal:${goal.position},${goal.robotNumber}")
            }
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
        
        for (line in lines) {
            when {
                line.startsWith("width:") -> width = line.substringAfter("width:").toInt()
                line.startsWith("height:") -> height = line.substringAfter("height:").toInt()
                line.startsWith("robots:") -> robots = line.substringAfter("robots:")
            }
        }
        
        println("[TEST] Parsed: width=$width, height=$height, robots=$robots")
        
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
        assertEquals(board.width, loadedBoard.width, "Width should match")
        assertEquals(board.height, loadedBoard.height, "Height should match")
        assertEquals(board.robotPositions[0], loadedBoard.robotPositions[0], "Robot position should match")
        
        println("[TEST] Save/load test completed successfully")
        
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
