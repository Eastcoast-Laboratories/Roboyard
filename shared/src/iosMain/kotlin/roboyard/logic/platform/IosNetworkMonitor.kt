package roboyard.logic.platform

import platform.Network.NWPathMonitor
import platform.Network.NWInterface
import platform.Network.NWPath
import platform.Foundation.NSOperationQueue
import roboyard.logic.network.NetworkMonitor

/**
 * iOS implementation of NetworkMonitor using NWPathMonitor.
 */
class IosNetworkMonitor : NetworkMonitor {
    private var pathMonitor: NWPathMonitor? = null
    private var currentPath: NWPath? = null
    private var isAvailable = false

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        pathMonitor = NWPathMonitor()
        pathMonitor?.pathUpdateHandler = { path ->
            currentPath = path
            isAvailable = path.status == NWPath.Status.Satisfied
        }
        
        val queue = NSOperationQueue.currentQueue
        pathMonitor?.start(queue)
    }

    override fun isNetworkAvailable(): Boolean {
        return isAvailable
    }

    fun stopMonitoring() {
        pathMonitor?.cancel()
        pathMonitor = null
    }
}
