package roboyard.ui.compose

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * iOS entry point: the SwiftUI shell calls this to host the shared Compose UI.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    App()
}
