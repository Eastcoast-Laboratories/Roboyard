package roboyard.logic.platform

import roboyard.logic.network.NetworkMonitor

/**
 * iOS placeholder implementation of NetworkMonitor.
 * TODO: Implement with platform.Network.NWPathMonitor for production iOS.
 */
class IosNetworkMonitor : NetworkMonitor {
    override fun isNetworkAvailable(): Boolean = true
}
