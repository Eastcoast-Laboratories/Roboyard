package roboyard.eclabs;

import android.app.Activity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import timber.log.Timber;

/**
 * Manager class for handling game history entries.
 * Provides methods for saving, loading, and managing history entries.
 */
public class GameHistoryManager {
    private static final String HISTORY_DIR = "history";
    private static final String HISTORY_INDEX_FILE = "history_index.json";
    private static final int MAX_HISTORY_ENTRIES = 20;

    /**
     * Initialize the history directory if it doesn't exist
     */
    public static void initialize(Activity activity) {
        try {
            // Create history directory if it doesn't exist
            File historyDir = new File(activity.getFilesDir(), HISTORY_DIR);
            if (!historyDir.exists()) {
                boolean created = historyDir.mkdir();
                if (created) {
                    Timber.d("Created history directory: %s", historyDir.getAbsolutePath());
                } else {
                    Timber.e("Failed to create history directory");
                }
            }

            // Create empty history index file if it doesn't exist
            if (!FileReadWrite.privateDataExists(activity, HISTORY_INDEX_FILE)) {
                JSONObject indexJson = new JSONObject();
                indexJson.put("historyEntries", new JSONArray());
                FileReadWrite.writePrivateData(activity, HISTORY_INDEX_FILE, indexJson.toString());
                Timber.d("Created empty history index file");
            }
        } catch (Exception e) {
            Timber.e("Error initializing history manager: %s", e.getMessage());
        }
    }

