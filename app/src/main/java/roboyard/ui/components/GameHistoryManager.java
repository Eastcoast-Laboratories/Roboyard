package roboyard.ui.components;

import android.app.Activity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import roboyard.logic.core.Constants;
import roboyard.logic.core.GameHistoryEntry;
import roboyard.logic.core.GameState;
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
                        int countBefore = existing.getCompletionCount();
                        if (entry.getMovesMade() > 0) {
                            existing.recordCompletion(entry.getPlayDuration(), entry.getMovesMade());
                            Timber.d("[HISTORY_FLOW] addHistoryEntry(existing): recordCompletion called, movesMade=%d, countBefore=%d, countAfter=%d",
                                    entry.getMovesMade(), countBefore, existing.getCompletionCount());
                        } else {
                            Timber.d("[HISTORY_FLOW] addHistoryEntry(existing): movesMade=0, skipping recordCompletion, count stays %d", countBefore);
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
                        // lastSolvedWithoutHints / lastPerfectlySolvedWithoutHints:
                        // Only update if new entry has a more recent no-hints solve
                        if (entry.getLastSolvedWithoutHints() > existing.getLastSolvedWithoutHints()) {
                            existing.setLastSolvedWithoutHints(entry.getLastSolvedWithoutHints());
                        }
                        if (entry.getLastPerfectlySolvedWithoutHints() > existing.getLastPerfectlySolvedWithoutHints()) {
                            existing.setLastPerfectlySolvedWithoutHints(entry.getLastPerfectlySolvedWithoutHints());
                        }
                        
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
                // If game was completed (movesMade > 0) AND completionCount is still 0 (not pre-set from server restore),
                // record the completion on the new entry
                if (entry.getMovesMade() > 0 && entry.getCompletionCount() == 0) {
                    entry.recordCompletion(entry.getPlayDuration(), entry.getMovesMade());
                    Timber.d("[HISTORY_FLOW] addHistoryEntry(new): recordCompletion called on new entry, movesMade=%d, countAfter=%d",
                            entry.getMovesMade(), entry.getCompletionCount());
                } else if (entry.getCompletionCount() > 0) {
                    Timber.d("[HISTORY_FLOW] addHistoryEntry(new): completionCount already set to %d (server restore), skipping recordCompletion",
                            entry.getCompletionCount());
                } else {
                    Timber.d("[HISTORY_FLOW] addHistoryEntry(new): movesMade=0, new entry added with completionCount=0");
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
        boolean anyMigrated = false;
        try {
            String indexJson = FileReadWrite.readPrivateData(activity, HISTORY_INDEX_FILE);
            
            if (indexJson != null && !indexJson.isEmpty()) {
                JSONArray entriesArray;
                
                // Handle both formats: direct array [...] and wrapped object {"historyEntries":[...]}
                if (indexJson.trim().startsWith("{")) {
                    // Wrapped format - extract the array
                    JSONObject wrapperObject = new JSONObject(indexJson);
                    entriesArray = wrapperObject.getJSONArray("historyEntries");
                } else {
                    // Direct array format
                    entriesArray = new JSONArray(indexJson);
                }
                
                for (int i = 0; i < entriesArray.length(); i++) {
                    JSONObject entryJson = entriesArray.getJSONObject(i);
                    GameHistoryEntry entry = new GameHistoryEntry();
                    
                    // MIGRATION: Remove "history/" prefix from old entries (Android doesn't allow path separators in filenames)
                    String mapPath = entryJson.getString("mapPath");
                    if (mapPath.startsWith("history/")) {
                        mapPath = mapPath.substring(8); // Remove "history/" prefix
                        Timber.d("[HISTORY_MIGRATION] Removed 'history/' prefix from mapPath: %s", mapPath);
                        anyMigrated = true;
                    }
                    entry.setMapPath(mapPath);
                    entry.setMapName(entryJson.optString("mapName", "Unnamed"));
                    entry.setTimestamp(entryJson.getLong("timestamp"));
                    entry.setPlayDuration(entryJson.getInt("playDuration"));
                    entry.setMovesMade(entryJson.getInt("movesMade"));
                    entry.setOptimalMoves(entryJson.optInt("optimalMoves", 0));
                    entry.setBoardSize(entryJson.optString("boardSize", ""));
                    entry.setPreviewImagePath(entryJson.optString("previewImagePath", ""));
                    
                    // Load difficulty - support both int (new) and string (legacy migration)
                    int difficultyId = Constants.DIFFICULTY_BEGINNER; // default
                    if (entryJson.has("difficulty")) {
                        Object diffValue = entryJson.get("difficulty");
                        if (diffValue instanceof Integer) {
                            difficultyId = (Integer) diffValue;
                        } else if (diffValue instanceof String) {
                            // Migration: convert old string values to int
                            String diffStr = (String) diffValue;
                            difficultyId = migrateDifficultyStringToInt(diffStr);
                            Timber.d("[HISTORY_MIGRATION] Converted difficulty '%s' to %d", diffStr, difficultyId);
                        }
                    }
                    entry.setDifficulty(difficultyId);
                    
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
                    
                    // Load stars earned
                    entry.setStarsEarned(entryJson.optInt("starsEarned", 0));
                    
                    // Load hint tracking fields
                    entry.setMaxHintUsed(entryJson.optInt("maxHintUsed", -1));
                    entry.setSolvedWithoutHints(entryJson.optBoolean("solvedWithoutHints", false));
                    entry.setEverUsedHints(entryJson.optBoolean("everUsedHints", false));
                    // Load no-hints timestamp fields (0 = never solved without hints)
                    entry.setLastSolvedWithoutHints(entryJson.optLong("lastSolvedWithoutHints", 0));
                    entry.setLastPerfectlySolvedWithoutHints(entryJson.optLong("lastPerfectlySolvedWithoutHints", 0));
                    
                    // MIGRATION: If mapSignature is missing, compute it from the saved game file
                    if (entry.getMapSignature() == null || entry.getMapSignature().isEmpty()) {
                        computeAndSetMapSignature(activity, entry);
                        anyMigrated = true;
                    }
                    
                    entries.add(entry);
                }
                
                // Save index if any entries were migrated (to persist the computed signatures or removed prefixes)
                if (anyMigrated) {
                    saveHistoryIndex(activity, entries);
                    Timber.d("[HISTORY_MIGRATION] Saved migrated entries (signatures or removed 'history/' prefix) to index");
                }
            }
        } catch (Exception e) {
            Timber.e("Error loading history entries: %s", e.getMessage());
        }
        return entries;
    }

    /**
     * Compute and set mapSignature for a history entry by loading its saved game file.
     * This is used for migration of old entries that don't have mapSignature stored.
     * @param activity The activity context
     * @param entry The history entry to update
     */
    private static void computeAndSetMapSignature(Activity activity, GameHistoryEntry entry) {
        try {
            // Read the save data from the history file
            File historyFile = new File(entry.getMapPath());
            if (!historyFile.isAbsolute()) {
                historyFile = activity.getFileStreamPath(entry.getMapPath());
            }
            if (!historyFile.exists()) {
                Timber.w("[HISTORY_MIGRATION] Cannot compute mapSignature: file not found: %s", entry.getMapPath());
                return;
            }
            
            StringBuilder saveData = new StringBuilder();
            try (java.io.FileInputStream fis = new java.io.FileInputStream(historyFile);
                 java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(fis))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    saveData.append(line).append("\n");
                }
            }
            
            // Parse the save data to get the GameState
            GameState state = GameState.parseFromSaveData(saveData.toString(), activity.getApplication());
            if (state != null) {
                // Compute signatures from the loaded state
                String wallSig = state.generateWallSignature();
                String posSig = state.generatePositionSignature();
                String mapSig = state.generateMapSignature();
                
                entry.setWallSignature(wallSig);
                entry.setPositionSignature(posSig);
                entry.setMapSignature(mapSig);
                
                Timber.d("[HISTORY_MIGRATION] Computed mapSignature for '%s': %s", entry.getMapName(), mapSig);
            } else {
                Timber.w("[HISTORY_MIGRATION] Failed to parse GameState for: %s", entry.getMapPath());
            }
        } catch (Exception e) {
            Timber.e(e, "[HISTORY_MIGRATION] Error computing mapSignature for: %s", entry.getMapPath());
        }
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
                
                // Save stars earned
                entryJson.put("starsEarned", entry.getStarsEarned());
                
                // Save hint tracking fields
                entryJson.put("maxHintUsed", entry.getMaxHintUsed());
                entryJson.put("solvedWithoutHints", entry.isSolvedWithoutHints());
                entryJson.put("everUsedHints", entry.isEverUsedHints());
                entryJson.put("lastSolvedWithoutHints", entry.getLastSolvedWithoutHints());
                entryJson.put("lastPerfectlySolvedWithoutHints", entry.getLastPerfectlySolvedWithoutHints());
                // Save difficulty as int ID (0-3), not localized string
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
        // An entry is created on the first move (before completion), so we check
        // completionCount == 0 to distinguish "started but not yet completed" from "already completed before".
        return existing == null || existing.getCompletionCount() == 0;
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
     * Get the total count of unique completed levels from history.
     * Only entries with map names like "Level N" or matching level file paths are counted.
     * @param activity The activity context
     * @return Number of unique completed levels in history
     */
    public static int getUniqueCompletedLevelCount(Activity activity) {
        List<GameHistoryEntry> entries = getHistoryEntries(activity);
        java.util.Set<String> uniqueLevelKeys = new java.util.HashSet<>();

        for (GameHistoryEntry entry : entries) {
            String levelKey = extractLevelKey(entry);
            if (levelKey != null) {
                uniqueLevelKeys.add(levelKey);
            }
        }
        Timber.d("[GAME_HISTORY][ACHIEVEMENTS][LEVEL] getUniqueCompletedLevelCount: Found %d unique levels", uniqueLevelKeys.size());
        return uniqueLevelKeys.size();
    }

    /**
     * Get the total count of unique completed levels that earned at least three stars.
     * @param activity The activity context
     * @return Number of unique 3-star levels in history
     */
    public static int getUniqueThreeStarLevelCount(Activity activity) {
        List<GameHistoryEntry> entries = getHistoryEntries(activity);
        java.util.Set<String> uniqueLevelKeys = new java.util.HashSet<>();

        for (GameHistoryEntry entry : entries) {
            if (entry.getStarsEarned() < 3) {
                continue;
            }

            String levelKey = extractLevelKey(entry);
            if (levelKey != null) {
                uniqueLevelKeys.add(levelKey);
            }
        }

        Timber.d("[GAME_HISTORY][ACHIEVEMENTS][LEVEL] getUniqueThreeStarLevelCount: Found %d unique 3-star levels", uniqueLevelKeys.size());
        return uniqueLevelKeys.size();
    }

    private static String extractLevelKey(GameHistoryEntry entry) {
        String mapName = entry.getMapName();
        if (mapName != null && mapName.matches("(?i)Level \\d+")) {
            int id = Integer.parseInt(mapName.trim().split("\\s+")[1]);
            
            String levelKey = id >= 141 ? "custom_level_" + id : "level_" + id;
            // Timber.d("[GAME_HISTORY][ACHIEVEMENTS][LEVEL] extractLevelKey: Found level key for entry: %s", levelKey);
            return levelKey;
        }

        String mapPath = entry.getMapPath();
        if (mapPath != null) {
            String base = mapPath.contains("/")
                    ? mapPath.substring(mapPath.lastIndexOf('/') + 1)
                    : mapPath;
            if (base.startsWith("level_") || base.startsWith("custom_level_")) {
                // Timber.d("[GAME_HISTORY][ACHIEVEMENTS][LEVEL] extractLevelKey: Found level key for entry: %s", base);
                return base.endsWith(".txt") ? base.substring(0, base.length() - 4) : base;
            }
        }
        // Timber.d("[GAME_HISTORY][ACHIEVEMENTS][LEVEL] extractLevelKey: No level key found for entry: %s", entry);
        return null;
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
    
    /**
     * Migrate old string difficulty values to int IDs.
     * Supports both English and German localized strings.
     * @param difficultyStr The old string difficulty value
     * @return The corresponding difficulty ID (0-3)
     */
    private static int migrateDifficultyStringToInt(String difficultyStr) {
        if (difficultyStr == null || difficultyStr.isEmpty()) {
            return Constants.DIFFICULTY_BEGINNER;
        }
        
        String lower = difficultyStr.toLowerCase().trim();
        
        // English strings
        if (lower.contains("beginner") || lower.contains("easy")) {
            return Constants.DIFFICULTY_BEGINNER;
        } else if (lower.contains("intermediate") || lower.contains("advanced") || lower.contains("medium")) {
            return Constants.DIFFICULTY_ADVANCED;
        } else if (lower.contains("insane") || lower.contains("hard")) {
            return Constants.DIFFICULTY_INSANE;
        } else if (lower.contains("impossible") || lower.contains("expert")) {
            return Constants.DIFFICULTY_IMPOSSIBLE;
        }
        
        // German strings (Anfänger, Fortgeschritten, verrückt, Unmöglich)
        if (lower.substring(0, 3).equals("anf")) {
            return Constants.DIFFICULTY_BEGINNER;
        } else if (lower.substring(0, 3).equals("for")) {
            return Constants.DIFFICULTY_ADVANCED;
        } else if (lower.substring(0, 3).equals("ver")) {
            return Constants.DIFFICULTY_INSANE;
        } else if (lower.substring(0, 3).equals("unm")) {
            return Constants.DIFFICULTY_IMPOSSIBLE;
        }
        
        // Default fallback
        Timber.w("[HISTORY_MIGRATION] Unknown difficulty string: '%s', defaulting to BEGINNER", difficultyStr);
        return Constants.DIFFICULTY_BEGINNER;
    }
}
