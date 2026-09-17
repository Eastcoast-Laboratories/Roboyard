package roboyard.logic.platform

import android.content.Intent
import android.net.Uri

/**
 * Android implementation: opens the URL via ACTION_VIEW intent.
 * Uses the application context provided by the app module.
 */
actual fun openUrl(url: String): Boolean {
    val context = appContextProvider?.invoke() ?: return false
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }
}

/** Set by the app module (RoboyardApplication) so shared code can launch intents. */
var appContextProvider: (() -> android.content.Context)? = null

/**
 * Android implementation: browser intents can only issue GET requests, so the
 * pre-built GET auto-login URL is opened directly (unchanged behavior).
 */
actual fun openAutoLoginUrl(autoLoginUrl: String): Boolean = openUrl(autoLoginUrl)
