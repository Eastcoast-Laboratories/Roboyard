package roboyard.ui.compose

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
