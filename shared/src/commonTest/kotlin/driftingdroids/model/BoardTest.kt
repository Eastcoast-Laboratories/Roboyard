package driftingdroids.model

import kotlin.test.Test
import kotlin.test.assertEquals

class BoardTest {
    @Test
    fun testBoardStandardSize() {
        assertEquals(12, Board.WIDTH_STANDARD)
        assertEquals(14, Board.HEIGHT_STANDARD)
    }

    @Test
    fun testBoardSizeLimits() {
        assertEquals(3, Board.WIDTH_MIN)
        assertEquals(100, Board.WIDTH_MAX)
        assertEquals(3, Board.HEIGHT_MIN)
        assertEquals(100, Board.HEIGHT_MAX)
    }

    @Test
    fun testBoardSizeMax() {
        assertEquals(4096, Board.SIZE_MAX)
    }

    @Test
    fun testNumRobotsStandard() {
        assertEquals(4, Board.NUMROBOTS_STANDARD)
    }
}
