package roboyard.logic.achievements

/**
 * Platform-agnostic interface for syncing achievements to a server.
 * Implementations are provided by the platform layer (Android, iOS, Desktop).
 */
interface AchievementSyncClient {
    /**
     * Check if the user is logged in to the server.
     */
    val isLoggedIn: Boolean

    /**
     * Sync achievements to the server.
     * @param achievementsJson JSON array of achievement data
     * @param statsJson JSON object of stats data
     * @param callback Callback for sync result
     */
    fun syncAchievements(achievementsJson: String, statsJson: String, callback: AchievementSyncCallback)

    /**
     * Fetch achievements from the server.
     * @param callback Callback for fetch result
     */
    fun fetchAchievements(callback: AchievementFetchCallback)
}

/**
 * Callback for achievement sync operations.
 */
interface AchievementSyncCallback {
    fun onSuccess(syncedCount: Int, newAchievements: Int, latestAppVersion: String?)
    fun onError(error: String?)
}

/**
 * Callback for achievement fetch operations.
 */
interface AchievementFetchCallback {
    /**
     * @param achievementsJson JSON array of achievements from server
     * @param statsJson JSON object of stats from server (may be null)
     */
    fun onSuccess(achievementsJson: String, statsJson: String?)
    fun onError(error: String?)
}
