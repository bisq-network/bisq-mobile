@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package network.bisq.mobile.data.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.model.PlatformInfo
import network.bisq.mobile.domain.model.PlatformType
import network.bisq.mobile.domain.utils.getLogger
import org.koin.android.ext.koin.androidContext
import org.koin.core.scope.Scope
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.util.TimeZone

actual fun formatDateTime(
    epochMillis: Long,
    timeZoneId: String?,
): String = format(epochMillis, timeZoneId, "yyyy-MM-dd HH:mm:ss", Locale.getDefault())

actual fun formatMediumDateTime(
    epochMillis: Long,
    timeZoneId: String?,
    includeSeconds: Boolean,
): String {
    val pattern = if (includeSeconds) "MMM d, yyyy  HH:mm:ss" else "MMM d, yyyy  HH:mm"
    return format(epochMillis, timeZoneId, pattern, Locale.ENGLISH)
}

// Constructing a SimpleDateFormat dominates the cost of rendering a timestamp, and these run once
// per visible row per recomposition. SimpleDateFormat is not thread-safe, so the cache is per
// thread; the key carries locale and zone so an in-app language change or a device zone change is
// still picked up. Both patterns are explicit, so unlike the iOS styled formatter nothing here
// depends on a device date/time preference the key could miss. Zone ids come from callers, so the
// map is capped rather than trusted to stay small; dropping everything on overflow costs one
// rebuild.
private const val MAX_CACHED_FORMATTERS = 32

private val dateFormatters =
    object : ThreadLocal<MutableMap<String, SimpleDateFormat>>() {
        override fun initialValue() = mutableMapOf<String, SimpleDateFormat>()
    }

private fun format(
    epochMillis: Long,
    timeZoneId: String?,
    pattern: String,
    locale: Locale,
): String {
    val zone = resolveTimeZone(timeZoneId)
    val key = "$pattern|${locale.toLanguageTag()}|${zone.id}"
    val cache = dateFormatters.get()!!
    if (cache.size >= MAX_CACHED_FORMATTERS && key !in cache) cache.clear()
    val formatter =
        cache.getOrPut(key) {
            SimpleDateFormat(pattern, locale).apply { timeZone = zone }
        }
    return formatter.format(Date(epochMillis))
}

// TimeZone.getTimeZone silently answers GMT for an id it does not know, which would render a
// different wall clock than iOS does for the same bad input. Detect that by comparing ids and fall
// back to the device zone, matching resolveTimeZone in the iOS actual.
private fun resolveTimeZone(timeZoneId: String?): TimeZone {
    if (timeZoneId == null) return TimeZone.getDefault()
    val zone = TimeZone.getTimeZone(timeZoneId)
    return if (zone.id == timeZoneId) zone else TimeZone.getDefault()
}

actual fun encodeURIParam(param: String): String = Uri.encode(param)

actual fun getDeviceLanguageCode(): String = Locale.getDefault().language

actual fun setupUncaughtExceptionHandler(onCrash: (Throwable) -> Unit) {
    val originalHandler = Thread.getDefaultUncaughtExceptionHandler()

    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        println("Uncaught exception on thread: ${thread.name}")
        throwable.printStackTrace()
        try {
            // Call the error handler immediately on the current thread
            onCrash(throwable)
        } catch (e: Exception) {
            println("Error in exception handler: ${e.message}")
            e.printStackTrace()
        }
        // For non-main thread exceptions or if recovery failed, call original handler
        originalHandler?.uncaughtException(thread, throwable)
    }
}

class AndroidUrlLauncher(
    private val context: Context,
) : UrlLauncher {
    private val log = getLogger("AndroidUrlLauncher")

    override suspend fun openUrl(url: String): Boolean {
        if (tryOpenUrl(url)) return true
        val fallback = playStoreHttpsFallback(url) ?: return false
        log.w { "No handler for market:// URL; falling back to Play Store HTTPS listing" }
        return tryOpenUrl(fallback)
    }

    private fun tryOpenUrl(url: String): Boolean {
        val safeUrl = sanitizeUrlForLog(url)
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        try {
            context.startActivity(intent)
            return true
        } catch (_: ActivityNotFoundException) {
            log.w { "No activity found to handle URL (install a browser or check link): $safeUrl" }
            return false
        } catch (e: Exception) {
            log.e(e) { "Failed to open URL: $safeUrl" }
            return false
        }
    }

    private fun playStoreHttpsFallback(url: String): String? {
        val uri = runCatching { url.toUri() }.getOrNull() ?: return null
        if (uri.scheme != "market") return null
        val packageName = uri.getQueryParameter("id")?.takeIf { it.isNotBlank() } ?: return null
        return AppUpdateUrls.playStoreDetailsUrl(packageName)
    }

    private fun sanitizeUrlForLog(rawUrl: String): String {
        val uri = runCatching { rawUrl.toUri() }.getOrNull()
        return if (uri != null) {
            buildString {
                append(uri.scheme ?: "unknown")
                uri.host?.let { append("://").append(it) }
                uri.path?.let { append(it) }
            }.take(256)
        } else {
            "invalid-url"
        }
    }
}

