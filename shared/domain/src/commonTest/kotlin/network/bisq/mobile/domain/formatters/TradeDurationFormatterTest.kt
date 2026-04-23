package network.bisq.mobile.domain.formatters

import network.bisq.mobile.i18n.I18nSupport
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TradeDurationFormatterTest {
    private val takeOffer = 1_000_000L

    @BeforeTest
    fun setup() {
        I18nSupport.initialize("en")
    }

    @Test
    fun `formatAge returns empty when trade not completed`() {
        assertEquals("", TradeDurationFormatter.formatAge(tradeCompletedDate = null, takeOfferDate = takeOffer))
    }

    @Test
    fun `formatAge returns N_A when completed before take offer`() {
        assertEquals(
            "N/A",
            TradeDurationFormatter.formatAge(tradeCompletedDate = takeOffer - 1, takeOfferDate = takeOffer),
        )
    }

    @Test
    fun `formatAge formats zero duration`() {
        assertEquals(
            "0 min, 0 sec",
            TradeDurationFormatter.formatAge(tradeCompletedDate = takeOffer, takeOfferDate = takeOffer),
        )
    }

    @Test
    fun `formatAge formats sub minute`() {
        assertEquals(
            "0 min, 45 sec",
            TradeDurationFormatter.formatAge(tradeCompletedDate = takeOffer + 45_000, takeOfferDate = takeOffer),
        )
    }

    @Test
    fun `formatAge formats minutes and seconds only`() {
        assertEquals(
            "3 min, 20 sec",
            TradeDurationFormatter.formatAge(tradeCompletedDate = takeOffer + (3 * 60 + 20) * 1000L, takeOfferDate = takeOffer),
        )
    }

    @Test
    fun `formatAge formats hours branch`() {
        // 2 h 5 min 7 s
        val durationMs = ((2 * 3600) + (5 * 60) + 7) * 1000L
        assertEquals(
            "2 hours, 5 min, 7 sec",
            TradeDurationFormatter.formatAge(tradeCompletedDate = takeOffer + durationMs, takeOfferDate = takeOffer),
        )
    }

    @Test
    fun `formatAge formats exactly twenty four hours as one day`() {
        val durationMs = 24L * 3600L * 1000L
        assertEquals(
            "1 day, 0 hours, 0 min, 0 sec",
            TradeDurationFormatter.formatAge(tradeCompletedDate = takeOffer + durationMs, takeOfferDate = takeOffer),
        )
    }

    @Test
    fun `formatAge formats multiple days with plural`() {
        val durationMs = 48L * 3600L * 1000L
        assertEquals(
            "2 days, 0 hours, 0 min, 0 sec",
            TradeDurationFormatter.formatAge(tradeCompletedDate = takeOffer + durationMs, takeOfferDate = takeOffer),
        )
    }

    @Test
    fun `formatAge day branch includes residual hours minutes seconds`() {
        // 1 day + 2 h + 3 min + 4 s
        val durationMs = (24L * 3600L + 2L * 3600L + 3L * 60L + 4L) * 1000L
        assertEquals(
            "1 day, 2 hours, 3 min, 4 sec",
            TradeDurationFormatter.formatAge(tradeCompletedDate = takeOffer + durationMs, takeOfferDate = takeOffer),
        )
    }

    @Test
    fun `formatAge day branch zero hours still uses minute and second remainders`() {
        // 1 day + 0 h + 3 min + 4 s
        val durationMs = (24L * 3600L + 3L * 60L + 4L) * 1000L
        assertEquals(
            "1 day, 0 hours, 3 min, 4 sec",
            TradeDurationFormatter.formatAge(tradeCompletedDate = takeOffer + durationMs, takeOfferDate = takeOffer),
        )
    }

    @Test
    fun `formatAge hours branch zero minutes still uses second remainder`() {
        // 2 h + 0 min + 7 s
        val durationMs = (2L * 3600L + 7L) * 1000L
        assertEquals(
            "2 hours, 0 min, 7 sec",
            TradeDurationFormatter.formatAge(tradeCompletedDate = takeOffer + durationMs, takeOfferDate = takeOffer),
        )
    }
}
