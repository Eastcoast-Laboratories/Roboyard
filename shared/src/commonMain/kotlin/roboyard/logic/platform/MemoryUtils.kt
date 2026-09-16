package roboyard.logic.platform

/**
 * Request a garbage collection run.
 * JVM platforms forward to System.gc(); other platforms may ignore it.
 * Used after releasing large solver state to reduce heap pressure.
 */
expect fun requestGc()
