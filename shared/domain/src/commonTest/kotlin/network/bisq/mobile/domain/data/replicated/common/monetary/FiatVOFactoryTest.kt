package network.bisq.mobile.domain.data.replicated.common.monetary

import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.faceValueToLong
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.fromFaceValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FiatVOFactoryTest {
    @Test
    fun `from with all parameters creates correct FiatVO`() {
        val fiat = FiatVOFactory.from("USD", 10000L, "USD", 4, 2)
        assertEquals("USD", fiat.id)
        assertEquals(10000L, fiat.value)
        assertEquals("USD", fiat.code)
        assertEquals(4, fiat.precision)
        assertEquals(2, fiat.lowPrecision)
    }

    @Test
    fun `from with value and code creates FiatVO with default precision`() {
        val fiat = FiatVOFactory.from(10000L, "EUR")
        assertEquals("EUR", fiat.id)
        assertEquals(10000L, fiat.value)
        assertEquals("EUR", fiat.code)
        assertEquals(4, fiat.precision)
        assertEquals(2, fiat.lowPrecision)
    }

    @Test
    fun `from with value code and precision creates correct FiatVO`() {
        val fiat = FiatVOFactory.from(10000L, "GBP", 6)
        assertEquals("GBP", fiat.id)
        assertEquals(10000L, fiat.value)
        assertEquals("GBP", fiat.code)
        assertEquals(6, fiat.precision)
        assertEquals(2, fiat.lowPrecision)
    }

    @Test
    fun `fromFaceValue creates correct FiatVO`() {
        val fiat = FiatVOFactory.fromFaceValue(1.0, "USD")
        assertEquals("USD", fiat.code)
        assertEquals(10000L, fiat.value) // 1.0 * 10^4 = 10000
    }

    @Test
    fun `fromFaceValue handles decimal values`() {
        val fiat = FiatVOFactory.fromFaceValue(1.5, "USD")
        assertEquals(15000L, fiat.value) // 1.5 * 10^4 = 15000
    }

    @Test
    fun `faceValueToLong converts correctly with default precision`() {
        assertEquals(10000L, FiatVOFactory.faceValueToLong(1.0))
        assertEquals(15000L, FiatVOFactory.faceValueToLong(1.5))
        assertEquals(12345L, FiatVOFactory.faceValueToLong(1.2345))
    }

    @Test
    fun `faceValueToLong converts correctly with custom precision`() {
        assertEquals(100L, FiatVOFactory.faceValueToLong(1.0, 2))
        assertEquals(1000L, FiatVOFactory.faceValueToLong(1.0, 3))
    }

    @Test
    fun `faceValueToLong handles zero`() {
        assertEquals(0L, FiatVOFactory.faceValueToLong(0.0))
    }

    @Test
    fun `faceValueToLong throws for overflow value`() {
        assertFailsWith<RuntimeException> {
            FiatVOFactory.faceValueToLong(Double.MAX_VALUE)
        }
    }
}
