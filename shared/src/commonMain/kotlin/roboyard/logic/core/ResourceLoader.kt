package roboyard.logic.core

/**
 * Platform-specific resource loader for level files.
 * Expect/actual implementation for different platforms.
 */
expect object ResourceLoader {
    fun loadLevelContent(levelId: Int): String?
}
