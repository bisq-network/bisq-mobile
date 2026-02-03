package network.bisq.mobile.domain.utils

import network.bisq.mobile.domain.utils.MathUtils.roundTo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MathUtilsTest {
    // Tolerance for floating-point comparisons
    private val tolerance = 1e-9

    @Test
    fun `roundTo zero places rounds to integer`() {
        assertEquals(3.0, 3.14159.roundTo(0), tolerance)
        assertEquals(4.0, 3.5.roundTo(0), tolerance)
        assertEquals(3.0, 3.4.roundTo(0), tolerance)
    }

    @Test
    fun `roundTo one place rounds correctly`() {
        assertEquals(3.1, 3.14159.roundTo(1), tolerance)
        assertEquals(3.2, 3.15.roundTo(1), tolerance)
        assertEquals(3.1, 3.14.roundTo(1), tolerance)
    }

    @Test
    fun `roundTo two places rounds correctly`() {
        assertEquals(3.14, 3.14159.roundTo(2), tolerance)
        assertEquals(3.15, 3.146.roundTo(2), tolerance)
        assertEquals(3.14, 3.144.roundTo(2), tolerance)
    }

    @Test
    fun `roundTo handles negative numbers`() {
        assertEquals(-3.14, (-3.14159).roundTo(2), tolerance)
        assertEquals(-3.15, (-3.146).roundTo(2), tolerance)
        assertEquals(-4.0, (-3.5).roundTo(0), tolerance)
    }

    @Test
    fun `roundTo handles zero`() {
        assertEquals(0.0, 0.0.roundTo(0), tolerance)
        assertEquals(0.0, 0.0.roundTo(2), tolerance)
        assertEquals(0.0, 0.001.roundTo(2), tolerance)
    }

    @Test
    fun `roundTo handles large precision`() {
        assertEquals(3.14159, 3.14159.roundTo(5), tolerance)
        assertEquals(3.141593, 3.1415926.roundTo(6), tolerance)
    }

    @Test
    fun `roundTo throws for negative places`() {
        assertFailsWith<IllegalArgumentException> {
            3.14.roundTo(-1)
        }
    }

    @Test
    fun `roundTo handles whole numbers`() {
        assertEquals(5.0, 5.0.roundTo(0), tolerance)
        assertEquals(5.0, 5.0.roundTo(2), tolerance)
    }

    @Test
    fun `roundTo handles very small numbers`() {
        assertEquals(0.001, 0.0014.roundTo(3), tolerance)
        assertEquals(0.002, 0.0015.roundTo(3), tolerance)
    }
}
