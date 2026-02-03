package network.bisq.mobile.domain.data.replicated.common

import kotlin.test.Test
import kotlin.test.assertEquals

class MathTest {
    // Tolerance for floating-point comparisons
    private val eps = 1e-9

    // roundDouble tests
    @Test
    fun `roundDouble rounds to zero decimal places`() {
        assertEquals(3.0, roundDouble(3.14159, 0), eps)
        assertEquals(4.0, roundDouble(3.5, 0), eps)
        assertEquals(3.0, roundDouble(3.4, 0), eps)
    }

    @Test
    fun `roundDouble rounds to two decimal places`() {
        assertEquals(3.14, roundDouble(3.14159, 2), eps)
        assertEquals(3.15, roundDouble(3.146, 2), eps)
        assertEquals(3.14, roundDouble(3.144, 2), eps)
    }

    @Test
    fun `roundDouble handles negative numbers`() {
        assertEquals(-3.14, roundDouble(-3.14159, 2), eps)
        assertEquals(-3.15, roundDouble(-3.146, 2), eps)
    }

    @Test
    fun `roundDouble handles zero`() {
        assertEquals(0.0, roundDouble(0.0, 0), eps)
        assertEquals(0.0, roundDouble(0.0, 2), eps)
    }

    @Test
    fun `roundDouble handles large precision`() {
        assertEquals(3.14159, roundDouble(3.14159, 5), eps)
    }

    // scaleDownByPowerOf10 tests
    @Test
    fun `scaleDownByPowerOf10 scales down by power of 10`() {
        assertEquals(1.0, scaleDownByPowerOf10(100L, 2), eps)
        assertEquals(10.0, scaleDownByPowerOf10(1000L, 2), eps)
        assertEquals(0.01, scaleDownByPowerOf10(1L, 2), eps)
    }

    @Test
    fun `scaleDownByPowerOf10 handles zero precision`() {
        assertEquals(100.0, scaleDownByPowerOf10(100L, 0), eps)
        assertEquals(1.0, scaleDownByPowerOf10(1L, 0), eps)
    }

    @Test
    fun `scaleDownByPowerOf10 handles large values`() {
        assertEquals(1000000.0, scaleDownByPowerOf10(100000000L, 2), eps)
    }

    @Test
    fun `scaleDownByPowerOf10 handles satoshi to BTC conversion`() {
        // 100,000,000 satoshis = 1 BTC (8 decimal places)
        assertEquals(1.0, scaleDownByPowerOf10(100000000L, 8), eps)
        assertEquals(0.5, scaleDownByPowerOf10(50000000L, 8), eps)
        assertEquals(0.00000001, scaleDownByPowerOf10(1L, 8), eps)
    }

    @Test
    fun `scaleDownByPowerOf10 handles negative values`() {
        assertEquals(-1.0, scaleDownByPowerOf10(-100L, 2), eps)
        assertEquals(-0.01, scaleDownByPowerOf10(-1L, 2), eps)
    }

    @Test
    fun `scaleDownByPowerOf10 handles zero value`() {
        assertEquals(0.0, scaleDownByPowerOf10(0L, 2), eps)
        assertEquals(0.0, scaleDownByPowerOf10(0L, 8), eps)
    }
}
