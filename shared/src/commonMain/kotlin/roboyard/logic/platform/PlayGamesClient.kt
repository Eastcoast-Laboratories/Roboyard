package roboyard.logic.platform

/**
 * Platform-agnostic interface for Google Play Games Services integration.
 * Implementations are provided by the platform layer (Android only; no-op on iOS/Desktop).
 */
interface PlayGamesClient {
    /**
     * Unlock an achievement by its local achievement ID.
     * No-op if Play Games is not enabled or user is not signed in.
     * @param achievementId The local achievement ID (e.g., "first_game")
     */
    fun unlockAchievement(achievementId: String)
}
