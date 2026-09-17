package roboyard.logic.achievements

import com.google.gson.JsonParser
import roboyard.logic.network.RoboyardApiClient

/**
 * AchievementSyncClient backed by the shared RoboyardApiClient.
 * Used on Android and Desktop — identical behavior on both platforms.
 */
class ApiAchievementSyncClient(private val apiClient: RoboyardApiClient) : AchievementSyncClient {

    override val isLoggedIn: Boolean
        get() = apiClient.isLoggedIn

    override fun syncAchievements(
        achievementsJson: String,
        statsJson: String,
        callback: AchievementSyncCallback
    ) {
        val achievements = JsonParser.parseString(achievementsJson).asJsonArray
        val stats = JsonParser.parseString(statsJson).asJsonObject
        apiClient.syncAchievements(
            achievements,
            stats,
            object : RoboyardApiClient.ApiCallback<RoboyardApiClient.AchievementSyncResult?> {
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
        apiClient.fetchAchievements(object :
            RoboyardApiClient.ApiCallback<RoboyardApiClient.AchievementFetchResult?> {
            override fun onSuccess(result: RoboyardApiClient.AchievementFetchResult?) {
                callback.onSuccess(result?.achievements?.toString() ?: "[]", result?.stats?.toString())
            }

            override fun onError(error: String?) {
                callback.onError(error)
            }
        })
    }
}
