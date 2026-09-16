package roboyard.logic.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapGenerationValidatorTest {

    @Test
    fun test_validMoveCount_acceptedOnFirstAttempt() {
        val validator = MapGenerationValidator(maxAttempts = 3)
        val decision = validator.evaluate(moveCount = 17, isTrivial = false, minMoves = 17, maxMoves = 99)
        assertTrue(decision.accepted)
        assertFalse(decision.shouldRetry)
        assertFalse(decision.usedFallback)
        assertEquals(1, decision.attempt)
        assertEquals(17, decision.moveCount)
        assertNull(decision.rejectionReason)
        assertEquals(0, validator.currentAttemptCount())
        assertEquals(1000, MapGenerationValidator.MAX_GENERATION_ATTEMPTS)
    }

    @Test
    fun test_tooEasy_retriesThenAcceptedFallback() {
        val validator = MapGenerationValidator(maxAttempts = 3)

        val first = validator.evaluate(moveCount = 5, isTrivial = false, minMoves = 17, maxMoves = 99)
        assertFalse(first.accepted)
        assertTrue(first.shouldRetry)
        assertFalse(first.usedFallback)
        assertEquals(1, first.attempt)
        assertEquals(MapGenerationRejectionReason.TOO_EASY, first.rejectionReason)
        assertEquals(1, validator.currentAttemptCount())

        val second = validator.evaluate(moveCount = 5, isTrivial = false, minMoves = 17, maxMoves = 99)
        assertFalse(second.accepted)
        assertTrue(second.shouldRetry)
        assertEquals(2, second.attempt)
        assertEquals(2, validator.currentAttemptCount())

        val third = validator.evaluate(moveCount = 5, isTrivial = false, minMoves = 17, maxMoves = 99)
        assertTrue(third.accepted)
        assertFalse(third.shouldRetry)
        assertTrue(third.usedFallback)
        assertEquals(3, third.attempt)
        assertEquals(MapGenerationRejectionReason.TOO_EASY, third.rejectionReason)
        assertEquals(5, third.moveCount)
        assertEquals(0, validator.currentAttemptCount())
    }

    @Test
    fun test_tooHard_returnsRetry() {
        val validator = MapGenerationValidator(maxAttempts = 3)
        val decision = validator.evaluate(moveCount = 15, isTrivial = false, minMoves = 4, maxMoves = 10)
        assertFalse(decision.accepted)
        assertTrue(decision.shouldRetry)
        assertFalse(decision.usedFallback)
        assertEquals(1, decision.attempt)
        assertEquals(MapGenerationRejectionReason.TOO_HARD, decision.rejectionReason)
        assertEquals(15, decision.moveCount)
    }

    @Test
    fun test_noSolutionAndTrivial_reasons() {
        val validator = MapGenerationValidator(maxAttempts = 3)

        val nullDecision = validator.evaluate(moveCount = null, isTrivial = false, minMoves = 17, maxMoves = 99)
        assertFalse(nullDecision.accepted)
        assertTrue(nullDecision.shouldRetry)
        assertEquals(MapGenerationRejectionReason.NO_SOLUTION, nullDecision.rejectionReason)
        assertNull(nullDecision.moveCount)

        val zeroDecision = validator.evaluate(moveCount = 0, isTrivial = false, minMoves = 17, maxMoves = 99)
        assertEquals(MapGenerationRejectionReason.NO_SOLUTION, zeroDecision.rejectionReason)

        val trivialDecision = MapGenerationValidator(maxAttempts = 3)
            .evaluate(moveCount = 17, isTrivial = true, minMoves = 17, maxMoves = 99)
        assertEquals(MapGenerationRejectionReason.TRIVIAL, trivialDecision.rejectionReason)
        assertTrue(trivialDecision.shouldRetry)
    }

    @Test
    fun test_repeatedNoSolution_reachesFallback() {
        val validator = MapGenerationValidator(maxAttempts = 3)
        validator.evaluate(moveCount = null, isTrivial = false, minMoves = 17, maxMoves = 99)
        validator.evaluate(moveCount = null, isTrivial = false, minMoves = 17, maxMoves = 99)
        val third = validator.evaluate(moveCount = null, isTrivial = false, minMoves = 17, maxMoves = 99)
        assertTrue(third.accepted)
        assertFalse(third.shouldRetry)
        assertTrue(third.usedFallback)
        assertEquals(3, third.attempt)
        assertEquals(MapGenerationRejectionReason.NO_SOLUTION, third.rejectionReason)
        assertEquals(0, validator.currentAttemptCount())
    }

    @Test
    fun test_reset_clearsAttempts() {
        val validator = MapGenerationValidator(maxAttempts = 3)
        validator.evaluate(moveCount = 5, isTrivial = false, minMoves = 17, maxMoves = 99)
        validator.evaluate(moveCount = 5, isTrivial = false, minMoves = 17, maxMoves = 99)
        assertEquals(2, validator.currentAttemptCount())
        validator.reset()
        assertEquals(0, validator.currentAttemptCount())
        assertFailsWith<IllegalArgumentException> { MapGenerationValidator(maxAttempts = 0) }
        assertFailsWith<IllegalArgumentException> { MapGenerationValidator(maxAttempts = -1) }
    }
}
