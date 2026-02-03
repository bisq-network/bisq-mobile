package network.bisq.mobile.domain.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MonetarySliderTest {
    private val min = 0L
    private val max = 100_000L // 10.0000 units
    private val step = 10_000L

    @Test
    fun zeroFraction_returnsMin() {
        val amount = MonetarySlider.fractionToAmountLong(0f, min, max, step)
        assertEquals(min, amount)
    }

    @Test
    fun oneFraction_returnsMax() {
        val amount = MonetarySlider.fractionToAmountLong(1f, min, max, step)
        assertEquals(max, amount)
    }

    @Test
    fun increasingFraction_increasesAmount() {
        val a = MonetarySlider.fractionToAmountLong(0.25f, min, max, step)
        val b = MonetarySlider.fractionToAmountLong(0.50f, min, max, step)
        val c = MonetarySlider.fractionToAmountLong(0.75f, min, max, step)
        assertTrue(a < b && b < c)
    }

    @Test
    fun `fractionToAmountLong clamps negative fraction to min`() {
        val amount = MonetarySlider.fractionToAmountLong(-0.5f, min, max, step)
        assertEquals(min, amount)
    }

    @Test
    fun `fractionToAmountLong clamps fraction above 1 to max`() {
        val amount = MonetarySlider.fractionToAmountLong(1.5f, min, max, step)
        assertEquals(max, amount)
    }

    @Test
    fun `fractionToAmountLong returns min when min equals max`() {
        val amount = MonetarySlider.fractionToAmountLong(0.5f, 50_000L, 50_000L, step)
        assertEquals(50_000L, amount)
    }

    @Test
    fun `fractionToAmountLong throws for zero step`() {
        assertFailsWith<IllegalArgumentException> {
            MonetarySlider.fractionToAmountLong(0.5f, min, max, 0L)
        }
    }

    @Test
    fun `fractionToAmountLong throws for negative step`() {
        assertFailsWith<IllegalArgumentException> {
            MonetarySlider.fractionToAmountLong(0.5f, min, max, -1L)
        }
    }

    @Test
    fun `fractionToAmountLong throws when max less than min`() {
        assertFailsWith<IllegalArgumentException> {
            MonetarySlider.fractionToAmountLong(0.5f, 100_000L, 50_000L, step)
        }
    }

    @Test
    fun `minorToFraction returns 0 for min amount`() {
        val fraction = MonetarySlider.minorToFraction(min, min, max)
        assertEquals(0f, fraction)
    }

    @Test
    fun `minorToFraction returns 1 for max amount`() {
        val fraction = MonetarySlider.minorToFraction(max, min, max)
        assertEquals(1f, fraction)
    }

    @Test
    fun `minorToFraction returns 0 when min equals max`() {
        val fraction = MonetarySlider.minorToFraction(50_000L, 50_000L, 50_000L)
        assertEquals(0f, fraction)
    }

    @Test
    fun `minorToFraction returns correct fraction for midpoint`() {
        val fraction = MonetarySlider.minorToFraction(50_000L, min, max)
        assertEquals(0.5f, fraction, 0.001f)
    }

    @Test
    fun `faceValueToFraction returns 0 for min face value`() {
        val fraction = MonetarySlider.faceValueToFraction(0.0, min, max, 4)
        assertEquals(0f, fraction)
    }

    @Test
    fun `faceValueToFraction returns 1 for max face value`() {
        val fraction = MonetarySlider.faceValueToFraction(10.0, min, max, 4)
        assertEquals(1f, fraction)
    }

    @Test
    fun `faceValueToFraction returns correct fraction for midpoint`() {
        val fraction = MonetarySlider.faceValueToFraction(5.0, min, max, 4)
        assertEquals(0.5f, fraction, 0.001f)
    }

    @Test
    fun `round trip fraction to amount and back`() {
        // Use a fraction that aligns with step boundaries for exact round-trip
        val originalFraction = 0.5f
        val amount = MonetarySlider.fractionToAmountLong(originalFraction, min, max, step)
        val resultFraction = MonetarySlider.minorToFraction(amount, min, max)
        assertEquals(originalFraction, resultFraction, 0.01f)
    }
}
