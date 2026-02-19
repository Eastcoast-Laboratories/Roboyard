package roboyard.ui.components;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;
import roboyard.logic.core.LevelCompletionData;

/**
 * Manages level completion data, including saving and loading statistics.
 */
public class LevelCompletionManager {
    private static final String PREFS_NAME = "level_completion_prefs";
    private static final String COMPLETION_DATA_KEY = "completion_data";
    private static final String LAST_PLAYED_LEVEL_KEY = "last_played_level";
    
    private static LevelCompletionManager instance;
    private Map<Integer, LevelCompletionData> completionDataMap;
    private final Context context;
    
    private LevelCompletionManager(Context context) {
        this.context = context.getApplicationContext();
        this.completionDataMap = new HashMap<>();
        loadCompletionData();
    }
    
    public static synchronized LevelCompletionManager getInstance(Context context) {
        if (instance == null) {
            instance = new LevelCompletionManager(context);
        }
        return instance;
    }
    
    /**
     * Get completion data for a specific level
     * @param levelId The level ID
     * @return The completion data, or a new empty data object if none exists
     */
    public LevelCompletionData getLevelCompletionData(int levelId) {
        if (!completionDataMap.containsKey(levelId)) {
            completionDataMap.put(levelId, new LevelCompletionData(levelId));
        }
        return completionDataMap.get(levelId);
    }
    
    /**
     * Save completion data for a level, only updating values that are better than existing ones
     * @param data The completion data to save
     */
    public void saveLevelCompletionData(LevelCompletionData data) {
        int levelId = data.getLevelId();
        
        Timber.d("[LEVEL_COMPLETION] Saving data for level %d - Stars: %d, Moves: %d, Time: %d, Squares: %d",
                levelId, data.getStars(), data.getMovesNeeded(), data.getTimeNeeded(), data.getSquaresSurpassed());
        
        // Check if we already have data for this level
        if (completionDataMap.containsKey(levelId)) {
            LevelCompletionData existingData = completionDataMap.get(levelId);
            
            Timber.d("[LEVEL_COMPLETION] Existing data - Stars: %d, Moves: %d, Time: %d, Squares: %d",
                    existingData.getStars(), existingData.getMovesNeeded(), 
                    existingData.getTimeNeeded(), existingData.getSquaresSurpassed());
            
            // Only update stars if new value is greater
            boolean starsImproved = data.getStars() > existingData.getStars();
            boolean starsAtLeastSame = data.getStars() >= existingData.getStars();

            if (starsImproved) {
                Timber.d("[LEVEL_COMPLETION] Stars improved from %d to %d - updating stars and related metrics",
                        existingData.getStars(), data.getStars());

                // If stars have improved, update stars and related metrics
                existingData.setStars(data.getStars());
                existingData.setOptimalMoves(data.getOptimalMoves());
            } else {
                Timber.d("[LEVEL_COMPLETION] Stars not improved (%d vs %d) - not updating stars",
                        data.getStars(), existingData.getStars());
            }

            // Only update hintsShown and robotsUsed if stars are at least the same
            if (data.isCompleted() && starsAtLeastSame) {
                existingData.setCompleted(true);
                existingData.setHintsShown(data.getHintsShown());
                existingData.setRobotsUsed(data.getRobotsUsed());
            }

            // Always update moves if it's lower (better) than existing value
            if (data.isCompleted() && (existingData.getMovesNeeded() == 0 || data.getMovesNeeded() < existingData.getMovesNeeded())) {
                Timber.d("[LEVEL_COMPLETION] Moves improved from %d to %d",
                        existingData.getMovesNeeded(), data.getMovesNeeded());
                existingData.setMovesNeeded(data.getMovesNeeded());
            }

            // Always update time if it's lower (faster) than existing value
            if (data.isCompleted() && (existingData.getTimeNeeded() == 0 || data.getTimeNeeded() < existingData.getTimeNeeded())) {
                Timber.d("[LEVEL_COMPLETION] Time improved from %d to %d",
                        existingData.getTimeNeeded(), data.getTimeNeeded());
                existingData.setTimeNeeded(data.getTimeNeeded());
            }

            // Always update squares if it's more (better) than existing value
            if (data.isCompleted() && data.getSquaresSurpassed() > existingData.getSquaresSurpassed()) {
                Timber.d("[LEVEL_COMPLETION] Squares improved from %d to %d",
                        existingData.getSquaresSurpassed(), data.getSquaresSurpassed());
                existingData.setSquaresSurpassed(data.getSquaresSurpassed());
            }
            
            // Use the updated existing data
            completionDataMap.put(levelId, existingData);
        } else {
            // No existing data, just add the new data directly
            Timber.d("[LEVEL_COMPLETION] No existing data for level %d, adding new data", levelId);
            completionDataMap.put(levelId, data);
        }
        
        // Save changes to SharedPreferences
        saveCompletionData();
    }
    
