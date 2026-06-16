package roboyard.logic.auth

/**
 * Desktop implementation of AuthManager.
 * Auth is not implemented on Desktop, so this is a no-op.
 */
class DesktopAuthManager : AuthManager {
    override fun isLoggedIn(): Boolean {
        return false
    }
    
    override fun verifyToken(callback: (Boolean) -> Unit) {
        callback(false)
    }
    
    override fun login(username: String, password: String, callback: (Boolean) -> Unit) {
        callback(false)
    }
    
    override fun logout() {
        // No-op on Desktop
    }
    
    override fun getToken(): String? {
        return null
    }
}

actual fun getAuthManager(): AuthManager {
    return DesktopAuthManager()
}
