@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package network.bisq.mobile.domain

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import java.util.Locale
import java.util.Properties
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


actual fun encodeURIParam(param: String): String {
    return URLEncoder.encode(param, StandardCharsets.UTF_8.toString())
}

actual fun getPlatformSettings(): Settings {
    return Settings()
}

actual fun getDeviceLanguageCode(): String {
    return Locale.getDefault().language
}

actual fun setupUncaughtExceptionHandler(onCrash: () -> Unit) {
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        println("Uncaught exception on thread: ${thread.name}")
        throwable.printStackTrace()

        // TODO report to some sort non-survaillant crashlytics?

        // Let the UI react
        onCrash()
    }
}

class AndroidUrlLauncher(private val context: Context) : UrlLauncher {
    override fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}

class AndroidPlatformInfo : PlatformInfo {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatformInfo(): PlatformInfo = AndroidPlatformInfo()


actual fun loadProperties(fileName: String): Map<String, String> {
    val properties = Properties()
    val classLoader = Thread.currentThread().contextClassLoader
    val resource = classLoader?.getResourceAsStream(fileName)
        ?: throw IllegalArgumentException("Resource not found: $fileName")
    properties.load(resource)

    return properties.entries.associate { it.key.toString() to it.value.toString() }
}

@Serializable(with = PlatformImageSerializer::class)
actual class PlatformImage(val bitmap: ImageBitmap) {
    actual companion object {
        actual fun deserialize(data: ByteArray): PlatformImage {
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
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

actual val decimalFormatter: DecimalFormatter = object : DecimalFormatter {
    private val formatters: MutableMap<Int, DecimalFormat> = mutableMapOf()
    override fun format(value: Double, precision: Int): String {
        formatters.getOrPut(precision) { DecimalFormat(generatePattern(precision)) }
        return formatters[precision]!!.format(value)
    }

    private fun generatePattern(precision: Int): String {
        return if (precision > 0) {
            buildString {
                append("0.")
                repeat(precision) { append("0") }
            }
        } else {
            "0"
        }
    }
}
