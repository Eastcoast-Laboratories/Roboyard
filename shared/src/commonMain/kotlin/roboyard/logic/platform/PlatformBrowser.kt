package roboyard.logic.platform

/**
 * Open a URL in the system browser.
 * @return true if the URL was handed off to a browser, false otherwise
 */
expect fun openUrl(url: String): Boolean

/**
 * Open an auto-login URL. The server endpoint is POST-only (token must not
 * appear in URLs/logs), so platforms that can render a local auto-submitting
 * POST form (desktop) do that; platforms limited to GET intents (Android, iOS)
 * fall back to opening the GET URL directly.
 */
expect fun openAutoLoginUrl(autoLoginUrl: String): Boolean

/**
 * Build a minimal HTML page that auto-submits a POST form to [actionUrl]
 * carrying [params] as hidden inputs. Shared so all platforms use the same
 * markup and it can be unit-tested.
 */
fun buildAutoSubmitPostFormHtml(actionUrl: String, params: Map<String, String>): String {
    val inputs = params.entries.joinToString("") { (k, v) ->
        "<input type=\"hidden\" name=\"${k.htmlEscape()}\" value=\"${v.htmlEscape()}\">"
    }
    return """<!DOCTYPE html><html><body onload="document.forms[0].submit()">
<form method="POST" action="${actionUrl.htmlEscape()}">$inputs</form>
<noscript><p>JavaScript required — <button type="submit" formmethod="post" formaction="${actionUrl.htmlEscape()}">Continue</button></p></noscript>
</body></html>"""
}

private fun String.htmlEscape(): String =
    replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&#39;")
