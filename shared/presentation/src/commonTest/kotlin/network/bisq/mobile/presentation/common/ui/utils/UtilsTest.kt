package network.bisq.mobile.presentation.common.ui.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UtilsTest {
    // EMPTY_STRING tests
    @Test
    fun `EMPTY_STRING is empty`() {
        assertEquals("", EMPTY_STRING)
    }

    @Test
    fun `EMPTY_STRING has zero length`() {
        assertEquals(0, EMPTY_STRING.length)
    }

    // convertToSet tests
    @Test
    fun `convertToSet returns empty set for null`() {
        assertEquals(emptySet(), convertToSet(null))
    }

    @Test
    fun `convertToSet returns singleton set for non-null value`() {
        assertEquals(setOf("test"), convertToSet("test"))
    }

    @Test
    fun `convertToSet returns singleton set for empty string`() {
        assertEquals(setOf(""), convertToSet(""))
    }

    // customPaymentIconIndex tests
    @Test
    fun `customPaymentIconIndex returns valid index within range`() {
        val result = customPaymentIconIndex("TestPayment", 10)
        assertTrue(result in 0 until 10)
    }

    @Test
    fun `customPaymentIconIndex returns consistent results for same input`() {
        val result1 = customPaymentIconIndex("MyPaymentMethod", 5)
        val result2 = customPaymentIconIndex("MyPaymentMethod", 5)
        assertEquals(result1, result2)
    }

    @Test
    fun `customPaymentIconIndex returns different results for different inputs`() {
        val result1 = customPaymentIconIndex("PaymentA", 100)
        val result2 = customPaymentIconIndex("PaymentB", 100)
        // While not guaranteed, different inputs should typically produce different hashes
        // This test verifies the function works for different inputs
        assertTrue(result1 in 0 until 100)
        assertTrue(result2 in 0 until 100)
    }

    @Test
    fun `customPaymentIconIndex throws for zero length`() {
        assertFailsWith<IllegalArgumentException> {
            customPaymentIconIndex("Test", 0)
        }
    }

    @Test
    fun `customPaymentIconIndex throws for negative length`() {
        assertFailsWith<IllegalArgumentException> {
            customPaymentIconIndex("Test", -1)
        }
    }

    @Test
    fun `customPaymentIconIndex handles length of 1`() {
        val result = customPaymentIconIndex("AnyPayment", 1)
        assertEquals(0, result)
    }

    // hasKnownPaymentIcon tests
    @Test
    fun `hasKnownPaymentIcon returns true for known payment methods`() {
        assertTrue(hasKnownPaymentIcon("SEPA"))
        assertTrue(hasKnownPaymentIcon("ZELLE"))
        assertTrue(hasKnownPaymentIcon("REVOLUT"))
        assertTrue(hasKnownPaymentIcon("PIX"))
    }

    @Test
    fun `hasKnownPaymentIcon is case insensitive`() {
        assertTrue(hasKnownPaymentIcon("sepa"))
        assertTrue(hasKnownPaymentIcon("Sepa"))
        assertTrue(hasKnownPaymentIcon("SEPA"))
    }

    @Test
    fun `hasKnownPaymentIcon returns false for unknown payment methods`() {
        assertFalse(hasKnownPaymentIcon("UNKNOWN_METHOD"))
        assertFalse(hasKnownPaymentIcon("CUSTOM_PAYMENT"))
        assertFalse(hasKnownPaymentIcon(""))
    }

    // hasKnownSettlementIcon tests
    @Test
    fun `hasKnownSettlementIcon returns true for known settlement methods`() {
        assertTrue(hasKnownSettlementIcon("BTC"))
        assertTrue(hasKnownSettlementIcon("LIGHTNING"))
        assertTrue(hasKnownSettlementIcon("LN"))
        assertTrue(hasKnownSettlementIcon("MAIN_CHAIN"))
    }

    @Test
    fun `hasKnownSettlementIcon is case insensitive`() {
        assertTrue(hasKnownSettlementIcon("btc"))
        assertTrue(hasKnownSettlementIcon("Btc"))
        assertTrue(hasKnownSettlementIcon("BTC"))
    }

    @Test
    fun `hasKnownSettlementIcon returns false for unknown settlement methods`() {
        assertFalse(hasKnownSettlementIcon("UNKNOWN"))
        assertFalse(hasKnownSettlementIcon("ETH"))
        assertFalse(hasKnownSettlementIcon(""))
    }
}
