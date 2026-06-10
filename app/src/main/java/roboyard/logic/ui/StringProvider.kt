package roboyard.logic.ui

/**
 * Platform-agnostic interface for accessing string resources
 * Implementations are provided by the UI layer (Android, iOS, Desktop)
 */
fun interface StringProvider {
    /**
     * Get a string by resource name
     * @param name The resource name (e.g., "achievement_category_progress")
     * @return The localized string, or null if not found
     */
    fun getString(name: String): String?
    
    /**
     * Get a string with format arguments
     * @param name The resource name
     * @param formatArgs Format arguments
     * @return The formatted localized string, or null if not found
     */
    fun getString(name: String, vararg formatArgs: Any): String? {
        val base = getString(name) ?: return null
        return try {
            String.format(base, *formatArgs)
        } catch (e: Exception) {
            base
        }
    }
}
