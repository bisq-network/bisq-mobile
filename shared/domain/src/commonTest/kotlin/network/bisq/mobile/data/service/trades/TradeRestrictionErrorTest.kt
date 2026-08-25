package network.bisq.mobile.data.service.trades

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TradeRestrictionErrorTest {
    // Verbatim texts from bisq2 BisqEasyTradeService.verifyMinVersionForTrading() /
    // verifyTradingNotOnHalt(); classification matches on fragments of these.
    private val minVersionMessage =
        "For trading you need to have version 2.1.12 installed. " +
            "The Bisq security manager has published an emergency alert with a min. version required for trading."
    private val haltMessage =
        "Trading is on halt for security reasons. " +
            "The Bisq security manager has published an emergency alert with haltTrading set to true"

    @Test
    fun minVersionMessage_isClassifiedWithExtractedVersion() {
        assertEquals(
            TradeRestrictionError.MinVersionRequired("2.1.12"),
            TradeRestrictionError.fromMessage(minVersionMessage),
        )
    }

    @Test
    fun minVersionMessage_wrappedByApiErrorPrefix_isStillClassified() {
        // In client mode the text arrives as a 400 body prefixed by the node's input validation.
        assertEquals(
            TradeRestrictionError.MinVersionRequired("2.1.12"),
            TradeRestrictionError.fromMessage("Invalid input: $minVersionMessage"),
        )
    }

    @Test
    fun haltTradingMessage_isClassified() {
        assertEquals(
            TradeRestrictionError.TradingHalted,
            TradeRestrictionError.fromMessage(haltMessage),
        )
    }

    @Test
    fun minVersionFragmentWithoutExtractableVersion_isNotClassified() {
        assertNull(
            TradeRestrictionError.fromMessage(
                "An emergency alert with a min. version required for trading is active.",
            ),
        )
    }

    @Test
    fun unrelatedMessage_isNotClassified() {
        assertNull(TradeRestrictionError.fromMessage("Connection reset by peer"))
    }

    @Test
    fun nullMessage_isNotClassified() {
        assertNull(TradeRestrictionError.fromMessage(null))
    }
}
