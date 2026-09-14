package roboyard.logic.util

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTimeZone
import platform.Foundation.systemTimeZone
import platform.Foundation.timeIntervalSince1970

/**
 * iOS implementation of DateUtils using Foundation.
 */
actual object DateUtils {
    actual fun getTimezoneOffsetMs(timestampMs: Long): Long {
        val tz = NSTimeZone.systemTimeZone()
        return tz.secondsFromGMT().toLong() * 1000L
    }

    actual fun formatDateIso(timestampMs: Long): String {
        val formatter = NSDateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.timeZone = NSTimeZone.systemTimeZone()
        val date = NSDate(timestampMs.toDouble() / 1000.0)
        return formatter.stringFromDate(date)
    }

    actual fun parseDateIso(dateString: String): Long {
        return try {
            val formatter = NSDateFormatter()
            formatter.dateFormat = "yyyy-MM-dd"
            formatter.timeZone = NSTimeZone.systemTimeZone()
            val date = formatter.dateFromString(dateString)
            if (date != null) {
                (date.timeIntervalSince1970 * 1000.0).toLong()
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    actual fun parseTimestampIso(timestampString: String): Long {
        return try {
            val formatter = NSDateFormatter()
            formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ssZZZZZ"
            formatter.timeZone = NSTimeZone.systemTimeZone()
            val date = formatter.dateFromString(timestampString)
            if (date != null) {
                (date.timeIntervalSince1970 * 1000.0).toLong()
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    actual fun getTimezoneId(): String {
        return NSTimeZone.systemTimeZone().name
    }
}
