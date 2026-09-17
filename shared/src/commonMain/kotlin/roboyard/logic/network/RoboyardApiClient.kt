package roboyard.logic.network

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import roboyard.logic.storage.PlatformStorage
import roboyard.logic.util.RLog

/**
 * API client for roboyard.z11.de authentication and map sharing.
 * Shared implementation used by Android and Desktop (Compose).
 *
 * @param storage platform key-value storage for token/credentials persistence
 * @param callbackScope scope on which ApiCallback methods are invoked
 *        (Android passes a Main scope; desktop passes a background scope)
 * @param installSourceProvider platform hook returning the install source
 *        (e.g. "com.android.vending" on Android, "desktop" on desktop)
 */
class RoboyardApiClient private constructor(
    private val storage: PlatformStorage,
    private val callbackScope: CoroutineScope,
    private val installSourceProvider: () -> String
) {
    private val ioScope = CoroutineScope(Dispatchers.Default)
    private val log = RLog.tag(TAG)

    interface ApiCallback<T> {
        fun onSuccess(result: T?)
        fun onError(error: String?)

        /** Called when the server requires a newer app version (needs_update response). Default: treat as error. */
        fun onNeedsUpdate() {
            onError("needs_update")
        }
    }

    class LoginResult(
        @JvmField val token: String?,
        @JvmField val userName: String?,
        @JvmField val email: String?,
        @JvmField val userId: Int
    )

    class ShareResult @JvmOverloads constructor(
        @JvmField val mapId: Int,
        @JvmField val shareUrl: String?,
        @JvmField val isDuplicate: Boolean = false
    )

    /** Install source (store) of this app, e.g. "com.android.vending", "desktop". */
    val installSource: String
        get() = try {
            installSourceProvider()
        } catch (e: Exception) {
            log.e("[INSTALL_SOURCE] Failed to get install source")
            "unknown"
        }

    /** Check if user is logged in. */
    val isLoggedIn: Boolean
        get() {
            val token = storage.getString(KEY_AUTH_TOKEN, null)
            log.d("[AUTH_DEBUG] isLoggedIn check: token=%s", if (token != null) "present" else "null")
            return token != null
        }

    /** Get the logged-in user's email. */
    val userEmail: String?
        get() = storage.getString(KEY_USER_EMAIL, null)

    /** Get the logged-in user's name. */
    val userName: String?
        get() = storage.getString(KEY_USER_NAME, null)

    /** Get the auth token for auto-login URL. */
    val authToken: String?
        get() = storage.getString(KEY_AUTH_TOKEN, null)

    /** API base URL — configurable via the api_base_url storage key (for local dev backend). */
    var baseUrl: String
        get() = storage.getString(KEY_API_BASE_URL, null) ?: DEFAULT_BASE_URL
        set(value) = storage.putString(KEY_API_BASE_URL, value)

    /**
     * Build a URL that auto-logs the user in via token before redirecting to the target page.
     * If the user is not logged in, returns the original URL unchanged.
     */
    fun buildAutoLoginUrl(targetUrl: String): String {
        val token = this.authToken ?: return targetUrl

        var redirectPath = targetUrl
        if (redirectPath.startsWith(baseUrl)) {
            redirectPath = redirectPath.substring(baseUrl.length)
        }

        return try {
            val encodedRedirect = urlEncodeUtf8(redirectPath)
            val autoLoginUrl = "$baseUrl/auto-login?token=$token&redirect=$encodedRedirect"
            log.d("[AUTO_LOGIN] Built auto-login URL for redirect: %s", redirectPath)
            autoLoginUrl
        } catch (e: Exception) {
            log.e("[AUTO_LOGIN] Error encoding redirect URL")
            targetUrl
        }
    }

    /**
     * Login to roboyard.z11.de.
     * Supports email, username, user ID, or email prefix (if unique).
     */
    fun login(identifier: String?, password: String?, callback: ApiCallback<LoginResult?>) {
        ioScope.launch {
            try {
                val requestBody = JsonObject()
                requestBody.addProperty("identifier", identifier)
                requestBody.addProperty("password", password)
                requestBody.addProperty("ver", API_VERSION)
                requestBody.addProperty("install_source", installSource)

                val response = makePostRequest("/api/mobile/login", requestBody.toString())
                val json = JsonParser.parseString(response).asJsonObject

                if (json.optBoolean("needs_update")) {
                    postNeedsUpdate(callback)
                    return@launch
                }
                if (json.has("error")) {
                    postError(callback, json.get("error").asString)
                    return@launch
                }

                val token = json.get("token").asString
                val userObject = json.getAsJsonObject("user")
                val userName = userObject.optString("name", "")
                val email = userObject.optString("email", "")
                val userId = userObject.optInt("id", -1)

                storage.putString(KEY_AUTH_TOKEN, token)
                storage.putString(KEY_USER_EMAIL, email ?: "")
                storage.putString(KEY_USER_NAME, userName ?: "")
                storage.putInt(KEY_USER_ID, userId)
                if (password != null) {
                    storage.putString(KEY_USER_PASSWORD, password)
                }

                log.d("[AUTH_DEBUG] Token saved to storage: %s", token.substring(0, min(10, token.length)) + "...")
                postSuccess(callback, LoginResult(token, userName, email, userId))
                log.d("Login successful for: %s", email)
            } catch (e: Exception) {
                log.e(e, "Error during login")
                postError(callback, "Network error: " + e.message)
            }
        }
    }

    /**
     * Register a new account on roboyard.z11.de.
     */
    fun register(name: String?, email: String?, password: String?, callback: ApiCallback<LoginResult?>) {
        ioScope.launch {
            try {
                val requestBody = JsonObject()
                requestBody.addProperty("name", name)
                requestBody.addProperty("email", email)
                requestBody.addProperty("password", password)
                requestBody.addProperty("password_confirmation", password)
                requestBody.addProperty("ver", API_VERSION)
                requestBody.addProperty("install_source", installSource)

                val response = makePostRequest("/api/mobile/register", requestBody.toString())
                val json = JsonParser.parseString(response).asJsonObject

                if (json.optBoolean("needs_update")) {
                    postNeedsUpdate(callback)
                    return@launch
                }
                if (json.has("error")) {
                    postError(callback, json.get("error").asString)
                    return@launch
                }

                val token = json.get("token").asString
                // The API returns the user id inside the "user" object; "user_id" is kept as fallback
                val userId = json.optInt("user_id",
                    json.optJsonObject("user")?.optInt("id", -1) ?: -1)

                storage.putString(KEY_AUTH_TOKEN, token)
                if (email != null) storage.putString(KEY_USER_EMAIL, email)
                if (name != null) storage.putString(KEY_USER_NAME, name)
                storage.putInt(KEY_USER_ID, userId)
                if (password != null) storage.putString(KEY_USER_PASSWORD, password)

                postSuccess(callback, LoginResult(token, name, email, userId))
                log.d("Registration successful for: %s", email)
            } catch (e: Exception) {
                log.e(e, "Error during registration")
                postError(callback, "Network error: " + e.message)
            }
        }
    }

    /**
     * Verify the stored auth token is still valid.
     * Should be called on app start to ensure user stays logged in.
     */
    fun verifyToken(callback: ApiCallback<Boolean?>) {
        if (!this.isLoggedIn) {
            // No token, but maybe we still have stored credentials from a previous session
            val email = storage.getString(KEY_USER_EMAIL, null)
            val password = storage.getString(KEY_USER_PASSWORD, null)
            if (email != null && password != null) {
                log.d("[AUTH_DEBUG] No token but stored credentials found, attempting re-login")
                tryReLoginOrLogout(callback)
            } else {
                log.d("[AUTH_DEBUG] No token and no stored credentials, user must login manually")
                postSuccess(callback, false)
            }
            return
        }

        ioScope.launch {
            try {
                val response = makeAuthenticatedPostRequest("/api/mobile/verify-token", "{}")
                val json = JsonParser.parseString(response).asJsonObject

                if (json.has("error")) {
                    log.d("[AUTH_DEBUG] Token verification failed, attempting auto re-login")
                    tryReLoginOrLogout(callback)
                    return@launch
                }
                log.d("[AUTH_DEBUG] Token verified successfully")
                postSuccess(callback, true)
            } catch (e: Exception) {
                log.e(e, "[AUTH_DEBUG] Error during token verification")
                // Don't logout on network error - token might still be valid
                postSuccess(callback, false)
            }
        }
    }

    /**
     * Attempt re-login using stored credentials. If re-login fails, perform full logout.
     */
    private fun tryReLoginOrLogout(callback: ApiCallback<Boolean?>) {
        val email = storage.getString(KEY_USER_EMAIL, null)
        val password = storage.getString(KEY_USER_PASSWORD, null)

        if (email == null || password == null) {
            log.d("[AUTH_DEBUG] No stored credentials for re-login, logging out")
            logout()
            postSuccess(callback, false)
            return
        }

        log.d("[AUTH_DEBUG] Re-login with stored credentials for: %s", email)
        login(email, password, object : ApiCallback<LoginResult?> {
            override fun onSuccess(result: LoginResult?) {
                log.d("[AUTH_DEBUG] Auto re-login successful")
                postSuccess(callback, true)
            }

            override fun onError(error: String?) {
                log.e("[AUTH_DEBUG] Auto re-login failed: %s — logging out", error)
                logout()
                postSuccess(callback, false)
            }
        })
    }

    /**
     * Attempt to re-login using stored credentials.
     * Called automatically when a 401 Unauthorized error occurs.
     */
    fun attemptReLogin(callback: ApiCallback<Boolean?>) {
        val email = storage.getString(KEY_USER_EMAIL, null)
        val password = storage.getString(KEY_USER_PASSWORD, null)

        if (email == null || password == null) {
            log.d("[AUTO_RELOGIN] No stored credentials, cannot re-login")
            postSuccess(callback, false)
            return
        }

        log.d("[AUTO_RELOGIN] Attempting to re-login as: %s", email)
        login(email, password, object : ApiCallback<LoginResult?> {
            override fun onSuccess(result: LoginResult?) {
                log.d("[AUTO_RELOGIN] Re-login successful")
                postSuccess(callback, true)
            }

            override fun onError(error: String?) {
                log.e("[AUTO_RELOGIN] Re-login failed: %s", error)
                postSuccess(callback, false)
            }
        })
    }

    /** Logout from roboyard.z11.de. */
    fun logout() {
        storage.remove(KEY_AUTH_TOKEN)
        storage.remove(KEY_USER_EMAIL)
        storage.remove(KEY_USER_NAME)
        storage.remove(KEY_USER_ID)
        storage.remove(KEY_USER_PASSWORD)
        log.d("Logged out")
    }

    /** Share a map to roboyard.z11.de using the logged-in account. */
    fun shareMap(mapData: String?, mapName: String?, callback: ApiCallback<ShareResult?>) {
        if (!this.isLoggedIn) {
            postError(callback, "Not logged in")
            return
        }

        ioScope.launch {
            try {
                val requestBody = JsonObject()
                requestBody.addProperty("map_data", mapData)
                requestBody.addProperty("ver", API_VERSION)
                if (!mapName.isNullOrEmpty()) {
                    requestBody.addProperty("name", mapName)
                }

                val response = makeAuthenticatedPostRequest("/api/mobile/maps", requestBody.toString())
                val json = JsonParser.parseString(response).asJsonObject

                if (json.optBoolean("needs_update")) {
                    postNeedsUpdate(callback)
                    return@launch
                }
                if (json.has("error")) {
                    postError(callback, json.get("error").asString)
                    return@launch
                }

                val mapId = json.get("map_id").asInt
                val shareUrl = json.optString("share_url", "$baseUrl/maps/$mapId")
                val isDuplicate = json.optBoolean("duplicate")

                postSuccess(callback, ShareResult(mapId, shareUrl, isDuplicate))
                if (isDuplicate) log.d("Map already exists: %d", mapId)
                else log.d("Map shared successfully: %d", mapId)
            } catch (e: Exception) {
                log.e(e, "Error during map share")
                postError(callback, "Network error: " + e.message)
            }
        }
    }

    // ---------- HTTP plumbing ----------

    private fun makePostRequest(endpoint: String?, body: String): String {
        val (code, text) = PlatformHttp.request(
            "POST", baseUrl + endpoint,
            mapOf(
                "Content-Type" to "application/json",
                "Accept" to "application/json",
                "X-App-Version" to API_VERSION.toString()
            ),
            body
        )
        return wrapErrorResponse(code, text)
    }

    private fun makeAuthenticatedPostRequest(endpoint: String?, body: String): String {
        val token = storage.getString(KEY_AUTH_TOKEN, null)
            ?: throw java.io.IOException("Not authenticated")
        val (code, text) = PlatformHttp.request(
            "POST", baseUrl + endpoint,
            mapOf(
                "Content-Type" to "application/json",
                "Accept" to "application/json",
                "Authorization" to "Bearer $token"
            ),
            body
        )
        return wrapErrorResponse(code, text)
    }

    private fun makeAuthenticatedGetRequest(endpoint: String?): String {
        val token = storage.getString(KEY_AUTH_TOKEN, null)
            ?: throw java.io.IOException("Not authenticated")
        val (code, text) = PlatformHttp.request(
            "GET", baseUrl + endpoint,
            mapOf(
                "Accept" to "application/json",
                "Authorization" to "Bearer $token"
            ),
            null
        )
        return wrapErrorResponse(code, text)
    }

    /** Wrap non-2xx responses in an error JSON, mirroring the Android implementation. */
    private fun wrapErrorResponse(responseCode: Int, responseStr: String): String {
        log.d("Response (%d): %s", responseCode, responseStr)
        if (responseCode >= 400) {
            try {
                val errorJson = JsonParser.parseString(responseStr).asJsonObject
                if (!errorJson.has("error")) {
                    errorJson.addProperty(
                        "error",
                        errorJson.optString("message", "Request failed with code $responseCode")
                    )
                }
                return errorJson.toString()
            } catch (e: Exception) {
                return "{\"error\": \"Request failed with code $responseCode\"}"
            }
        }
        return responseStr
    }

    private fun <T> postSuccess(callback: ApiCallback<T?>, result: T?) {
        callbackScope.launch { callback.onSuccess(result) }
    }

    private fun <T> postError(callback: ApiCallback<T?>, error: String?) {
        callbackScope.launch { callback.onError(error) }
    }

    private fun <T> postNeedsUpdate(callback: ApiCallback<T?>) {
        callbackScope.launch { callback.onNeedsUpdate() }
    }

    // ---------- Result classes ----------

    /** Result class for achievement sync. */
    class AchievementSyncResult(
        @JvmField val success: Boolean,
        @JvmField val syncedCount: Int,
        @JvmField val newAchievements: Int,
        @JvmField val statsUpdated: Boolean,
        @JvmField val latestAppVersion: String?
    )

    /** Result class for fetching achievements from server. */
    class AchievementFetchResult(
        @JvmField val achievements: JsonArray?,
        @JvmField val stats: JsonObject?
    )

    /** Fetch achievements from the server for the logged-in user. */
    fun fetchAchievements(callback: ApiCallback<AchievementFetchResult?>) {
        if (!this.isLoggedIn) {
            postError(callback, "Not logged in")
            return
        }

        ioScope.launch {
            try {
                val response = makeAuthenticatedGetRequest("/api/mobile/achievements")
                val json = JsonParser.parseString(response).asJsonObject

                if (json.has("error")) {
                    postError(callback, json.get("error").asString)
                    return@launch
                }

                val user = json.getAsJsonObject("user")
                val achievements = user.getAsJsonArray("achievements")
                val stats = user.optJsonObject("stats")

                postSuccess(callback, AchievementFetchResult(achievements, stats))
                log.d("[ACHIEVEMENT_FETCH] Fetched %d achievements from server", achievements.size())
            } catch (e: Exception) {
                log.e(e, "[ACHIEVEMENT_FETCH] Error during fetch")
                postError(callback, "Network error: " + e.message)
            }
        }
    }

    // ---------- Save game sync ----------

    /** Upload save games to server. */
    fun syncSaveGames(saves: JsonArray?, callback: ApiCallback<Int?>) {
        if (!this.isLoggedIn) {
            postError(callback, "Not logged in")
            return
        }

        ioScope.launch {
            try {
                val requestBody = JsonObject()
                requestBody.add("saves", saves)

                val response = makeAuthenticatedPostRequest("/api/mobile/saves/sync", requestBody.toString())
                val json = JsonParser.parseString(response).asJsonObject

                if (json.has("error")) {
                    postError(callback, json.get("error").asString)
                    return@launch
                }

                val syncedCount = json.optInt("synced_count", 0)
                postSuccess(callback, syncedCount)
                log.d("[SAVE_SYNC] Uploaded %d save games to server", syncedCount)
            } catch (e: Exception) {
                log.e(e, "[SAVE_SYNC] Error during upload")
                postError(callback, "Network error: " + e.message)
            }
        }
    }

    /** Download save games from server. */
    fun fetchSaveGames(callback: ApiCallback<JsonArray?>) {
        if (!this.isLoggedIn) {
            postError(callback, "Not logged in")
            return
        }

        ioScope.launch {
            try {
                val response = makeAuthenticatedGetRequest("/api/mobile/saves")
                val json = JsonParser.parseString(response).asJsonObject

                if (json.has("error")) {
                    postError(callback, json.get("error").asString)
                    return@launch
                }

                val saves = json.getAsJsonArray("saves")
                postSuccess(callback, saves)
                log.d("[SAVE_SYNC] Fetched %d save games from server", saves.size())
            } catch (e: Exception) {
                log.e(e, "[SAVE_SYNC] Error during fetch")
                postError(callback, "Network error: " + e.message)
            }
        }
    }

    // ---------- Game history sync ----------

    /** Upload game history to server. */
    fun syncHistory(history: JsonArray?, callback: ApiCallback<JsonObject?>) {
        if (!this.isLoggedIn) {
            postError(callback, "Not logged in")
            return
        }

        ioScope.launch {
            try {
                val requestBody = JsonObject()
                requestBody.add("history", history)

                val response = makeAuthenticatedPostRequest("/api/mobile/history/sync", requestBody.toString())
                val json = JsonParser.parseString(response).asJsonObject

                if (json.has("error")) {
                    postError(callback, json.get("error").asString)
                    return@launch
                }

                val syncedCount = json.optInt("synced_count", 0)
                val skippedCount = json.optInt("skipped_count", 0)
                val totalEntries = json.optInt("total_entries", 0)
                log.d(
                    "[HISTORY_SYNC] Server response: synced=%d, skipped=%d, total=%d",
                    syncedCount, skippedCount, totalEntries
                )

                if (json.has("details")) {
                    val details = json.getAsJsonArray("details")
                    for (i in 0 until details.size()) {
                        val detail = details.get(i).asJsonObject
                        val action = detail.optString("action", "unknown")
                        val mapName = detail.optString("map_name", "Unknown")
                        when (action) {
                            "updated" -> log.d("[HISTORY_SYNC] ✓ Updated '%s': %s", mapName, detail.optString("changes", ""))
                            "created" -> log.d("[HISTORY_SYNC] ✓ Created '%s': stars=%d", mapName, detail.optInt("stars_earned", 0))
                            "skipped" -> log.d("[HISTORY_SYNC] ⊘ Skipped '%s': %s", mapName, detail.optString("reason", ""))
                        }
                    }
                }

                postSuccess(callback, json)
            } catch (e: Exception) {
                log.e(e, "[HISTORY_SYNC] Error during upload")
                postError(callback, "Network error: " + e.message)
            }
        }
    }

    /** Download game history from server. */
    fun fetchHistory(callback: ApiCallback<JsonArray?>) {
        if (!this.isLoggedIn) {
            postError(callback, "Not logged in")
            return
        }

        ioScope.launch {
            try {
                val response = makeAuthenticatedGetRequest("/api/mobile/history")
                val json = JsonParser.parseString(response).asJsonObject

                if (json.has("error")) {
                    postError(callback, json.get("error").asString)
                    return@launch
                }

                val history = json.getAsJsonArray("history")
                postSuccess(callback, history)
                log.d("[HISTORY_SYNC] Fetched %d history entries from server", history.size())
            } catch (e: Exception) {
                log.e(e, "[HISTORY_SYNC] Error during fetch")
                postError(callback, "Network error: " + e.message)
            }
        }
    }

    // ---------- Achievement sync ----------

    /** Sync achievements to the server. */
    fun syncAchievements(
        achievements: JsonArray?,
        stats: JsonObject?,
        callback: ApiCallback<AchievementSyncResult?>
    ) {
        if (!this.isLoggedIn) {
            postError(callback, "Not logged in")
            return
        }

        ioScope.launch {
            try {
                val requestBody = JsonObject()
                requestBody.add("achievements", achievements)
                if (stats != null) requestBody.add("stats", stats)

                val response = makeAuthenticatedPostRequest("/api/mobile/achievements/sync", requestBody.toString())
                val json = JsonParser.parseString(response).asJsonObject

                if (json.has("error")) {
                    postError(callback, json.get("error").asString)
                    return@launch
                }

                val result = AchievementSyncResult(
                    json.optBoolean("success"),
                    json.optInt("synced_count", 0),
                    json.optInt("new_achievements", 0),
                    json.optBoolean("stats_updated"),
                    if (json.has("latest_app_version") && !json.get("latest_app_version").isJsonNull)
                        json.optString("latest_app_version", null) else null
                )
                postSuccess(callback, result)
                log.d("[ACHIEVEMENT_SYNC] Sync successful: %d synced, %d new", result.syncedCount, result.newAchievements)
            } catch (e: Exception) {
                log.e(e, "[ACHIEVEMENT_SYNC] Error during sync")
                postError(callback, "Network error: " + e.message)
            }
        }
    }

    companion object {
        private const val TAG = "RoboyardApi"
        const val DEFAULT_BASE_URL = "https://roboyard.z11.de"
        const val KEY_API_BASE_URL = "api_base_url"

        /** Current API protocol version sent with every request. Increment on breaking changes. */
        const val API_VERSION: Int = 1
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_PASSWORD = "user_password"

        @Volatile
        private var instance: RoboyardApiClient? = null

        @JvmStatic
        @Synchronized
        fun getInstance(
            storage: PlatformStorage,
            callbackScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
            installSourceProvider: () -> String = { "unknown" }
        ): RoboyardApiClient {
            if (instance == null) {
                instance = RoboyardApiClient(storage, callbackScope, installSourceProvider)
            }
            return instance!!
        }

        /** Test hook: reset the singleton (used by tests switching backends). */
        @JvmStatic
        fun resetInstance() {
            instance = null
        }
    }
}
