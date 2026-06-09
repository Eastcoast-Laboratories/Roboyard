package roboyard.logic.network

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber.Forest.d
import timber.log.Timber.Forest.e
import timber.log.Timber.Forest.tag
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.min

/**
 * API client for roboyard.z11.de authentication and map sharing.
 */
class RoboyardApiClient private constructor(context: Context) {
    private val context: Context
    private val prefs: SharedPreferences
    private val executor: ExecutorService
    private val mainHandler: Handler

    interface ApiCallback<T> {
        fun onSuccess(result: T?)
        fun onError(error: String?)

        /** Called when the server requires a newer app version (needs_update response). Default: treat as error.  */
        fun onNeedsUpdate() {
            onError("needs_update")
        }
    }

    class LoginResult(
        @JvmField val token: String?,
        @JvmField val userName: String?,
        val email: String?,
        val userId: Int
    )

    class ShareResult @JvmOverloads constructor(
        @JvmField val mapId: Int,
        @JvmField val shareUrl: String?,
        @JvmField val isDuplicate: Boolean = false
    )

    init {
        this.context = context.getApplicationContext()
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        this.executor = Executors.newSingleThreadExecutor()
        this.mainHandler = Handler(Looper.getMainLooper())
    }

    val installSource: String
        /**
         * Get the install source (store) of this app using PackageManager.
         * Uses getInstallSourceInfo() on API 30+ and getInstallerPackageName() on older versions.
         * @return e.g. "com.android.vending" (Play Store), "com.amazon.venezia" (Amazon), "sideload", etc.
         */
        get() {
            try {
                val packageName = context.getPackageName()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val info =
                        context.getPackageManager().getInstallSourceInfo(packageName)
                    val installer = info.getInstallingPackageName()
                    return if (installer != null) installer else "sideload"
                } else {
                    @Suppress("deprecation") val installer =
                        context.getPackageManager().getInstallerPackageName(packageName)
                    return if (installer != null) installer else "sideload"
                }
            } catch (e: Exception) {
                e(e, "[INSTALL_SOURCE] Failed to get install source")
                return "unknown"
            }
        }

    val isLoggedIn: Boolean
        /**
         * Check if user is logged in.
         */
        get() {
            val token =
                prefs.getString(KEY_AUTH_TOKEN, null)
            tag(TAG).d(
                "[AUTH_DEBUG] isLoggedIn check: token=%s",
                if (token != null) "present" else "null"
            )
            return token != null
        }

    val userEmail: String?
        /**
         * Get the logged-in user's email.
         */
        get() = prefs.getString(KEY_USER_EMAIL, null)

    val userName: String?
        /**
         * Get the logged-in user's name.
         */
        get() = prefs.getString(KEY_USER_NAME, null)


    val authToken: String?
        /**
         * Get the auth token for auto-login URL.
         */
        get() = prefs.getString(KEY_AUTH_TOKEN, null)

    /**
     * Build a URL that auto-logs the user in via token before redirecting to the target page.
     * If the user is not logged in, returns the original URL unchanged.
     * @param targetUrl The target URL path (e.g. "/share_map?data=...")
     * @return Auto-login URL with token and redirect, or original URL if not logged in
     */
    fun buildAutoLoginUrl(targetUrl: String): String {
        val token = this.authToken
        if (token == null) {
            return targetUrl
        }


        // Extract the path from the target URL (strip BASE_URL if present)
        var redirectPath = targetUrl
        if (redirectPath.startsWith(BASE_URL)) {
            redirectPath = redirectPath.substring(BASE_URL.length)
        }

        try {
            val encodedRedirect = URLEncoder.encode(redirectPath, "UTF-8")
            val autoLoginUrl =
                BASE_URL + "/auto-login?token=" + token + "&redirect=" + encodedRedirect
            d("[AUTO_LOGIN] Built auto-login URL for redirect: %s", redirectPath)
            return autoLoginUrl
        } catch (e: UnsupportedEncodingException) {
            e(e, "[AUTO_LOGIN] Error encoding redirect URL")
            return targetUrl
        }
    }

    /**
     * Login to roboyard.z11.de.
     * Supports email, username, user ID, or email prefix (if unique).
     */
    fun login(identifier: String?, password: String?, callback: ApiCallback<LoginResult?>) {
        executor.execute(Runnable {
            try {
                val requestBody = JSONObject()
                requestBody.put("identifier", identifier) // Changed from "email" to "identifier"
                requestBody.put("password", password)
                requestBody.put("ver", API_VERSION)
                requestBody.put("install_source", this.installSource)

                val response = makePostRequest("/api/mobile/login", requestBody.toString())
                val json = JSONObject(response)

                if (json.optBoolean("needs_update", false)) {
                    mainHandler.post(Runnable { callback.onNeedsUpdate() })
                    return@Runnable
                }
                if (json.has("error")) {
                    postError<LoginResult?>(callback, json.getString("error"))
                    return@Runnable
                }

                val token = json.getString("token")
                val userObject = json.getJSONObject("user")
                val userName = userObject.optString("name", "")
                val email = userObject.optString("email", "")
                val userId = userObject.optInt("id", -1)


                // Save credentials including password for auto re-login
                prefs.edit()
                    .putString(KEY_AUTH_TOKEN, token)
                    .putString(KEY_USER_EMAIL, email)
                    .putString(KEY_USER_NAME, userName)
                    .putInt(KEY_USER_ID, userId)
                    .putString(KEY_USER_PASSWORD, password)
                    .apply()

                tag(TAG).d(
                    "[AUTH_DEBUG] Token saved to SharedPreferences: %s",
                    token.substring(0, min(10, token.length)) + "..."
                )

                val result = LoginResult(token, userName, email, userId)
                postSuccess<LoginResult?>(callback, result)

                tag(TAG).d("Login successful for: %s", email)
            } catch (e: JSONException) {
                tag(TAG).e(e, "JSON error during login")
                postError<LoginResult?>(callback, "Invalid response from server")
            } catch (e: IOException) {
                tag(TAG).e(e, "Network error during login")
                postError<LoginResult?>(callback, "Network error: " + e.message)
            }
        })
    }

    /**
     * Register a new account on roboyard.z11.de.
     */
    fun register(
        name: String?,
        email: String?,
        password: String?,
        callback: ApiCallback<LoginResult?>
    ) {
        executor.execute(Runnable {
            try {
                val requestBody = JSONObject()
                requestBody.put("name", name)
                requestBody.put("email", email)
                requestBody.put("password", password)
                requestBody.put("password_confirmation", password)
                requestBody.put("ver", API_VERSION)
                requestBody.put("install_source", this.installSource)

                val response = makePostRequest("/api/mobile/register", requestBody.toString())
                val json = JSONObject(response)

                if (json.optBoolean("needs_update", false)) {
                    mainHandler.post(Runnable { callback.onNeedsUpdate() })
                    return@Runnable
                }
                if (json.has("error")) {
                    postError<LoginResult?>(callback, json.getString("error"))
                    return@Runnable
                }

                val token = json.getString("token")
                val userId = json.optInt("user_id", -1)


                // Save credentials including password for auto re-login
                prefs.edit()
                    .putString(KEY_AUTH_TOKEN, token)
                    .putString(KEY_USER_EMAIL, email)
                    .putString(KEY_USER_NAME, name)
                    .putInt(KEY_USER_ID, userId)
                    .putString(KEY_USER_PASSWORD, password)
                    .apply()

                val result = LoginResult(token, name, email, userId)
                postSuccess<LoginResult?>(callback, result)

                tag(TAG).d("Registration successful for: %s", email)
            } catch (e: JSONException) {
                tag(TAG).e(e, "JSON error during registration")
                postError<LoginResult?>(callback, "Invalid response from server")
            } catch (e: IOException) {
                tag(TAG).e(e, "Network error during registration")
                postError<LoginResult?>(callback, "Network error: " + e.message)
            }
        })
    }

    /**
     * Verify the stored auth token is still valid.
     * Should be called on app start to ensure user stays logged in.
     */
    fun verifyToken(callback: ApiCallback<Boolean?>) {
        if (!this.isLoggedIn) {
            // No token, but maybe we still have stored credentials from a previous session
            // (e.g. token was cleared due to a previous bug). Try re-login before giving up.
            val email = prefs.getString(KEY_USER_EMAIL, null)
            val password = prefs.getString(KEY_USER_PASSWORD, null)
            if (email != null && password != null) {
                tag(TAG).d("[AUTH_DEBUG] No token but stored credentials found, attempting re-login")
                tryReLoginOrLogout(callback)
            } else {
                tag(TAG).d("[AUTH_DEBUG] No token and no stored credentials, user must login manually")
                postSuccess<Boolean?>(callback, false)
            }
            return
        }

        executor.execute(Runnable {
            try {
                // Try to make an authenticated request to verify token
                val response = makeAuthenticatedPostRequest("/api/mobile/verify-token", "{}")
                val json = JSONObject(response)

                if (json.has("error")) {
                    // Token is invalid - try to re-login with stored credentials before giving up
                    tag(TAG).d("[AUTH_DEBUG] Token verification failed, attempting auto re-login")
                    tryReLoginOrLogout(callback)
                    return@Runnable
                }


                // Token is valid
                tag(TAG).d("[AUTH_DEBUG] Token verified successfully")
                postSuccess<Boolean?>(callback, true)
            } catch (e: JSONException) {
                tag(TAG).e(
                    e,
                    "[AUTH_DEBUG] JSON error during token verification, attempting auto re-login"
                )
                tryReLoginOrLogout(callback)
            } catch (e: IOException) {
                tag(TAG).e(e, "[AUTH_DEBUG] Network error during token verification")
                // Don't logout on network error - token might still be valid
                postSuccess<Boolean?>(callback, false)
            }
        })
    }

    /**
     * Attempt re-login using stored credentials. If re-login fails, perform full logout.
     * This is used when the token is invalid/expired, so we don't lose the user's session
     * just because the server-side token expired.
     */
    private fun tryReLoginOrLogout(callback: ApiCallback<Boolean?>) {
        val email = prefs.getString(KEY_USER_EMAIL, null)
        val password = prefs.getString(KEY_USER_PASSWORD, null)

        if (email == null || password == null) {
            tag(TAG).d("[AUTH_DEBUG] No stored credentials for re-login, logging out")
            logout()
            postSuccess<Boolean?>(callback, false)
            return
        }

        tag(TAG).d("[AUTH_DEBUG] Re-login with stored credentials for: %s", email)
        login(email, password, object : ApiCallback<LoginResult?> {
            override fun onSuccess(result: LoginResult?) {
                tag(TAG).d("[AUTH_DEBUG] Auto re-login successful")
                postSuccess<Boolean?>(callback, true)
            }

            override fun onError(error: String?) {
                tag(TAG).e("[AUTH_DEBUG] Auto re-login failed: %s — logging out", error)
                logout()
                postSuccess<Boolean?>(callback, false)
            }
        })
    }

    /**
     * Attempt to re-login using stored credentials.
     * Called automatically when a 401 Unauthorized error occurs.
     */
    fun attemptReLogin(callback: ApiCallback<Boolean?>) {
        val email = prefs.getString(KEY_USER_EMAIL, null)
        val password = prefs.getString(KEY_USER_PASSWORD, null)

        if (email == null || password == null) {
            tag(TAG).d("[AUTO_RELOGIN] No stored credentials, cannot re-login")
            postSuccess<Boolean?>(callback, false)
            return
        }

        tag(TAG).d("[AUTO_RELOGIN] Attempting to re-login as: %s", email)
        login(email, password, object : ApiCallback<LoginResult?> {
            override fun onSuccess(result: LoginResult?) {
                tag(TAG).d("[AUTO_RELOGIN] Re-login successful")
                postSuccess<Boolean?>(callback, true)
            }

            override fun onError(error: String?) {
                tag(TAG).e("[AUTO_RELOGIN] Re-login failed: %s", error)
                postSuccess<Boolean?>(callback, false)
            }
        })
    }

    /**
     * Logout from roboyard.z11.de.
     */
    fun logout() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_PASSWORD)
            .apply()

        tag(TAG).d("Logged out")
    }

    /**
     * Share a map to roboyard.z11.de using the logged-in account.
     */
    fun shareMap(mapData: String?, mapName: String?, callback: ApiCallback<ShareResult?>) {
        if (!this.isLoggedIn) {
            postError<ShareResult?>(callback, "Not logged in")
            return
        }

        executor.execute(Runnable {
            try {
                val requestBody = JSONObject()
                requestBody.put("map_data", mapData)
                requestBody.put("ver", API_VERSION)
                if (mapName != null && !mapName.isEmpty()) {
                    requestBody.put("name", mapName)
                }

                val response =
                    makeAuthenticatedPostRequest("/api/mobile/maps", requestBody.toString())
                val json = JSONObject(response)

                if (json.optBoolean("needs_update", false)) {
                    mainHandler.post(Runnable { callback.onNeedsUpdate() })
                    return@Runnable
                }
                if (json.has("error")) {
                    postError<ShareResult?>(callback, json.getString("error"))
                    return@Runnable
                }

                val mapId = json.getInt("map_id")
                val shareUrl = json.optString("share_url", BASE_URL + "/maps/" + mapId)
                val isDuplicate = json.optBoolean("duplicate", false)

                val result = ShareResult(mapId, shareUrl, isDuplicate)
                postSuccess<ShareResult?>(callback, result)

                if (isDuplicate) {
                    tag(TAG).d("Map already exists: %d", mapId)
                } else {
                    tag(TAG).d("Map shared successfully: %d", mapId)
                }
            } catch (e: JSONException) {
                tag(TAG).e(e, "JSON error during map share")
                postError<ShareResult?>(callback, "Invalid response from server")
            } catch (e: IOException) {
                tag(TAG).e(e, "Network error during map share")
                postError<ShareResult?>(callback, "Network error: " + e.message)
            }
        })
    }

    /**
     * Make a POST request without authentication.
     */
    @Throws(IOException::class)
    private fun makePostRequest(endpoint: String?, body: String): String {
        val url = URL(BASE_URL + endpoint)
        val conn = url.openConnection() as HttpURLConnection

        try {
            conn.setRequestMethod("POST")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("X-App-Version", API_VERSION.toString())
            conn.setDoOutput(true)
            conn.setConnectTimeout(15000)
            conn.setReadTimeout(15000)

            conn.getOutputStream().use { os ->
                val input = body.toByteArray(StandardCharsets.UTF_8)
                os.write(input, 0, input.size)
            }
            return readResponse(conn)
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Make a POST request with authentication.
     */
    @Throws(IOException::class)
    private fun makeAuthenticatedPostRequest(endpoint: String?, body: String): String {
        val token = prefs.getString(KEY_AUTH_TOKEN, null)
        if (token == null) {
            throw IOException("Not authenticated")
        }

        val url = URL(BASE_URL + endpoint)
        val conn = url.openConnection() as HttpURLConnection

        try {
            conn.setRequestMethod("POST")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Authorization", "Bearer " + token)
            conn.setDoOutput(true)
            conn.setConnectTimeout(15000)
            conn.setReadTimeout(15000)

            conn.getOutputStream().use { os ->
                val input = body.toByteArray(StandardCharsets.UTF_8)
                os.write(input, 0, input.size)
            }
            return readResponse(conn)
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Make a GET request with authentication.
     */
    @Throws(IOException::class)
    private fun makeAuthenticatedGetRequest(endpoint: String?): String {
        val token = prefs.getString(KEY_AUTH_TOKEN, null)
        if (token == null) {
            throw IOException("Not authenticated")
        }

        val url = URL(BASE_URL + endpoint)
        val conn = url.openConnection() as HttpURLConnection

        try {
            conn.setRequestMethod("GET")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Authorization", "Bearer " + token)
            conn.setConnectTimeout(15000)
            conn.setReadTimeout(15000)

            return readResponse(conn)
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Read the response from an HTTP connection.
     */
    @Throws(IOException::class)
    private fun readResponse(conn: HttpURLConnection): String {
        val responseCode = conn.getResponseCode()

        val reader: BufferedReader?
        if (responseCode >= 200 && responseCode < 300) {
            reader =
                BufferedReader(InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))
        } else {
            reader =
                BufferedReader(InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))
        }

        val response = StringBuilder()
        var line: String?
        while ((reader.readLine().also { line = it }) != null) {
            response.append(line)
        }
        reader.close()

        val responseStr = response.toString()
        tag(TAG).d("Response (%d): %s", responseCode, responseStr)


        // If error response, wrap in error JSON
        if (responseCode >= 400) {
            try {
                val errorJson = JSONObject(responseStr)
                if (!errorJson.has("error")) {
                    errorJson.put(
                        "error",
                        errorJson.optString("message", "Request failed with code " + responseCode)
                    )
                }
                return errorJson.toString()
            } catch (e: JSONException) {
                return "{\"error\": \"Request failed with code " + responseCode + "\"}"
            }
        }

        return responseStr
    }

    /**
     * Post success callback to main thread.
     */
    private fun <T> postSuccess(callback: ApiCallback<T?>, result: T?) {
        mainHandler.post(Runnable { callback.onSuccess(result) })
    }

    /**
     * Post error callback to main thread.
     */
    private fun <T> postError(callback: ApiCallback<T?>, error: String?) {
        mainHandler.post(Runnable { callback.onError(error) })
    }

    /**
     * Result class for achievement sync.
     */
    class AchievementSyncResult(
        @JvmField val success: Boolean,
        @JvmField val syncedCount: Int,
        @JvmField val newAchievements: Int,
        @JvmField val statsUpdated: Boolean,
        @JvmField val latestAppVersion: String?
    )

    /**
     * Result class for fetching achievements from server.
     */
    class AchievementFetchResult(achievements: JSONArray?, stats: JSONObject?) {
        @JvmField
        val achievements: JSONArray?
        @JvmField
        val stats: JSONObject?

        init {
            this.achievements = achievements
            this.stats = stats
        }
    }

    /**
     * Fetch achievements from roboyard.z11.de for the logged-in user.
     * Used to restore achievements on a new device after login.
     * 
     * @param callback Callback for result
     */
    fun fetchAchievements(callback: ApiCallback<AchievementFetchResult?>) {
        if (!this.isLoggedIn) {
            postError<AchievementFetchResult?>(callback, "Not logged in")
            return
        }

        executor.execute(Runnable {
            try {
                val response = makeAuthenticatedGetRequest("/api/mobile/achievements")
                val json = JSONObject(response)

                if (json.has("error")) {
                    postError<AchievementFetchResult?>(callback, json.getString("error"))
                    return@Runnable
                }

                val user = json.getJSONObject("user")
                val achievements = user.getJSONArray("achievements")
                val stats = user.optJSONObject("stats")

                val result = AchievementFetchResult(achievements, stats)
                postSuccess<AchievementFetchResult?>(callback, result)

                tag(TAG).d(
                    "[ACHIEVEMENT_FETCH] Fetched %d achievements from server",
                    achievements.length()
                )
            } catch (e: JSONException) {
                tag(TAG).e(e, "[ACHIEVEMENT_FETCH] JSON error during fetch")
                postError<AchievementFetchResult?>(callback, "Invalid response from server")
            } catch (e: IOException) {
                tag(TAG).e(e, "[ACHIEVEMENT_FETCH] Network error during fetch")
                postError<AchievementFetchResult?>(callback, "Network error: " + e.message)
            }
        })
    }

    // ========== SAVE GAME SYNC ==========
    /**
     * Upload save games to server.
     */
    fun syncSaveGames(saves: JSONArray?, callback: ApiCallback<Int?>) {
        if (!this.isLoggedIn) {
            postError<Int?>(callback, "Not logged in")
            return
        }

        executor.execute(Runnable {
            try {
                val requestBody = JSONObject()
                requestBody.put("saves", saves)

                val response =
                    makeAuthenticatedPostRequest("/api/mobile/saves/sync", requestBody.toString())
                val json = JSONObject(response)

                if (json.has("error")) {
                    postError<Int?>(callback, json.getString("error"))
                    return@Runnable
                }

                val syncedCount = json.optInt("synced_count", 0)
                postSuccess<Int?>(callback, syncedCount)

                tag(TAG).d("[SAVE_SYNC] Uploaded %d save games to server", syncedCount)
            } catch (e: JSONException) {
                tag(TAG).e(e, "[SAVE_SYNC] JSON error during upload")
                postError<Int?>(callback, "Invalid response from server")
            } catch (e: IOException) {
                tag(TAG).e(e, "[SAVE_SYNC] Network error during upload")
                postError<Int?>(callback, "Network error: " + e.message)
            }
        })
    }

    /**
     * Download save games from server.
     */
    fun fetchSaveGames(callback: ApiCallback<JSONArray?>) {
        if (!this.isLoggedIn) {
            postError<JSONArray?>(callback, "Not logged in")
            return
        }

        executor.execute(Runnable {
            try {
                val response = makeAuthenticatedGetRequest("/api/mobile/saves")
                val json = JSONObject(response)

                if (json.has("error")) {
                    postError<JSONArray?>(callback, json.getString("error"))
                    return@Runnable
                }

                val saves = json.getJSONArray("saves")
                postSuccess<JSONArray?>(callback, saves)

                tag(TAG).d("[SAVE_SYNC] Fetched %d save games from server", saves.length())
            } catch (e: JSONException) {
                tag(TAG).e(e, "[SAVE_SYNC] JSON error during fetch")
                postError<JSONArray?>(callback, "Invalid response from server")
            } catch (e: IOException) {
                tag(TAG).e(e, "[SAVE_SYNC] Network error during fetch")
                postError<JSONArray?>(callback, "Network error: " + e.message)
            }
        })
    }

    // ========== GAME HISTORY SYNC ==========
    /**
     * Upload game history to server.
     */
    fun syncHistory(history: JSONArray?, callback: ApiCallback<JSONObject?>) {
        if (!this.isLoggedIn) {
            postError<JSONObject?>(callback, "Not logged in")
            return
        }

        executor.execute(Runnable {
            try {
                val requestBody = JSONObject()
                requestBody.put("history", history)

                val response =
                    makeAuthenticatedPostRequest("/api/mobile/history/sync", requestBody.toString())
                val json = JSONObject(response)

                if (json.has("error")) {
                    postError<JSONObject?>(callback, json.getString("error"))
                    return@Runnable
                }

                val syncedCount = json.optInt("synced_count", 0)
                val skippedCount = json.optInt("skipped_count", 0)
                val totalEntries = json.optInt("total_entries", 0)

                tag(TAG).d(
                    "[HISTORY_SYNC] Server response: synced=%d, skipped=%d, total=%d",
                    syncedCount, skippedCount, totalEntries
                )


                // Log details if available
                if (json.has("details")) {
                    val details = json.getJSONArray("details")
                    for (i in 0..<details.length()) {
                        val detail = details.getJSONObject(i)
                        val action = detail.optString("action", "unknown")
                        val mapName = detail.optString("map_name", "Unknown")
                        val stars = detail.optInt("stars_earned", 0)

                        if ("updated" == action) {
                            val changes = detail.optString("changes", "")
                            tag(TAG).d("[HISTORY_SYNC] ✓ Updated '%s': %s", mapName, changes)
                        } else if ("created" == action) {
                            tag(TAG).d("[HISTORY_SYNC] ✓ Created '%s': stars=%d", mapName, stars)
                        } else if ("skipped" == action) {
                            val reason = detail.optString("reason", "")
                            tag(TAG).d("[HISTORY_SYNC] ⊘ Skipped '%s': %s", mapName, reason)
                        }
                    }
                }

                postSuccess<JSONObject?>(callback, json)
            } catch (e: JSONException) {
                tag(TAG).e(e, "[HISTORY_SYNC] JSON error during upload")
                postError<JSONObject?>(callback, "Invalid response from server")
            } catch (e: IOException) {
                tag(TAG).e(e, "[HISTORY_SYNC] Network error during upload")
                postError<JSONObject?>(callback, "Network error: " + e.message)
            }
        })
    }

    /**
     * Download game history from server.
     */
    fun fetchHistory(callback: ApiCallback<JSONArray?>) {
        if (!this.isLoggedIn) {
            postError<JSONArray?>(callback, "Not logged in")
            return
        }

        executor.execute(Runnable {
            try {
                val response = makeAuthenticatedGetRequest("/api/mobile/history")
                val json = JSONObject(response)

                if (json.has("error")) {
                    postError<JSONArray?>(callback, json.getString("error"))
                    return@Runnable
                }

                val history = json.getJSONArray("history")
                postSuccess<JSONArray?>(callback, history)

                tag(TAG).d(
                    "[HISTORY_SYNC] Fetched %d history entries from server",
                    history.length()
                )
            } catch (e: JSONException) {
                tag(TAG).e(e, "[HISTORY_SYNC] JSON error during fetch")
                postError<JSONArray?>(callback, "Invalid response from server")
            } catch (e: IOException) {
                tag(TAG).e(e, "[HISTORY_SYNC] Network error during fetch")
                postError<JSONArray?>(callback, "Network error: " + e.message)
            }
        })
    }

    // ========== ACHIEVEMENT SYNC ==========
    /**
     * Sync achievements to roboyard.z11.de.
     * 
     * @param achievements List of achievement data (id, unlocked, timestamp)
     * @param stats Game statistics
     * @param callback Callback for result
     */
    fun syncAchievements(
        achievements: JSONArray?,
        stats: JSONObject?,
        callback: ApiCallback<AchievementSyncResult?>
    ) {
        if (!this.isLoggedIn) {
            postError<AchievementSyncResult?>(callback, "Not logged in")
            return
        }

        executor.execute(Runnable {
            try {
                val requestBody = JSONObject()
                requestBody.put("achievements", achievements)
                if (stats != null) {
                    requestBody.put("stats", stats)
                }

                val response = makeAuthenticatedPostRequest(
                    "/api/mobile/achievements/sync",
                    requestBody.toString()
                )
                val json = JSONObject(response)

                if (json.has("error")) {
                    postError<AchievementSyncResult?>(callback, json.getString("error"))
                    return@Runnable
                }

                val success = json.optBoolean("success", false)
                val syncedCount = json.optInt("synced_count", 0)
                val newAchievements = json.optInt("new_achievements", 0)
                val statsUpdated = json.optBoolean("stats_updated", false)
                val latestAppVersion =
                    if (json.isNull("latest_app_version")) null else json.optString(
                        "latest_app_version",
                        null
                    )

                val result = AchievementSyncResult(
                    success,
                    syncedCount,
                    newAchievements,
                    statsUpdated,
                    latestAppVersion
                )
                postSuccess<AchievementSyncResult?>(callback, result)

                tag(TAG).d(
                    "[ACHIEVEMENT_SYNC] Sync successful: %d synced, %d new",
                    syncedCount,
                    newAchievements
                )
            } catch (e: JSONException) {
                tag(TAG).e(e, "[ACHIEVEMENT_SYNC] JSON error during sync")
                postError<AchievementSyncResult?>(callback, "Invalid response from server")
            } catch (e: IOException) {
                tag(TAG).e(e, "[ACHIEVEMENT_SYNC] Network error during sync")
                postError<AchievementSyncResult?>(callback, "Network error: " + e.message)
            }
        })
    }

    companion object {
        private const val TAG = "RoboyardApi"
        private const val BASE_URL = "https://roboyard.z11.de"
        private const val PREFS_NAME = "roboyard_api"

        /** Current API protocol version sent with every request. Increment on breaking changes.  */
        const val API_VERSION: Int = 1
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_PASSWORD = "user_password"

        private var instance: RoboyardApiClient? = null

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): RoboyardApiClient {
            if (instance == null) {
                instance = RoboyardApiClient(context)
            }
            return instance!!
        }
    }
}
