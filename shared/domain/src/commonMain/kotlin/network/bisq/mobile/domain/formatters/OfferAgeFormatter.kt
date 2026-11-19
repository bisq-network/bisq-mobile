package network.bisq.mobile.domain.formatters

import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.i18n.i18nPlural

object OfferAgeFormatter {
    private const val MILLIS_PER_DAY = 86_400_000L

    /**
     * "Today", "1 day" or "N days" since the offer was created, like desktop's
     * `TimeFormatter.formatAgeInDays`.
     *
     * Counts elapsed 24-hour periods, not calendar days: no time zone, so two devices always agree.
     * Not [DateUtils.periodFrom], which counts UTC calendar days. A creation date in the future
     * (clock skew) reads "Today"; one before the epoch is treated as the epoch, so the subtraction
     * cannot overflow.
     */
    fun formatAgeInDays(createdAtMillis: Long): String {
        val now = DateUtils.now()
        val days = ((now - createdAtMillis.coerceIn(0L, now)) / MILLIS_PER_DAY).toInt()
        return if (days == 0) "temporal.today".i18n() else "temporal.day".i18nPlural(days)
    }
}
