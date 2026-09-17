package roboyard.logic.network

import java.net.InetSocketAddress
import java.net.Socket

/**
 * Desktop implementation of NetworkMonitor.
 * Checks reachability of the configured API host with a short socket connect.
 */
class DesktopNetworkMonitor(private val hostProvider: () -> String) : NetworkMonitor {

    override fun isNetworkAvailable(): Boolean {
        return try {
            val host = hostProvider()
                .removePrefix("http://").removePrefix("https://")
                .substringBefore("/").substringBefore(":")
            val port = if (hostProvider().startsWith("http://")) 80 else 443
            Socket().use { it.connect(InetSocketAddress(host, port), 2000) }
            true
        } catch (e: Exception) {
            false
        }
    }
}
