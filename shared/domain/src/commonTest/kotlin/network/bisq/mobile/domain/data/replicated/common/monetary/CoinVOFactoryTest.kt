package network.bisq.mobile.domain.data.replicated.common.monetary

import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVOFactory.bitcoinFrom
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVOFactory.faceValueToLong
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVOFactory.from
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVOFactory.fromFaceValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CoinVOFactoryTest {
    @Test
    fun `from with all parameters creates correct CoinVO`() {
        val coin = CoinVOFactory.from("BTC", 100000000L, "BTC", 8, 4)
        assertEquals("BTC", coin.id)
        assertEquals(100000000L, coin.value)
        assertEquals("BTC", coin.code)
        assertEquals(8, coin.precision)
        assertEquals(4, coin.lowPrecision)
    }

    @Test
    fun `from with value and code creates CoinVO with default precision`() {
        val coin = CoinVOFactory.from(100000000L, "BTC")
        assertEquals("BTC", coin.id)
        assertEquals(100000000L, coin.value)
        assertEquals("BTC", coin.code)
        assertEquals(8, coin.precision)
        assertEquals(4, coin.lowPrecision)
    }

    @Test
    fun `from with value code and precision creates correct CoinVO`() {
        val coin = CoinVOFactory.from(100000000L, "BTC", 6)
        assertEquals("BTC", coin.id)
        assertEquals(100000000L, coin.value)
        assertEquals("BTC", coin.code)
        assertEquals(6, coin.precision)
        assertEquals(4, coin.lowPrecision)
    }

    @Test
    fun `bitcoinFrom creates BTC CoinVO`() {
        val coin = CoinVOFactory.bitcoinFrom(100000000L)
        assertEquals("BTC", coin.code)
        assertEquals(100000000L, coin.value)
        assertEquals(8, coin.precision)
    }

    @Test
    fun `fromFaceValue creates correct CoinVO`() {
        val coin = CoinVOFactory.fromFaceValue(1.0, "BTC")
        assertEquals("BTC", coin.code)
        assertEquals(100000000L, coin.value) // 1.0 BTC = 100,000,000 satoshis
    }

    @Test
    fun `fromFaceValue handles decimal values`() {
        val coin = CoinVOFactory.fromFaceValue(0.5, "BTC")
        assertEquals(50000000L, coin.value) // 0.5 BTC = 50,000,000 satoshis
    }

    @Test
    fun `faceValueToLong converts correctly with default precision`() {
        assertEquals(100000000L, CoinVOFactory.faceValueToLong(1.0))
        assertEquals(50000000L, CoinVOFactory.faceValueToLong(0.5))
        assertEquals(1L, CoinVOFactory.faceValueToLong(0.00000001)) // 1 satoshi
    }

    @Test
    fun `faceValueToLong converts correctly with custom precision`() {
        assertEquals(100L, CoinVOFactory.faceValueToLong(1.0, 2))
        assertEquals(1000L, CoinVOFactory.faceValueToLong(1.0, 3))
    }

    @Test
    fun `faceValueToLong handles zero`() {
        assertEquals(0L, CoinVOFactory.faceValueToLong(0.0))
    }

    @Test
    fun `faceValueToLong throws for overflow value`() {
        assertFailsWith<RuntimeException> {
            CoinVOFactory.faceValueToLong(Double.MAX_VALUE)
        }
    }
}
