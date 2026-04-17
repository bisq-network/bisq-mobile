package network.bisq.mobile.domain.formatters

import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.i18n.i18nPlural

object TradeDurationFormatter {
    fun formatAge(
        tradeCompletedDate: Long?,
        takeOfferDate: Long,
    ): String {
        if (tradeCompletedDate == null) return ""
        val duration = tradeCompletedDate - takeOfferDate
        if (duration < 0L) return "data.na".i18n()

        val sec = duration / 1000L
        val min = sec / 60L
        val secPart = sec % 60L
        val hours = min / 60L
        val minPart = min % 60L
        val days = hours / 24L
        val hoursPart = hours % 24L

        return if (days > 0L) {
            val dayString = "temporal.day".i18nPlural(days.toInt())
            "$dayString, $hoursPart hours, $minPart min, $secPart sec"
        } else if (hours > 0L) {
            "$hours hours, $minPart min, $secPart sec"
        } else {
            "$minPart min, $secPart sec"
        }
    }
}
