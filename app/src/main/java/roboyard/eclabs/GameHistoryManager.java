package roboyard.eclabs;

import android.app.Activity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import roboyard.logic.core.GameHistoryEntry;
import timber.log.Timber;

/**
 * Manager class for handling game history entries.
 * Provides methods for saving, loading, and managing history entries.
 */
public class GameHistoryManager {
    private static final String HISTORY_DIR = "history";
    private static final String HISTORY_INDEX_FILE = "history_index.json";
    private static final int MAX_HISTORY_ENTRIES = 11;

    /**
     * Initialize the history directory if it doesn't exist
     */
    public static void initialize(Activity activity) {
        try {
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
            
            // Check if we already have an entry with the same mapName
            boolean updated = false;
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).getMapName().equals(entry.getMapName())) {
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
            
            Timber.d("Added history entry: %s", entry.getMapPath());
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
     * @param activity the activity
     * @return the next available index
     */
    public static int getNextHistoryIndex(Activity activity) {
        List<GameHistoryEntry> entries = getHistoryEntries(activity);
        
        if (entries.isEmpty()) {
            return 0; // Start with index 0 if no entries exist
        } else {
            // Get the highest index and add 1
            int maxIndex = 0;
            for (GameHistoryEntry entry : entries) {
                if (entry.getHistoryIndex() > maxIndex) {
                    maxIndex = entry.getHistoryIndex();
                }
            }
            return maxIndex + 1;
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
        try {
            // Look for entry by its file path, not by array index
            String targetPath = indexToPath(index);
            Timber.d("Looking for history entry with path: %s", targetPath);
            
            List<GameHistoryEntry> entries = getHistoryEntries(activity);
            for (GameHistoryEntry entry : entries) {
                if (entry.getMapPath().equals(targetPath)) {
                    Timber.d("Found history entry for index %d: %s", index, entry.getMapPath());
                    return entry;
                }
            }
            
            Timber.d("No history entry found for index: %d (path: %s)", index, targetPath);
        } catch (Exception e) {
            Timber.e("Error getting history entry: %s", e.getMessage());
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
     * Konvertiert einen History-Index in einen Dateipfad
     */
    public static String indexToPath(int index) {
        return "history_" + index + ".txt";
    }
    
    /**
     * Extrahiert den Index aus einem History-Pfad
     * @return den Index oder -1 bei ungültigem Format
     */
    public static int pathToIndex(String path) {
        if (path == null || !path.startsWith("history_") || !path.endsWith(".txt")) {
            return -1;
        }
        
        try {
            return Integer.parseInt(path.substring(8, path.length() - 4));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    /**
     * Ermittelt den höchsten History-Index aller vorhandenen Einträge
     * @return den höchsten Index oder -1 wenn keine Einträge vorhanden
     */
    public static int getHighestHistoryIndex(Activity activity) {
        int highestIndex = -1;
        List<GameHistoryEntry> entries = getHistoryEntries(activity);
        
        for (GameHistoryEntry entry : entries) {
            String path = entry.getMapPath();
            int index = pathToIndex(path);
            if (index > highestIndex) {
                highestIndex = index;
            }
        }
        
        return highestIndex;
    }

    /**
     * Delete a history entry by path
     * @param activity The activity context
     * @param mapPath The map path to delete
     * @return true if the history entry was deleted successfully
     */
    public static boolean deleteHistoryEntry(Activity activity, String mapPath) {
        try {
            Timber.d("[HISTORY_DELETE] Attempting to delete history entry: %s", mapPath);
            
            // Initialize if needed
            initialize(activity);
            
            // Extract file name from path if it's a full path
            String fileName = mapPath;
            if (mapPath.contains("/")) {
                fileName = mapPath.substring(mapPath.lastIndexOf("/") + 1);
            }
            
            // Load existing history entries
            List<GameHistoryEntry> historyEntries = getHistoryEntries(activity);
            if (historyEntries == null) {
                Timber.e("[HISTORY_DELETE] Failed to load history entries");
                return false;
            }
            
            Timber.d("[HISTORY_DELETE] Loaded %d history entries, looking for: '%s' (fileName: '%s')", historyEntries.size(), mapPath, fileName);
            for (int i = 0; i < historyEntries.size(); i++) {
                Timber.d("[HISTORY_DELETE] Entry %d: mapPath='%s'", i, historyEntries.get(i).getMapPath());
            }
            
            // Find the entry to delete
            GameHistoryEntry entryToDelete = null;
            for (GameHistoryEntry entry : historyEntries) {
                String entryPath = entry.getMapPath();
                String entryFileName = entryPath;
                if (entryPath.contains("/")) {
                    entryFileName = entryPath.substring(entryPath.lastIndexOf("/") + 1);
                }
                
                // Match by either full path or just filename
                if (entryPath.equals(mapPath) || entryFileName.equals(fileName)) {
                    entryToDelete = entry;
                    Timber.d("Found entry to delete: %s", entry.getMapName());
                    break;
                }
            }
            
            if (entryToDelete == null) {
                Timber.e("History entry not found for path: %s", mapPath);
                return false;
            }
            
            // Remove the entry from the list
            historyEntries.remove(entryToDelete);
            
            // Delete the actual file - try both the given path and the path from the entry
            boolean fileDeleted = false;
            
            // Try with the mapPath parameter
            File historyFile = new File(mapPath);
            if (historyFile.exists()) {
                fileDeleted = historyFile.delete();
                if (fileDeleted) {
                    Timber.d("Deleted history file at path: %s", mapPath);
                } else {
                    Timber.e("Failed to delete history file: %s", mapPath);
                }
            }
            
            // If that didn't work, try with the entry's path
            if (!fileDeleted && !entryToDelete.getMapPath().equals(mapPath)) {
                File entryFile = new File(entryToDelete.getMapPath());
                if (entryFile.exists()) {
                    fileDeleted = entryFile.delete();
                    if (fileDeleted) {
                        Timber.d("Deleted history file at entry path: %s", entryToDelete.getMapPath());
                    } else {
                        Timber.e("Failed to delete history file at entry path: %s", entryToDelete.getMapPath());
                    }
                }
            }
            
            // Try with the file in the history directory
            if (!fileDeleted) {
                File historyDir = new File(activity.getFilesDir(), HISTORY_DIR);
                File fileInHistoryDir = new File(historyDir, fileName);
                if (fileInHistoryDir.exists()) {
                    fileDeleted = fileInHistoryDir.delete();
                    if (fileDeleted) {
                        Timber.d("Deleted history file in history directory: %s", fileInHistoryDir.getAbsolutePath());
                    } else {
                        Timber.e("Failed to delete history file in history directory: %s", fileInHistoryDir.getAbsolutePath());
                    }
                }
            }
            
            // Update the history index regardless of file deletion success
            boolean indexUpdated = saveHistoryIndex(activity, historyEntries);
            if (!indexUpdated) {
                Timber.e("Failed to update history index after deletion");
                return false;
            }
            
            Timber.d("Successfully deleted history entry: %s", mapPath);
            return true;
        } catch (Exception e) {
            Timber.e("Error deleting history entry: %s - %s", mapPath, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
