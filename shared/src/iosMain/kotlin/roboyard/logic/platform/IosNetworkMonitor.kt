package roboyard.logic.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UIntVarOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.SystemConfiguration.SCNetworkReachabilityCreateWithName
import platform.SystemConfiguration.SCNetworkReachabilityGetFlags
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsConnectionRequired
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsReachable
import roboyard.logic.network.NetworkMonitor

/**
 * iOS implementation of NetworkMonitor via SCNetworkReachability.
 *
 * Network.framework's NWPathMonitor is a C API and not bound in
 * Kotlin/Native; SCNetworkReachability provides a synchronous flag check.
 * The probe targets the backend host since the monitor only gates sync work.
 */
@OptIn(ExperimentalForeignApi::class)
class IosNetworkMonitor : NetworkMonitor {

    override fun isNetworkAvailable(): Boolean = memScoped {
        val target = SCNetworkReachabilityCreateWithName(null, "roboyard.z11.de")
            ?: return@memScoped false
        val flags = alloc<UIntVarOf<UInt>>()
        if (!SCNetworkReachabilityGetFlags(target, flags.ptr)) {
            return@memScoped false
        }
        val f = flags.ptr[0]
        // Reachable and no connection still required
        (f and kSCNetworkReachabilityFlagsReachable) != 0u &&
            (f and kSCNetworkReachabilityFlagsConnectionRequired) == 0u
    }
}
