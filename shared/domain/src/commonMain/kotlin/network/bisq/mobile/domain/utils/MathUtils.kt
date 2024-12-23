package network.bisq.mobile.domain.utils

import kotlin.math.pow
import kotlin.math.round

object MathUtils {
    fun Double.roundTo(places: Int): Double {
        require(places >= 0) { "Decimal places must be non-negative" }
        val factor = 10.0.pow(places)
        return round(this * factor) / factor
    }
}