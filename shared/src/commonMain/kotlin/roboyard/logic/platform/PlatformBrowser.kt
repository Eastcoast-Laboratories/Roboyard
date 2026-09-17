package roboyard.logic.platform

/**
 * Open a URL in the system browser.
 * @return true if the URL was handed off to a browser, false otherwise
 */
expect fun openUrl(url: String): Boolean
