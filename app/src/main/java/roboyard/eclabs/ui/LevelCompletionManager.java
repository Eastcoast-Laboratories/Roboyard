package roboyard.eclabs.ui;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Manages level completion data, including saving and loading statistics.
 */
public class LevelCompletionManager {
    private static final String PREFS_NAME = "level_completion_prefs";
    private static final String COMPLETION_DATA_KEY = "completion_data";
    
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
        
        // Check if we already have data for this level
        if (completionDataMap.containsKey(levelId)) {
            LevelCompletionData existingData = completionDataMap.get(levelId);
            
            // Always mark as completed if the new data says it's completed
            if (data.isCompleted()) {
                existingData.setCompleted(true);
            }
            
            // Check if star count has changed
            boolean starsChanged = data.getStars() > existingData.getStars();
            
            // Only update stars if new value is greater
            if (starsChanged) {
                // If stars have improved, update stars and related metrics
                existingData.setStars(data.getStars());
                existingData.setHintsShown(data.getHintsShown());
                existingData.setRobotsUsed(data.getRobotsUsed());
                existingData.setOptimalMoves(data.getOptimalMoves());
            }
            
            // Only update moves if it's lower (better) than existing value
            // Or if there was no previous value (movesNeeded == 0)
            if (existingData.getMovesNeeded() == 0 || data.getMovesNeeded() < existingData.getMovesNeeded()) {
                existingData.setMovesNeeded(data.getMovesNeeded());
            }
            
            // Only update time if it's lower (faster) than existing value
            // Or if there was no previous value (timeNeeded == 0)
            if (existingData.getTimeNeeded() == 0 || data.getTimeNeeded() < existingData.getTimeNeeded()) {
                existingData.setTimeNeeded(data.getTimeNeeded());
            }
            
            // Only update squares surpassed if it's lower (more efficient) than existing value
            // Or if there was no previous value (squaresSurpassed == 0)
            if (existingData.getSquaresSurpassed() == 0 || data.getSquaresSurpassed() < existingData.getSquaresSurpassed()) {
                existingData.setSquaresSurpassed(data.getSquaresSurpassed());
            }
            
            // Use the updated existing data
            completionDataMap.put(levelId, existingData);
        } else {
            // No existing data, just add the new data directly
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
     * Get the number of stars earned for a specific level
     * @param levelId The level ID to check
     * @return The number of stars earned (0-3)
     */
    public int getStarsForLevel(int levelId) {
        LevelCompletionData data = getLevelCompletionData(levelId);
        int stars = data.getStars();
        Timber.d("Getting stars for level %d: %d", levelId, stars);
        return stars;
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
                Type type = new TypeToken<Map<Integer, LevelCompletionData>>(){}.getType();
                Map<Integer, LevelCompletionData> loadedData = gson.fromJson(json, type);
                
                if (loadedData != null) {
                    completionDataMap = loadedData;
                    Timber.d("Loaded completion data for %d levels: %s", 
                            completionDataMap.size(), completionDataMap.toString());
                } else {
                    Timber.w("Loaded data was null despite having JSON");
                }
            } catch (Exception e) {
                Timber.e(e, "Error loading level completion data");
            }
        } else {
            Timber.d("No completion data found in SharedPreferences");
        }
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
