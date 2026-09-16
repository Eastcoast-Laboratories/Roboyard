package roboyard.logic.core

enum class MapGenerationRejectionReason {
    NO_SOLUTION,
    TRIVIAL,
    TOO_EASY,
    TOO_HARD
}

data class MapGenerationDecision(
    val accepted: Boolean,
    val shouldRetry: Boolean,
    val usedFallback: Boolean,
    val attempt: Int,
    val moveCount: Int?,
    val rejectionReason: MapGenerationRejectionReason?
)

class MapGenerationValidator(
    private val maxAttempts: Int = MAX_GENERATION_ATTEMPTS
) {
    private var attempts = 0

    init {
        require(maxAttempts > 0) { "maxAttempts must be positive" }
    }

    fun evaluate(
        moveCount: Int?,
        isTrivial: Boolean,
        minMoves: Int,
        maxMoves: Int
    ): MapGenerationDecision {
        val reason = when {
            moveCount == null || moveCount <= 0 -> MapGenerationRejectionReason.NO_SOLUTION
            isTrivial -> MapGenerationRejectionReason.TRIVIAL
            moveCount < minMoves -> MapGenerationRejectionReason.TOO_EASY
            moveCount > maxMoves -> MapGenerationRejectionReason.TOO_HARD
            else -> null
        }

        if (reason == null) {
            val completedAttempt = attempts + 1
            attempts = 0
            return MapGenerationDecision(
                accepted = true,
                shouldRetry = false,
                usedFallback = false,
                attempt = completedAttempt,
                moveCount = moveCount,
                rejectionReason = null
            )
        }

        attempts++
        if (attempts >= maxAttempts) {
            val completedAttempt = attempts
            attempts = 0
            return MapGenerationDecision(
                accepted = true,
                shouldRetry = false,
                usedFallback = true,
                attempt = completedAttempt,
                moveCount = moveCount,
                rejectionReason = reason
            )
        }
        return MapGenerationDecision(
            accepted = false,
            shouldRetry = true,
            usedFallback = false,
            attempt = attempts,
            moveCount = moveCount,
            rejectionReason = reason
        )
    }

    fun reset() {
        attempts = 0
    }

    fun currentAttemptCount(): Int = attempts

    companion object {
        const val MAX_GENERATION_ATTEMPTS = 1000
    }
}
