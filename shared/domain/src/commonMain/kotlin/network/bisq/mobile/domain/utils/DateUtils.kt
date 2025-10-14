package network.bisq.mobile.domain.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until
import network.bisq.mobile.domain.formatDateTime
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.i18n.i18nPlural

object DateUtils {

    // Allow clock injection for testing
    internal var clock: Clock = Clock.System

    fun now() = clock.now().toEpochMilliseconds()

    /**
     * @return years, months, days past since timestamp
     */
    fun periodFrom(timetamp: Long): Triple<Int, Int, Int> {
        val creationInstant = Instant.fromEpochMilliseconds(timetamp)
        val creationDate = creationInstant.toLocalDateTime(TimeZone.UTC).date
        val currentDate = clock.now().toLocalDateTime(TimeZone.UTC).date

        // Calculate the difference
        val period = creationDate.until(currentDate, DateTimeUnit.DAY)
        val years = period / 365
        val remainingDaysAfterYears = period % 365
        val months = remainingDaysAfterYears / 30
        val days = remainingDaysAfterYears % 30

        // Format the result
        return Triple(years, months, days)
    }

    /**
     * Calculate and format the time elapsed since the given timestamp with proper i18n
     * @param epochMillis The timestamp in milliseconds since epoch
     * @return Formatted string like "3 min ago", "2 hours ago", etc.
     */
    fun lastSeen(epochMillis: Long): String {
        val lastActivityInstant = Instant.fromEpochMilliseconds(epochMillis)
        val currentInstant = clock.now()

        val durationInSeconds = lastActivityInstant
            .until(currentInstant, DateTimeUnit.SECOND)
            .coerceAtLeast(0)

        // Treat "now" as online instead of "0 sec ago"
        if (durationInSeconds == 0L) return "temporal.online".i18n()

        return when {
            durationInSeconds < 60L -> "mobile.temporal.second".i18nPlural(durationInSeconds.toInt())
            durationInSeconds < 3_600L -> "mobile.temporal.minute".i18nPlural((durationInSeconds / 60).toInt())
            durationInSeconds < 86_400L -> "mobile.temporal.hour".i18nPlural((durationInSeconds / 3_600).toInt())
            durationInSeconds < 2_592_000L -> "mobile.temporal.dayAgo".i18nPlural((durationInSeconds / 86_400).toInt()) // ~30 days
            durationInSeconds < 31_536_000L -> "mobile.temporal.monthAgo".i18nPlural((durationInSeconds / 2_592_000).toInt()) // ~365 days
            else -> "mobile.temporal.yearAgo".i18nPlural((durationInSeconds / 31_536_000).toInt())
        }
    }

    fun toDateTime(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val localDateTime = instant.toLocalDateTime(timeZone)
        return formatDateTime(localDateTime)
    }

    /**
     * Format profile age with proper i18n and pluralization
     * @param profileAgeTimestamp The timestamp in milliseconds since epoch
     * @return Formatted string like "2 years, 3 months, 5 days" or "less than a day"
     */
    fun formatProfileAge(profileAgeTimestamp: Long): String {
        val (years, months, days) = periodFrom(profileAgeTimestamp)

        val parts = listOfNotNull(
            if (years > 0) "temporal.year".i18nPlural(years) else null,
            // months not avail in default properties
            if (months > 0) "mobile.temporal.month".i18nPlural(months) else null,
            if (days > 0) "temporal.day".i18nPlural(days) else null
        )

        return if (parts.isEmpty()) {
            "mobile.temporal.lessThanADay".i18n()
        } else {
            parts.joinToString(", ")
        }
    }
}