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
