package roboyard.logic.achievements

import roboyard.logic.ui.StringProvider

/**
 * Categories for achievements.
 * Central definition of category names, display order, and string resource keys.
 * 
 * Display names are loaded from strings.xml using the stringResName.
 * Example: PROGRESSION uses "achievement_category_progression" from strings.xml
 */
enum class AchievementCategory(stringResName: String, displayOrder: Int) {
    // Display order is determined by the order of enum values
    PROGRESSION("achievement_category_progression", 0),
    PERFORMANCE("achievement_category_performance", 1),
    MASTERY("achievement_category_mastery", 2),
    RANDOM_SPEED("achievement_category_random_speed", 3),
    RANDOM_STREAKS("achievement_category_random_streaks", 4),
    RANDOM_DIFFICULTY("achievement_category_random_difficulty", 5),
    RANDOM_SOLUTION("achievement_category_random_solution", 6),
    RANDOM_RESOLUTION("achievement_category_random_resolution", 7),
    RANDOM_TARGETS("achievement_category_random_targets", 8),
    RANDOM_ROBOTS("achievement_category_random_robots", 9),
    RANDOM_COVERAGE("achievement_category_random_coverage", 10),
    SPECIAL("achievement_login_streak", 11),
    CHALLENGE("achievement_category_challenge", 99); // Disabled, high order to sort last

    /**
     * Get the string resource name for this category.
     * Use with context.getResources().getIdentifier() to get the actual string.
     */
    @JvmField
    val stringResName: String?

    /**
     * Get the display order for sorting categories.
     */
    @JvmField
    val displayOrder: Int

    init {
        this.stringResName = stringResName
        this.displayOrder = displayOrder
    }

    /**
     * Get the localized display name for this category.
     * @param stringResolver Function to resolve string resource by name (e.g., Android context)
     * @return The localized category name from strings.xml
     */
    fun getDisplayName(stringResolver: (String) -> String?): String {
        val resolved = stringResolver(stringResName!!)
        return resolved ?: name.replace("_", " ")
    }

    val isEnabled: Boolean
        /**
         * Check if this category is enabled (has achievements).
         */
        get() = this != AchievementCategory.CHALLENGE // CHALLENGE is disabled
}
