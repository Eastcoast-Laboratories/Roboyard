package roboyard.eclabs.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages difficulty preferences for both legacy canvas-based UI
 * and modern fragment-based UI.
 * This ensures consistent difficulty settings across both implementations.
 */
public class DifficultyManager {
    private static final String PREFS_NAME = "RoboyardDifficultyPrefs";
    private static final String KEY_DIFFICULTY = "difficulty";
    
    // Difficulty constants
    public static final int DIFFICULTY_EASY = 0;
    public static final int DIFFICULTY_NORMAL = 1;
    public static final int DIFFICULTY_HARD = 2;
    
    private static DifficultyManager instance;
    private final SharedPreferences prefs;
    
    /**
     * Get the singleton instance of DifficultyManager
     * @param context Application context
     * @return DifficultyManager instance
     */
    public static synchronized DifficultyManager getInstance(Context context) {
        if (instance == null) {
            instance = new DifficultyManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor to prevent direct instantiation
     * @param context Application context
     */
    private DifficultyManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Get the current difficulty level
     * @return Current difficulty level (DIFFICULTY_EASY, DIFFICULTY_NORMAL, or DIFFICULTY_HARD)
     */
    public int getDifficulty() {
        return prefs.getInt(KEY_DIFFICULTY, DIFFICULTY_NORMAL); // Default to normal difficulty
    }
    
    /**
     * Set the difficulty level
     * @param difficulty Difficulty level to set (DIFFICULTY_EASY, DIFFICULTY_NORMAL, or DIFFICULTY_HARD)
     */
    public void setDifficulty(int difficulty) {
        if (difficulty < DIFFICULTY_EASY || difficulty > DIFFICULTY_HARD) {
            throw new IllegalArgumentException("Invalid difficulty level: " + difficulty);
        }
        
        prefs.edit().putInt(KEY_DIFFICULTY, difficulty).apply();
    }
    
    /**
     * Get a string representation of the current difficulty level
     * @return String representation of the current difficulty level ("Easy", "Normal", or "Hard")
     */
    public String getDifficultyString() {
        switch (getDifficulty()) {
            case DIFFICULTY_EASY:
                return "Easy";
            case DIFFICULTY_NORMAL:
                return "Normal";
            case DIFFICULTY_HARD:
                return "Hard";
            default:
                return "Normal";
        }
    }
    
    /**
     * Reset difficulty to default (DIFFICULTY_NORMAL)
     */
    public void resetToDefault() {
        setDifficulty(DIFFICULTY_NORMAL);
    }
}
