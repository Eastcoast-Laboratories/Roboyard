package roboyard

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import roboyard.ui.compose.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Roboyard",
        state = rememberWindowState(width = 400.dp, height = 700.dp)
    ) {
        App()
    }
}
