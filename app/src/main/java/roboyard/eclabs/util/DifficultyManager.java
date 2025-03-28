package roboyard.eclabs.util;

import android.content.Context;
import android.content.SharedPreferences;

import roboyard.eclabs.Constants;
import timber.log.Timber;

/**
 * Manages difficulty preferences for both legacy canvas-based UI
 * and modern fragment-based UI.
 * This ensures consistent difficulty settings across both implementations.
 */
public class DifficultyManager {
    private static final String PREFS_NAME = "RoboyardDifficultyPrefs";
    private static final String KEY_DIFFICULTY = "difficulty";
    
    // Difficulty constants
    public static final int DIFFICULTY_BEGINNER = Constants.DIFFICULTY_BEGINNER;
    public static final int DIFFICULTY_INTERMEDIATE = Constants.DIFFICULTY_INTERMEDIATE;
    public static final int DIFFICULTY_INSANE = Constants.DIFFICULTY_INSANE;
    public static final int DIFFICULTY_IMPOSSIBLE = Constants.DIFFICULTY_IMPOSSIBLE;
    
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
     * @return Current difficulty level (DIFFICULTY_BEGINNER, DIFFICULTY_INTERMEDIATE, DIFFICULTY_INSANE, or DIFFICULTY_IMPOSSIBLE)
     */
    public int getDifficulty() {
        Timber.d("[DIFFICULTY][SOLUTION SOLVER][MOVES] Getting difficulty level: %d, default to %d", prefs.getInt(KEY_DIFFICULTY, DIFFICULTY_BEGINNER), DIFFICULTY_BEGINNER);
        return prefs.getInt(KEY_DIFFICULTY, DIFFICULTY_BEGINNER); // Default to beginner difficulty
    }
    
    /**
     * Set the difficulty level
     * @param difficulty Difficulty level to set (DIFFICULTY_BEGINNER, DIFFICULTY_INTERMEDIATE, DIFFICULTY_INSANE, or DIFFICULTY_IMPOSSIBLE)
     */
    public void setDifficulty(int difficulty) {
        if (difficulty < DIFFICULTY_BEGINNER || difficulty > DIFFICULTY_IMPOSSIBLE) {
            throw new IllegalArgumentException("Invalid difficulty level: " + difficulty);
        }
        
        Timber.d("[DIFFICULTY] Setting difficulty to %d", difficulty);
        prefs.edit().putInt(KEY_DIFFICULTY, difficulty).apply();
    }
    
    /**
     * Get a string representation of the current difficulty level
     * @return String representation of the current difficulty level ("Beginner", "Intermediate", etc.)
     */
    public String getDifficultyString() {
        int difficulty = getDifficulty();
        switch (difficulty) {
            case DIFFICULTY_BEGINNER:
                return "Beginner";
            case DIFFICULTY_INTERMEDIATE:
                return "Intermediate";
            case DIFFICULTY_INSANE:
                return "Insane";
            case DIFFICULTY_IMPOSSIBLE:
                return "Impossible";
            default:
                return "Beginner";
        }
    }
}
