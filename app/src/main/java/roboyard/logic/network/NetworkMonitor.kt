package roboyard.logic.network

/**
 * Platform-agnostic network monitoring interface.
 * Abstracts Android-specific ConnectivityManager for KMP compatibility.
 */
interface NetworkMonitor {
    /**
     * Check if network is available and connected.
     * @return true if network is available, false otherwise
     */
    fun isNetworkAvailable(): Boolean
}
