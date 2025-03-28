package network.bisq.mobile.domain.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until
import network.bisq.mobile.domain.formatDateTime
import network.bisq.mobile.i18n.i18n

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

    //todo used for last user activity which should be in format: "3 min, 22 sec ago"
    fun lastSeen(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val localDateTime = instant.toLocalDateTime(timeZone)
        return localDateTime.toString()
            .split(".")[0] // remove ms
            .replace("T", " ") // separate date time
    }

    fun toDateTime(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val localDateTime = instant.toLocalDateTime(timeZone)
        return formatDateTime(localDateTime)
    }

}