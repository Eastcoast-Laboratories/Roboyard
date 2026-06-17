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
            // Simple string formatting without String.format
            key.formatSimple(*args)
        } catch (e: Exception) {
            key
        }
    }

    private fun String.formatSimple(vararg args: Any?): String {
        var result = this
        args.forEach { arg ->
            result = result.replaceFirst("%d", arg?.toString() ?: "null")
                .replaceFirst("%s", arg?.toString() ?: "null")
                .replaceFirst("%f", arg?.toString() ?: "null")
        }
        return result
    }
}
