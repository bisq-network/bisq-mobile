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

    fun now() = Clock.System.now().toEpochMilliseconds()

    /**
     * @return years, months, days past since timestamp
     */
    fun periodFrom(timetamp: Long): Triple<Int, Int, Int> {
        val creationInstant = Instant.fromEpochMilliseconds(timetamp)
        val creationDate = creationInstant.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

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
     * Calculate and format the time elapsed since the given timestamp
     * @param epochMillis The timestamp in milliseconds since epoch
     * @return Formatted string like "3 min ago", "2 hours ago", etc.
     */
    fun lastSeen(epochMillis: Long): String {
        val lastActivityInstant = Instant.fromEpochMilliseconds(epochMillis)
        val currentInstant = Clock.System.now()
        
        val durationInSeconds = lastActivityInstant.until(currentInstant, DateTimeUnit.SECOND)
        
        return when {
            durationInSeconds < 60 -> "$durationInSeconds sec ago"
            durationInSeconds < 3600 -> "${durationInSeconds / 60} min ago"
            durationInSeconds < 86400 -> "${durationInSeconds / 3600} hours ago"
            durationInSeconds < 2592000 -> "${durationInSeconds / 86400} days ago" // ~30 days
            durationInSeconds < 31536000 -> "${durationInSeconds / 2592000} months ago" // ~365 days
            else -> "${durationInSeconds / 31536000} years ago"
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
            if (months > 0) "temporal.month".i18nPlural(months) else null,
            if (days > 0) "temporal.day".i18nPlural(days) else null
        )

        return if (parts.isEmpty()) {
            "temporal.lessThanADay".i18n()
        } else {
            parts.joinToString(", ")
        }
    }
}