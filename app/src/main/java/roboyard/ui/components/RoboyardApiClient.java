package roboyard.ui.components;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import timber.log.Timber;

/**
 * API client for roboyard.z11.de authentication and map sharing.
 */
public class RoboyardApiClient {
    
    private static final String TAG = "RoboyardApi";
    private static final String BASE_URL = "https://roboyard.z11.de";
    private static final String PREFS_NAME = "roboyard_api";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_ID = "user_id";
    
    private static RoboyardApiClient instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final ExecutorService executor;
    private final Handler mainHandler;
    
    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
    
    public static class LoginResult {
        public final String token;
        public final String userName;
        public final String email;
        public final int userId;
        
        public LoginResult(String token, String userName, String email, int userId) {
            this.token = token;
            this.userName = userName;
            this.email = email;
            this.userId = userId;
        }
    }
    
    public static class ShareResult {
        public final int mapId;
        public final String shareUrl;
        public final boolean isDuplicate;
        
        public ShareResult(int mapId, String shareUrl) {
            this(mapId, shareUrl, false);
        }
        
        public ShareResult(int mapId, String shareUrl, boolean isDuplicate) {
            this.mapId = mapId;
            this.shareUrl = shareUrl;
            this.isDuplicate = isDuplicate;
        }
    }
    
