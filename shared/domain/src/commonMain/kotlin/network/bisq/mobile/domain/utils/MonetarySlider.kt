package network.bisq.mobile.domain.utils

import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.faceValueToLong

import kotlin.math.roundToLong

/**
 * Helpers to convert between slider fractions [0f..1f] and monetary amounts represented
 * as minor units (Long) with a given step (default: 10,000 for fiat with 4 decimals).
 *
 * All calculations use Double/Long to avoid float precision drift at large magnitudes.
 */
object MonetarySlider {
    /**
     * Maps a slider fraction in [0f..1f] to a minor-unit amount within [minAmount, maxAmount].
     * Values are rounded to the nearest step.
     */
    fun fractionToAmountLong(
        fraction: Float,
        minAmount: Long,
        maxAmount: Long,
        step: Long = 10_000L
    ): Long {
        require(step > 0L) { "step must be > 0" }
        require(maxAmount >= minAmount) { "maxAmount must be >= minAmount" }
        if (maxAmount == minAmount) return minAmount

        val clamped = fraction.coerceIn(0f, 1f)
        val rangeD = (maxAmount - minAmount).toDouble()
        val valueD = minAmount.toDouble() + (clamped.toDouble() * rangeD)
        val rounded = (valueD / step.toDouble()).roundToLong() * step
        return rounded.coerceIn(minAmount, maxAmount)
    }

    /**
     * Converts a face value (e.g., 7525000.0) to a slider fraction in [0f..1f]
     * using exact minor-units conversion to avoid rounding drift.
     */
    fun faceValueToFraction(
        faceValue: Double,
        minAmount: Long,
        maxAmount: Long,
        precision: Int = 4
    ): Float {
        val minor = FiatVOFactory.faceValueToLong(faceValue, precision)
        return minorToFraction(minor, minAmount, maxAmount)
    }

    /**
     * Converts a minor-unit amount to a slider fraction in [0f..1f].
     */
    fun minorToFraction(
        minorAmount: Long,
        minAmount: Long,
        maxAmount: Long
    ): Float {
        val range = (maxAmount - minAmount).takeIf { it != 0L } ?: return 0f
        val inFraction = (minorAmount - minAmount).toDouble() / range.toDouble()
        return inFraction.toFloat()
    }
}

