package roboyard.eclabs.achievements;

/**
 * Represents a single achievement in the game.
 */
public class Achievement {
    private final String id;
    private final String nameKey;        // String resource key for name
    private final String descriptionKey; // String resource key for description
    private final AchievementCategory category;
    private final int iconResId;         // Legacy fallback icon resource
    private final int spriteIndex;       // Index in the sprite sheet (0-63)
    private boolean unlocked;
    private long unlockedTimestamp;
    
    /**
     * Create an achievement with a sprite sheet icon index.
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
    
    public AchievementCategory getCategory() {
        return category;
    }
    
    public int getIconResId() {
        return iconResId;
    }
    
    /**
     * Get the sprite sheet icon index (0-63).
     * @return The sprite index
     */
    public int getSpriteIndex() {
        return spriteIndex;
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
