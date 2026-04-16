package roboyard.eclabs.achievements;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * Tests that streak date conversions are timezone-consistent.
 * The day number must always represent the local date, and round-trip
 * conversions (dayNumber → dateString → dayNumber) must be identity.
 */
public class StreakTimezoneTest {

    private static final long NORMAL_DAY_MS = 24L * 60L * 60L * 1000L;

    /**
     * Replicate getTodayDate() logic
     */
    private long getTodayDate(long nowMillis, TimeZone tz) {
        int offsetMs = tz.getOffset(nowMillis);
        return (nowMillis + offsetMs) / NORMAL_DAY_MS;
    }

    /**
     * Replicate dayNumberToDateString() logic
     */
    private String dayNumberToDateString(long dayNumber, TimeZone tz) {
        long timestampMs = dayNumber * NORMAL_DAY_MS;
        int offsetMs = tz.getOffset(timestampMs);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(tz);
        return sdf.format(new Date(timestampMs - offsetMs));
    }

    /**
     * Replicate parseDateStringToDayNumber() logic
     */
    private long parseDateStringToDayNumber(String dateString, TimeZone tz) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setTimeZone(tz);
            long timestampMs = sdf.parse(dateString).getTime();
            int offsetMs = tz.getOffset(timestampMs);
            return (timestampMs + offsetMs) / NORMAL_DAY_MS;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testRoundTrip_BerlinTimezone_April17() {
        TimeZone berlin = TimeZone.getTimeZone("Europe/Berlin");

        // April 17, 2026 01:30 CEST = April 16, 23:30 UTC
        // In UTC millis: we need to compute this
        long april16_midnight_utc = computeEpochMs(2026, 4, 16, 0, 0);
        long nowMillis = april16_midnight_utc + 23 * 3600_000L + 30 * 60_000L; // 23:30 UTC

        long dayNumber = getTodayDate(nowMillis, berlin);
        String dateStr = dayNumberToDateString(dayNumber, berlin);
        long roundTripped = parseDateStringToDayNumber(dateStr, berlin);

        assertEquals("Round-trip day number must match", dayNumber, roundTripped);
        assertEquals("Date string must be April 17 (local Berlin time)", "2026-04-17", dateStr);
    }

    @Test
    public void testRoundTrip_BerlinTimezone_April16_noon() {
        TimeZone berlin = TimeZone.getTimeZone("Europe/Berlin");

        // April 16, 2026 12:00 CEST = April 16, 10:00 UTC
        long april16_midnight_utc = computeEpochMs(2026, 4, 16, 0, 0);
        long nowMillis = april16_midnight_utc + 10 * 3600_000L; // 10:00 UTC

        long dayNumber = getTodayDate(nowMillis, berlin);
        String dateStr = dayNumberToDateString(dayNumber, berlin);
        long roundTripped = parseDateStringToDayNumber(dateStr, berlin);

        assertEquals("Round-trip day number must match", dayNumber, roundTripped);
        assertEquals("Date string must be April 16", "2026-04-16", dateStr);
    }

    @Test
    public void testRoundTrip_UTC() {
        TimeZone utc = TimeZone.getTimeZone("UTC");

        long april17_midnight_utc = computeEpochMs(2026, 4, 17, 0, 0);
        long nowMillis = april17_midnight_utc + 1 * 3600_000L; // 01:00 UTC

        long dayNumber = getTodayDate(nowMillis, utc);
        String dateStr = dayNumberToDateString(dayNumber, utc);
        long roundTripped = parseDateStringToDayNumber(dateStr, utc);

        assertEquals("Round-trip day number must match", dayNumber, roundTripped);
        assertEquals("Date string must be April 17", "2026-04-17", dateStr);
    }

    @Test
    public void testDayDifference_BerlinTimezone() {
        TimeZone berlin = TimeZone.getTimeZone("Europe/Berlin");

        long april16Day = parseDateStringToDayNumber("2026-04-16", berlin);
        long april17Day = parseDateStringToDayNumber("2026-04-17", berlin);

        assertEquals("April 16 and 17 must differ by exactly 1", 1, april17Day - april16Day);
    }

    @Test
    public void testGetTodayDate_BerlinAfterMidnight_returnsLocalDate() {
        TimeZone berlin = TimeZone.getTimeZone("Europe/Berlin");

        // April 17, 2026 00:30 CEST = April 16, 22:30 UTC
        long april16_midnight_utc = computeEpochMs(2026, 4, 16, 0, 0);
        long nowMillis = april16_midnight_utc + 22 * 3600_000L + 30 * 60_000L;

        long dayNumber = getTodayDate(nowMillis, berlin);
        String dateStr = dayNumberToDateString(dayNumber, berlin);

        assertEquals("After midnight CEST, date must be April 17", "2026-04-17", dateStr);
    }

    @Test
    public void testGetTodayDate_BerlinBeforeMidnight_returnsSameDay() {
        TimeZone berlin = TimeZone.getTimeZone("Europe/Berlin");

        // April 16, 2026 23:30 CEST = April 16, 21:30 UTC
        long april16_midnight_utc = computeEpochMs(2026, 4, 16, 0, 0);
        long nowMillis = april16_midnight_utc + 21 * 3600_000L + 30 * 60_000L;

        long dayNumber = getTodayDate(nowMillis, berlin);
        String dateStr = dayNumberToDateString(dayNumber, berlin);

        assertEquals("Before midnight CEST, date must still be April 16", "2026-04-16", dateStr);
    }

    @Test
    public void testServerDateParsing_BerlinTimezone() {
        TimeZone berlin = TimeZone.getTimeZone("Europe/Berlin");

        // Server returns "2026-04-16"
        // At 01:30 CEST (April 17 local), daysSince should be 1
        long april16_midnight_utc = computeEpochMs(2026, 4, 16, 0, 0);
        long nowMillis = april16_midnight_utc + 23 * 3600_000L + 30 * 60_000L; // 23:30 UTC = 01:30 CEST

        long today = getTodayDate(nowMillis, berlin);
        long serverDay = parseDateStringToDayNumber("2026-04-16", berlin);
        long daysSince = today - serverDay;

        assertEquals("Days since April 16, viewed from April 17 01:30 CEST", 1, daysSince);
    }

    /**
     * Compute epoch millis for a given UTC date/time
     */
    private long computeEpochMs(int year, int month, int day, int hour, int minute) {
        java.util.Calendar cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(year, month - 1, day, hour, minute, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
