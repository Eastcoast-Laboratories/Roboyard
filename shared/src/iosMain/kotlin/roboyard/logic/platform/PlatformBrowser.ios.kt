package roboyard.logic.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * iOS implementation: opens the URL via UIApplication.
 */
actual fun openUrl(url: String): Boolean {
    val nsUrl = NSURL.URLWithString(url) ?: return false
    val app = UIApplication.sharedApplication
    return if (app.canOpenURL(nsUrl)) {
        app.openURL(nsUrl)
        true
    } else {
        false
    }
}
