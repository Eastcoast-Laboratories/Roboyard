package roboyard.logic.network

import driftingdroids.model.Board

/**
 * Desktop implementation of DeepLinkHandler.
 * Deep links are not supported on Desktop, so this returns null.
 */
class DesktopDeepLinkHandler : DeepLinkHandler {
    override fun parseDeepLink(url: String): Board? {
        // Deep links are not supported on Desktop
        return null
    }
    
    override fun isValidDeepLink(url: String): Boolean {
        // Deep links are not supported on Desktop
        return false
    }
}

actual fun getDeepLinkHandler(): DeepLinkHandler {
    return DesktopDeepLinkHandler()
}
