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
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
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

actual fun formatDateTime(dateTime: LocalDateTime): String {
    val timeZone = TimeZone.currentSystemDefault()
    val instant = dateTime.toInstant(TimeZone.of(timeZone.id)) // Convert to Instant
    val date = Date(instant.toEpochMilliseconds()) // Convert to Java Date
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return formatter.format(date)
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

    override fun openUrl(url: String): Boolean {
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
    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.TRANSPARENT)
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

@Suppress("DEPRECATION") // suppresses Locale(language) constructor
actual fun setDefaultLocale(language: String) {
    // Use Locale.forLanguageTag to support BCP‑47 (e.g., "en-US").
    val locale = runCatching { Locale.forLanguageTag(language) }.getOrElse { Locale(language) }
    Locale.setDefault(locale)
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
