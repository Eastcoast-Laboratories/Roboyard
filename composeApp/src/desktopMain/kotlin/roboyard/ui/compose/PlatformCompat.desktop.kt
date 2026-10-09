package roboyard.ui.compose

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

actual fun platformSha256(bytes: ByteArray): ByteArray =
    MessageDigest.getInstance("SHA-256").digest(bytes)

actual class PlatformDateFormat actual constructor(pattern: String, deviceLocale: Boolean) {
    private val sdf = SimpleDateFormat(pattern, if (deviceLocale) Locale.getDefault() else Locale.US)
    actual fun format(epochMillis: Long): String = sdf.format(Date(epochMillis))
}

actual fun platformMemoryStatsMb(): Pair<Long, Long>? {
    val runtime = Runtime.getRuntime()
    val used = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    val max = runtime.maxMemory() / 1024 / 1024
    return used to max
}

actual fun platformRestartApp() {
    val javaBin = System.getProperty("java.home") + "/bin/java"
    val cmd = arrayListOf(
        javaBin, "-cp", System.getProperty("java.class.path"),
        "roboyard.MainKt"
    )
    ProcessBuilder(cmd).start()
    exitProcess(0)
}
