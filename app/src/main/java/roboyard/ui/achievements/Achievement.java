package roboyard.ui.achievements;

/**
 * Represents a single achievement in the game.
 */
public class Achievement {
    private final String id;
    private final String nameKey;        // String resource key for name
    private final String descriptionKey; // String resource key for description
    private final AchievementCategory category;
    private final int iconResId;         // Legacy fallback icon resource
    private final int spriteIndex;       // Index in the sprite sheet (0-63), deprecated
    private final String iconDrawableName; // Name of drawable resource (e.g., "1_lightning")
    private boolean unlocked;
    private long unlockedTimestamp;
    private transient Object[] nameFormatArgs;
    private transient Object[] descriptionFormatArgs;
    
    /**
     * Create an achievement with a sprite sheet icon index (deprecated).
     * @param id Unique achievement ID
     * @param nameKey String resource key for name
     * @param descriptionKey String resource key for description
     * @param category Achievement category
     * @param spriteIndex Index in the sprite sheet (0-63)
     */
    public Achievement(String id, String nameKey, String descriptionKey, 
                       AchievementCategory category, int spriteIndex) {
        this.id = id;
        this.nameKey = nameKey;
        this.descriptionKey = descriptionKey;
        this.category = category;
        this.spriteIndex = spriteIndex;
        this.iconResId = 0; // Not used when spriteIndex is set
        this.iconDrawableName = null; // Not used when spriteIndex is set
        this.unlocked = false;
        this.unlockedTimestamp = 0;
    }
    
    /**
     * Create an achievement with a drawable resource name.
     * @param id Unique achievement ID
     * @param nameKey String resource key for name
     * @param descriptionKey String resource key for description
     * @param category Achievement category
     * @param iconDrawableName Name of drawable resource (e.g., "1_lightning")
     */
    public Achievement(String id, String nameKey, String descriptionKey, 
                       AchievementCategory category, String iconDrawableName) {
        this.id = id;
        this.nameKey = nameKey;
        this.descriptionKey = descriptionKey;
        this.category = category;
        this.iconDrawableName = iconDrawableName;
        this.spriteIndex = -1; // Not used when iconDrawableName is set
        this.iconResId = 0; // Not used when iconDrawableName is set
        this.unlocked = false;
        this.unlockedTimestamp = 0;
    }
    
    public String getId() {
        return id;
    }
    
    public String getNameKey() {
        return nameKey;
    }
    
    public String getDescriptionKey() {
        return descriptionKey;
    }
    
    public Achievement setNameFormatArgs(Object... args) {
        this.nameFormatArgs = args;
        return this;
    }

    public Achievement setDescriptionFormatArgs(Object... args) {
        this.descriptionFormatArgs = args;
        return this;
    }
    
    public Object[] getNameFormatArgs() {
        return nameFormatArgs;
    }

    public Object[] getDescriptionFormatArgs() {
        return descriptionFormatArgs;
    }
    
    public AchievementCategory getCategory() {
        return category;
    }
    
    /**
     * Get the sprite sheet icon index (0-63).
     * @return The sprite index
     */
    public int getSpriteIndex() {
        return spriteIndex;
    }
    
    /**
     * Get the drawable resource name for the icon.
     * @return The drawable resource name (e.g., "1_lightning")
     */
    public String getIconDrawableName() {
        return iconDrawableName;
    }
    
    public boolean isUnlocked() {
        return unlocked;
    }
    
    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
        if (unlocked && unlockedTimestamp == 0) {
            this.unlockedTimestamp = System.currentTimeMillis();
        }
    }
    
    public long getUnlockedTimestamp() {
        return unlockedTimestamp;
    }
    
    public void setUnlockedTimestamp(long timestamp) {
        this.unlockedTimestamp = timestamp;
    }
}
