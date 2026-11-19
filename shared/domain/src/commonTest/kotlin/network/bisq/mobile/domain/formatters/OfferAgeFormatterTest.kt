package network.bisq.mobile.domain.formatters

import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.i18n.I18nSupport
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class OfferAgeFormatterTest {
    private val originalClock = DateUtils.clock
    private val now = Instant.parse("2024-01-15T12:00:00Z").toEpochMilliseconds()
    private val fixedClock =
        object : Clock {
            override fun now(): Instant = Instant.fromEpochMilliseconds(now)
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
    fun `an offer created now is Today`() {
        assertEquals("Today", OfferAgeFormatter.formatAgeInDays(now))
    }

    @Test
    fun `an offer just under 24 hours old is still Today`() {
        assertEquals("Today", OfferAgeFormatter.formatAgeInDays(now - DAY + MINUTE))
    }

    @Test
    fun `an offer exactly 24 hours old is 1 day`() {
        assertEquals("1 day", OfferAgeFormatter.formatAgeInDays(now - DAY))
    }

    @Test
    fun `an offer just under 48 hours old is still 1 day`() {
        assertEquals("1 day", OfferAgeFormatter.formatAgeInDays(now - 2 * DAY + MINUTE))
    }

    @Test
    fun `an offer 48 hours old is 2 days`() {
        assertEquals("2 days", OfferAgeFormatter.formatAgeInDays(now - 2 * DAY))
    }

    @Test
    fun `an old offer stays in days and never switches to years`() {
        assertEquals("812 days", OfferAgeFormatter.formatAgeInDays(now - 812 * DAY))
    }

    @Test
    fun `an offer dated in the future is Today`() {
        // Clock skew between the maker and this device must never produce a negative age.
        assertEquals("Today", OfferAgeFormatter.formatAgeInDays(now + DAY))
    }

    @Test
    fun `an absurd creation date does not overflow`() {
        val ageAtEpoch = OfferAgeFormatter.formatAgeInDays(0L)

        assertEquals(ageAtEpoch, OfferAgeFormatter.formatAgeInDays(Long.MIN_VALUE))
        assertEquals("Today", OfferAgeFormatter.formatAgeInDays(Long.MAX_VALUE))
    }

    private companion object {
        const val MINUTE = 60_000L
        const val DAY = 86_400_000L
    }
}
