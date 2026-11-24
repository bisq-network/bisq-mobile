package network.bisq.mobile.data.replicated.common.util

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.RoundingMode
import kotlin.math.pow

/**
 * Rounds half away from zero on the decimal representation of [value], like bisq2's
 * `MathUtils.roundDouble` (`BigDecimal.valueOf(value).setScale(precision, HALF_UP)`), so that
 * amounts rounded here match the amounts the trusted node and the peer compute.
 */
fun roundDouble(
    value: Double,
    precision: Int,
): Double =
    BigDecimal
        .fromDouble(value)
        .roundToDigitPositionAfterDecimalPoint(precision.toLong(), RoundingMode.ROUND_HALF_AWAY_FROM_ZERO)
        .doubleValue(false)

fun scaleDownByPowerOf10(
    value: Long,
    precision: Int,
): Double = value.toDouble() / 10.0.pow(precision)
