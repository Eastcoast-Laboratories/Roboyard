package roboyard.logic.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GameLogicTest {
    @Test
    fun testGameLogicCreation() {
        val gameLogic = GameLogic(
            boardWidth = 12,
            boardHeight = 14,
            difficultyLevel = GameLogic.DIFFICULTY_BEGINNER
        )
        
        assertNotNull(gameLogic)
    }

    @Test
    fun testDifficultyLevels() {
        assertEquals(0, GameLogic.DIFFICULTY_BEGINNER)
        assertEquals(1, GameLogic.DIFFICULTY_ADVANCED)
        assertEquals(2, GameLogic.DIFFICULTY_INSANE)
        assertEquals(3, GameLogic.DIFFICULTY_IMPOSSIBLE)
    }
}
