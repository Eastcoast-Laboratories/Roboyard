package roboyard.logic.platform

import java.awt.Desktop
import java.net.URI

/**
 * Desktop implementation: opens the URL in the system browser via java.awt.Desktop.
 */
actual fun openUrl(url: String): Boolean {
    return try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
            true
        } else {
            println("[BROWSER] Desktop browse not supported, URL: $url")
            false
        }
    } catch (e: Exception) {
        println("[BROWSER] Failed to open URL $url: ${e.message}")
        false
    }
}
