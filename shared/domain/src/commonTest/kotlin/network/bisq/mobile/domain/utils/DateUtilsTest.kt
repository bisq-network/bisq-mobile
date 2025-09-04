package network.bisq.mobile.domain.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import network.bisq.mobile.i18n.I18nSupport
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DateUtilsTest {

    private val originalClock = DateUtils.clock
    private val fixedInstant = Instant.parse("2024-01-15T12:00:00Z")
    private val fixedClock = object : Clock {
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

    @Test
    fun `formatProfileAge should return less than a day for very recent timestamp`() {
        val result = DateUtils.formatProfileAge(fixedInstant.toEpochMilliseconds())
        assertEquals("less than a day", result)
    }

    @Test
    fun `formatProfileAge should return less than a day for timestamp within same day`() {
        val sameDay = LocalDate(2024, 1, 15).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        val result = DateUtils.formatProfileAge(sameDay)
        assertEquals("less than a day", result)
    }

    @Test
    fun `formatProfileAge should format single day correctly`() {
        val oneDayAgo = LocalDate(2024, 1, 14).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        val result = DateUtils.formatProfileAge(oneDayAgo)
        assertEquals("1 day", result)
    }

    @Test
    fun `formatProfileAge should format multiple days correctly`() {
        val fiveDaysAgo = LocalDate(2024, 1, 10).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        val result = DateUtils.formatProfileAge(fiveDaysAgo)
        assertEquals("5 days", result)
    }

    @Test
    fun `formatProfileAge should format single month correctly`() {
        // 35 days ago = 1 month, 5 days (using 30-day month approximation)
        val thirtyFiveDaysAgo = fixedInstant.toEpochMilliseconds() - (35 * 24 * 60 * 60 * 1000L)
        val result = DateUtils.formatProfileAge(thirtyFiveDaysAgo)
        assertTrue(result.contains("1 month"))
        assertTrue(result.contains("5 days"))
    }

    @Test
    fun `formatProfileAge should format multiple months correctly`() {
        // 65 days ago = 2 months, 5 days (using 30-day month approximation)
        val sixtyFiveDaysAgo = fixedInstant.toEpochMilliseconds() - (65 * 24 * 60 * 60 * 1000L)
        val result = DateUtils.formatProfileAge(sixtyFiveDaysAgo)
        assertTrue(result.contains("2 months"))
        assertTrue(result.contains("5 days"))
    }

    @Test
    fun `formatProfileAge should format single year correctly`() {
        // 400 days ago = 1 year, 1 month, 5 days (using 365-day year, 30-day month approximation)
        val fourHundredDaysAgo = fixedInstant.toEpochMilliseconds() - (400 * 24 * 60 * 60 * 1000L)
        val result = DateUtils.formatProfileAge(fourHundredDaysAgo)
        assertTrue(result.contains("1 year"))
        assertTrue(result.contains("1 month"))
        assertTrue(result.contains("5 days"))
    }

    @Test
    fun `formatProfileAge should format multiple years correctly`() {
        // 800 days ago = 2 years, 2 months, 10 days (using 365-day year, 30-day month approximation)
        val eightHundredDaysAgo = fixedInstant.toEpochMilliseconds() - (800 * 24 * 60 * 60 * 1000L)
        val result = DateUtils.formatProfileAge(eightHundredDaysAgo)
        assertTrue(result.contains("2 years"))
        assertTrue(result.contains("2 months"))
        assertTrue(result.contains("10 days"))
    }

    @Test
    fun `formatProfileAge should handle exact year boundary`() {
        // 365 days ago = exactly 1 year
        val exactlyOneYear = fixedInstant.toEpochMilliseconds() - (365 * 24 * 60 * 60 * 1000L)
        val result = DateUtils.formatProfileAge(exactlyOneYear)
        assertEquals("1 year", result)
    }

    @Test
    fun `formatProfileAge should handle exact month boundary`() {
        // 30 days ago = exactly 1 month
        val exactlyOneMonth = fixedInstant.toEpochMilliseconds() - (30 * 24 * 60 * 60 * 1000L)
        val result = DateUtils.formatProfileAge(exactlyOneMonth)
        assertEquals("1 month", result)
    }

    @Test
    fun `periodFrom should calculate correct periods`() {
        val testTimestamp = fixedInstant.toEpochMilliseconds() - (400 * 24 * 60 * 60 * 1000L) // 400 days ago

        val (years, months, days) = DateUtils.periodFrom(testTimestamp)

        // 400 days = 1 year (365 days) + 35 days remaining = 1 month (30 days) + 5 days
        assertEquals(1, years)
        assertEquals(1, months)
        assertEquals(5, days)
    }

    @Test
    fun `lastSeen should return online for current timestamp`() {
        val result = DateUtils.lastSeen(fixedInstant.toEpochMilliseconds())
        assertEquals("Online", result)
    }

    @Test
    fun `lastSeen should return online for future timestamp`() {
        val futureTimestamp = fixedInstant.toEpochMilliseconds() + (30 * 1000)
        val result = DateUtils.lastSeen(futureTimestamp)
        assertEquals("Online", result)
    }

    @Test
    fun `lastSeen should return localized seconds ago for recent activity`() {
        val thirtySecondsAgo = fixedInstant.toEpochMilliseconds() - (30 * 1000)
        val result = DateUtils.lastSeen(thirtySecondsAgo)
        assertEquals("30 sec ago", result)
    }

    @Test
    fun `lastSeen should return localized minutes ago for activity within hour`() {
        val fifteenMinutesAgo = fixedInstant.toEpochMilliseconds() - (15 * 60 * 1000)
        val result = DateUtils.lastSeen(fifteenMinutesAgo)
        assertEquals("15 min ago", result)
    }

    @Test
    fun `lastSeen should return localized hours ago for activity within day`() {
        val threeHoursAgo = fixedInstant.toEpochMilliseconds() - (3 * 60 * 60 * 1000)
        val result = DateUtils.lastSeen(threeHoursAgo)
        assertEquals("3 hours ago", result)
    }

    @Test
    fun `lastSeen should return localized days ago for activity within month`() {
        val fiveDaysAgo = fixedInstant.toEpochMilliseconds() - (5 * 24 * 60 * 60 * 1000L)
        val result = DateUtils.lastSeen(fiveDaysAgo)
        assertEquals("5 days ago", result)
    }

    @Test
    fun `lastSeen should return localized months ago for activity within year`() {
        val twoMonthsAgo = fixedInstant.toEpochMilliseconds() - (60 * 24 * 60 * 60 * 1000L)
        val result = DateUtils.lastSeen(twoMonthsAgo)
        assertEquals("2 months ago", result)
    }

    @Test
    fun `lastSeen should return localized years ago for old activity`() {
        val twoYearsAgo = fixedInstant.toEpochMilliseconds() - (2 * 365 * 24 * 60 * 60 * 1000L)
        val result = DateUtils.lastSeen(twoYearsAgo)
        assertEquals("2 years ago", result)
    }

    @Test
    fun `lastSeen should handle single unit correctly`() {
        val oneMinuteAgo = fixedInstant.toEpochMilliseconds() - (60 * 1000)
        val result = DateUtils.lastSeen(oneMinuteAgo)
        assertEquals("1 min ago", result)
    }
}
