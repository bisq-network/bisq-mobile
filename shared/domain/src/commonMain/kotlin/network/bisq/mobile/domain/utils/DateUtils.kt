package network.bisq.mobile.domain.utils

import network.bisq.mobile.data.utils.formatDateTime
import network.bisq.mobile.data.utils.formatMediumDateTime
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.i18n.i18nPlural
import kotlin.time.Clock

/**
 * Date handling for the shared domain, deliberately free of kotlinx-datetime.
 *
 * kotlinx-datetime maps to java.time on Android, which only exists from API 26. clientApp has
 * minSdk 24 and ships without core library desugaring, so any kotlinx-datetime call crashed on API
 * 24 and 25 devices with `NoClassDefFoundError: Ljava/time/LocalDateTime;`. Everything here is
 * therefore plain epoch-millis arithmetic, with the calendar work delegated to the platform
 * formatters in `PlatformDomainAbstractions` (SimpleDateFormat on Android, NSDateFormatter on iOS).
 *
 * Before reintroducing a date library, check that it does not reach for java.time, or enable
 * desugaring in apps/clientApp/build.gradle.kts. `DateUtilsCharacterizationTest` pins the output of
 * every function against the values the previous kotlinx-datetime implementation produced.
 */
object DateUtils {
    private const val MILLIS_PER_DAY = 86_400_000L

    // 0001-01-01T00:00:00Z and 9999-12-31T23:59:59.999Z. Timestamps are clamped to this range so a
    // corrupt or hostile value cannot overflow the elapsed-millis subtraction, truncate the year
    // count when it is narrowed to Int, or push a formatter into an absurd calendar year.
    //
    // The clamp is deliberately silent — no log, no exception. These functions run on a hot path
    // (once per visible row per recomposition), so logging here would need rate limiting to be
    // affordable, and a bad timestamp is a data problem that the layer receiving it should report,
    // not something the renderer can act on. The accepted trade-off is that a corrupt value shows
    // up as a nonsense date on screen rather than in the logs; if that ever needs investigating,
    // validate at ingestion instead of unpicking it here.
    // kotlinx-datetime used to provide this bound implicitly by saturating at the Instant limits,
    // so the clamp also keeps the pre-removal behaviour rather than introducing a new policy.
    private const val MIN_TIMESTAMP = -62_135_596_800_000L
    private const val MAX_TIMESTAMP = 253_402_300_799_999L

    // Allow clock injection for testing
    internal var clock: Clock = Clock.System

    fun now() = clock.now().toEpochMilliseconds()

    private fun Long.clampToSupportedRange() = coerceIn(MIN_TIMESTAMP, MAX_TIMESTAMP)

    /**
     * @return years, months, days past since timestamp
     */
    fun periodFrom(timestamp: Long): Triple<Int, Int, Int> {
        // Epoch day in UTC; floorDiv keeps pre-epoch timestamps on the correct day
        val creationDay = timestamp.clampToSupportedRange().floorDiv(MILLIS_PER_DAY)
        val currentDay = clock.now().toEpochMilliseconds().floorDiv(MILLIS_PER_DAY)

        // Calculate the difference
        val period = currentDay - creationDay
        val years = (period / 365).toInt()
        val remainingDaysAfterYears = period % 365
        val months = (remainingDaysAfterYears / 30).toInt()
        val days = (remainingDaysAfterYears % 30).toInt()

        // Format the result
        return Triple(years, months, days)
    }

    /**
     * Calculate and format the time elapsed since the given timestamp with proper i18n
     * @param epochMillis The timestamp in milliseconds since epoch
     * @return Formatted string like "3 min ago", "2 hours ago", etc.
     */
    fun lastSeen(epochMillis: Long): String {
        val durationInSeconds =
            ((clock.now().toEpochMilliseconds() - epochMillis.clampToSupportedRange()) / 1_000)
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

    /**
     * @param timeZoneId IANA zone id, or null for the system default zone
     */
    fun toDateTime(
        epochMillis: Long,
        timeZoneId: String? = null,
    ): String = formatDateTime(epochMillis.clampToSupportedRange(), timeZoneId)

    /**
     * @param timeZoneId IANA zone id, or null for the system default zone
     */
    fun toMediumDateTime(
        epochMillis: Long,
        timeZoneId: String? = null,
        includeSeconds: Boolean = false,
    ): String = formatMediumDateTime(epochMillis.clampToSupportedRange(), timeZoneId, includeSeconds)

    /**
     * Format profile age with proper i18n and pluralization
     * @param profileAgeTimestamp The timestamp in milliseconds since epoch
     * @return Formatted string like "2 years, 3 months, 5 days" or "less than a day"
     */
    fun formatProfileAge(profileAgeTimestamp: Long): String {
        val (years, months, days) = periodFrom(profileAgeTimestamp)

        val parts =
            listOfNotNull(
                if (years > 0) "temporal.year".i18nPlural(years) else null,
                // months not avail in default properties
                if (months > 0) "mobile.temporal.month".i18nPlural(months) else null,
                if (days > 0) "temporal.day".i18nPlural(days) else null,
            )

        return if (parts.isEmpty()) {
            "mobile.temporal.lessThanADay".i18n()
        } else {
            parts.joinToString(", ")
        }
    }
}
