package roboyard.ui.compose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import roboyard.logic.achievements.AchievementManager
import roboyard.logic.achievements.ApiAchievementSyncClient
import roboyard.logic.audio.getSoundManager
import roboyard.logic.core.Constants
import roboyard.logic.core.Preferences
import roboyard.logic.core.ResourceLoader
import roboyard.logic.managers.SyncHooks
import roboyard.logic.managers.SyncManager
import roboyard.logic.network.NetworkMonitor
import roboyard.logic.network.RoboyardApiClient
import roboyard.logic.storage.PlatformStorage
import roboyard.logic.util.RLog

private val log = RLog.tag("AppServices")

/**
 * Platform-independent app startup wiring shared by the desktop and iOS entry
 * points (Android parity: RoboyardApplication + MainActivity). Must run before
 * the first GameSession/GameState is created — Preferences supplies robot
 * count, board size and every other setting to the game logic.
 *
 * @param installSource reported to the API ("desktop", "ios", ...)
 * @param networkMonitorFactory builds the platform monitor; receives the API
 *        base URL provider
 * @param parseIsoTimestamp server ISO-8601 timestamp to epoch millis
 * @param accessibilityActive screen reader running — fresh installs then get
 *        accessibility-friendly defaults
 */
fun initAppServices(
    storage: PlatformStorage,
    installSource: String,
    networkMonitorFactory: (baseUrl: () -> String) -> NetworkMonitor,
    parseIsoTimestamp: (String) -> Long,
    accessibilityActive: Boolean = false
) {
    Preferences.storageProvider = { storage }
    Preferences.initialize(storage, accessibilityActive)
    log.d("[APP_INIT] Preferences initialized (%s): robotCount=%d, board=%dx%d",
        installSource, Preferences.robotCount, Preferences.boardSizeWidth, Preferences.boardSizeHeight)

    // Shared API client + sync manager (parity with Android ApiClientProvider)
    val apiClient = RoboyardApiClient.getInstance(storage, CoroutineScope(Dispatchers.Default), { installSource })
    AchievementManager.getInstance(storage).syncClient = ApiAchievementSyncClient(apiClient)
    val syncManager = SyncManager.getInstance(
        storage,
        networkMonitorFactory { apiClient.baseUrl },
        apiClient,
        SyncHooks(
            listSaveFileNames = { storage.listFilesInDir(Constants.SAVE_DIRECTORY) },
            readMapAsset = { path ->
                // "level_N.txt" -> bundled level content
                path.removePrefix("level_").removeSuffix(".txt").toIntOrNull()
                    ?.let { ResourceLoader.loadLevelContent(it) }
            },
            parseIsoTimestamp = parseIsoTimestamp,
            syncAchievementsToServer = { AchievementManager.getInstance(storage).syncToServer() }
        )
    )
    // Android parity: MainActivity verifies the token and auto-syncs on resume;
    // desktop and iOS do this once at startup.
    apiClient.verifyToken(object : RoboyardApiClient.ApiCallback<Boolean?> {
        override fun onSuccess(result: Boolean?) {}
        override fun onError(error: String?) {}
    })
    syncManager.syncOnResume()

    // Android parity: MainActivity starts the background music at startup
    getSoundManager().setBackgroundVolume(Preferences.backgroundSoundVolume)
}
