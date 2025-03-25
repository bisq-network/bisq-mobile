package network.bisq.mobile.domain.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until
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

        val month = localDateTime.month.name.lowercase().replaceFirstChar { it.uppercaseChar() }.take(3)
        val day = localDateTime.dayOfMonth
        val year = localDateTime.year

        val hour24 = localDateTime.hour
        val minute = localDateTime.minute
        // TODO support non US formats as well
        val (hour12, ampm) = when {
            hour24 == 0 -> 12 to "AM"
            hour24 < 12 -> hour24 to "AM"
            hour24 == 12 -> 12 to "PM"
            else -> (hour24 - 12) to "PM"
        }

        val paddedMinute = minute.toString().padStart(2, '0')
        val atString = "temporal.at".i18n();
        return "$month $day, $year $atString $hour12:$paddedMinute $ampm"
    }

}