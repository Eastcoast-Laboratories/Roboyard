package roboyard.logic.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import roboyard.logic.network.RoboyardApiClient
import roboyard.logic.storage.PlatformStorage
import roboyard.logic.storage.getPlatformStorage

/**
 * Desktop implementation of AuthManager backed by the shared RoboyardApiClient.
 */
class DesktopAuthManager(storage: PlatformStorage) : AuthManager {

    private val apiClient = RoboyardApiClient.getInstance(
        storage,
        CoroutineScope(Dispatchers.Default),
        { "desktop" }
    )

    override fun isLoggedIn(): Boolean = apiClient.isLoggedIn

    override fun verifyToken(callback: (Boolean) -> Unit) {
        apiClient.verifyToken(object : RoboyardApiClient.ApiCallback<Boolean?> {
            override fun onSuccess(result: Boolean?) = callback(result == true)
            override fun onError(error: String?) = callback(false)
        })
    }

    override fun login(username: String, password: String, callback: (Boolean) -> Unit) {
        apiClient.login(username, password, object : RoboyardApiClient.ApiCallback<RoboyardApiClient.LoginResult?> {
            override fun onSuccess(result: RoboyardApiClient.LoginResult?) = callback(result != null)
            override fun onError(error: String?) = callback(false)
        })
    }

    override fun logout() = apiClient.logout()

    override fun getToken(): String? = apiClient.authToken
}

actual fun getAuthManager(): AuthManager {
    return DesktopAuthManager(getPlatformStorage())
}
