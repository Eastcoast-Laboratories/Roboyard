package roboyard.eclabs.achievements;

/**
 * Represents a single achievement in the game.
 */
public class Achievement {
    private final String id;
    private final String nameKey;        // String resource key for name
    private final String descriptionKey; // String resource key for description
    private final AchievementCategory category;
    private final int iconResId;
    private boolean unlocked;
    private long unlockedTimestamp;
    
    public Achievement(String id, String nameKey, String descriptionKey, 
                       AchievementCategory category, int iconResId) {
        this.id = id;
        this.nameKey = nameKey;
        this.descriptionKey = descriptionKey;
        this.category = category;
        this.iconResId = iconResId;
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
