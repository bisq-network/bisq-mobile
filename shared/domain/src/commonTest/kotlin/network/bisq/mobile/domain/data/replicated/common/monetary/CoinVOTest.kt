package network.bisq.mobile.domain.data.replicated.common.monetary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CoinVOTest {
    @Test
    fun `properties are accessible`() {
        val coin = CoinVO("BTC", 100000000L, "BTC", 8, 4)
        assertEquals("BTC", coin.id)
        assertEquals(100000000L, coin.value)
        assertEquals("BTC", coin.code)
        assertEquals(8, coin.precision)
        assertEquals(4, coin.lowPrecision)
    }

    @Test
    fun `round returns CoinVO with rounded value`() {
        val coin = CoinVO("BTC", 123456789L, "BTC", 8, 4)
        val rounded = coin.round(4)
        assertIs<CoinVO>(rounded)
    }

    @Test
    fun `data class equality works`() {
        val coin1 = CoinVO("BTC", 100000000L, "BTC", 8, 4)
        val coin2 = CoinVO("BTC", 100000000L, "BTC", 8, 4)
        assertEquals(coin1, coin2)
    }

    @Test
    fun `data class copy works`() {
        val original = CoinVO("BTC", 100000000L, "BTC", 8, 4)
        val copied = original.copy(value = 200000000L)
        assertEquals(200000000L, copied.value)
        assertEquals("BTC", copied.code)
    }

    @Test
    fun `implements MonetaryVO interface`() {
        val coin: MonetaryVO = CoinVO("BTC", 100000000L, "BTC", 8, 4)
        assertEquals("BTC", coin.code)
        assertEquals(100000000L, coin.value)
    }
}
