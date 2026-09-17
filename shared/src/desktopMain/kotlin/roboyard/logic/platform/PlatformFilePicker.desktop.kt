package roboyard.logic.platform

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * Desktop implementation using AWT FileDialog (native OS dialog).
 * Blocks the calling thread — call from a background coroutine.
 */
actual fun saveTextToFileWithDialog(content: String, suggestedFileName: String): Boolean {
    val dialog = FileDialog(null as Frame?, "Save File", FileDialog.SAVE)
    dialog.file = suggestedFileName
    dialog.isVisible = true
    val dir = dialog.directory ?: return false
    val file = dialog.file ?: return false
    return try {
        File(dir, file).writeText(content)
        true
    } catch (e: Exception) {
        System.err.println("[FILE_PICKER] save failed: ${e.message}")
        false
    }
}

actual fun loadTextFromFileWithDialog(): String? {
    val dialog = FileDialog(null as Frame?, "Open File", FileDialog.LOAD)
    dialog.isVisible = true
    val dir = dialog.directory ?: return null
    val file = dialog.file ?: return null
    return try {
        File(dir, file).readText()
    } catch (e: Exception) {
        System.err.println("[FILE_PICKER] load failed: ${e.message}")
        null
    }
}
