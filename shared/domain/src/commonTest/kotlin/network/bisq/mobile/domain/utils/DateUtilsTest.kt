package network.bisq.mobile.domain.utils

import kotlinx.datetime.Clock
import network.bisq.mobile.i18n.I18nSupport
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DateUtilsTest {

    @BeforeTest
    fun setupI18n() {
        I18nSupport.initialize("en")
    }

    @Test
    fun `formatProfileAge should return less than a day for very recent timestamp`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val result = DateUtils.formatProfileAge(now)
        assertEquals("less than a day", result)
    }

    @Test
    fun `formatProfileAge should return less than a day for timestamp within same day`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val hoursAgo = now - (12 * 60 * 60 * 1000) // 12 hours ago in milliseconds
        val result = DateUtils.formatProfileAge(hoursAgo)
        assertEquals("less than a day", result)
    }

    @Test
    fun `formatProfileAge should format single day correctly`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val oneDayAgo = now - (24 * 60 * 60 * 1000) // 1 day ago in milliseconds
        val result = DateUtils.formatProfileAge(oneDayAgo)
        assertEquals("1 day", result)
    }

    @Test
    fun `formatProfileAge should format multiple days correctly`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val fiveDaysAgo = now - (5 * 24 * 60 * 60 * 1000) // 5 days ago in milliseconds
        val result = DateUtils.formatProfileAge(fiveDaysAgo)
        assertEquals("5 days", result)
    }

    @Test
    fun `formatProfileAge should format single month correctly`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val oneMonthAgo = now - (35 * 24 * 60 * 60 * 1000L) // ~1 month, 5 days ago in milliseconds
        val result = DateUtils.formatProfileAge(oneMonthAgo)
        assertTrue(result.contains("1 month"))
        assertTrue(result.contains("5 days"))
    }

    @Test
    fun `formatProfileAge should format multiple months correctly`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val twoMonthsAgo = now - (65 * 24 * 60 * 60 * 1000L) // ~2 months, 5 days ago in milliseconds
        val result = DateUtils.formatProfileAge(twoMonthsAgo)
        assertTrue(result.contains("2 months"))
        assertTrue(result.contains("5 days"))
    }

    @Test
    fun `formatProfileAge should format single year correctly`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val oneYearAgo = now - (400 * 24 * 60 * 60 * 1000L) // ~1 year, 1 month, 5 days ago in milliseconds
        val result = DateUtils.formatProfileAge(oneYearAgo)
        assertTrue(result.contains("1 year"))
        assertTrue(result.contains("1 month"))
        assertTrue(result.contains("5 days"))
    }

    @Test
    fun `formatProfileAge should format multiple years correctly`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val twoYearsAgo = now - (800 * 24 * 60 * 60 * 1000L) // ~2 years, 2 months, 10 days ago in milliseconds
        val result = DateUtils.formatProfileAge(twoYearsAgo)
        assertTrue(result.contains("2 years"))
        assertTrue(result.contains("2 months"))
        assertTrue(result.contains("10 days"))
    }

    @Test
    fun `formatProfileAge should handle exact year boundary`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val exactlyOneYear = now - (365 * 24 * 60 * 60 * 1000L) // exactly 1 year ago in milliseconds
        val result = DateUtils.formatProfileAge(exactlyOneYear)
        assertEquals("1 year", result)
    }

    @Test
    fun `formatProfileAge should handle exact month boundary`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val exactlyOneMonth = now - (30 * 24 * 60 * 60 * 1000L) // exactly 1 month ago in milliseconds
        val result = DateUtils.formatProfileAge(exactlyOneMonth)
        assertEquals("1 month", result)
    }

    @Test
    fun `periodFrom should calculate correct periods`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val testTimestamp = now - (400 * 24 * 60 * 60 * 1000L) // ~1 year, 1 month, 5 days ago in milliseconds

        val (years, months, days) = DateUtils.periodFrom(testTimestamp)

        assertEquals(1, years)
        assertEquals(1, months)
        assertEquals(5, days)
    }
}
