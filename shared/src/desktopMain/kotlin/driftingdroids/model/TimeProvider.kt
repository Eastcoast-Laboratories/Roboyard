package driftingdroids.model

/**
 * Desktop-specific implementation for time provider.
 */
actual object TimeProvider {
    actual fun currentTimeMillis(): Long = System.currentTimeMillis()
    actual fun nanoTime(): Long = System.nanoTime()
}
