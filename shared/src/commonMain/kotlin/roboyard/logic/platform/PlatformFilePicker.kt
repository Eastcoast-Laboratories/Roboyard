package roboyard.logic.platform

/**
 * Show a native save-file dialog and write [content] to the chosen file.
 * @param suggestedFileName file name suggested to the user
 * @return true if the file was written, false if cancelled or failed
 */
expect fun saveTextToFileWithDialog(content: String, suggestedFileName: String): Boolean

/**
 * Show a native open-file dialog and return the chosen file's text content.
 * @return file contents, or null if cancelled or unreadable
 */
expect fun loadTextFromFileWithDialog(): String?
