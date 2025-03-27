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
     * Save completion data for a level
     * @param data The completion data to save
     */
    public void saveLevelCompletionData(LevelCompletionData data) {
        completionDataMap.put(data.getLevelId(), data);
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
