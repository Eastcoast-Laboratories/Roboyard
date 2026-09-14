package roboyard.logic.achievements

import android.app.Activity
import android.content.Context
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import roboyard.logic.network.RoboyardApiClient
import roboyard.platform.AndroidStorage
import roboyard.logic.platform.PlayGamesClient
import roboyard.logic.ui.StringProvider
import roboyard.logic.ui.UiNotifier

/**
 * Android-specific factory for AchievementManager.
 * Provides Context-based factory methods and wires Android-specific dependencies.
 */
object AchievementManagerFactory {
    private var currentInstance: AchievementManager? = null
    private var playGamesClient: AndroidPlayGamesClient? = null

    @JvmStatic
    fun getInstance(context: Context): AchievementManager {
        if (currentInstance == null) {
            val storage = AndroidStorage.getInstance(context)
            val stringProvider = AndroidStringProvider(context)
            currentInstance = AchievementManager.getInstance(storage, stringProvider, null)
            // Wire up Android-specific clients
            currentInstance!!.syncClient = AndroidAchievementSyncClient(context)
            playGamesClient = AndroidPlayGamesClient(context)
            currentInstance!!.playGamesClient = playGamesClient
            currentInstance!!.streakDataProvider = StreakManagerFactory.getInstance(context)
        }
        return currentInstance!!
    }

    @JvmStatic
    fun setCurrentActivity(activity: Activity) {
        // Create Android UiNotifier and set it on the AchievementManager
        val notifier = AndroidUiNotifier(activity)
        getInstance(activity).setUiNotifier(notifier)
        // Pass the activity to the Play Games client for achievement unlocking
        playGamesClient?.setCurrentActivity(activity)
    }

    @JvmStatic
    fun clearCurrentActivity() {
        currentInstance?.setUiNotifier(null)
        playGamesClient?.setCurrentActivity(null)
    }
}

/**
 * Android StringProvider implementation using Context.getString().
 */
class AndroidStringProvider(private val context: Context) : StringProvider {
    override fun getString(name: String): String? {
        return try {
            val resId = context.resources.getIdentifier(name, "string", context.packageName)
            if (resId != 0) context.getString(resId) else null
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Android UiNotifier implementation using Toast.
 */
class AndroidUiNotifier(private val activity: Activity) : UiNotifier {
    override fun showMessage(message: String) {
        activity.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun showLongMessage(message: String) {
        activity.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        }
    }
}

/**
 * Android implementation of AchievementSyncClient using RoboyardApiClient.
 */
private class AndroidAchievementSyncClient(private val context: Context) : AchievementSyncClient {
    override val isLoggedIn: Boolean
        get() = RoboyardApiClient.getInstance(context).isLoggedIn

    override fun syncAchievements(
        achievementsJson: String,
        statsJson: String,
        callback: AchievementSyncCallback
    ) {
        val apiClient = RoboyardApiClient.getInstance(context)
        val achievements = JSONArray(achievementsJson)
        val stats = JSONObject(statsJson)
        apiClient.syncAchievements(achievements, stats, object : RoboyardApiClient.ApiCallback<RoboyardApiClient.AchievementSyncResult?> {
            override fun onSuccess(result: RoboyardApiClient.AchievementSyncResult?) {
                callback.onSuccess(
                    result?.syncedCount ?: 0,
                    result?.newAchievements ?: 0,
                    result?.latestAppVersion
                )
            }

            override fun onError(error: String?) {
                callback.onError(error)
            }
        })
    }

    override fun fetchAchievements(callback: AchievementFetchCallback) {
        val apiClient = RoboyardApiClient.getInstance(context)
        apiClient.fetchAchievements(object : RoboyardApiClient.ApiCallback<RoboyardApiClient.AchievementFetchResult?> {
            override fun onSuccess(result: RoboyardApiClient.AchievementFetchResult?) {
                val achievementsJson = result?.achievements?.toString() ?: "[]"
                val statsJson = result?.stats?.toString()
                callback.onSuccess(achievementsJson, statsJson)
            }

            override fun onError(error: String?) {
                callback.onError(error)
            }
        })
    }
}

/**
 * Android implementation of PlayGamesClient using PlayGamesManager.
 * Stores the current Activity reference needed by PlayGamesManager.unlockAchievement().
 */
private class AndroidPlayGamesClient(private val context: Context) : PlayGamesClient {
    private var currentActivity: Activity? = null

    fun setCurrentActivity(activity: Activity?) {
        this.currentActivity = activity
    }

    override fun unlockAchievement(achievementId: String) {
        val activity = currentActivity ?: return
        try {
            val playGames = roboyard.platform.PlayGamesManager.getInstance(context)
            playGames.unlockAchievement(activity, achievementId)
        } catch (e: Exception) {
            // Play Games not available or not initialized - no-op
        }
    }
}
