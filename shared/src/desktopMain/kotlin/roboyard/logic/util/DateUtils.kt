package roboyard.logic.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Android/JVM implementation of DateUtils using java.util.
 */
actual object DateUtils {
    actual fun getTimezoneOffsetMs(timestampMs: Long): Long {
        return TimeZone.getDefault().getOffset(timestampMs).toLong()
    }

    actual fun formatDateIso(timestampMs: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestampMs))
    }

    actual fun parseDateIso(dateString: String): Long {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateString).time
        } catch (e: Exception) {
            0L
        }
    }

    actual fun parseTimestampIso(timestampString: String): Long {
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(timestampString).time
        } catch (e: Exception) {
            0L
        }
    }

    actual fun getTimezoneId(): String {
        return TimeZone.getDefault().id
    }
}
