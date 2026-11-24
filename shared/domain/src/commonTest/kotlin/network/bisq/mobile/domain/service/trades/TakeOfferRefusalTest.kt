package network.bisq.mobile.domain.service.trades

import network.bisq.mobile.i18n.I18nSupport
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TakeOfferRefusalTest {
    private val reputationRefusal =
        "The seller's reputation score of 1900 is too low for the trade amount. The trade amount requires a " +
            "reputation score of 2200, the seller's score is below that even with the 5% tolerance " +
            "(at least 200 extra reputation points for a positive score)."

    @BeforeTest
    fun setUp() {
        I18nSupport.initialize("en")
    }

    @Test
    fun `classifies the error codes of a node with the structured take offer refusal`() {
        assertEquals(TakeOfferRefusal.SELLER_REPUTATION_TOO_LOW, TakeOfferRefusal.fromErrorCode("SELLER_REPUTATION_TOO_LOW"))
        assertEquals(TakeOfferRefusal.MARKET_PRICE_NOT_CURRENT, TakeOfferRefusal.fromErrorCode("MARKET_PRICE_NOT_CURRENT"))
        assertNull(TakeOfferRefusal.fromErrorCode("HALT_TRADING"))
        assertNull(TakeOfferRefusal.fromErrorCode(null))
    }

    @Test
    fun `classifies the core text of the seller reputation refusal also when wrapped`() {
        assertEquals(TakeOfferRefusal.SELLER_REPUTATION_TOO_LOW, TakeOfferRefusal.fromMessage(reputationRefusal))
        assertEquals(TakeOfferRefusal.SELLER_REPUTATION_TOO_LOW, TakeOfferRefusal.fromMessage("Invalid input: $reputationRefusal"))
        assertEquals(
            TakeOfferRefusal.SELLER_REPUTATION_TOO_LOW,
            TakeOfferRefusal.fromMessage("An error occurred at the peers side at taking the offer: $reputationRefusal"),
        )
    }

    @Test
    fun `classifies every market price refusal text of the core`() {
        listOf(
            "No market price available. market=EUR/BTC",
            "No USD market price available.",
            "Market price is outdated. marketPrice=MarketPrice(...)",
            "USD market price is outdated. marketPrice=MarketPrice(...)",
            "Market prices are not from the same time. fiatMarketPrice=...; usdMarketPrice=...",
            "Invalid input: Market price is outdated. marketPrice=MarketPrice(...)",
        ).forEach { message ->
            assertEquals(TakeOfferRefusal.MARKET_PRICE_NOT_CURRENT, TakeOfferRefusal.fromMessage(message), message)
        }
    }

    @Test
    fun `recognises its own translated message`() {
        TakeOfferRefusal.entries.forEach { refusal ->
            assertEquals(refusal, TakeOfferRefusal.fromMessage(refusal.message()))
        }
    }

    @Test
    fun `other texts are not a take offer refusal`() {
        assertNull(TakeOfferRefusal.fromMessage("Could not find matching offer"))
        assertNull(TakeOfferRefusal.fromMessage("Trading is on halt"))
        assertNull(TakeOfferRefusal.fromMessage(null))
    }

    @Test
    fun `the refusals are expected protocol rejections so analytics does not capture them`() {
        assertTrue(ExpectedTradeProtocolRejection.isExpected(reputationRefusal))
        assertTrue(ExpectedTradeProtocolRejection.isExpected("USD market price is outdated. marketPrice=..."))
    }
}
