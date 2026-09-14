package roboyard.logic.achievements

import android.content.Context
import roboyard.platform.AndroidStorage

/**
 * Android-specific factory for StreakManager.
 * Provides a Context-based factory method for Java callers.
 */
object StreakManagerFactory {
    @JvmStatic
    fun getInstance(context: Context): StreakManager {
        return StreakManager.getInstance(
            AndroidStorage.getInstance(context),
            AchievementManagerFactory.getInstance(context)
        )
    }
}
