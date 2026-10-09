package roboyard.logic.util

/**
 * Platform-specific date utilities for streak calculation.
 * Provides timezone-aware day numbering and ISO date formatting.
 */
expect object DateUtils {
    /**
     * Get the timezone offset in milliseconds for the given timestamp.
     */
    fun getTimezoneOffsetMs(timestampMs: Long): Long

    /**
     * Format a timestamp as ISO date string (yyyy-MM-dd).
     */
    fun formatDateIso(timestampMs: Long): String

    /**
     * Parse an ISO date string (yyyy-MM-dd) to timestamp in milliseconds.
     * Returns 0 on parse failure.
     */
    fun parseDateIso(dateString: String): Long

    /**
     * Parse a full ISO 8601 timestamp string (yyyy-MM-dd'T'HH:mm:ssXXX) to timestamp in milliseconds.
     * Returns 0 on parse failure.
     */
    fun parseTimestampIso(timestampString: String): Long

    /**
     * Get the system timezone ID (e.g., "Europe/Berlin", "America/New_York").
     */
    fun getTimezoneId(): String
}

/**
 * Pure-Kotlin UTC timestamp formatters used by history sync — no expect/actual
 * needed, kotlin.time.Instant is multiplatform.
 */
@OptIn(kotlin.time.ExperimentalTime::class)
object DateFormatUtils {
    /** "yyyy-MM-dd'T'HH:mm:ss+00:00" — matches Android SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX") with TZ=UTC. */
    fun formatIsoUtcOffset(timestampMs: Long): String =
        kotlin.time.Instant.fromEpochMilliseconds(timestampMs)
            .toString().take(19) + "+00:00"

    /** "yyyy-MM-dd HH:mm:ss" in UTC — for the [HISTORY_SYNC] timezone-debug log. */
    fun formatUtcDateTime(timestampMs: Long): String =
        kotlin.time.Instant.fromEpochMilliseconds(timestampMs)
            .toString().take(19).replace('T', ' ')

    /**
     * "yyyy-MM-dd HH:mm:ss" in the system timezone — matches Android
     * SimpleDateFormat("yyyy-MM-dd HH:mm:ss") with the default timezone.
     */
    fun formatLocalDateTime(timestampMs: Long): String =
        formatUtcDateTime(timestampMs + DateUtils.getTimezoneOffsetMs(timestampMs))
}
