package driftingdroids.model

/**
 * Dummy L10n class that just returns the input string.
 * Used as a placeholder for proper localization.
 */
object L10N {
    fun getString(key: String): String {
        return key
    }

    fun getString(key: String, vararg args: Any): String {
        return try {
            String.format(key, *args)
        } catch (e: Exception) {
            key
        }
    }
}
