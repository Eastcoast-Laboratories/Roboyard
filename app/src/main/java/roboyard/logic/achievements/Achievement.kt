package roboyard.logic.achievements

/**
 * Represents a single achievement in the game.
 */
class Achievement {
    @JvmField
    val id: String?
    @JvmField
    val nameKey: String? // String resource key for name
    @JvmField
    val descriptionKey: String? // String resource key for description
    @JvmField
    val category: AchievementCategory?
    private val iconResId: Int // Legacy fallback icon resource

    /**
     * Get the sprite sheet icon index (0-63).
     * @return The sprite index
     */
    @JvmField
    val spriteIndex: Int // Index in the sprite sheet (0-63), deprecated

    /**
     * Get the drawable resource name for the icon.
     * @return The drawable resource name (e.g., "1_lightning")
     */
    @JvmField
    val iconDrawableName: String? // Name of drawable resource (e.g., "1_lightning")
    private var unlocked: Boolean
    @JvmField
    var unlockedTimestamp: Long

    @Transient
    var nameFormatArgs: Array<out Any?>? = null
        private set

    @Transient
    var descriptionFormatArgs: Array<out Any?>? = null
        private set

    /**
     * Create an achievement with a sprite sheet icon index (deprecated).
     * @param id Unique achievement ID
     * @param nameKey String resource key for name
     * @param descriptionKey String resource key for description
     * @param category Achievement category
     * @param spriteIndex Index in the sprite sheet (0-63)
     */
    constructor(
        id: String?, nameKey: String?, descriptionKey: String?,
        category: AchievementCategory?, spriteIndex: Int
    ) {
        this.id = id
        this.nameKey = nameKey
        this.descriptionKey = descriptionKey
        this.category = category
        this.spriteIndex = spriteIndex
        this.iconResId = 0 // Not used when spriteIndex is set
        this.iconDrawableName = null // Not used when spriteIndex is set
        this.unlocked = false
        this.unlockedTimestamp = 0
    }

    /**
     * Create an achievement with a drawable resource name.
     * @param id Unique achievement ID
     * @param nameKey String resource key for name
     * @param descriptionKey String resource key for description
     * @param category Achievement category
     * @param iconDrawableName Name of drawable resource (e.g., "1_lightning")
     */
    constructor(
        id: String?, nameKey: String?, descriptionKey: String?,
        category: AchievementCategory?, iconDrawableName: String?
    ) {
        this.id = id
        this.nameKey = nameKey
        this.descriptionKey = descriptionKey
        this.category = category
        this.iconDrawableName = iconDrawableName
        this.spriteIndex = -1 // Not used when iconDrawableName is set
        this.iconResId = 0 // Not used when iconDrawableName is set
        this.unlocked = false
        this.unlockedTimestamp = 0
    }

    fun setNameFormatArgs(vararg args: Any?): Achievement {
        this.nameFormatArgs = args as Array<out Any?>
        return this
    }

    fun setDescriptionFormatArgs(vararg args: Any?): Achievement {
        this.descriptionFormatArgs = args as Array<out Any?>
        return this
    }

    fun isUnlocked(): Boolean {
        return unlocked
    }

    fun setUnlocked(unlocked: Boolean) {
        this.unlocked = unlocked
        if (unlocked && unlockedTimestamp == 0L) {
            this.unlockedTimestamp = System.currentTimeMillis()
        }
    }
}
