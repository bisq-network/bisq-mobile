package network.bisq.mobile.domain.utils

import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Instant

/**
 * Pins the [DateUtils] formatting that depends on the JVM default locale and time zone, against the
 * values the previous kotlinx-datetime implementation produced. Lives in androidUnitTest because
 * only here can both defaults be fixed, and it also covers the per-thread formatter cache the
 * Android actuals use.
 */
class DateUtilsFormatAndroidTest {
    private val originalLocale = Locale.getDefault()
    private val originalTimeZone = java.util.TimeZone.getDefault()

    @BeforeTest
    fun setup() {
        Locale.setDefault(Locale.US)
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/New_York"))
    }

    @AfterTest
    fun tearDown() {
        Locale.setDefault(originalLocale)
        java.util.TimeZone.setDefault(originalTimeZone)
    }

    private fun millisOf(isoInstant: String) = Instant.parse(isoInstant).toEpochMilliseconds()

    @Test
    fun `toDateTime formats in the requested zone`() {
        assertEquals(
            "2024-01-15 12:00:00",
            DateUtils.toDateTime(millisOf("2024-01-15T12:00:00Z"), "UTC"),
        )
    }

    @Test
    fun `toDateTime falls back to the system default zone`() {
        assertEquals(
            "2024-01-15 07:00:00",
            DateUtils.toDateTime(millisOf("2024-01-15T12:00:00Z")),
        )
    }

    @Test
    fun `toMediumDateTime falls back to the system default zone`() {
        // The shape both production callers use: no explicit zone
        assertEquals(
            "Jan 15, 2024  07:00",
            DateUtils.toMediumDateTime(millisOf("2024-01-15T12:00:00Z")),
        )
    }

    @Test
    fun `toDateTime applies daylight saving time of the default zone`() {
        assertEquals(
            "2024-07-15 08:00:00",
            DateUtils.toDateTime(millisOf("2024-07-15T12:00:00Z")),
        )
    }

    @Test
    fun `toDateTime applies a sub hour zone offset`() {
        assertEquals(
            "2024-01-15 17:45:00",
            DateUtils.toDateTime(millisOf("2024-01-15T12:00:00Z"), "Asia/Kathmandu"),
        )
    }

    @Test
    fun `toDateTime handles the epoch`() {
        assertEquals("1970-01-01 00:00:00", DateUtils.toDateTime(0L, "UTC"))
    }

    @Test
    fun `toDateTime handles pre epoch timestamps`() {
        assertEquals(
            "1969-12-31 23:59:30",
            DateUtils.toDateTime(millisOf("1969-12-31T23:59:30Z"), "UTC"),
        )
    }

    @Test
    fun `toDateTime falls back to the default zone for an unknown zone id`() {
        // java.util.TimeZone.getTimeZone answers GMT for an unknown id; the actual detects that and
        // uses the device zone instead, so both platforms render the same thing for a bad id
        assertEquals(
            "2024-01-15 07:00:00",
            DateUtils.toDateTime(millisOf("2024-01-15T12:00:00Z"), "Not/AZone"),
        )
    }

    @Test
    fun `toDateTime still honours an explicit GMT zone id`() {
        assertEquals(
            "2024-01-15 12:00:00",
            DateUtils.toDateTime(millisOf("2024-01-15T12:00:00Z"), "GMT"),
        )
    }

    @Test
    fun `toMediumDateTime ignores the default locale`() {
        Locale.setDefault(Locale.GERMANY)
        assertEquals(
            "Jan 15, 2024  12:00",
            DateUtils.toMediumDateTime(millisOf("2024-01-15T12:00:00Z"), "UTC"),
        )
    }

    @Test
    fun `cached formatters follow a change of the default zone`() {
        val noonUtc = millisOf("2024-01-15T12:00:00Z")
        assertEquals("2024-01-15 07:00:00", DateUtils.toDateTime(noonUtc))

        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kathmandu"))

        assertEquals("2024-01-15 17:45:00", DateUtils.toDateTime(noonUtc))
    }

    @Test
    fun `cached formatters follow a change of the default locale`() {
        val noonUtc = millisOf("2024-01-15T12:00:00Z")
        val beforeLocaleChange = DateUtils.toDateTime(noonUtc, "UTC")
        assertEquals("2024-01-15 12:00:00", beforeLocaleChange)

        // The calendar is requested explicitly rather than relying on th-TH defaulting to Buddhist,
        // so the expected year does not depend on the platform's locale data
        Locale.setDefault(Locale.forLanguageTag("th-TH-u-ca-buddhist"))

        val afterLocaleChange = DateUtils.toDateTime(noonUtc, "UTC")
        assertNotEquals(beforeLocaleChange, afterLocaleChange, "a stale cached formatter would repeat the old year")
        assertEquals("2567-01-15 12:00:00", afterLocaleChange)
    }

    @Test
    fun `absurd timestamps are clamped to the supported range`() {
        // The clamp bound is 0001-01-01 proleptic Gregorian; SimpleDateFormat renders pre-1582
        // dates in the Julian calendar, which is two days behind
        assertEquals("0001-01-03 00:00:00", DateUtils.toDateTime(Long.MIN_VALUE, "UTC"))
        assertEquals("9999-12-31 23:59:59", DateUtils.toDateTime(Long.MAX_VALUE, "UTC"))
        assertEquals("Dec 31, 9999  23:59", DateUtils.toMediumDateTime(Long.MAX_VALUE, "UTC"))
    }
}