    /**
     * Add a new history entry
     *
     * @return
     */
    public static Boolean addHistoryEntry(Activity activity, GameHistoryEntry entry) {
        try {
            // Load existing entries
            List<GameHistoryEntry> entries = getHistoryEntries(activity);
            
            // Check if we already have an entry with the same mapPath
            boolean updated = false;
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).getMapPath().equals(entry.getMapPath())) {
                    // Update existing entry
                    entries.set(i, entry);
                    updated = true;
                    break;
                }
            }
            
            // Add new entry if not updated
            if (!updated) {
                entries.add(entry);
            }
            
            // Sort by timestamp (newest first)
            Collections.sort(entries, new Comparator<GameHistoryEntry>() {
                @Override
                public int compare(GameHistoryEntry o1, GameHistoryEntry o2) {
                    return Long.compare(o2.getTimestamp(), o1.getTimestamp());
                }
            });
            
            // Trim to max entries
            if (entries.size() > MAX_HISTORY_ENTRIES) {
                // Get entries to remove
                List<GameHistoryEntry> entriesToRemove = 
                    entries.subList(MAX_HISTORY_ENTRIES, entries.size());
                
                // Delete files for removed entries
                for (GameHistoryEntry entryToRemove : entriesToRemove) {
                    deleteHistoryFiles(activity, entryToRemove);
                }
                
                // Trim the list
                entries = entries.subList(0, MAX_HISTORY_ENTRIES);
            }
            
            // Save updated index
            Boolean isSaved = saveHistoryIndex(activity, entries);
            
            Timber.d("Added/updated history entry: %s", entry.getMapPath());
            return isSaved;
        } catch (Exception e) {
            Timber.e("Error adding history entry: %s", e.getMessage());
            return false;
        }
    }

    /**
     * Get all history entries
     */
    public static List<GameHistoryEntry> getHistoryEntries(Activity activity) {
        List<GameHistoryEntry> entries = new ArrayList<>();
        try {
            String indexJson = FileReadWrite.readPrivateData(activity, HISTORY_INDEX_FILE);
            
            if (indexJson != null && !indexJson.isEmpty()) {
                JSONObject root = new JSONObject(indexJson);
                JSONArray entriesArray = root.getJSONArray("historyEntries");
                
                for (int i = 0; i < entriesArray.length(); i++) {
                    JSONObject entryJson = entriesArray.getJSONObject(i);
                    GameHistoryEntry entry = new GameHistoryEntry();
                    
                    entry.setMapPath(entryJson.getString("mapPath"));
                    entry.setMapName(entryJson.optString("mapName", "Unnamed"));
                    entry.setTimestamp(entryJson.getLong("timestamp"));
                    entry.setPlayDuration(entryJson.getInt("playDuration"));
                    entry.setMovesMade(entryJson.getInt("movesMade"));
                    entry.setOptimalMoves(entryJson.optInt("optimalMoves", 0));
                    entry.setBoardSize(entryJson.getString("boardSize"));
                    entry.setPreviewImagePath(entryJson.getString("previewImagePath"));
                    
                    entries.add(entry);
                }
            }
        } catch (Exception e) {
            Timber.e("Error loading history entries: %s", e.getMessage());
        }
        return entries;
    }

    /**
     * Delete a history entry
     */
    public static void deleteHistoryEntry(Activity activity, GameHistoryEntry entry) {
        try {
            // Load existing entries
            List<GameHistoryEntry> entries = getHistoryEntries(activity);
            
            // Remove the entry
            boolean removed = false;
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).getMapPath().equals(entry.getMapPath())) {
                    entries.remove(i);
                    removed = true;
                    break;
                }
            }
            
            if (removed) {
                // Delete the files
                deleteHistoryFiles(activity, entry);
                
                // Save updated index
                saveHistoryIndex(activity, entries);
                
                Timber.d("Deleted history entry: %s", entry.getMapPath());
            }
        } catch (Exception e) {
            Timber.e("Error deleting history entry: %s", e.getMessage());
        }
    }

    /**
     * Delete history files for an entry
     */
    private static void deleteHistoryFiles(Activity activity, GameHistoryEntry entry) {
        try {
            // Delete map file
            FileReadWrite.deletePrivateData(activity, entry.getMapPath());
            
            // Delete preview image if it exists
            if (entry.getPreviewImagePath() != null) {
                FileReadWrite.deletePrivateData(activity, entry.getPreviewImagePath());
            }
        } catch (Exception e) {
            Timber.e("Error deleting history files: %s", e.getMessage());
        }
    }

    /**
     * Save the history index file
     *
     * @return
     */
    private static Boolean saveHistoryIndex(Activity activity, List<GameHistoryEntry> entries) {
        try {
            JSONObject root = new JSONObject();
            JSONArray entriesArray = new JSONArray();
            
            for (GameHistoryEntry entry : entries) {
                JSONObject entryJson = new JSONObject();
                entryJson.put("mapPath", entry.getMapPath());
                entryJson.put("mapName", entry.getMapName());
                entryJson.put("timestamp", entry.getTimestamp());
                entryJson.put("playDuration", entry.getPlayDuration());
                entryJson.put("movesMade", entry.getMovesMade());
                entryJson.put("optimalMoves", entry.getOptimalMoves());
                entryJson.put("boardSize", entry.getBoardSize());
                entryJson.put("previewImagePath", entry.getPreviewImagePath());
                
                entriesArray.put(entryJson);
            }
            
            root.put("historyEntries", entriesArray);
            
            Boolean isSaved = FileReadWrite.writePrivateData(activity, HISTORY_INDEX_FILE, root.toString());
            
            Timber.d("Saved history index with %d entries", entries.size());
            return isSaved;
        } catch (Exception e) {
            Timber.e("Error saving history index: %s", e.getMessage());
            return false;
        }
    }

    /**
     * Get the next available history index
     */
    public static int getNextHistoryIndex(Activity activity) {
        List<GameHistoryEntry> entries = getHistoryEntries(activity);
        return entries.size();
    }

    /**
     * Promote a history entry to a permanent save
     */
    public static void promoteToSave(Activity activity, GameHistoryEntry entry, int saveSlot) {
        try {
            // Read the history file
            String historyPath = entry.getMapPath();
            String saveData = FileReadWrite.readPrivateData(activity, historyPath);
            
            if (saveData != null && !saveData.isEmpty()) {
                // Write to save slot
                String savePath = "map" + saveSlot + ".txt";
                FileReadWrite.writePrivateData(activity, savePath, saveData);
                
                // Copy preview image if it exists
                if (entry.getPreviewImagePath() != null) {
                    String previewPath = entry.getPreviewImagePath();
                    String previewData = FileReadWrite.readPrivateData(activity, previewPath);
                    if (previewData != null && !previewData.isEmpty()) {
                        String savePreviewPath = "map" + saveSlot + "_preview.png";
                        FileReadWrite.writePrivateData(activity, savePreviewPath, previewData);
                    }
                }
                
                Timber.d("Promoted history entry to save slot %d", saveSlot);
            }
        } catch (Exception e) {
            Timber.e("Error promoting history to save: %s", e.getMessage());
        }
    }

    /**
     * Update an existing history entry with new play duration and moves
     */
    public static void updateHistoryEntry(Activity activity, String mapPath, 
                                         int newDuration, int newMoves) {
        try {
            // Load existing entries
            List<GameHistoryEntry> entries = getHistoryEntries(activity);
            
            // Find and update the entry
            boolean updated = false;
            for (GameHistoryEntry entry : entries) {
                if (entry.getMapPath().equals(mapPath)) {
                    entry.setPlayDuration(newDuration);
                    entry.setMovesMade(newMoves);
                    updated = true;
                    break;
                }
            }
            
            if (updated) {
                // Save updated index
                saveHistoryIndex(activity, entries);
                Timber.d("Updated history entry: %s, duration=%d, moves=%d", 
                        mapPath, newDuration, newMoves);
            }
        } catch (Exception e) {
            Timber.e("Error updating history entry: %s", e.getMessage());
        }
    }
    
    /**
     * Update an existing history entry
     */
    public static void updateHistoryEntry(Activity activity, GameHistoryEntry updatedEntry) {
        try {
            // Load existing entries
            List<GameHistoryEntry> entries = getHistoryEntries(activity);
            
            // Find and update the entry
            boolean updated = false;
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).getMapPath().equals(updatedEntry.getMapPath())) {
                    entries.set(i, updatedEntry);
                    updated = true;
                    break;
                }
            }
            
            if (updated) {
                // Save updated index
                saveHistoryIndex(activity, entries);
                Timber.d("Updated history entry: %s", updatedEntry.getMapPath());
            }
        } catch (Exception e) {
            Timber.e("Error updating history entry: %s", e.getMessage());
        }
    }
    
    /**
     * Get a history entry by index
     */
    public static GameHistoryEntry getHistoryEntry(Activity activity, int index) {
        List<GameHistoryEntry> entries = getHistoryEntries(activity);
        if (index >= 0 && index < entries.size()) {
            return entries.get(index);
        }
        return null;
    }
    
    /**
     * Find the index of a history entry by map path
     */
    public static int getHistoryIndex(Activity activity, String mapPath) {
        List<GameHistoryEntry> entries = getHistoryEntries(activity);
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getMapPath().equals(mapPath)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Promote a history entry to a save game
     */
    public static void promoteHistoryEntryToSave(Activity activity, int historyIndex) {
        try {
            // Get the history entry
            GameHistoryEntry entry = getHistoryEntry(activity, historyIndex);
            if (entry == null) {
                Timber.e("History entry not found for index: %d", historyIndex);
                return;
            }
            
            // Get the next available save slot (start from 1 to skip autosave)
            int saveSlot = 1;
            while (FileReadWrite.privateDataFileExists(activity, SaveGameScreen.getMapPath(saveSlot))) {
                saveSlot++;
                if (saveSlot > 20) {
                    // Limit to 20 save slots, overwrite the last one
                    saveSlot = 20;
                    break;
                }
            }
            
            // Read history data
            String historyPath = entry.getMapPath();
            String historyData = FileReadWrite.readPrivateData(activity, historyPath);
            if (historyData == null || historyData.isEmpty()) {
                Timber.e("Failed to read history data: %s", historyPath);
                return;
            }
            
            // Write to save slot
            String savePath = SaveGameScreen.getMapPath(saveSlot);
            FileReadWrite.writePrivateData(activity, savePath, historyData);
            
            Timber.d("Promoted history entry %s to save slot %d", entry.getMapPath(), saveSlot);
            
            // Invalidate save cache
            SaveGameScreen.clearCachesForMap(savePath);
        } catch (Exception e) {
            Timber.e("Error promoting history entry to save: %s", e.getMessage());
        }
    }
}
