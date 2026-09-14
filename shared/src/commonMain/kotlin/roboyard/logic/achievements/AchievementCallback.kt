package roboyard.logic.achievements

/**
 * Callback interface for achievement notifications from StreakManager.
 * Decouples StreakManager from AchievementManager so both can live in shared.
 */
interface AchievementCallback {
    fun onComebackPlayer(daysAway: Int)
    fun onDailyLogin(currentStreak: Int)
    fun updateDailyLoginStreak(maxStreak: Int)
}
