package roboyard.platform

import android.app.Activity
import android.content.Context
import roboyard.eclabs.BuildConfig
import roboyard.logic.achievements.AchievementManager
import timber.log.Timber

/**
 * Manager for Google Play Games Services integration.
 * Handles authentication and achievement syncing.
 *
 * This class is guarded by BuildConfig.ENABLE_PLAY_GAMES flag.
 * F-Droid flavor version - GMS disabled, all methods are no-ops.
 */
class PlayGamesManager private constructor(context: Context) {

    private val context: Context = context.applicationContext
    private var isInitialized = false
    private var isSignedIn = false

    /**
     * Initialize Play Games SDK. Call this in Application.onCreate() or MainActivity.onCreate().
     * No-op for F-Droid flavor.
     */
    fun initialize() {
        if (!BuildConfig.ENABLE_PLAY_GAMES) {
            Timber.d("%s Play Games disabled in this build", TAG)
            return
        }

        if (isInitialized) {
            Timber.d("%s Already initialized", TAG)
            return
        }

        try {
            // GMS not available in F-Droid flavor
            isInitialized = true
            Timber.d("%s SDK initialized successfully", TAG)
        } catch (e: Exception) {
            Timber.e(e, "%s Failed to initialize SDK", TAG)
        }
    }

    /**
     * Unlock an achievement by its local ID.
     * Maps local achievement IDs to Google Play Games achievement IDs.
     * No-op for F-Droid flavor.
     *
     * @param activity The current activity
     * @param localAchievementId The local achievement ID (e.g., "first_game")
     */
    fun unlockAchievement(activity: Activity, localAchievementId: String) {
        if (!BuildConfig.ENABLE_PLAY_GAMES || !isInitialized || !isSignedIn) {
            Timber.d(
                "%s Cannot unlock achievement %s - not ready (enabled=%b, init=%b, signedIn=%b)",
                TAG,
                localAchievementId,
                BuildConfig.ENABLE_PLAY_GAMES,
                isInitialized,
                isSignedIn
            )
            return
        }

        val playGamesId = AchievementManager.getInstance(context)
            .getPlayGamesAchievementId(localAchievementId)
        if (playGamesId == null || playGamesId.startsWith("REPLACE_")) {
            Timber.w("%s Achievement ID not configured for: %s", TAG, localAchievementId)
            return
        }

        try {
            // GMS not available in F-Droid flavor - no-op
            Timber.d("%s Unlocked achievement: %s -> %s", TAG, localAchievementId, playGamesId)
        } catch (e: Exception) {
            Timber.e(e, "%s Failed to unlock achievement: %s", TAG, localAchievementId)
        }
    }

    companion object {
        private const val TAG = "[PLAY_GAMES]"

        @Volatile
        private var instance: PlayGamesManager? = null

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): PlayGamesManager {
            return instance ?: PlayGamesManager(context).also { instance = it }
        }
    }
}
