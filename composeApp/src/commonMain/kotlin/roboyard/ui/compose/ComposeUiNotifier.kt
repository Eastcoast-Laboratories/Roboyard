package roboyard.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow
import roboyard.logic.platform.PlatformInfo
import roboyard.logic.platform.shareText
import roboyard.logic.ui.UiNotifier

/**
 * Compose implementation of UiNotifier. Messages are published to [messages];
 * App.kt renders them as a transient toast-style overlay (Android Toast parity).
 */
class ComposeUiNotifier : UiNotifier {
    private val _messages = MutableStateFlow<String?>(null)
    val messages: StateFlow<String?> = _messages

    override fun showMessage(message: String) {
        _messages.value = message
    }

    fun dismiss() {
        _messages.value = null
    }
}


/**
 * Log viewer dialog — equivalent of Android's Settings "View Error Logs"
 * logcat dialog (last 500 lines via LogBuffer), including its "Share" action
 * for bug reports.
 */
@Composable
fun LogViewerDialog(onDismiss: () -> Unit) {
    val lines = roboyard.logic.util.LogBuffer.getLines()
    val report = buildLogReport(lines)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error Logs (last ${lines.size} lines)") },
        text = {
            Column(modifier = Modifier.desktopVerticalScroll()) {
                if (lines.isEmpty()) {
                    Text("No log entries yet", color = Color.Gray, fontSize = 12.sp)
                } else {
                    SelectionContainer {
                        Text(text = report, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { shareText("Roboyard Error Logs", report) }) { Text("Share") }
                TextButton(onClick = {
                    roboyard.logic.util.LogBuffer.clear()
                    onDismiss()
                }) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

/** Log lines prefixed with app/OS version so shared reports identify the build. */
private fun buildLogReport(lines: List<String>): String {
    val header = "Roboyard ${PlatformInfo.getAppVersionName()} (${PlatformInfo.getAppVersionCode()}) " +
        "on ${PlatformInfo.getOsVersionString()}"
    return header + "\n\n" + lines.joinToString("\n")
}
