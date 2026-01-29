package roboyard.eclabs.achievements;

import android.content.Context;

/**
 * Categories for achievements.
 * Central definition of category names, display order, and string resource keys.
 * 
 * Display names are loaded from strings.xml using the stringResName.
 * Example: PROGRESSION uses "achievement_category_progression" from strings.xml
 */
public enum AchievementCategory {
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
    SPECIAL("achievement_category_special", 11),
    CHALLENGE("achievement_category_challenge", 99); // Disabled, high order to sort last

    private final String stringResName;
    private final int displayOrder;

    AchievementCategory(String stringResName, int displayOrder) {
        this.stringResName = stringResName;
        this.displayOrder = displayOrder;
    }

    /**
     * Get the string resource name for this category.
     * Use with context.getResources().getIdentifier() to get the actual string.
     */
    public String getStringResName() {
        return stringResName;
    }

    /**
     * Get the display order for sorting categories.
     */
    public int getDisplayOrder() {
        return displayOrder;
    }

    /**
     * Get the localized display name for this category.
     * @param context Android context for accessing resources
     * @return The localized category name from strings.xml
     */
    public String getDisplayName(Context context) {
        int resId = context.getResources().getIdentifier(stringResName, "string", context.getPackageName());
        if (resId != 0) {
            return context.getString(resId);
        }
        // Fallback to enum name if string resource not found
        return name().replace("_", " ");
    }

    /**
     * Check if this category is enabled (has achievements).
     */
    public boolean isEnabled() {
        return this != CHALLENGE; // CHALLENGE is disabled
    }
}
