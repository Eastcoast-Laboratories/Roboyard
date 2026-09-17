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

/**
 * Desktop implementation: the server's /auto-login endpoint is POST-only, so
 * the token is moved into an auto-submitting POST form written to a temp HTML
 * file which is then opened in the browser.
 */
actual fun openAutoLoginUrl(autoLoginUrl: String): Boolean {
    return try {
        val query = autoLoginUrl.substringAfter('?', "")
        val params = query.split('&').mapNotNull {
            val idx = it.indexOf('=')
            if (idx < 0) null else it.substring(0, idx) to
                java.net.URLDecoder.decode(it.substring(idx + 1), "UTF-8")
        }.toMap()
        val token = params["token"] ?: return openUrl(autoLoginUrl)
        val base = autoLoginUrl.substringBefore('?')
        val redirect = params["redirect"]
        val action = if (redirect != null)
            "$base?redirect=" + java.net.URLEncoder.encode(redirect, "UTF-8") else base

        val file = java.io.File.createTempFile("roboyard-autologin", ".html")
        file.writeText(buildAutoSubmitPostFormHtml(action, mapOf("token" to token)))
        file.deleteOnExit()
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(file.toURI())
            true
        } else {
            println("[BROWSER] Desktop browse not supported for auto-login")
            false
        }
    } catch (e: Exception) {
        println("[BROWSER] Failed to open auto-login URL: ${e.message}")
        false
    }
}
