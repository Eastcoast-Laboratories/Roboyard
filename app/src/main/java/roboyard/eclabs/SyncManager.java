package roboyard.eclabs;

import android.app.Activity;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import roboyard.logic.core.Constants;
import roboyard.logic.core.GameHistoryEntry;
import timber.log.Timber;

/**
 * Central sync manager for uploading/downloading save games and history to/from server.
 * Handles bidirectional sync on login and periodic uploads.
 */
public class SyncManager {
    
    private static SyncManager instance;
    private final Context context;
    
    private SyncManager(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public static synchronized SyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new SyncManager(context);
        }
        return instance;
    }
    
    // ========== SAVE GAMES ==========
    
    /**
     * Upload all local save games to server.
     */
    public void uploadSaveGames() {
        RoboyardApiClient apiClient = RoboyardApiClient.getInstance(context);
        if (!apiClient.isLoggedIn()) {
            Timber.d("[SAVE_SYNC] Not logged in, skipping save game upload");
            return;
        }
        
        try {
            File saveDir = new File(context.getFilesDir(), Constants.SAVE_DIRECTORY);
            if (!saveDir.exists()) {
                Timber.d("[SAVE_SYNC] No save directory, nothing to upload");
                return;
            }
            
            JSONArray savesArray = new JSONArray();
            File[] saveFiles = saveDir.listFiles();
            if (saveFiles == null) return;
            
            for (File saveFile : saveFiles) {
                if (!saveFile.getName().startsWith(Constants.SAVE_FILENAME_PREFIX)) continue;
                
                try {
                    // Extract slot ID from filename (save_X.dat)
                    String name = saveFile.getName();
                    String idStr = name.replace(Constants.SAVE_FILENAME_PREFIX, "")
                                       .replace(Constants.SAVE_FILENAME_EXTENSION, "");
                    int slotId = Integer.parseInt(idStr);
                    
                    // Read save data
                    String saveData = readFileContent(saveFile);
                    if (saveData == null || saveData.isEmpty()) continue;
                    
                    JSONObject saveJson = new JSONObject();
                    saveJson.put("slot_id", slotId);
                    saveJson.put("save_data", saveData);
                    saveJson.put("map_name", extractMapName(saveData));
                    saveJson.put("board_width", extractBoardWidth(saveData));
                    saveJson.put("board_height", extractBoardHeight(saveData));
                    saveJson.put("is_solved", saveData.contains("SOLVED:true"));
                    
                    savesArray.put(saveJson);
                } catch (Exception e) {
                    Timber.e(e, "[SAVE_SYNC] Error reading save file: %s", saveFile.getName());
                }
            }
            
            if (savesArray.length() == 0) {
                Timber.d("[SAVE_SYNC] No save games to upload");
                return;
            }
            
            Timber.d("[SAVE_SYNC] Uploading %d save games to server", savesArray.length());
            apiClient.syncSaveGames(savesArray, new RoboyardApiClient.ApiCallback<Integer>() {
                @Override
                public void onSuccess(Integer syncedCount) {
                    Timber.d("[SAVE_SYNC] Upload complete: %d save games synced", syncedCount);
                }
                
                @Override
                public void onError(String error) {
                    Timber.e("[SAVE_SYNC] Upload failed: %s", error);
                }
            });
            
        } catch (Exception e) {
            Timber.e(e, "[SAVE_SYNC] Error uploading save games");
        }
    }
    
    /**
     * Download save games from server and write to local storage.
     */
    public void downloadSaveGames(RoboyardApiClient.ApiCallback<Integer> callback) {
        RoboyardApiClient apiClient = RoboyardApiClient.getInstance(context);
        if (!apiClient.isLoggedIn()) {
            Timber.d("[SAVE_SYNC] Not logged in, skipping save game download");
            if (callback != null) callback.onError("Not logged in");
            return;
        }
        
        apiClient.fetchSaveGames(new RoboyardApiClient.ApiCallback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray saves) {
                int restoredCount = 0;
                
                try {
                    File saveDir = new File(context.getFilesDir(), Constants.SAVE_DIRECTORY);
                    if (!saveDir.exists()) {
                        saveDir.mkdirs();
                    }
                    
                    for (int i = 0; i < saves.length(); i++) {
                        JSONObject save = saves.getJSONObject(i);
                        int slotId = save.getInt("slot_id");
                        String saveData = save.getString("save_data");
                        
                        String fileName = Constants.SAVE_FILENAME_PREFIX + slotId + Constants.SAVE_FILENAME_EXTENSION;
                        File saveFile = new File(saveDir, fileName);
                        
                        // Only download if local file doesn't exist (don't overwrite local saves)
                        if (!saveFile.exists()) {
                            try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                                fos.write(saveData.getBytes(StandardCharsets.UTF_8));
                                restoredCount++;
                                Timber.d("[SAVE_SYNC] Restored save game slot %d from server", slotId);
                            }
                        } else {
                            Timber.d("[SAVE_SYNC] Skipping slot %d - local save exists", slotId);
                        }
                    }
                    
                    Timber.d("[SAVE_SYNC] Download complete: %d save games restored", restoredCount);
                    
                } catch (JSONException | IOException e) {
                    Timber.e(e, "[SAVE_SYNC] Error restoring save games");
                    if (callback != null) callback.onError("Error restoring saves: " + e.getMessage());
                    return;
                }
                
                if (callback != null) callback.onSuccess(restoredCount);
            }
            
            @Override
            public void onError(String error) {
                Timber.e("[SAVE_SYNC] Download failed: %s", error);
                if (callback != null) callback.onError(error);
            }
        });
    }
    
    // ========== GAME HISTORY ==========
    
    /**
     * Upload all local history entries to server.
     */
    public void uploadHistory(Activity activity) {
        RoboyardApiClient apiClient = RoboyardApiClient.getInstance(context);
        if (!apiClient.isLoggedIn()) {
            Timber.d("[HISTORY_SYNC] Not logged in, skipping history upload");
            return;
        }
        
        try {
            List<GameHistoryEntry> entries = GameHistoryManager.getHistoryEntries(activity);
            if (entries.isEmpty()) {
                Timber.d("[HISTORY_SYNC] No history entries to upload");
                return;
            }
            
            JSONArray historyArray = new JSONArray();
            
            for (GameHistoryEntry entry : entries) {
                // Read the actual map data from the history file
                String mapData = readHistoryFileData(activity, entry);
                if (mapData == null || mapData.isEmpty()) continue;
                
                JSONObject historyJson = new JSONObject();
                historyJson.put("map_name", entry.getMapName());
                historyJson.put("save_data", mapData);
                historyJson.put("board_width", extractBoardWidthFromSize(entry.getBoardSize()));
                historyJson.put("board_height", extractBoardHeightFromSize(entry.getBoardSize()));
                historyJson.put("move_count", entry.getMovesMade());
                historyJson.put("is_solved", entry.getMovesMade() > 0);
                historyJson.put("play_time_seconds", entry.getPlayDuration());
                historyJson.put("played_at", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US).format(new java.util.Date(entry.getTimestamp())));
                
                historyArray.put(historyJson);
            }
            
            if (historyArray.length() == 0) {
                Timber.d("[HISTORY_SYNC] No valid history entries to upload");
                return;
            }
            
            Timber.d("[HISTORY_SYNC] Uploading %d history entries to server", historyArray.length());
            apiClient.syncHistory(historyArray, new RoboyardApiClient.ApiCallback<Integer>() {
                @Override
                public void onSuccess(Integer syncedCount) {
                    Timber.d("[HISTORY_SYNC] Upload complete: %d history entries synced", syncedCount);
                }
                
                @Override
                public void onError(String error) {
                    Timber.e("[HISTORY_SYNC] Upload failed: %s", error);
                }
            });
            
        } catch (Exception e) {
            Timber.e(e, "[HISTORY_SYNC] Error uploading history");
        }
    }
    
    /**
     * Download history entries from server and write to local storage.
     */
    public void downloadHistory(Activity activity, RoboyardApiClient.ApiCallback<Integer> callback) {
        RoboyardApiClient apiClient = RoboyardApiClient.getInstance(context);
        if (!apiClient.isLoggedIn()) {
            Timber.d("[HISTORY_SYNC] Not logged in, skipping history download");
            if (callback != null) callback.onError("Not logged in");
            return;
        }
        
        apiClient.fetchHistory(new RoboyardApiClient.ApiCallback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray history) {
                int restoredCount = 0;
                
                try {
                    GameHistoryManager.initialize(activity);
                    List<GameHistoryEntry> existingEntries = GameHistoryManager.getHistoryEntries(activity);
                    
                    for (int i = 0; i < history.length(); i++) {
                        JSONObject entry = history.getJSONObject(i);
                        String mapName = entry.optString("map_name", "Unnamed");
                        String saveData = entry.getString("save_data");
                        
                        // Check if we already have this entry locally (by map name)
                        boolean exists = false;
                        for (GameHistoryEntry existing : existingEntries) {
                            if (existing.getMapName().equals(mapName)) {
                                exists = true;
                                break;
                            }
                        }
                        
                        if (!exists) {
                            // Save the map data to a history file
                            int nextIndex = GameHistoryManager.getNextHistoryIndex(activity);
                            String historyPath = GameHistoryManager.indexToPath(nextIndex);
                            
                            FileReadWrite.writePrivateData(activity, historyPath, saveData);
                            
                            // Create a history entry
                            GameHistoryEntry historyEntry = new GameHistoryEntry();
                            historyEntry.setMapPath(historyPath);
                            historyEntry.setMapName(mapName);
                            historyEntry.setTimestamp(parseTimestamp(entry.optString("played_at", null)));
                            historyEntry.setPlayDuration(entry.optInt("play_time_seconds", 0));
                            historyEntry.setMovesMade(entry.optInt("move_count", 0));
                            historyEntry.setOptimalMoves(0);
                            historyEntry.setBoardSize(entry.optInt("board_width", 12) + "x" + entry.optInt("board_height", 12));
                            historyEntry.setPreviewImagePath("");
                            
                            GameHistoryManager.addHistoryEntry(activity, historyEntry);
                            restoredCount++;
                            
                            Timber.d("[HISTORY_SYNC] Restored history entry: %s", mapName);
                        }
                    }
                    
                    Timber.d("[HISTORY_SYNC] Download complete: %d history entries restored", restoredCount);
                    
                } catch (Exception e) {
                    Timber.e(e, "[HISTORY_SYNC] Error restoring history");
                    if (callback != null) callback.onError("Error restoring history: " + e.getMessage());
                    return;
                }
                
                if (callback != null) callback.onSuccess(restoredCount);
            }
            
            @Override
            public void onError(String error) {
                Timber.e("[HISTORY_SYNC] Download failed: %s", error);
                if (callback != null) callback.onError(error);
            }
        });
    }
    
    // ========== FULL SYNC ON LOGIN ==========
    
    /**
     * Perform full sync after login: download everything from server, then upload local data.
     */
    public void fullSyncOnLogin(Activity activity, RoboyardApiClient.ApiCallback<String> callback) {
        Timber.d("[FULL_SYNC] Starting full sync after login");
        
        // Step 1: Download save games
        downloadSaveGames(new RoboyardApiClient.ApiCallback<Integer>() {
            @Override
            public void onSuccess(Integer savesRestored) {
                Timber.d("[FULL_SYNC] Save games downloaded: %d restored", savesRestored);
                
                // Step 2: Download history
                downloadHistory(activity, new RoboyardApiClient.ApiCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer historyRestored) {
                        Timber.d("[FULL_SYNC] History downloaded: %d restored", historyRestored);
                        
                        // Step 3: Upload local data to server
                        uploadSaveGames();
                        uploadHistory(activity);
                        
                        String summary = savesRestored + " saves, " + historyRestored + " history entries restored";
                        Timber.d("[FULL_SYNC] Full sync complete: %s", summary);
                        if (callback != null) callback.onSuccess(summary);
                    }
                    
                    @Override
                    public void onError(String error) {
                        Timber.e("[FULL_SYNC] History download failed: %s", error);
                        // Still try to upload local data
                        uploadSaveGames();
                        uploadHistory(activity);
                        if (callback != null) callback.onSuccess(savesRestored + " saves restored (history failed)");
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                Timber.e("[FULL_SYNC] Save game download failed: %s", error);
                // Still try history
                downloadHistory(activity, new RoboyardApiClient.ApiCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer historyRestored) {
                        uploadSaveGames();
                        uploadHistory(activity);
                        if (callback != null) callback.onSuccess(historyRestored + " history entries restored (saves failed)");
                    }
                    
                    @Override
                    public void onError(String histError) {
                        if (callback != null) callback.onError("Sync failed: saves=" + error + ", history=" + histError);
                    }
                });
            }
        });
    }
    
    // ========== HELPER METHODS ==========
    
    private String readFileContent(File file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (content.length() > 0) content.append("\n");
                content.append(line);
            }
            return content.toString();
        } catch (IOException e) {
            Timber.e(e, "[SYNC] Error reading file: %s", file.getName());
            return null;
        }
    }
    
    private String readHistoryFileData(Activity activity, GameHistoryEntry entry) {
        try {
            return FileReadWrite.readPrivateData(activity, entry.getMapPath());
        } catch (Exception e) {
            Timber.e(e, "[HISTORY_SYNC] Error reading history file: %s", entry.getMapPath());
            return null;
        }
    }
    
    private long parseTimestamp(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isEmpty()) {
            return System.currentTimeMillis();
        }
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US);
            java.util.Date date = sdf.parse(isoTimestamp);
            return date != null ? date.getTime() : System.currentTimeMillis();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }
    
    private String extractMapName(String saveData) {
        // Try to extract NAME:xxx; from save data
        int nameStart = saveData.indexOf("NAME:");
        if (nameStart >= 0) {
            int nameEnd = saveData.indexOf(";", nameStart);
            if (nameEnd > nameStart) {
                return saveData.substring(nameStart + 5, nameEnd);
            }
        }
        return null;
    }
    
    private int extractBoardWidth(String saveData) {
        return extractSizeComponent(saveData, 0, 12);
    }
    
    private int extractBoardHeight(String saveData) {
        return extractSizeComponent(saveData, 1, 12);
    }
    
    private int extractSizeComponent(String saveData, int index, int defaultValue) {
        int sizeStart = saveData.indexOf("SIZE:");
        if (sizeStart >= 0) {
            int sizeEnd = saveData.indexOf(";", sizeStart);
            if (sizeEnd > sizeStart) {
                String sizeStr = saveData.substring(sizeStart + 5, sizeEnd);
                String[] parts = sizeStr.split(",");
                if (parts.length > index) {
                    try {
                        return Integer.parseInt(parts[index].trim());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                }
            }
        }
        return defaultValue;
    }
    
    private int extractBoardWidthFromSize(String boardSize) {
        if (boardSize != null && boardSize.contains("x")) {
            try {
                return Integer.parseInt(boardSize.split("x")[0].trim());
            } catch (NumberFormatException e) {
                return 12;
            }
        }
        return 12;
    }
    
    private int extractBoardHeightFromSize(String boardSize) {
        if (boardSize != null && boardSize.contains("x")) {
            try {
                return Integer.parseInt(boardSize.split("x")[1].trim());
            } catch (NumberFormatException e) {
                return 12;
            }
        }
        return 12;
    }
}
