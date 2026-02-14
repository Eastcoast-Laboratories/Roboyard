package roboyard.ui.components;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Map;

import timber.log.Timber;
import roboyard.eclabs.BuildConfig;

/**
 * Manages export and import of all app data as JSON.
 * Includes: preferences, achievements, streaks, level completion, wall storage, save games.
 */
public class DataExportImportManager {
    
    private static final String TAG = "DataExportImport";
    
    // SharedPreferences names used in the app
    private static final String[] PREFS_NAMES = {
        "RoboYard",                  // Main preferences (Preferences.java)
        "roboyard_achievements",     // Achievement data (AchievementManager.java)
        "roboyard_streaks",          // Streak data (StreakManager.java)
        "level_completion_prefs",    // Level completion data (LevelCompletionManager.java)
        "WallStoragePrefs",          // Wall storage (WallStorage.java)
        "RoboyardUIPrefs"            // UI mode (UIModeManager.java)
    };
    
    private static final String SAVES_DIRECTORY = "saves";
    
    private final Context context;
    
    public DataExportImportManager(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * Export all app data to a JSON string.
     * @return JSON string containing all app data
     */
    public String exportAllData() {
        try {
            JSONObject root = new JSONObject();
            
            // Add metadata
            JSONObject metadata = new JSONObject();
            metadata.put("version", 1);
            metadata.put("exportTime", System.currentTimeMillis());
            metadata.put("appVersion", BuildConfig.VERSION_NAME);
            root.put("metadata", metadata);
            
            // Export all SharedPreferences
            JSONObject prefsData = new JSONObject();
            for (String prefsName : PREFS_NAMES) {
                JSONObject prefsJson = exportSharedPreferences(prefsName);
                if (prefsJson.length() > 0) {
                    prefsData.put(prefsName, prefsJson);
                }
            }
            root.put("preferences", prefsData);
            
            // Export save games
            JSONArray saveGames = exportSaveGames();
            root.put("saveGames", saveGames);
            
            // Export game history
            JSONArray gameHistory = exportGameHistory();
            root.put("gameHistory", gameHistory);
            
            Timber.tag(TAG).d("Exported all data successfully");
            return root.toString(2); // Pretty print with 2-space indent
            
        } catch (JSONException e) {
            Timber.tag(TAG).e(e, "Error exporting data");
            return null;
        }
    }
    
    /**
     * Export a single SharedPreferences file to JSON.
     * Exports all entries including those with null values.
     */
    private JSONObject exportSharedPreferences(String prefsName) throws JSONException {
        JSONObject result = new JSONObject();
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();
        
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value == null) {
                result.put(key, JSONObject.NULL);
            } else if (value instanceof Boolean) {
                result.put(key, (Boolean) value);
            } else if (value instanceof Integer) {
                result.put(key, (Integer) value);
            } else if (value instanceof Long) {
                result.put(key, (Long) value);
            } else if (value instanceof Float) {
                result.put(key, (Float) value);
            } else if (value instanceof String) {
                result.put(key, (String) value);
            } else if (value instanceof java.util.Set) {
                // Handle StringSet
                JSONArray jsonArray = new JSONArray();
                for (Object item : (java.util.Set<?>) value) {
                    jsonArray.put(item);
                }
                result.put(key, jsonArray);
            }
        }
        
