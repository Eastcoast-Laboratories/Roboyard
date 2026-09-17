package roboyard.logic.platform

/**
 * Android does not use modal file dialogs — data export uses share intents and
 * import uses document providers. These functions exist to satisfy the
 * multiplatform contract and report unsupported usage loudly.
 */
actual fun saveTextToFileWithDialog(content: String, suggestedFileName: String): Boolean {
    println("[FILE_PICKER] saveTextToFileWithDialog is not supported on Android")
    return false
}

actual fun loadTextFromFileWithDialog(): String? {
    println("[FILE_PICKER] loadTextFromFileWithDialog is not supported on Android")
    return null
}
