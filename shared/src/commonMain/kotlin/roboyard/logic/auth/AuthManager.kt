package roboyard.logic.auth

/**
 * Interface for managing authentication in a platform-agnostic way.
 * Handles login, logout, and token verification.
 */
interface AuthManager {
    /**
     * Check if the user is currently logged in.
     * @return true if logged in, false otherwise
     */
    fun isLoggedIn(): Boolean
    
    /**
     * Verify the current auth token with the server.
     * @param callback Callback with result (true if valid, false otherwise)
     */
    fun verifyToken(callback: (Boolean) -> Unit)
    
    /**
     * Log in with username and password.
     * @param username The username
     * @param password The password
     * @param callback Callback with result (true if successful, false otherwise)
     */
    fun login(username: String, password: String, callback: (Boolean) -> Unit)
    
    /**
     * Log out the current user.
     */
    fun logout()
    
    /**
     * Get the current auth token.
     * @return The auth token, or null if not logged in
     */
    fun getToken(): String?
}

/**
 * Factory function to get the platform-specific auth manager.
 */
expect fun getAuthManager(): AuthManager
