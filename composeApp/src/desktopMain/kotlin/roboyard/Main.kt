package roboyard

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import roboyard.ui.compose.App
import roboyard.logic.core.Preferences
import roboyard.logic.storage.getPlatformStorage

fun main() = application {
    // Initialize Preferences with desktop storage
    Preferences.storageProvider = { getPlatformStorage() }
    Preferences.initialize(getPlatformStorage())

    Window(
        onCloseRequest = ::exitApplication,
        title = "Roboyard",
        state = rememberWindowState(width = 400.dp, height = 800.dp)
    ) {
        App()
    }
}
