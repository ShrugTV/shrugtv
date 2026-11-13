package com.shrug.shrugtv.util;

import static org.junit.Assert.assertEquals;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.Test;

/** Replacement for Joda-Time: same output as the old PeriodFormatters. */
public class TimeUtilTest {

    @Test
    public void durationLikeJoda() {
        assertEquals("00:00:00", TimeUtil.hms(0));
        assertEquals("01:02:05", TimeUtil.hms(3725));
        assertEquals("00:59:59", TimeUtil.hms(3599));
        // Hours open at the top; Joda (normalizedStandard) would have split off days here
        assertEquals("25:00:00", TimeUtil.hms(90000));
        assertEquals("01:02", TimeUtil.hm(3725));
        assertEquals("00:00", TimeUtil.hm(59));
        assertEquals("00:00:00", TimeUtil.hms(-5));
    }

    @Test
    public void twitchDuration() {
        assertEquals(3723, TimeUtil.twitchDuration("1h2m3s"));
        assertEquals(123, TimeUtil.twitchDuration("2m3s"));
        assertEquals(3, TimeUtil.twitchDuration("3s"));
        assertEquals(36000, TimeUtil.twitchDuration("10h0m0s"));
        assertEquals(-1, TimeUtil.twitchDuration("abc"));
        assertEquals(-1, TimeUtil.twitchDuration(""));
        assertEquals(-1, TimeUtil.twitchDuration(null));
    }

    @Test
    public void isoFromHelixAndFromOldDatabase() {
        ZonedDateTime expected = ZonedDateTime.of(2026, 10, 28, 6, 0, 0, 0, ZoneOffset.UTC);
        // Helix
        assertEquals(expected.toInstant(), TimeUtil.parseIso("2026-10-28T06:00:00Z").toInstant());
        // written by Joda: new DateTime(DateTimeZone.UTC).toString()
        assertEquals(expected.plusNanos(123_000_000).toInstant(), TimeUtil.parseIso("2026-10-28T06:00:00.123Z").toInstant());
        // old Joda parser "yyyy-MM-dd'T'HH:mm:ss.SSSZ" with a numeric offset
        assertEquals(expected.plusNanos(123_000_000).toInstant(), TimeUtil.parseIso("2026-10-28T07:00:00.123+0100").toInstant());
        // what is written now can be read again
        String now = TimeUtil.isoNowUtc();
        assertEquals(now, TimeUtil.parseIso(now).toInstant().toString());
    }
}
