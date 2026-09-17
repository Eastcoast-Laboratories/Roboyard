package roboyard.logic.platform

/**
 * iOS file dialogs are not implemented yet — iOS export/import will use the
 * system document picker when the iOS UI is built.
 */
actual fun saveTextToFileWithDialog(content: String, suggestedFileName: String): Boolean {
    println("[FILE_PICKER] saveTextToFileWithDialog is not supported on iOS yet")
    return false
}

actual fun loadTextFromFileWithDialog(): String? {
    println("[FILE_PICKER] loadTextFromFileWithDialog is not supported on iOS yet")
    return null
}
