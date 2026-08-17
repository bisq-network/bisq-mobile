package network.bisq.mobile.domain.utils

import network.bisq.mobile.i18n.I18nSupport
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Pins the observable behaviour of [DateUtils] so the kotlinx-datetime removal can be verified
 * against the values the kotlinx-based implementation produced.
 */
class DateUtilsCharacterizationTest {
    private val originalClock = DateUtils.clock
    private val fixedInstant = Instant.parse("2024-01-15T12:00:00Z")
    private val now = fixedInstant.toEpochMilliseconds()
    private val fixedClock =
        object : Clock {
            override fun now(): Instant = fixedInstant
        }

    @BeforeTest
    fun setup() {
        I18nSupport.initialize("en")
        DateUtils.clock = fixedClock
    }

    @AfterTest
    fun tearDown() {
        DateUtils.clock = originalClock
    }

    private fun millisOf(isoInstant: String) = Instant.parse(isoInstant).toEpochMilliseconds()

    private fun daysAgo(days: Long) = now - days * 86_400_000L

    // -------------------- periodFrom: UTC calendar-day arithmetic --------------------

    @Test
    fun `periodFrom returns zero period for the current instant`() {
        assertEquals(Triple(0, 0, 0), DateUtils.periodFrom(now))
    }

    @Test
    fun `periodFrom counts UTC day boundaries crossed rather than elapsed 24h spans`() {
        // 13 hours earlier, but on the previous UTC day
        assertEquals(Triple(0, 0, 1), DateUtils.periodFrom(millisOf("2024-01-14T23:00:00Z")))
    }

    @Test
    fun `periodFrom ignores time of day within the same UTC day`() {
        assertEquals(Triple(0, 0, 0), DateUtils.periodFrom(millisOf("2024-01-15T00:00:00Z")))
    }

    @Test
    fun `periodFrom treats 365 days as exactly one year`() {
        assertEquals(Triple(1, 0, 0), DateUtils.periodFrom(daysAgo(365)))
    }

    @Test
    fun `periodFrom folds 364 days into 12 months and 4 days`() {
        // Documents the 365-day year over 30-day month approximation
        assertEquals(Triple(0, 12, 4), DateUtils.periodFrom(daysAgo(364)))
    }

    @Test
    fun `periodFrom handles multi year spans`() {
        assertEquals(Triple(2, 0, 0), DateUtils.periodFrom(daysAgo(730)))
    }

    @Test
    fun `periodFrom returns negative components for future timestamps`() {
        assertEquals(Triple(-1, -1, -5), DateUtils.periodFrom(daysAgo(-400)))
    }

    @Test
    fun `periodFrom handles the epoch`() {
        assertEquals(Triple(54, 0, 27), DateUtils.periodFrom(0L))
    }

    @Test
    fun `periodFrom handles pre epoch timestamps`() {
        assertEquals(Triple(54, 0, 28), DateUtils.periodFrom(millisOf("1969-12-31T12:00:00Z")))
    }

    @Test
    fun `periodFrom clamps an absurdly old timestamp to year 1`() {
        assertEquals(Triple(2024, 4, 19), DateUtils.periodFrom(Long.MIN_VALUE))
    }

    @Test
    fun `periodFrom clamps an absurdly future timestamp to year 9999`() {
        assertEquals(Triple(-7981, -3, -4), DateUtils.periodFrom(Long.MAX_VALUE))
    }

    // -------------------- lastSeen: unit thresholds --------------------

    @Test
    fun `lastSeen reports online below one second`() {
        assertEquals("Online", DateUtils.lastSeen(now - 999))
    }

    @Test
    fun `lastSeen reports online for future timestamps`() {
        assertEquals("Online", DateUtils.lastSeen(daysAgo(-365)))
    }

    @Test
    fun `lastSeen switches units at the second boundaries`() {
        assertEquals("1 sec ago", DateUtils.lastSeen(now - 1_000))
        assertEquals("59 sec ago", DateUtils.lastSeen(now - 59_000))
        assertEquals("1 min ago", DateUtils.lastSeen(now - 60_000))
    }