class AndroidAppUpdateLinker(
    private val context: Context,
    private val installingPackageNameProvider: (Context) -> String? = ::resolveInstallingPackageName,
) : AppUpdateLinker {
    private companion object {
        private const val GOOGLE_PLAY_INSTALLER = "com.android.vending"
        private const val GOOGLE_PLAY_FEEDBACK = "com.google.android.feedback"

        fun resolveInstallingPackageName(context: Context): String? =
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    context.packageManager
                        .getInstallSourceInfo(context.packageName)
                        .installingPackageName
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getInstallerPackageName(context.packageName)
                }
            }.getOrNull()
    }

    override fun getUpdateUrl(): String =
        if (isGooglePlayInstall()) {
            AppUpdateUrls.playStoreMarketUrl(context.packageName)
        } else {
            AppUpdateUrls.GITHUB_RELEASES
        }

    private fun isGooglePlayInstall(): Boolean {
        val installer = runCatching { installingPackageNameProvider(context) }.getOrNull() ?: return false
        return installer == GOOGLE_PLAY_INSTALLER || installer == GOOGLE_PLAY_FEEDBACK
    }
}

class AndroidPlatformInfo : PlatformInfo {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val type = PlatformType.ANDROID
}

actual fun getPlatformInfo(): PlatformInfo = AndroidPlatformInfo()

actual fun loadProperties(fileName: String): Map<String, String> {
    val properties = Properties()
    val classLoader = Thread.currentThread().contextClassLoader
    val resource =
        classLoader?.getResourceAsStream(fileName)
            ?: throw IllegalArgumentException("Resource not found: $fileName")
    // Read .properties using UTF-8 to support non-ASCII characters consistently
    resource.reader(Charsets.UTF_8).use { reader ->
        properties.load(reader)
    }

    return properties.entries.associate { it.key.toString() to it.value.toString() }
}

@Serializable(with = PlatformImageSerializer::class)
actual class PlatformImage(
    val bitmap: ImageBitmap,
) {
    actual companion object {
        actual fun deserialize(data: ByteArray): PlatformImage {
            val bitmap =
                BitmapFactory.decodeByteArray(data, 0, data.size)
                    ?: throw IllegalArgumentException("Failed to decode image data")
            return PlatformImage(bitmap.asImageBitmap())
        }
    }

    actual fun serialize(): ByteArray {
        val androidBitmap = bitmap.asAndroidBitmap()
        val stream = ByteArrayOutputStream()
        androidBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}

actual fun createEmptyImage(): PlatformImage {
    // 16x16 neutral-grey square. Aligned with the iOS actual so the fallback path
    // produces the same predictable visual on both platforms (also serialises to a
    // valid PNG, unlike a 1x1 transparent image — see iOS comment).
    val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.GRAY)
    return PlatformImage(bitmap.asImageBitmap())
}

actual val decimalFormatter: DecimalFormatter =
    object : DecimalFormatter {
        private val formatters: MutableMap<Triple<Int, Locale, Boolean>, DecimalFormat> = mutableMapOf()

        override fun format(
            value: Double,
            precision: Int,
            useGrouping: Boolean,
        ): String {
            val locale = Locale.getDefault()
            val key = Triple(precision, locale, useGrouping)
            val formatter =
                formatters.getOrPut(key) {
                    val format = DecimalFormat(generatePattern(precision), DecimalFormatSymbols(locale))
                    format.isGroupingUsed = useGrouping
                    format
                }
            return formatter.format(value)
        }

        private fun generatePattern(precision: Int): String =
            if (precision > 0) {
                buildString {
                    append("#,##0.")
                    repeat(precision) { append("0") }
                }
            } else {
                "#,##0"
            }
    }

actual fun setDefaultLocale(language: String) {
    // Strict BCP‑47 parse — unlike forLanguageTag, rejects invalid/partial tags (e.g. "en-X").
    val locale =
        runCatching { Locale.Builder().setLanguageTag(language).build() }
            .getOrNull()
            ?.takeUnless { it.language.isEmpty() }
            ?: Locale.ENGLISH
    try {
        Locale.setDefault(locale)
    } catch (e: SecurityException) {
        // Preview/layoutlib forbids writing user.language; everywhere else must fail so
        // I18nSupport/NodeSettings do not publish a language that was not applied.
        if (!isComposePreviewLocaleSandbox(e)) throw e
    }
}

internal fun isComposePreviewLocaleSandbox(error: SecurityException): Boolean {
    if (error.javaClass.name.contains("RenderSecurity")) return true
    return error.stackTrace.any { frame ->
        val className = frame.className
        className.startsWith("com.android.tools.rendering") ||
            className.startsWith("com.android.layoutlib")
    }
}

actual fun getDecimalSeparator(): Char = DecimalFormatSymbols(Locale.getDefault()).decimalSeparator

actual fun getGroupingSeparator(): Char = DecimalFormatSymbols(Locale.getDefault()).groupingSeparator

actual fun String.toDoubleOrNullLocaleAware(): Double? {
    val trimmedString = this.trim()
    val format = NumberFormat.getInstance(Locale.getDefault())
    val parsePosition = ParsePosition(0)

    val parsedNumber = format.parse(trimmedString, parsePosition)

    return if (parsedNumber != null && parsePosition.index == trimmedString.length) {
        parsedNumber.toDouble()
    } else {
        null
    }
}

actual fun getLocaleCurrencyName(currencyCode: String): String {
    val javaLocale = Locale.getDefault()
    return runCatching {
        Currency.getInstance(currencyCode).getDisplayName(javaLocale)
    }.getOrElse {
        // Fallback gracefully when currency code is not recognized by the platform
        currencyCode
    }
}

actual fun Scope.getStorageDir(): String = androidContext().filesDir.absolutePath
