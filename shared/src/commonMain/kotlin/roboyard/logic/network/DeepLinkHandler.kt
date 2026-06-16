package roboyard.logic.network

import driftingdroids.model.Board

/**
 * Interface for handling deep links in a platform-agnostic way.
 * Deep links allow loading maps from external sources (e.g., roboyard.z11.de).
 */
interface DeepLinkHandler {
    /**
     * Parse a deep link URL and return a Board if valid.
     * @param url The deep link URL (e.g., "roboyard://random?data=...")
     * @return Board if parsing successful, null otherwise
     */
    fun parseDeepLink(url: String): Board?
    
    /**
     * Check if a URL is a valid deep link for this app.
     * @param url The URL to check
     * @return true if valid deep link, false otherwise
     */
    fun isValidDeepLink(url: String): Boolean
}

/**
 * Factory function to get the platform-specific deep link handler.
 */
expect fun getDeepLinkHandler(): DeepLinkHandler