    @Test
    fun `lastSeen switches units at the minute boundary`() {
        assertEquals("59 min ago", DateUtils.lastSeen(now - 3_599_000))
        assertEquals("1 hour ago", DateUtils.lastSeen(now - 3_600_000))
    }

    @Test
    fun `lastSeen switches units at the hour boundary`() {
        assertEquals("23 hours ago", DateUtils.lastSeen(now - 86_399_000))
        assertEquals("1 day ago", DateUtils.lastSeen(now - 86_400_000))
    }

    @Test
    fun `lastSeen switches units at the 30 day boundary`() {
        assertEquals("29 days ago", DateUtils.lastSeen(now - 2_591_999_000))
        assertEquals("1 month ago", DateUtils.lastSeen(now - 2_592_000_000))
    }

    @Test
    fun `lastSeen switches units at the 365 day boundary`() {
        assertEquals("12 months ago", DateUtils.lastSeen(now - 31_535_999_000))
        assertEquals("1 year ago", DateUtils.lastSeen(now - 31_536_000_000))
    }

    @Test
    fun `lastSeen truncates sub second remainders`() {
        assertEquals("1 min ago", DateUtils.lastSeen(now - 60_999))
    }

    @Test
    fun `lastSeen handles the epoch`() {
        assertEquals("54 years ago", DateUtils.lastSeen(0L))
    }

    @Test
    fun `lastSeen handles pre epoch timestamps`() {
        assertEquals("54 years ago", DateUtils.lastSeen(-1_000L))
    }

    @Test
    fun `lastSeen clamps an absurdly old timestamp to year 1 instead of overflowing`() {
        assertEquals("2024 years ago", DateUtils.lastSeen(Long.MIN_VALUE))
    }

    @Test
    fun `lastSeen reports online for an absurdly future timestamp`() {
        assertEquals("Online", DateUtils.lastSeen(Long.MAX_VALUE))
    }

    // -------------------- toMediumDateTime: fixed English format --------------------

    @Test
    fun `toMediumDateTime formats a single digit day without padding`() {
        assertEquals(
            "Jan 5, 2024  09:07",
            DateUtils.toMediumDateTime(millisOf("2024-01-05T09:07:03Z"), "UTC"),
        )
    }

    @Test
    fun `toMediumDateTime pads hours minutes and seconds`() {
        assertEquals(
            "Jan 5, 2024  09:07:03",
            DateUtils.toMediumDateTime(millisOf("2024-01-05T09:07:03Z"), "UTC", includeSeconds = true),
        )
    }

    @Test
    fun `toMediumDateTime abbreviates every month the way kotlinx did`() {
        // September is the one that differs between abbreviation sets (Sep vs Sept)
        val abbreviations =
            (1..12).map { month ->
                val padded = month.toString().padStart(2, '0')
                DateUtils.toMediumDateTime(millisOf("2024-$padded-01T00:00:00Z"), "UTC").substringBefore(' ')
            }
        assertEquals(
            listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
            abbreviations,
        )
    }

    @Test
    fun `toMediumDateTime handles the epoch`() {
        assertEquals("Jan 1, 1970  00:00", DateUtils.toMediumDateTime(0L, "UTC"))
    }

    @Test
    fun `toMediumDateTime handles pre epoch timestamps`() {
        assertEquals(
            "Dec 31, 1969  23:59:30",
            DateUtils.toMediumDateTime(millisOf("1969-12-31T23:59:30Z"), "UTC", includeSeconds = true),
        )
    }

    @Test
    fun `toMediumDateTime applies a negative zone offset`() {
        assertEquals(
            "Jan 15, 2024  07:00",
            DateUtils.toMediumDateTime(now, "America/New_York"),
        )
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
        assertEquals(
            "Jan 15, 2024  17:45",
            DateUtils.toMediumDateTime(now, "Asia/Kathmandu"),
        )
    }

    @Test
    fun `toMediumDateTime shifts the year when the zone crosses new year`() {
        assertEquals(
            "Dec 31, 2023  19:30",
            DateUtils.toMediumDateTime(millisOf("2024-01-01T00:30:00Z"), "America/New_York"),
        )
    }
}
