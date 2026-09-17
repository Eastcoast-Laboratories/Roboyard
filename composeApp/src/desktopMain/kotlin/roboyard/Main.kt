package roboyard

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.time.OffsetDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import roboyard.ui.compose.App
import roboyard.logic.achievements.AchievementManager
import roboyard.logic.achievements.ApiAchievementSyncClient
import roboyard.logic.core.Constants
import roboyard.logic.core.Preferences
import roboyard.logic.core.ResourceLoader
import roboyard.logic.audio.getSoundManager
import roboyard.logic.managers.SyncHooks
import roboyard.logic.managers.SyncManager
import roboyard.logic.network.DesktopNetworkMonitor
import roboyard.logic.network.RoboyardApiClient
import roboyard.logic.storage.getPlatformStorage

fun main(args: Array<String>) = application {
    // Desktop deep links arrive as a command-line argument
    // (e.g. `run --args="roboyard://map?data=..."`)
    val pendingDeepLink = args.firstOrNull()?.takeIf {
        roboyard.logic.network.DeepLinkHandler.isValidDeepLink(it)
    }
    // Initialize Preferences with desktop storage
    val storage = getPlatformStorage()
    Preferences.storageProvider = { storage }
    Preferences.initialize(storage)

    // Shared API client + sync manager (parity with Android ApiClientProvider)
    val apiClient = RoboyardApiClient.getInstance(
        storage,
        CoroutineScope(Dispatchers.Default),
        { "desktop" }
    )
    AchievementManager.getInstance(storage).syncClient = ApiAchievementSyncClient(apiClient)
    val syncManager = SyncManager.getInstance(
        storage,
        DesktopNetworkMonitor { apiClient.baseUrl },
        apiClient,
        SyncHooks(
            listSaveFileNames = { storage.listFilesInDir(Constants.SAVE_DIRECTORY) },
            readMapAsset = { path ->
                // "level_N.txt" -> bundled level content
                path.removePrefix("level_").removeSuffix(".txt").toIntOrNull()
                    ?.let { ResourceLoader.loadLevelContent(it) }
            },
            parseIsoTimestamp = { iso -> OffsetDateTime.parse(iso).toInstant().toEpochMilli() },
            syncAchievementsToServer = { AchievementManager.getInstance(storage).syncToServer() }
        )
    )
    // Android parity: MainActivity verifies the token and auto-syncs on resume;
    // on desktop this happens once at startup.
    apiClient.verifyToken(object : RoboyardApiClient.ApiCallback<Boolean?> {
        override fun onSuccess(result: Boolean?) {}
        override fun onError(error: String?) {}
    })
    syncManager.syncOnResume()

    // Android parity: MainActivity starts the background music service at startup
    getSoundManager().setBackgroundVolume(Preferences.backgroundSoundVolume)

    val windowState = rememberWindowState(
        placement = if (Preferences.fullscreenEnabled) WindowPlacement.Fullscreen else WindowPlacement.Floating,
        width = 400.dp,
        height = 800.dp
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = "Roboyard",
        state = windowState
    ) {
        // Desktop drag-scroll driver: this runtime does not deliver pointer
        // move events to Compose handlers while a mouse button is held —
        // they arrive coalesced at release. Raw AWT MOUSE_DRAGGED events do
        // arrive continuously, so we apply drag deltas to the ScrollState
        // that the pressed scrollable registered in DesktopDragScroll, and
        // force a synchronous repaint so the content moves visibly per event.
        androidx.compose.runtime.DisposableEffect(Unit) {
            var lastY: Int? = null
            val awtListener = java.awt.event.AWTEventListener { e ->
                if (e !is java.awt.event.MouseEvent) return@AWTEventListener
                when (e.id) {
                    java.awt.event.MouseEvent.MOUSE_PRESSED -> lastY = e.y
                    java.awt.event.MouseEvent.MOUSE_DRAGGED -> {
                        val state = roboyard.ui.compose.DesktopDragScroll.activeState
                        val y = e.y
                        val prev = lastY
                        lastY = y
                        if (state != null && prev != null && y != prev) {
                            val consumed = state.dispatchRawDelta(-(y - prev).toFloat())
                            if (consumed != 0f) {
                                println("[AWT_SCROLL] dy=${y - prev} scroll=${state.value}")
                                (e.component as? javax.swing.JComponent)
                                    ?.paintImmediately(0, 0, e.component.width, e.component.height)
                            }
                        }
                    }
                    java.awt.event.MouseEvent.MOUSE_RELEASED -> {
                        lastY = null
                        roboyard.ui.compose.DesktopDragScroll.activeState = null
                    }
                }
            }
            java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(
                awtListener,
                java.awt.AWTEvent.MOUSE_EVENT_MASK or java.awt.AWTEvent.MOUSE_MOTION_EVENT_MASK
            )
            onDispose {
                java.awt.Toolkit.getDefaultToolkit().removeAWTEventListener(awtListener)
            }
        }
        // Android parity: SoundService pauses background music when the app
        // loses focus (Activity onPause) and resumes on onResume
        androidx.compose.runtime.DisposableEffect(windowState) {
            val listener = object : java.awt.event.WindowFocusListener {
                override fun windowGainedFocus(e: java.awt.event.WindowEvent?) {
                    getSoundManager().resumeBackground()
                }
                override fun windowLostFocus(e: java.awt.event.WindowEvent?) {
                    getSoundManager().pauseBackground()
                }
            }
            window.addWindowFocusListener(listener)
            onDispose { window.removeWindowFocusListener(listener) }
        }
        App(
            onFullscreenChanged = { enabled ->
                windowState.placement = if (enabled) WindowPlacement.Fullscreen else WindowPlacement.Floating
            },
            pendingDeepLink = pendingDeepLink
        )
    }
}
