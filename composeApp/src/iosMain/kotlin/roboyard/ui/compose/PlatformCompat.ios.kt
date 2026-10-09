package roboyard.ui.compose

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSProcessInfo
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localeWithLocaleIdentifier
import platform.darwin.KERN_SUCCESS
import platform.darwin.MACH_TASK_BASIC_INFO
import platform.darwin.MACH_TASK_BASIC_INFO_COUNT
import platform.darwin.mach_msg_type_number_tVar
import platform.darwin.mach_task_basic_info_data_t
import platform.darwin.mach_task_self_
import platform.darwin.task_info
import kotlin.system.exitProcess

@OptIn(ExperimentalForeignApi::class)
actual fun platformSha256(bytes: ByteArray): ByteArray {
    val md = ByteArray(CC_SHA256_DIGEST_LENGTH)
    bytes.usePinned { inPin ->
        md.usePinned { outPin ->
            CC_SHA256(
                if (bytes.isEmpty()) null else inPin.addressOf(0),
                bytes.size.convert(),
                outPin.addressOf(0).reinterpret()
            )
        }
    }
    return md
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class PlatformDateFormat actual constructor(pattern: String, deviceLocale: Boolean) {
    private val formatter = NSDateFormatter().apply {
        dateFormat = pattern
        if (!deviceLocale) {
            locale = NSLocale.localeWithLocaleIdentifier("en_US_POSIX")
        }
    }

    actual fun format(epochMillis: Long): String =
        formatter.stringFromDate(
            NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)
        )
}

@OptIn(ExperimentalForeignApi::class)
actual fun platformMemoryStatsMb(): Pair<Long, Long>? = memScoped {
    val info = alloc<mach_task_basic_info_data_t>()
    val count = alloc<mach_msg_type_number_tVar>()
    count.value = MACH_TASK_BASIC_INFO_COUNT.toUInt()
    val kr = task_info(
        mach_task_self_,
        MACH_TASK_BASIC_INFO.toUInt(),
        info.ptr.reinterpret(),
        count.ptr
    )
    if (kr != KERN_SUCCESS) return null
    val used = info.resident_size.toLong() / 1024 / 1024
    val max = NSProcessInfo.processInfo.physicalMemory.toLong() / 1024 / 1024
    used to max
}

actual fun platformRestartApp() {
    // iOS apps cannot relaunch themselves; exiting lets the user reopen the app.
    println("[DEBUG] Self-relaunch unsupported on iOS, exiting")
    exitProcess(0)
}
