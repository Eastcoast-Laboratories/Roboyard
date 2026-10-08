package roboyard.logic.core

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for calculateHistoryStars — the star rating displayed above each map
 * in the save-screen history tab (Android + Compose share this logic).
 *
 * Rules:
 * - optimal <= 10: 3 at optimal, 2 at +1, 1 at +2, 0 beyond
 * - optimal > 10: 3 at optimal, 2 within +10%, 1 within +20%, 0 beyond
 *   (linear — e.g. 20-move level: 20->3, 21-22->2, 23-24->1, 25+->0)
 *
 * Run: ./gradlew :shared:desktopTest --tests "roboyard.logic.core.StarRatingTest"
 */
class StarRatingTest {

    // ---- small maps (optimal <= 10): fixed move offsets ----

    @Test
    fun smallLevel_optimalAndOffsets() {
        assertEquals(3, calculateHistoryStars(5, 5))
        assertEquals(3, calculateHistoryStars(4, 5)) // better than optimal
        assertEquals(2, calculateHistoryStars(6, 5))
        assertEquals(1, calculateHistoryStars(7, 5))
        assertEquals(0, calculateHistoryStars(8, 5))
        assertEquals(0, calculateHistoryStars(20, 5))
    }

    @Test
    fun smallLevel_boundaryOptimal10() {
        assertEquals(3, calculateHistoryStars(10, 10))
        assertEquals(2, calculateHistoryStars(11, 10))
        assertEquals(1, calculateHistoryStars(12, 10))
        assertEquals(0, calculateHistoryStars(13, 10))
    }

    // ---- large maps (optimal > 10): percentage thresholds ----

    @Test
    fun largeLevel_linearScaling20() {
        // user's example: optimal 20 → 2 stars for 21-22 (+10%), 1 for 23-24 (+20%), 0 beyond
        assertEquals(3, calculateHistoryStars(20, 20))
        assertEquals(3, calculateHistoryStars(19, 20)) // better than optimal
        assertEquals(2, calculateHistoryStars(21, 20))
        assertEquals(2, calculateHistoryStars(22, 20))
        assertEquals(1, calculateHistoryStars(23, 20))
        assertEquals(1, calculateHistoryStars(24, 20))
        assertEquals(0, calculateHistoryStars(25, 20))
        assertEquals(0, calculateHistoryStars(30, 20))
    }

    @Test
    fun largeLevel_boundaryOptimal11() {
        // optimal 11 → +10% = 12.1 → 12 still 2 stars; +20% = 13.2 → 13 still 1 star
        assertEquals(3, calculateHistoryStars(11, 11))
        assertEquals(2, calculateHistoryStars(12, 11))
        assertEquals(1, calculateHistoryStars(13, 11))
        assertEquals(0, calculateHistoryStars(14, 11))
    }

    // ---- invalid/missing data ----

    @Test
    fun missingData_returnsZero() {
        assertEquals(0, calculateHistoryStars(5, 0))
        assertEquals(0, calculateHistoryStars(0, 5))
        assertEquals(0, calculateHistoryStars(-1, 5))
    }
}