    private RoboyardApiClient(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public static synchronized RoboyardApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new RoboyardApiClient(context);
        }
        return instance;
    }
    
    /**
     * Check if user is logged in.
     */
    public boolean isLoggedIn() {
        return prefs.getString(KEY_AUTH_TOKEN, null) != null;
    }
    
    /**
     * Get the logged-in user's email.
     */
    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }
    
    /**
     * Get the logged-in user's name.
     */
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }
    
    
    /**
     * Get the auth token for auto-login URL.
     */
    public String getAuthToken() {
        return prefs.getString(KEY_AUTH_TOKEN, null);
    }
    
    /**
     * Build a URL that auto-logs the user in via token before redirecting to the target page.
     * If the user is not logged in, returns the original URL unchanged.
     * @param targetUrl The target URL path (e.g. "/share_map?data=...")
     * @return Auto-login URL with token and redirect, or original URL if not logged in
     */
    public String buildAutoLoginUrl(String targetUrl) {
        String token = getAuthToken();
        if (token == null) {
            return targetUrl;
        }
        
        // Extract the path from the target URL (strip BASE_URL if present)
        String redirectPath = targetUrl;
        if (redirectPath.startsWith(BASE_URL)) {
            redirectPath = redirectPath.substring(BASE_URL.length());
        }
        
        try {
            String encodedRedirect = java.net.URLEncoder.encode(redirectPath, "UTF-8");
            String autoLoginUrl = BASE_URL + "/auto-login?token=" + token + "&redirect=" + encodedRedirect;
            Timber.d("[AUTO_LOGIN] Built auto-login URL for redirect: %s", redirectPath);
            return autoLoginUrl;
        } catch (java.io.UnsupportedEncodingException e) {
            Timber.e(e, "[AUTO_LOGIN] Error encoding redirect URL");
            return targetUrl;
        }
    }
    
    /**
     * Login to roboyard.z11.de.
     */
    public void login(String email, String password, ApiCallback<LoginResult> callback) {
        executor.execute(() -> {
            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("email", email);
                requestBody.put("password", password);
                
                String response = makePostRequest("/api/mobile/login", requestBody.toString());
                JSONObject json = new JSONObject(response);
                
                if (json.has("error")) {
                    postError(callback, json.getString("error"));
                    return;
                }
                
                String token = json.getString("token");
                String userName = json.optString("name", email);
                int userId = json.optInt("user_id", -1);
                
                // Save credentials
                prefs.edit()
                    .putString(KEY_AUTH_TOKEN, token)
                    .putString(KEY_USER_EMAIL, email)
                    .putString(KEY_USER_NAME, userName)
                    .putInt(KEY_USER_ID, userId)
                    .apply();
                
                LoginResult result = new LoginResult(token, userName, email, userId);
                postSuccess(callback, result);
                
                Timber.tag(TAG).d("Login successful for: %s", email);
                
            } catch (JSONException e) {
                Timber.tag(TAG).e(e, "JSON error during login");
                postError(callback, "Invalid response from server");
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "Network error during login");
                postError(callback, "Network error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Register a new account on roboyard.z11.de.
     */
    public void register(String name, String email, String password, ApiCallback<LoginResult> callback) {
        executor.execute(() -> {
            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("name", name);
                requestBody.put("email", email);
                requestBody.put("password", password);
                requestBody.put("password_confirmation", password);
                
                String response = makePostRequest("/api/mobile/register", requestBody.toString());
                JSONObject json = new JSONObject(response);
                
                if (json.has("error")) {
                    postError(callback, json.getString("error"));
                    return;
                }
                
                String token = json.getString("token");
                int userId = json.optInt("user_id", -1);
                
                // Save credentials
                prefs.edit()
                    .putString(KEY_AUTH_TOKEN, token)
                    .putString(KEY_USER_EMAIL, email)
                    .putString(KEY_USER_NAME, name)
                    .putInt(KEY_USER_ID, userId)
                    .apply();
                
                LoginResult result = new LoginResult(token, name, email, userId);
                postSuccess(callback, result);
                
                Timber.tag(TAG).d("Registration successful for: %s", email);
                
            } catch (JSONException e) {
                Timber.tag(TAG).e(e, "JSON error during registration");
                postError(callback, "Invalid response from server");
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "Network error during registration");
                postError(callback, "Network error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Logout from roboyard.z11.de.
     */
    public void logout() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_ID)
            .apply();
        
        Timber.tag(TAG).d("Logged out");
    }
    
    /**
     * Share a map to roboyard.z11.de using the logged-in account.
     */
    public void shareMap(String mapData, String mapName, ApiCallback<ShareResult> callback) {
        if (!isLoggedIn()) {
            postError(callback, "Not logged in");
            return;
        }
        
        executor.execute(() -> {
            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("map_data", mapData);
                if (mapName != null && !mapName.isEmpty()) {
                    requestBody.put("name", mapName);
                }
                
                String response = makeAuthenticatedPostRequest("/api/mobile/maps", requestBody.toString());
                JSONObject json = new JSONObject(response);
                
                if (json.has("error")) {
                    postError(callback, json.getString("error"));
                    return;
                }
                
                int mapId = json.getInt("map_id");
                String shareUrl = json.optString("share_url", BASE_URL + "/maps/" + mapId);
                boolean isDuplicate = json.optBoolean("duplicate", false);
                
                ShareResult result = new ShareResult(mapId, shareUrl, isDuplicate);
                postSuccess(callback, result);
                
                if (isDuplicate) {
                    Timber.tag(TAG).d("Map already exists: %d", mapId);
                } else {
                    Timber.tag(TAG).d("Map shared successfully: %d", mapId);
                }
                
            } catch (JSONException e) {
                Timber.tag(TAG).e(e, "JSON error during map share");
                postError(callback, "Invalid response from server");
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "Network error during map share");
                postError(callback, "Network error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Make a POST request without authentication.
     */
    private String makePostRequest(String endpoint, String body) throws IOException {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            return readResponse(conn);
            
        } finally {
            conn.disconnect();
        }
    }
    
    /**
     * Make a POST request with authentication.
     */
    private String makeAuthenticatedPostRequest(String endpoint, String body) throws IOException {
        String token = prefs.getString(KEY_AUTH_TOKEN, null);
        if (token == null) {
            throw new IOException("Not authenticated");
        }
        
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            return readResponse(conn);
            
        } finally {
            conn.disconnect();
        }
    }
    
    /**
     * Make a GET request with authentication.
     */
    private String makeAuthenticatedGetRequest(String endpoint) throws IOException {
        String token = prefs.getString(KEY_AUTH_TOKEN, null);
        if (token == null) {
            throw new IOException("Not authenticated");
        }
        
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            
            return readResponse(conn);
            
        } finally {
            conn.disconnect();
        }
    }
    
    /**
     * Read the response from an HTTP connection.
     */
    private String readResponse(HttpURLConnection conn) throws IOException {
        int responseCode = conn.getResponseCode();
        
        BufferedReader reader;
        if (responseCode >= 200 && responseCode < 300) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        }
        
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        String responseStr = response.toString();
        Timber.tag(TAG).d("Response (%d): %s", responseCode, responseStr);
        
        // If error response, wrap in error JSON
        if (responseCode >= 400) {
            try {
                JSONObject errorJson = new JSONObject(responseStr);
                if (!errorJson.has("error")) {
                    errorJson.put("error", errorJson.optString("message", "Request failed with code " + responseCode));
                }
                return errorJson.toString();
            } catch (JSONException e) {
                return "{\"error\": \"Request failed with code " + responseCode + "\"}";
            }
        }
        
        return responseStr;
    }
    
    /**
     * Post success callback to main thread.
     */
    private <T> void postSuccess(ApiCallback<T> callback, T result) {
        mainHandler.post(() -> callback.onSuccess(result));
    }
    
    /**
     * Post error callback to main thread.
     */
    private <T> void postError(ApiCallback<T> callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }
    
    /**
     * Result class for achievement sync.
     */
    public static class AchievementSyncResult {
        public final boolean success;
        public final int syncedCount;
        public final int newAchievements;
        public final boolean statsUpdated;
        
        public AchievementSyncResult(boolean success, int syncedCount, int newAchievements, boolean statsUpdated) {
            this.success = success;
            this.syncedCount = syncedCount;
            this.newAchievements = newAchievements;
            this.statsUpdated = statsUpdated;
        }
    }
    
    /**
     * Result class for fetching achievements from server.
     */
    public static class AchievementFetchResult {
        public final JSONArray achievements;
        public final JSONObject stats;
        
        public AchievementFetchResult(JSONArray achievements, JSONObject stats) {
            this.achievements = achievements;
            this.stats = stats;
        }
    }
    
    /**
     * Fetch achievements from roboyard.z11.de for the logged-in user.
     * Used to restore achievements on a new device after login.
     * 
     * @param callback Callback for result
     */
    public void fetchAchievements(ApiCallback<AchievementFetchResult> callback) {
        if (!isLoggedIn()) {
            postError(callback, "Not logged in");
            return;
        }
        
        executor.execute(() -> {
            try {
                String response = makeAuthenticatedGetRequest("/api/mobile/achievements");
                JSONObject json = new JSONObject(response);
                
                if (json.has("error")) {
                    postError(callback, json.getString("error"));
                    return;
                }
                
                JSONObject user = json.getJSONObject("user");
                JSONArray achievements = user.getJSONArray("achievements");
                JSONObject stats = user.optJSONObject("stats");
                
                AchievementFetchResult result = new AchievementFetchResult(achievements, stats);
                postSuccess(callback, result);
                
                Timber.tag(TAG).d("[ACHIEVEMENT_FETCH] Fetched %d achievements from server", achievements.length());
                
            } catch (JSONException e) {
                Timber.tag(TAG).e(e, "[ACHIEVEMENT_FETCH] JSON error during fetch");
                postError(callback, "Invalid response from server");
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "[ACHIEVEMENT_FETCH] Network error during fetch");
                postError(callback, "Network error: " + e.getMessage());
            }
        });
    }
    
    // ========== SAVE GAME SYNC ==========
    
    /**
     * Upload save games to server.
     */
    public void syncSaveGames(JSONArray saves, ApiCallback<Integer> callback) {
        if (!isLoggedIn()) {
            postError(callback, "Not logged in");
            return;
        }
        
        executor.execute(() -> {
            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("saves", saves);
                
                String response = makeAuthenticatedPostRequest("/api/mobile/saves/sync", requestBody.toString());
                JSONObject json = new JSONObject(response);
                
                if (json.has("error")) {
                    postError(callback, json.getString("error"));
                    return;
                }
                
                int syncedCount = json.optInt("synced_count", 0);
                postSuccess(callback, syncedCount);
                
                Timber.tag(TAG).d("[SAVE_SYNC] Uploaded %d save games to server", syncedCount);
                
            } catch (JSONException e) {
                Timber.tag(TAG).e(e, "[SAVE_SYNC] JSON error during upload");
                postError(callback, "Invalid response from server");
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "[SAVE_SYNC] Network error during upload");
                postError(callback, "Network error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Download save games from server.
     */
    public void fetchSaveGames(ApiCallback<JSONArray> callback) {
        if (!isLoggedIn()) {
            postError(callback, "Not logged in");
            return;
        }
        
        executor.execute(() -> {
            try {
                String response = makeAuthenticatedGetRequest("/api/mobile/saves");
                JSONObject json = new JSONObject(response);
                
                if (json.has("error")) {
                    postError(callback, json.getString("error"));
                    return;
                }
                
                JSONArray saves = json.getJSONArray("saves");
                postSuccess(callback, saves);
                
                Timber.tag(TAG).d("[SAVE_SYNC] Fetched %d save games from server", saves.length());
                
            } catch (JSONException e) {
                Timber.tag(TAG).e(e, "[SAVE_SYNC] JSON error during fetch");
                postError(callback, "Invalid response from server");
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "[SAVE_SYNC] Network error during fetch");
                postError(callback, "Network error: " + e.getMessage());
            }
        });
    }
    
    // ========== GAME HISTORY SYNC ==========
    
    /**
     * Upload game history to server.
     */
    public void syncHistory(JSONArray history, ApiCallback<Integer> callback) {
        if (!isLoggedIn()) {
            postError(callback, "Not logged in");
            return;
        }
        
        executor.execute(() -> {
            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("history", history);
                
                String response = makeAuthenticatedPostRequest("/api/mobile/history/sync", requestBody.toString());
                JSONObject json = new JSONObject(response);
                
                if (json.has("error")) {
                    postError(callback, json.getString("error"));
                    return;
                }
                
                int syncedCount = json.optInt("synced_count", 0);
                postSuccess(callback, syncedCount);
                
                Timber.tag(TAG).d("[HISTORY_SYNC] Uploaded %d history entries to server", syncedCount);
                
            } catch (JSONException e) {
                Timber.tag(TAG).e(e, "[HISTORY_SYNC] JSON error during upload");
                postError(callback, "Invalid response from server");
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "[HISTORY_SYNC] Network error during upload");
                postError(callback, "Network error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Download game history from server.
     */
    public void fetchHistory(ApiCallback<JSONArray> callback) {
        if (!isLoggedIn()) {
            postError(callback, "Not logged in");
            return;
        }
        
        executor.execute(() -> {
            try {
                String response = makeAuthenticatedGetRequest("/api/mobile/history");
                JSONObject json = new JSONObject(response);
                
                if (json.has("error")) {
                    postError(callback, json.getString("error"));
                    return;
                }
                
                JSONArray history = json.getJSONArray("history");
                postSuccess(callback, history);
                
                Timber.tag(TAG).d("[HISTORY_SYNC] Fetched %d history entries from server", history.length());
                
            } catch (JSONException e) {
                Timber.tag(TAG).e(e, "[HISTORY_SYNC] JSON error during fetch");
                postError(callback, "Invalid response from server");
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "[HISTORY_SYNC] Network error during fetch");
                postError(callback, "Network error: " + e.getMessage());
            }
        });
    }
    
    // ========== ACHIEVEMENT SYNC ==========
    
    /**
     * Sync achievements to roboyard.z11.de.
     * 
     * @param achievements List of achievement data (id, unlocked, timestamp)
     * @param stats Game statistics
     * @param callback Callback for result
     */
    public void syncAchievements(JSONArray achievements, JSONObject stats, ApiCallback<AchievementSyncResult> callback) {
        if (!isLoggedIn()) {
            postError(callback, "Not logged in");
            return;
        }
        
        executor.execute(() -> {
            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("achievements", achievements);
                if (stats != null) {
                    requestBody.put("stats", stats);
                }
                
                String response = makeAuthenticatedPostRequest("/api/mobile/achievements/sync", requestBody.toString());
                JSONObject json = new JSONObject(response);
                
                if (json.has("error")) {
                    postError(callback, json.getString("error"));
                    return;
                }
                
                boolean success = json.optBoolean("success", false);
                int syncedCount = json.optInt("synced_count", 0);
                int newAchievements = json.optInt("new_achievements", 0);
                boolean statsUpdated = json.optBoolean("stats_updated", false);
                
                AchievementSyncResult result = new AchievementSyncResult(success, syncedCount, newAchievements, statsUpdated);
                postSuccess(callback, result);
                
                Timber.tag(TAG).d("[ACHIEVEMENT_SYNC] Sync successful: %d synced, %d new", syncedCount, newAchievements);
                
            } catch (JSONException e) {
                Timber.tag(TAG).e(e, "[ACHIEVEMENT_SYNC] JSON error during sync");
                postError(callback, "Invalid response from server");
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "[ACHIEVEMENT_SYNC] Network error during sync");
                postError(callback, "Network error: " + e.getMessage());
            }
        });
    }
}
