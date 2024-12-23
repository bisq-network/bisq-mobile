package network.bisq.mobile.domain.parser

import network.bisq.mobile.domain.utils.getLogger

object PercentageParser {
    fun parse(value: String): Double {
        val trimmed = value.replace(",", ".")
            .replace("%", "")
            .trim()
        if (trimmed.isEmpty()) {
            return 0.0
        }

        try {
            return trimmed.toDouble() / 100
        } catch (e: NumberFormatException) {
            getLogger("").w { "Parsing $value failed with ${e.message}" }
            return 0.0
        }
    }
}