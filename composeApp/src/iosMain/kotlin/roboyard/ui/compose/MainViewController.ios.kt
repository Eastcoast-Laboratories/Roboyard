package roboyard.ui.compose

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIAccessibilityIsVoiceOverRunning
import platform.UIKit.UIViewController
import roboyard.logic.platform.IosNetworkMonitor
import roboyard.logic.storage.getPlatformStorage
import roboyard.logic.util.DateUtils

/**
 * iOS entry point: the SwiftUI shell calls this to host the shared Compose UI.
 * App services (Preferences, API, sync) are wired before the first game is
 * created, as on desktop and Android.
 */
fun MainViewController(): UIViewController {
    initAppServices(
        storage = getPlatformStorage(),
        installSource = "ios",
        networkMonitorFactory = { IosNetworkMonitor() },
        parseIsoTimestamp = { iso -> DateUtils.parseTimestampIso(iso) },
        accessibilityActive = UIAccessibilityIsVoiceOverRunning()
    )
    return ComposeUIViewController {
        App()
    }
}
