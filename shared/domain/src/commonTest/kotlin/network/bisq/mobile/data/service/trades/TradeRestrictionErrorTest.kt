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

    // Verbatim text from bisq2 TradeRestrictedException.makerVersionBelowMinRequired().
    private val makerVersionMessage =
        "The maker of this offer uses a Bisq version below 2.1.14. " +
            "The Bisq security manager has published an emergency alert which requires this version for " +
            "trading, so the maker cannot accept the take offer request."

    @Test
    fun makerVersionMessage_isNeverClassifiedAsOwnMinVersion() {
        // MinVersionRequired tells the user to update their own app or node, which is wrong here.
        assertNull(TradeRestrictionError.fromMessage(makerVersionMessage))
        assertNull(TradeRestrictionError.fromMessage("Invalid input: $makerVersionMessage"))
    }

    @Test
    fun haltTradingErrorCode_isClassified() {
        assertEquals(
            TradeRestrictionError.TradingHalted,
            TradeRestrictionError.fromErrorCode("HALT_TRADING", null),
        )
    }

    @Test
    fun minVersionErrorCode_isClassifiedWithVersion() {
        assertEquals(
            TradeRestrictionError.MinVersionRequired("2.1.14"),
            TradeRestrictionError.fromErrorCode("MIN_VERSION_REQUIRED", "2.1.14"),
        )
    }

    @Test
    fun makerVersionErrorCode_isClassifiedWithVersion() {
        assertEquals(
            TradeRestrictionError.MakerVersionBelowMinRequired("2.1.14"),
            TradeRestrictionError.fromErrorCode("MAKER_VERSION_BELOW_MIN_REQUIRED", "2.1.14"),
        )
    }

    @Test
    fun versionErrorCodesWithoutVersion_areNotClassified() {
        assertNull(TradeRestrictionError.fromErrorCode("MIN_VERSION_REQUIRED", null))
        assertNull(TradeRestrictionError.fromErrorCode("MAKER_VERSION_BELOW_MIN_REQUIRED", " "))
    }

    @Test
    fun unknownOrMissingErrorCode_isNotClassified() {
        assertNull(TradeRestrictionError.fromErrorCode("SOME_FUTURE_RESTRICTION", "2.1.14"))
        assertNull(TradeRestrictionError.fromErrorCode(null, null))
    }
}
