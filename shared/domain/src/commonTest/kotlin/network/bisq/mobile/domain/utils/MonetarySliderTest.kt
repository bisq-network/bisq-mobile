package network.bisq.mobile.domain.utils

import kotlin.test.Test
import kotlin.test.assertEquals
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
}

