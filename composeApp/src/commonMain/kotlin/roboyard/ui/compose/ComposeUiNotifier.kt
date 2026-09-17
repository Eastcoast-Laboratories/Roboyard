package roboyard.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
 * Log viewer dialog — desktop equivalent of Android's logcat viewer
 * (Settings "View Logs" button, last 500 lines via LogBuffer).
 */
@Composable
fun LogViewerDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Logs (last ${roboyard.logic.util.LogBuffer.getLines().size} lines)") },
        text = {
            Column(modifier = Modifier.desktopVerticalScroll()) {
                val lines = roboyard.logic.util.LogBuffer.getLines()
                if (lines.isEmpty()) {
                    Text("No log entries yet", color = Color.Gray, fontSize = 12.sp)
                } else {
                    Text(
                        text = lines.joinToString("\n"),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    roboyard.logic.util.LogBuffer.clear()
                    onDismiss()
                }) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}
