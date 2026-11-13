package com.shrug.shrugtv.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern

object TimeUtil {

    private val TWITCH_DURATION = Pattern.compile("^(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?$")
    private val LEGACY_WITH_OFFSET = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ROOT)

    /** Dauer in Sekunden als "HH:mm:ss", z. B. 3725 -> "01:02:05", 90000 -> "25:00:00". */
    @JvmStatic
    fun hms(seconds: Long): String {
        val s = Math.max(0, seconds)
        return String.format(Locale.ROOT, "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
    }

    /** Dauer in Sekunden als "HH:mm", z. B. 3725 -> "01:02". */
    @JvmStatic
    fun hm(seconds: Long): String {
        val s = Math.max(0, seconds)
        return String.format(Locale.ROOT, "%02d:%02d", s / 3600, (s / 60) % 60)
    }

    /** Sekunden zwischen zwei Zeitpunkten (b nach a positiv). */
    @JvmStatic
    fun secondsBetween(a: ZonedDateTime, b: ZonedDateTime): Long {
        return Duration.between(a, b).seconds
    }

    /** Now in UTC (like Joda's new DateTime(DateTimeZone.UTC)). */
    @JvmStatic
    fun nowUtc(): ZonedDateTime {
        return ZonedDateTime.now(ZoneOffset.UTC)
    }

    /** Now in the device's zone (like Joda's DateTime.now()). */
    @JvmStatic
    fun now(): ZonedDateTime {
        return ZonedDateTime.now()
    }

    /** Day of the year, for picture signatures that change daily. */
    @JvmStatic
    fun dayOfYear(): Int {
        return LocalDate.now().dayOfYear
    }

    /**
     * ISO time from Helix ("2026-10-28T06:00:00Z") or from the database.
     */
    @JvmStatic
    fun parseIso(text: String): ZonedDateTime {
        return try {
            Instant.parse(text).atZone(ZoneOffset.UTC)
        } catch (e: RuntimeException) {
            OffsetDateTime.parse(text, LEGACY_WITH_OFFSET).atZoneSameInstant(ZoneOffset.UTC)
        }
    }

    /** Instant to store. */
    @JvmStatic
    fun isoNowUtc(): String {
        return Instant.now().toString()
    }

    /** Time "H:mm" in the device's zone for a timestamp in ms (chat). */
    @JvmStatic
    fun timeOfDay(millis: Long): String {
        return DateTimeFormatter.ofPattern("H:mm", Locale.ROOT)
            .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
    }

    /** Datum wie "Oct 28, 2026". */
    @JvmStatic
    fun date(time: ZonedDateTime): String {
        return DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()).format(time)
    }

    /** Twitch duration "1h2m3s", "2m3s" or "3s" in seconds; -1 if the format does not fit. */
    @JvmStatic
    fun twitchDuration(text: String?): Int {
        if (text == null) {
            return -1
        }
        val trimmed = text.trim()
        val m = TWITCH_DURATION.matcher(trimmed)
        if (!m.matches() || trimmed.isEmpty()) {
            return -1
        }
        val h = m.group(1)?.toIntOrNull() ?: 0
        val min = m.group(2)?.toIntOrNull() ?: 0
        val s = m.group(3)?.toIntOrNull() ?: 0
        return h * 3600 + min * 60 + s
    }
}
