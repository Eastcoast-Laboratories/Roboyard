package roboyard.ui.components;

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
    // Maps are never deleted - kept forever for unique map tracking

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
     * Add a new history entry or update existing one if map already exists.
     * Maps are identified by their mapSignature (unique combination of walls + positions).
     * Entries are never deleted - only updated with new completion data.
     *
     * @return true if entry was added/updated successfully
     */
    public static Boolean addHistoryEntry(Activity activity, GameHistoryEntry entry) {
        try {
            // Load existing entries
            List<GameHistoryEntry> entries = getHistoryEntries(activity);
            
            // Check if we already have an entry with the same mapSignature
            boolean updated = false;
            String newMapSignature = entry.getMapSignature();
            
            if (newMapSignature != null && !newMapSignature.isEmpty()) {
                for (int i = 0; i < entries.size(); i++) {
                    GameHistoryEntry existing = entries.get(i);
                    if (newMapSignature.equals(existing.getMapSignature())) {
                        // Same map found - only record completion if moves > 0 (game was actually played)
                        // Don't record completion for intermediate saves (e.g., when hints are shown)
                        if (entry.getMovesMade() > 0) {
                            existing.recordCompletion(entry.getPlayDuration(), entry.getMovesMade());
                        }
                        if (entry.getOptimalMoves() > 0) {
                            existing.setOptimalMoves(entry.getOptimalMoves());
                        }
                        // Merge hint tracking - once hints used, permanently marked
                        // Update maxHintUsed to the higher value (more hints = worse)
                        if (entry.getMaxHintUsed() > existing.getMaxHintUsed()) {
                            existing.setMaxHintUsed(entry.getMaxHintUsed());
                        }
                        // everUsedHints is cumulative: true if hints used in ANY attempt
                        if (entry.getMaxHintUsed() >= 0 || entry.isEverUsedHints()) {
                            existing.markEverUsedHints();
                        }
                        // solvedWithoutHints stays true only if FIRST completion was without hints
                        // Don't update it on subsequent completions
                        
                        // Log optimal solution achievement with full hint history
                        int optMoves = existing.getOptimalMoves() > 0 ? existing.getOptimalMoves() : entry.getOptimalMoves();
                        boolean isOptimal = optMoves > 0 && entry.getMovesMade() == optMoves;
                        if (isOptimal) {
                            boolean neverHints = !existing.isEverUsedHints();
                            Timber.d("[HISTORY] OPTIMAL SOLUTION on completion #%d: map=%s, moves=%d, " +
                                    "everUsedHints=%b, solvedWithoutHints=%b, qualifiesNoHints=%b",
                                    existing.getCompletionCount(), existing.getMapPath(),
                                    entry.getMovesMade(), existing.isEverUsedHints(),
                                    existing.isSolvedWithoutHints(), neverHints);
                        }
                        
                        updated = true;
                        Timber.d("[HISTORY] Updated existing map (completion #%d): %s, maxHintUsed=%d, everUsedHints=%b", 
                                existing.getCompletionCount(), existing.getMapPath(),
                                existing.getMaxHintUsed(), existing.isEverUsedHints());
                        break;
                    }
                }
            }
            
            // Fallback: check by mapName (legacy entries)
            if (!updated) {
                for (int i = 0; i < entries.size(); i++) {
                    if (entries.get(i).getMapName().equals(entry.getMapName())) {
                        GameHistoryEntry existing = entries.get(i);
                        // Only record completion if moves > 0 (game was actually played)
                        if (entry.getMovesMade() > 0) {
                            existing.recordCompletion(entry.getPlayDuration(), entry.getMovesMade());
                        }
                        updated = true;
                        break;
                    }
                }
            }
            
            // Add new entry if not updated
            if (!updated) {
                // If game was completed (movesMade > 0), record the completion on the new entry
                if (entry.getMovesMade() > 0) {
                    entry.recordCompletion(entry.getPlayDuration(), entry.getMovesMade());
                }
                entries.add(entry);
            }
            
            // Sort by lastCompletionTimestamp (most recently played first)
            Collections.sort(entries, new Comparator<GameHistoryEntry>() {
                @Override
                public int compare(GameHistoryEntry o1, GameHistoryEntry o2) {
                    long t1 = o1.getLastCompletionTimestamp() > 0 ? o1.getLastCompletionTimestamp() : o1.getTimestamp();
                    long t2 = o2.getLastCompletionTimestamp() > 0 ? o2.getLastCompletionTimestamp() : o2.getTimestamp();
                    return Long.compare(t2, t1);
                }
            });
            
            // No trimming - maps are kept forever for unique map tracking
            
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
                    
                    // Load difficulty
                    entry.setDifficulty(entryJson.optString("difficulty", ""));
                    
                    // Load new fields for unique map tracking
                    entry.setCompletionCount(entryJson.optInt("completionCount", 0));
                    entry.setLastCompletionTimestamp(entryJson.optLong("lastCompletionTimestamp", entry.getTimestamp()));
                    entry.setBestTime(entryJson.optInt("bestTime", entry.getPlayDuration()));
                    entry.setBestMoves(entryJson.optInt("bestMoves", entry.getMovesMade()));
                    entry.setWallSignature(entryJson.optString("wallSignature", null));
                    entry.setPositionSignature(entryJson.optString("positionSignature", null));
                    entry.setMapSignature(entryJson.optString("mapSignature", null));
                    
                    // Load completion timestamps array
                    if (entryJson.has("completionTimestamps")) {
                        JSONArray timestamps = entryJson.getJSONArray("completionTimestamps");
                        List<Long> completionTimestamps = new ArrayList<>();
                        for (int j = 0; j < timestamps.length(); j++) {
                            completionTimestamps.add(timestamps.getLong(j));
                        }
                        entry.setCompletionTimestamps(completionTimestamps);
                    } else {
                        // Legacy entry - create list with single timestamp
                        List<Long> completionTimestamps = new ArrayList<>();
                        completionTimestamps.add(entry.getTimestamp());
                        entry.setCompletionTimestamps(completionTimestamps);
                    }
                    
                    // Load hint tracking fields
                    entry.setMaxHintUsed(entryJson.optInt("maxHintUsed", -1));
                    entry.setSolvedWithoutHints(entryJson.optBoolean("solvedWithoutHints", false));
                    // everUsedHints: fallback to maxHintUsed>=0 for legacy entries
                    boolean legacyEverUsedHints = entry.getMaxHintUsed() >= 0;
                    entry.setEverUsedHints(entryJson.optBoolean("everUsedHints", legacyEverUsedHints));
                    
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
    public static Boolean saveHistoryIndex(Activity activity, List<GameHistoryEntry> entries) {
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
                
                // Save new fields for unique map tracking
                entryJson.put("completionCount", entry.getCompletionCount());
                entryJson.put("lastCompletionTimestamp", entry.getLastCompletionTimestamp());
                entryJson.put("bestTime", entry.getBestTime());
                entryJson.put("bestMoves", entry.getBestMoves());
                if (entry.getWallSignature() != null) {
                    entryJson.put("wallSignature", entry.getWallSignature());
                }
                if (entry.getPositionSignature() != null) {
                    entryJson.put("positionSignature", entry.getPositionSignature());
                }
                if (entry.getMapSignature() != null) {
                    entryJson.put("mapSignature", entry.getMapSignature());
                }
                
                // Save completion timestamps array
                JSONArray timestamps = new JSONArray();
                if (entry.getCompletionTimestamps() != null) {
                    for (Long ts : entry.getCompletionTimestamps()) {
                        timestamps.put(ts);
                    }
                }
                entryJson.put("completionTimestamps", timestamps);
                
                // Save hint tracking fields
                entryJson.put("maxHintUsed", entry.getMaxHintUsed());
                entryJson.put("solvedWithoutHints", entry.isSolvedWithoutHints());
                entryJson.put("everUsedHints", entry.isEverUsedHints());
                entryJson.put("difficulty", entry.getDifficulty());
                
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
    
    // ========== Unique Map Tracking Methods ==========
    
    /**
     * Check if a map with the given signature is being completed for the first time.
     * @param activity The activity context
     * @param mapSignature The unique map signature to check
     * @return true if this map has never been completed before
     */
    public static boolean isFirstCompletion(Activity activity, String mapSignature) {
        if (mapSignature == null || mapSignature.isEmpty()) {
            return true; // No signature = treat as new
        }
        GameHistoryEntry existing = findByMapSignature(activity, mapSignature);
        return existing == null;
    }
    
    /**
     * Find a history entry by its map signature.
     * @param activity The activity context
     * @param mapSignature The unique map signature to find
     * @return The matching entry, or null if not found
     */
    public static GameHistoryEntry findByMapSignature(Activity activity, String mapSignature) {
        if (mapSignature == null || mapSignature.isEmpty()) {
            return null;
        }
        List<GameHistoryEntry> entries = getHistoryEntries(activity);
        for (GameHistoryEntry entry : entries) {
            if (mapSignature.equals(entry.getMapSignature())) {
                return entry;
            }
        }
        return null;
    }
    
    /**
     * Find all history entries with the same wall signature (same walls, different positions).
     * @param activity The activity context
     * @param wallSignature The wall signature to match
     * @return List of entries with matching wall layout
     */
    public static List<GameHistoryEntry> findByWallSignature(Activity activity, String wallSignature) {
        List<GameHistoryEntry> result = new ArrayList<>();
        if (wallSignature == null || wallSignature.isEmpty()) {
            return result;
        }
        List<GameHistoryEntry> entries = getHistoryEntries(activity);
        for (GameHistoryEntry entry : entries) {
            if (wallSignature.equals(entry.getWallSignature())) {
                result.add(entry);
            }
        }
        return result;
    }
    
    /**
     * Get the total count of unique maps completed.
     * @param activity The activity context
     * @return Number of unique maps in history
     */
    public static int getUniqueMapCount(Activity activity) {
        return getHistoryEntries(activity).size();
    }
    
    /**
     * Get the completion count for a specific map.
     * @param activity The activity context
     * @param mapSignature The map signature to check
     * @return Number of times this map was completed, or 0 if never
     */
    public static int getCompletionCount(Activity activity, String mapSignature) {
        GameHistoryEntry entry = findByMapSignature(activity, mapSignature);
        return entry != null ? entry.getCompletionCount() : 0;
    }
}
