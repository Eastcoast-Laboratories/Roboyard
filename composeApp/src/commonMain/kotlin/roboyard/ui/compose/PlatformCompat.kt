package roboyard.ui.compose

/**
 * SHA-256 digest of the given bytes (MessageDigest on JVM, CommonCrypto on iOS).
 */
expect fun platformSha256(bytes: ByteArray): ByteArray

/**
 * Formats an epoch-millis timestamp using a SimpleDateFormat-style pattern
 * (e.g. "dd.MM.yyyy HH:mm").
 * @param deviceLocale true mirrors Locale.getDefault(), false mirrors Locale.US
 */
expect class PlatformDateFormat(pattern: String, deviceLocale: Boolean) {
    fun format(epochMillis: Long): String
}

/**
 * Used heap and maximum heap in MB for the debug memory readout,
 * or null when the platform cannot report memory stats.
 */
expect fun platformMemoryStatsMb(): Pair<Long, Long>?

/**
 * Relaunches the app process where the platform supports self-relaunch
 * (desktop JVM); otherwise logs and exits so the user can reopen the app.
 */
expect fun platformRestartApp()
