package roboyard.logic.platform

import android.content.Intent

/**
 * Android implementation: ACTION_SEND chooser, same as the SettingsFragment
 * "Share" button of the log dialog.
 */
actual fun shareText(subject: String, text: String): Boolean {
    val context = appContextProvider?.invoke() ?: run {
        println("[SHARE] appContextProvider not set — cannot share")
        return false
    }
    return try {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(send, subject)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
        true
    } catch (e: Exception) {
        println("[SHARE] Failed to share: ${e.message}")
        false
    }
}
