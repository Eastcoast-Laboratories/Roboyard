package roboyard.logic.ui

/**
 * Android implementation of getStringProvider.
 * The StringProvider is set from the app module at startup via setStringProvider().
 */

@Volatile
private var instance: StringProvider? = null

/**
 * Set the StringProvider instance from the app module.
 * Called at app startup from RoboyardApplication.onCreate().
 */
fun setStringProvider(provider: StringProvider) {
    instance = provider
}

actual fun getStringProvider(): StringProvider {
    return instance ?: object : StringProvider {
        override fun getString(name: String): String? = null
    }
}
