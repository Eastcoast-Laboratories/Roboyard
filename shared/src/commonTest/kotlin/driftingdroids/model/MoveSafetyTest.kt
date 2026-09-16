package driftingdroids.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MoveSafetyTest {

    private fun newBoard(): Board =
        Board.createBoardFreestyle(null, 4, 4, 4) ?: error("board creation failed")

    @Test
    fun test_identicalStates_throws() {
        val board = newBoard()
        val positions = intArrayOf(1, 2, 3, 4)
        assertFailsWith<IllegalArgumentException> {
            Move(board, positions, positions.copyOf(), 0)
        }
    }

    @Test
    fun test_twoChangedRobots_throws() {
        val board = newBoard()
        assertFailsWith<IllegalArgumentException> {
            Move(board, intArrayOf(1, 2, 3, 4), intArrayOf(1, 2, 5, 6), 0)
        }
        assertFailsWith<IllegalArgumentException> {
            Move(board, intArrayOf(1, 2, 3, 4), intArrayOf(1, 2, 3), 0)
        }
        assertFailsWith<IllegalArgumentException> {
            Move(board, intArrayOf(1, 2, 3), intArrayOf(1, 2, 4), 0)
        }
    }

    @Test
    fun test_nonAlignedMoves_throw() {
        val board = newBoard()
        assertFailsWith<IllegalArgumentException> {
            Move(board, intArrayOf(3, 5, 6, 7), intArrayOf(4, 5, 6, 7), 0)
        }
        assertFailsWith<IllegalArgumentException> {
            Move(board, intArrayOf(0, 1, 2, 3), intArrayOf(5, 1, 2, 3), 0)
        }
    }

    @Test
    fun test_validMoves_buildFinitePath() {
        val board = newBoard()

        val horizontal = Move(board, intArrayOf(1, 5, 6, 7), intArrayOf(3, 5, 6, 7), 0)
        assertEquals(3, horizontal.pathMap.size)
        assertEquals(Move.PATH_EAST, horizontal.pathMap[1])
        assertEquals(Move.PATH_EAST + Move.PATH_WEST, horizontal.pathMap[2])
        assertEquals(Move.PATH_WEST, horizontal.pathMap[3])

        val vertical = Move(board, intArrayOf(1, 5, 6, 7), intArrayOf(9, 5, 6, 7), 0)
        assertEquals(3, vertical.pathMap.size)
        assertEquals(Move.PATH_SOUTH, vertical.pathMap[1])
        assertEquals(Move.PATH_SOUTH + Move.PATH_NORTH, vertical.pathMap[5])
        assertEquals(Move.PATH_NORTH, vertical.pathMap[9])
    }
}
