package driftingdroids.model

/**
 * Platform-specific time provider for solver timing.
 * Expect/actual implementation for different platforms.
 */
expect object TimeProvider {
    fun currentTimeMillis(): Long
    fun nanoTime(): Long
    fun getRuntimeMemoryInfo(): RuntimeMemoryInfo
}

data class RuntimeMemoryInfo(
    val freeMemory: Long,
    val totalMemory: Long,
    val maxMemory: Long
)
