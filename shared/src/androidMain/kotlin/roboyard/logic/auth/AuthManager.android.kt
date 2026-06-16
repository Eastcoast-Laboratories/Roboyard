package roboyard.logic.auth

import android.content.Context

/**
 * Android implementation of AuthManager.
 * Simplified version that stores auth state in preferences.
 * Full RoboyardApiClient integration will be done when MainActivity is replaced.
 */
class AndroidAuthManager(private val context: Context) : AuthManager {
    
    private var loggedIn = false
    private var token: String? = null
    
    override fun isLoggedIn(): Boolean {
        return loggedIn
    }
    
    override fun verifyToken(callback: (Boolean) -> Unit) {
        // Placeholder for token verification
        // Will be integrated with RoboyardApiClient when MainActivity is replaced
        callback(loggedIn)
    }
    
    override fun login(username: String, password: String, callback: (Boolean) -> Unit) {
        // Placeholder for login
        // Will be integrated with RoboyardApiClient when MainActivity is replaced
        callback(false)
    }
    
    override fun logout() {
        loggedIn = false
        token = null
    }
    
    override fun getToken(): String? {
        return token
    }
}

actual fun getAuthManager(): AuthManager {
    throw IllegalStateException("AndroidAuthManager requires Context. Use AndroidAuthManager(context) directly.")
}
