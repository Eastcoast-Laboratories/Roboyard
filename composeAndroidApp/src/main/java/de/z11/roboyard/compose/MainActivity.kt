package de.z11.roboyard.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import roboyard.logic.managers.GameSession
import roboyard.logic.storage.getPlatformStorage
import roboyard.ui.compose.App

/**
 * Hosts the Compose Multiplatform UI from :composeApp.
 */
class MainActivity : ComponentActivity() {

    private lateinit var session: GameSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = GameSession(getPlatformStorage(), lifecycleScope)
        setContent { App(externalSession = session) }
    }

    // Android app parity: the game timer pauses in onPause and resumes in onResume
    override fun onPause() {
        super.onPause()
        session.pauseTimer()
    }

    override fun onResume() {
        super.onResume()
        session.resumeTimer()
    }
}
