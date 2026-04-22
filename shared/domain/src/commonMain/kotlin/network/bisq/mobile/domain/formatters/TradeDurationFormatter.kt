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

        val secStr = "temporal.second".i18nPlural(secPart.toInt())
        val minStr = "temporal.minute".i18nPlural(minPart.toInt())
        return when {
            days > 0L -> {
                val dayStr = "temporal.day".i18nPlural(days.toInt())
                val hourStr = "temporal.hour".i18nPlural(hoursPart.toInt())
                "$dayStr, $hourStr, $minStr, $secStr"
            }
            hours > 0L -> {
                val hourStr = "temporal.hour".i18nPlural(hours.toInt())
                "$hourStr, $minStr, $secStr"
            }
            else -> "$minStr, $secStr"
        }
    }
}
