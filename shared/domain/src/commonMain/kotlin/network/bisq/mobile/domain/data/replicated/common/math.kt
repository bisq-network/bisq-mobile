package network.bisq.mobile.domain.data.replicated.common

import kotlin.math.pow
import kotlin.math.round

fun roundDouble(value: Double, precision: Int): Double {
    val factor = 10.0.pow(precision)
    return round(value * factor) / factor
}

fun scaleDownByPowerOf10(value: Long, precision: Int): Double {
    return value.toDouble() / 10.0.pow(precision)
}