package roboyard.logic.achievements

/**
 * Provides streak data for achievement server sync.
 * Decouples AchievementManager from StreakManager to avoid circular dependencies.
 * StreakManager implements this interface; the platform factory wires it up.
 */
interface StreakDataProvider {
    val currentStreak: Int
    val lastLoginDateString: String?
    val longestStreak: Int
    val longestStreakDate: String?

    /**
     * Restore streak data from server (bidirectional sync).
     */
    fun restoreFromServer(
        serverStreak: Int,
        serverLastDate: String?,
        serverLongestStreak: Int,
        serverLongestStreakDate: String?
    )
}
