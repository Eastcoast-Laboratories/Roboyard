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