        Timber.tag(TAG).d("Exported %d entries from %s", allEntries.size(), prefsName);
        return result;
    }
    
    /**
     * Export all save games to a JSON array.
     */
    private JSONArray exportSaveGames() throws JSONException {
        JSONArray saveGames = new JSONArray();
        
        File savesDir = new File(context.getFilesDir(), SAVES_DIRECTORY);
        if (savesDir.exists() && savesDir.isDirectory()) {
            File[] files = savesDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".dat")) {
                        try {
                            String content = FileReadWrite.loadAbsoluteData(file.getAbsolutePath());
                            if (content != null && !content.isEmpty()) {
                                JSONObject saveGame = new JSONObject();
                                saveGame.put("filename", file.getName());
                                saveGame.put("content", content);
                                saveGame.put("lastModified", file.lastModified());
                                saveGames.put(saveGame);
                            }
                        } catch (Exception e) {
                            Timber.tag(TAG).e(e, "Error reading save file: %s", file.getName());
                        }
                    }
                }
            }
        }
        
        return saveGames;
    }
    
    /**
     * Export game history to a JSON array.
     */
    private JSONArray exportGameHistory() throws JSONException {
        JSONArray history = new JSONArray();
        
        File historyDir = new File(context.getFilesDir(), "history");
        if (historyDir.exists() && historyDir.isDirectory()) {
            File[] files = historyDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        try {
                            String content = FileReadWrite.loadAbsoluteData(file.getAbsolutePath());
                            if (content != null && !content.isEmpty()) {
                                JSONObject historyEntry = new JSONObject();
                                historyEntry.put("filename", file.getName());
                                historyEntry.put("content", content);
                                historyEntry.put("lastModified", file.lastModified());
                                history.put(historyEntry);
                            }
                        } catch (Exception e) {
                            Timber.tag(TAG).e(e, "Error reading history file: %s", file.getName());
                        }
                    }
                }
            }
        }
        
        return history;
    }
    
    /**
     * Import all app data from a JSON string.
     * @param jsonData JSON string containing app data
     * @return true if import was successful, false otherwise
     */
    public boolean importAllData(String jsonData) {
        try {
            JSONObject root = new JSONObject(jsonData);
            
            // Check version compatibility
            JSONObject metadata = root.optJSONObject("metadata");
            if (metadata != null) {
                int version = metadata.optInt("version", 1);
                Timber.tag(TAG).d("Importing data version %d", version);
            }
            
            // Import SharedPreferences
            JSONObject prefsData = root.optJSONObject("preferences");
            if (prefsData != null) {
                for (String prefsName : PREFS_NAMES) {
                    JSONObject prefsJson = prefsData.optJSONObject(prefsName);
                    if (prefsJson != null) {
                        importSharedPreferences(prefsName, prefsJson);
                    }
                }
            }
            
            // Import save games
            JSONArray saveGames = root.optJSONArray("saveGames");
            if (saveGames != null) {
                importSaveGames(saveGames);
            }
            
            // Import game history
            JSONArray gameHistory = root.optJSONArray("gameHistory");
            if (gameHistory != null) {
                importGameHistory(gameHistory);
            }
            
            Timber.tag(TAG).d("Imported all data successfully");
            return true;
            
        } catch (JSONException e) {
            Timber.tag(TAG).e(e, "Error importing data");
            return false;
        }
    }
    
    /**
     * Import a single SharedPreferences file from JSON.
     */
    private void importSharedPreferences(String prefsName, JSONObject prefsJson) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Clear existing data
        editor.clear();
        
        // Import all entries
        java.util.Iterator<String> keys = prefsJson.keys();
        int importedCount = 0;
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                Object value = prefsJson.get(key);
                
                if (value == JSONObject.NULL) {
                    // Skip null values
                    continue;
                } else if (value instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) value);
                    importedCount++;
                } else if (value instanceof Integer) {
                    editor.putInt(key, (Integer) value);
                    importedCount++;
                } else if (value instanceof Long) {
                    editor.putLong(key, (Long) value);
                    importedCount++;
                } else if (value instanceof Double) {
                    // JSON stores floats as doubles
                    editor.putFloat(key, ((Double) value).floatValue());
                    importedCount++;
                } else if (value instanceof String) {
                    editor.putString(key, (String) value);
                    importedCount++;
                } else if (value instanceof JSONArray) {
                    // Handle StringSet
                    JSONArray jsonArray = (JSONArray) value;
                    java.util.Set<String> stringSet = new java.util.HashSet<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        stringSet.add(jsonArray.getString(i));
                    }
                    editor.putStringSet(key, stringSet);
                    importedCount++;
                }
            } catch (JSONException e) {
                Timber.tag(TAG).e(e, "Error importing preference: %s", key);
            }
        }
        
        editor.apply();
        Timber.tag(TAG).d("Imported %d entries to %s", importedCount, prefsName);
    }
    
    /**
     * Import save games from a JSON array.
     */
    private void importSaveGames(JSONArray saveGames) throws JSONException {
        File savesDir = new File(context.getFilesDir(), SAVES_DIRECTORY);
        if (!savesDir.exists()) {
            savesDir.mkdirs();
        }
        
        for (int i = 0; i < saveGames.length(); i++) {
            JSONObject saveGame = saveGames.getJSONObject(i);
            String filename = saveGame.getString("filename");
            String content = saveGame.getString("content");
            
            try {
                File saveFile = new File(savesDir, filename);
                java.io.FileOutputStream fos = new java.io.FileOutputStream(saveFile);
                fos.write(content.getBytes());
                fos.close();
                Timber.tag(TAG).d("Imported save game: %s", filename);
            } catch (java.io.IOException e) {
                Timber.tag(TAG).e(e, "Error importing save game: %s", filename);
            }
        }
    }
    
    /**
     * Import game history from a JSON array.
     */
    private void importGameHistory(JSONArray gameHistory) throws JSONException {
        File historyDir = new File(context.getFilesDir(), "history");
        if (!historyDir.exists()) {
            historyDir.mkdirs();
        }
        
        for (int i = 0; i < gameHistory.length(); i++) {
            JSONObject historyEntry = gameHistory.getJSONObject(i);
            String filename = historyEntry.getString("filename");
            String content = historyEntry.getString("content");
            
            try {
                File historyFile = new File(historyDir, filename);
                java.io.FileOutputStream fos = new java.io.FileOutputStream(historyFile);
                fos.write(content.getBytes());
                fos.close();
                Timber.tag(TAG).d("Imported history entry: %s", filename);
            } catch (java.io.IOException e) {
                Timber.tag(TAG).e(e, "Error importing history entry: %s", filename);
            }
        }
    }
    
    /**
     * Reset all app data (clear all preferences and files).
     */
    public void resetAllData() {
        // Clear all SharedPreferences
        for (String prefsName : PREFS_NAMES) {
            SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
        }
        
        // Delete save games
        File savesDir = new File(context.getFilesDir(), SAVES_DIRECTORY);
        if (savesDir.exists()) {
            deleteDirectory(savesDir);
        }
        
        // Delete game history
        File historyDir = new File(context.getFilesDir(), "history");
        if (historyDir.exists()) {
            deleteDirectory(historyDir);
        }
        
        Timber.tag(TAG).d("Reset all data");
    }
    
    /**
     * Delete a directory and all its contents.
     */
    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        dir.delete();
    }
}
