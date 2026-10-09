package driftingdroids.model

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.Foundation.NSDate
import platform.Foundation.NSProcessInfo
import platform.Foundation.timeIntervalSince1970
import platform.posix.RUSAGE_SELF
import platform.posix.getrusage
import platform.posix.rusage

/**
 * iOS implementation of the solver time provider.
 * nanoTime() uses process uptime (monotonic, unaffected by wall clock changes).
 * getRuntimeMemoryInfo reports peak resident size via getrusage since Kotlin/Native
 * has no managed-heap notion like the JVM Runtime API.
 */
@OptIn(ExperimentalForeignApi::class)
actual object TimeProvider {
    actual fun currentTimeMillis(): Long =
        (NSDate().timeIntervalSince1970 * 1000.0).toLong()

    actual fun nanoTime(): Long =
        (NSProcessInfo.processInfo.systemUptime * 1_000_000_000.0).toLong()

    actual fun getRuntimeMemoryInfo(): RuntimeMemoryInfo {
        var maxResidentBytes = 0L
        memScoped {
            val usage = alloc<rusage>()
            if (getrusage(RUSAGE_SELF, usage.ptr) == 0) {
                maxResidentBytes = usage.ru_maxrss
            }
        }
        return RuntimeMemoryInfo(
            freeMemory = 0L,
            totalMemory = maxResidentBytes,
            maxMemory = NSProcessInfo.processInfo.physicalMemory.toLong()
        )
    }
}
