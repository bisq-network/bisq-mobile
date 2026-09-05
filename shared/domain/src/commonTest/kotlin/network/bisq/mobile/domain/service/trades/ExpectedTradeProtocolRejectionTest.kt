package network.bisq.mobile.domain.service.trades

import network.bisq.mobile.i18n.I18nSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExpectedTradeProtocolRejectionTest {
    @Test
    fun `issue 148 over-length BTC address is expected`() {
        assertTrue(
            ExpectedTradeProtocolRejection.isExpected(
                "Bitcoin address length must not be longer than 62",
            ),
        )
    }

    @Test
    fun `issue 147 take-offer amount too high is expected`() {
        assertTrue(
            ExpectedTradeProtocolRejection.isExpected(
                "Takers (buyers) Bitcoin amount is too high. " +
                    "This can be caused by differences in the 2 traders market price or by an attempt by the taker " +
                    "to manipulate the price.",
            ),
        )
    }

    @Test
    fun `typed expected failures and the other verify prefixes are expected`() {
        assertTrue(ExpectedTradeProtocolRejection.isExpected("Lightning invoice length must not be longer than 1000"))
        assertTrue(ExpectedTradeProtocolRejection.isExpected("Bitcoin payment data must not be empty"))
        assertTrue(ExpectedTradeProtocolRejection.isExpected("Takers (sellers) Bitcoin amount is too low. more detail"))
        assertTrue(ExpectedTradeProtocolRejection.isExpected("Could not find matching offer in BisqEasyOfferbookChannel"))
        assertTrue(ExpectedTradeProtocolRejection.isExpected("Mediators do not match.\nMaker's mediator: x"))
    }

    @Test
    fun `unrelated or unexpected FSM text is not expected`() {
        assertFalse(ExpectedTradeProtocolRejection.isExpected("boom"))
        assertFalse(ExpectedTradeProtocolRejection.isExpected("peer boom"))
        assertFalse(ExpectedTradeProtocolRejection.isExpected("IllegalStateException: FSM invariant broken"))
        assertFalse(ExpectedTradeProtocolRejection.isExpected("basf"))
    }

    @Test
    fun `extractExpected returns raw protocol text from client and node wraps`() {
        val raw =
            "Takers (buyers) Bitcoin amount is too high. " +
                "This can be caused by differences in the 2 traders market price or by an attempt by the taker " +
                "to manipulate the price."
        assertEquals(raw, ExpectedTradeProtocolRejection.extractExpected(raw))
        assertEquals(
            raw,
            ExpectedTradeProtocolRejection.extractExpected("Failed to take offer: $raw"),
        )
        assertEquals(
            raw,
            ExpectedTradeProtocolRejection.extractExpected(
                "The trade failed: '$raw'\n\nStack trace: bisq.trade.exceptions.TradeProtocolException",
            ),
        )
        assertEquals(raw, ExpectedTradeProtocolRejection.extractExpected(restApiPeerRejectionBody(raw)))
        assertNull(ExpectedTradeProtocolRejection.extractExpected("boom"))
        assertNull(ExpectedTradeProtocolRejection.extractExpected("Failed to take offer: nope"))
    }

    @Test
    fun `fromThrowable unwraps the TradeRestApi peer-rejection 400 body`() {
        val raw =
            "Takers (buyers) Bitcoin amount is too high. " +
                "This can be caused by differences in the 2 traders market price or by an attempt by the taker " +
                "to manipulate the price."
        val extracted = ExpectedTradeProtocolRejection.fromThrowable(RuntimeException(restApiPeerRejectionBody(raw)))
        assertEquals(raw, extracted)
        assertFalse(extracted.contains("ErrorStackTrace"), extracted)
        assertFalse(extracted.contains("TradeProtocolException"), extracted)
    }

    @Test
    fun `fromThrowable walks wrappers with a null message to the protocol text`() {
        val raw =
            "Takers (buyers) Bitcoin amount is too high. " +
                "This can be caused by differences in the 2 traders market price or by an attempt by the taker " +
                "to manipulate the price."
        val wrapped = RuntimeException(null as String?, IllegalStateException(raw))
        assertEquals(raw, ExpectedTradeProtocolRejection.fromThrowable(wrapped))
        assertEquals("boom", ExpectedTradeProtocolRejection.fromThrowable(RuntimeException("boom")))
        assertTrue(ExpectedTradeProtocolRejection.fromThrowable(RuntimeException()).contains("RuntimeException"))
    }

    @Test
    fun `fromThrowable maps a timeout wrapper to the send-timed-out copy`() {
        class TimeoutException : RuntimeException()

        I18nSupport.initialize("en")
        assertTrue(ExpectedTradeProtocolRejection.isTimeout(TimeoutException()))
        assertTrue(ExpectedTradeProtocolRejection.isTimeout(RuntimeException(TimeoutException())))
        val mapped = ExpectedTradeProtocolRejection.fromThrowable(TimeoutException())
        assertFalse(mapped.contains("TimeoutException"), mapped)
        assertTrue(mapped.contains("timed out"), mapped)
    }

    /**
     * bisq2 [TradeRestApi.takeOffer] reports a peer rejection as a plain-text 400:
     * `Invalid input: An error occurred at the peers side at taking the offer: <msg>. ErrorStackTrace: <500 chars>`.
     * The core sentence already ends with `.`, so the body has `.. ErrorStackTrace:`.
     */
    private fun restApiPeerRejectionBody(raw: String): String =
        "Invalid input: An error occurred at the peers side at taking the offer: $raw. " +
            "ErrorStackTrace: bisq.trade.exceptions.TradeProtocolException: $raw" +
            "\n\tat bisq.trade.bisq_easy.protocol.BisqEasyProtocol.onMessage(BisqEasyProtocol.java:1)" +
            "\n\tat java.base/java.lang.Thread.run(Thread.java:1)"
}
