package network.bisq.mobile.domain.service.trades

import kotlin.test.Test
import kotlin.test.assertFalse
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
}
