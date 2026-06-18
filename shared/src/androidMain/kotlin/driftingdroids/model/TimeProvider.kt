package driftingdroids.model

/**
 * Android-specific implementation for time provider.
 */
actual object TimeProvider {
    actual fun currentTimeMillis(): Long = System.currentTimeMillis()
    actual fun nanoTime(): Long = System.nanoTime()
    actual fun getRuntimeMemoryInfo(): RuntimeMemoryInfo {
        val runtime = Runtime.getRuntime()
        return RuntimeMemoryInfo(
            freeMemory = runtime.freeMemory(),
            totalMemory = runtime.totalMemory(),
            maxMemory = runtime.maxMemory()
        )
    }
}