    /**
     * Check if a level has been completed
     * @param levelId The level ID to check
     * @return true if the level is completed, false otherwise
     */
    public boolean isLevelCompleted(int levelId) {
        LevelCompletionData data = getLevelCompletionData(levelId);
        boolean completed = data.isCompleted();
        Timber.d("Checking if level %d is completed: %s", levelId, completed);
        return completed;
    }
    
    
    /**
     * Get the total number of stars earned across all levels
     * @return The total number of stars
     */
    public int getTotalStars() {
        int totalStars = 0;
        for (LevelCompletionData data : completionDataMap.values()) {
            if (data.isCompleted()) {
                totalStars += data.getStars();
            }
        }
        Timber.d("Total stars earned across all levels: %d", totalStars);
        return totalStars;
    }
    
    /**
     * Load all completion data from SharedPreferences
     */
    private void loadCompletionData() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(COMPLETION_DATA_KEY, null);
        
        Timber.d("Loading completion data, found JSON: %s", json != null ? "yes" : "no");
        
        if (json != null) {
            try {
                Gson gson = new Gson();
                // Use Runtime Type to avoid ProGuard issues
                Type mapType = new TypeToken<HashMap<Integer, LevelCompletionData>>() {}.getType();
                Map<Integer, LevelCompletionData> loadedData = gson.fromJson(json, mapType);
                
                if (loadedData != null) {
                    completionDataMap = loadedData;
                    Timber.d("Loaded completion data for %d levels", completionDataMap.size());
                    
                    // Debug output to show what was loaded
                    for (Map.Entry<Integer, LevelCompletionData> entry : completionDataMap.entrySet()) {
                        Timber.d("Loaded level %d: completed=%s, stars=%d", 
                                entry.getKey(), entry.getValue().isCompleted(), entry.getValue().getStars());
                    }
                } else {
                    Timber.w("Loaded data was null despite having JSON");
                }
            } catch (Exception e) {
                // More detailed error handling with UI feedback
                Timber.e(e, "Error loading level completion data");
                Toast.makeText(context, "Error loading level data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                // Create an empty map as fallback
                completionDataMap = new HashMap<>();
            }
        } else {
            Timber.d("No completion data found in SharedPreferences");
        }
    }
    
    /**
     * Set the last played level ID
     * @param levelId The level ID that was last played
     */
    public void setLastPlayedLevel(int levelId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(LAST_PLAYED_LEVEL_KEY, levelId).apply();
        Timber.d("[LEVEL_COMPLETION] Set last played level to %d", levelId);
    }
    
    /**
     * Get the last played level ID
     * @return The last played level ID, or 1 if none was set
     */
    public int getLastPlayedLevel() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(LAST_PLAYED_LEVEL_KEY, 1);
    }
    
    /**
     * Reset all level completion data
     */
    public void resetAll() {
        Timber.d("[LEVEL_COMPLETION] Resetting all level completion data");
        completionDataMap.clear();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        Timber.d("[LEVEL_COMPLETION] All level data reset successfully");
    }
    
    /**
     * Unlock 1 star per level for all levels except level 139 (used for debug/level design editor unlock)
     */
    public void unlockAllStars() {
        Timber.d("[LEVEL_COMPLETION] Unlocking 1 star per level (except level 139) for level design editor");
        
        for (int levelId = 1; levelId <= 140; levelId++) {
            if (levelId == 139) continue; // Level 139 stays locked
            LevelCompletionData data = getLevelCompletionData(levelId);
            data.setCompleted(true);
            data.setStars(1);
            data.setOptimalMoves(1);
            data.setTimeNeeded(1);
            data.setSquaresSurpassed(0);
        }
        
        saveCompletionData();
        Timber.d("[LEVEL_COMPLETION] 1 star per level unlocked (except level 139) successfully");
    }
    
    /**
     * Save all completion data to SharedPreferences
     */
    private void saveCompletionData() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        try {
            Gson gson = new Gson();
            String json = gson.toJson(completionDataMap);
            Timber.d("Saving completion data JSON: %s", json);
            editor.putString(COMPLETION_DATA_KEY, json);
            boolean success = editor.commit(); // Use commit() instead of apply() to get immediate result
            Timber.d("Saved completion data for %d levels, success: %s", completionDataMap.size(), success);
        } catch (Exception e) {
            Timber.e(e, "Error saving level completion data");
        }
    }
}
