package network.bisq.mobile.domain.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * iOS side of the [DateUtils] formatting contract, which is served by NSDateFormatter since the
 * kotlinx-datetime removal (see the note in DateUtils).
 *
 * [DateUtils.toDateTime] is locale dependent by design, so its exact output cannot be pinned here
 * the way the Android test pins it: the simulator locale is whatever the run inherits. These tests
 * assert the parts that must hold on every device, plus the zone handling that only exists in the
 * iOS actual.
 */
class DateUtilsFormatIosTest {
    private fun millisOf(isoInstant: String) = Instant.parse(isoInstant).toEpochMilliseconds()

    private val noonUtc = millisOf("2024-01-15T12:00:00Z")

    @Test
    fun `toMediumDateTime uses the English fixed format regardless of device locale`() {
        assertEquals("Jan 15, 2024  12:00", DateUtils.toMediumDateTime(noonUtc, "UTC"))
        assertEquals("Jan 15, 2024  12:00:00", DateUtils.toMediumDateTime(noonUtc, "UTC", includeSeconds = true))
    }

    @Test
    fun `toMediumDateTime applies a negative zone offset`() {
        assertEquals("Jan 15, 2024  07:00", DateUtils.toMediumDateTime(noonUtc, "America/New_York"))
    }

    @Test
    fun `toMediumDateTime applies daylight saving time`() {
        assertEquals(
            "Jul 15, 2024  08:00",
            DateUtils.toMediumDateTime(millisOf("2024-07-15T12:00:00Z"), "America/New_York"),
        )
    }

    @Test
    fun `toMediumDateTime applies a sub hour zone offset`() {
        assertEquals("Jan 15, 2024  17:45", DateUtils.toMediumDateTime(noonUtc, "Asia/Kathmandu"))
    }

    @Test
    fun `toMediumDateTime handles the epoch and pre epoch timestamps`() {
        assertEquals("Jan 1, 1970  00:00", DateUtils.toMediumDateTime(0L, "UTC"))
        assertEquals(
            "Dec 31, 1969  23:59:30",
            DateUtils.toMediumDateTime(millisOf("1969-12-31T23:59:30Z"), "UTC", includeSeconds = true),
        )
    }

    @Test
    fun `toMediumDateTime falls back to the default zone for an unknown zone id`() {
        // NSTimeZone.timeZoneWithName returns null for an unknown id, so the device zone is used.
        // The Android actual detects its own GMT fallback and matches this.
        assertEquals(
            DateUtils.toMediumDateTime(noonUtc),
            DateUtils.toMediumDateTime(noonUtc, "Not/AZone"),
        )
    }

    @Test
    fun `toDateTime renders the date components of the requested zone`() {
        val result = DateUtils.toDateTime(noonUtc, "UTC")
        assertTrue(result.isNotEmpty(), "toDateTime should return a non-empty string")
        assertTrue(result.contains("2024"), "Result '$result' should contain year 2024")
        assertTrue(result.contains("15"), "Result '$result' should contain day 15")
    }

    @Test
    fun `toDateTime shifts the day when the zone crosses midnight`() {
        // 00:30 UTC is still the previous day in New York
        val newYear = millisOf("2024-01-01T00:30:00Z")
        assertTrue(DateUtils.toDateTime(newYear, "UTC").contains("2024"))
        assertTrue(DateUtils.toDateTime(newYear, "America/New_York").contains("2023"))
    }

    @Test
    fun `toDateTime handles the epoch`() {
        assertTrue(DateUtils.toDateTime(0L, "UTC").contains("1970"))
    }

    @Test
    fun `toDateTime handles pre epoch timestamps`() {
        assertTrue(DateUtils.toDateTime(millisOf("1969-12-31T23:59:30Z"), "UTC").contains("1969"))
    }

    @Test
    fun `absurd timestamps are clamped to the supported range`() {
        // Exact rendering of year 1 differs per formatter, so compare against the clamp bounds
        // rather than pinning a literal
        val minSupported = -62_135_596_800_000L // 0001-01-01T00:00:00Z
        val maxSupported = 253_402_300_799_999L // 9999-12-31T23:59:59.999Z
        assertEquals(DateUtils.toDateTime(minSupported, "UTC"), DateUtils.toDateTime(Long.MIN_VALUE, "UTC"))
        assertEquals(DateUtils.toDateTime(maxSupported, "UTC"), DateUtils.toDateTime(Long.MAX_VALUE, "UTC"))
        assertEquals(DateUtils.toMediumDateTime(minSupported, "UTC"), DateUtils.toMediumDateTime(Long.MIN_VALUE, "UTC"))
        assertEquals(DateUtils.toMediumDateTime(maxSupported, "UTC"), DateUtils.toMediumDateTime(Long.MAX_VALUE, "UTC"))
        assertTrue(DateUtils.toMediumDateTime(Long.MAX_VALUE, "UTC").contains("9999"))
    }
}
