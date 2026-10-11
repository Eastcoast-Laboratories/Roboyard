package driftingdroids.model

/**
 * Process-wide heap guard for solver allocations.
 *
 * Several solver instances can run concurrently (map validation, live move
 * counter, next-move pre-computation) and each KnownStates trie enforces only
 * a per-instance limit. This guard checks the real remaining heap headroom
 * before every chunk allocation, so the combined memory of all solvers plus
 * the rest of the app cannot consume the whole heap — an OutOfMemoryError on
 * any thread, including system-injected ones, kills the process.
 */
object SolverHeapGuard {

    /** Denominator of the heap fraction reserved for the rest of the app (25%). */
    private const val MIN_FREE_DIVISOR = 4L

    /**
     * Bytes that can still be committed before the heap hits its growth limit
     * (Runtime-style accounting: maxMemory - totalMemory + freeMemory).
     */
    fun freeBytes(): Long {
        val mem = TimeProvider.getRuntimeMemoryInfo()
        return mem.maxMemory - mem.totalMemory + mem.freeMemory
    }

    /**
     * True while enough headroom remains for new solver chunk allocations.
     * Called per chunk (256KB node / 64KB leaf), so concurrent solver
     * instances cannot race each other over the reserve by more than
     * a single chunk each.
     */
    fun hasHeadroom(): Boolean {
        val mem = TimeProvider.getRuntimeMemoryInfo()
        val free = mem.maxMemory - mem.totalMemory + mem.freeMemory
        return free >= mem.maxMemory / MIN_FREE_DIVISOR
    }
}
