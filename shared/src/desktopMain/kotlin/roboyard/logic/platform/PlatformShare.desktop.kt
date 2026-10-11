package roboyard.logic.platform

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * Desktop implementation: there is no system share sheet, so the text is put
 * on the clipboard for pasting into a mail or issue.
 */
actual fun shareText(subject: String, text: String): Boolean {
    return try {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        println("[SHARE] '$subject' copied to clipboard (${text.length} chars)")
        true
    } catch (e: Exception) {
        println("[SHARE] Failed to copy to clipboard: ${e.message}")
        false
    }
}
