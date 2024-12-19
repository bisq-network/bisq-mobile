package network.bisq.mobile.presentation.ui.helpers

import kotlin.math.roundToInt

fun Double.toStringWith2Decimals(): String {
    val roundedNumber = (this * 100).roundToInt() / 100.0
    val isNumberHasSingleZero = roundedNumber.toString().split(".").getOrNull(1)?.length == 1
    return if (isNumberHasSingleZero) "${roundedNumber}0" else roundedNumber.toString()
}