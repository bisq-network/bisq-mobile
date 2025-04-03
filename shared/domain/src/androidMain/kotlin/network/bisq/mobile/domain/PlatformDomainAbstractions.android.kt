@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package network.bisq.mobile.domain

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import com.russhwolf.settings.Settings
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import java.util.Locale
import java.util.Properties
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

actual fun formatDateTime(dateTime: LocalDateTime): String {
    val kotlinTimeZone = TimeZone.currentSystemDefault()
    val instant = dateTime.toInstant(kotlinTimeZone) // Convert to Instant
    val date = Date(instant.toEpochMilliseconds()) // Convert to Java Date
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // Use the fully qualified name for Java's TimeZone
    formatter.timeZone = java.util.TimeZone.getDefault()

    return formatter.format(date)
}

actual fun encodeURIParam(param: String): String {
    return URLEncoder.encode(param, StandardCharsets.UTF_8.toString())
}

actual fun getPlatformSettings(): Settings {
    // For testing purposes, we can use an in-memory implementation
    return object : Settings {
        private val map = mutableMapOf<String, Any>()

        override val keys: Set<String>
            get() = map.keys
        override val size: Int
            get() = map.size

        override fun clear() = map.clear()

        override fun remove(key: String) { map.remove(key) }

        override fun hasKey(key: String): Boolean = map.containsKey(key)

        override fun putInt(key: String, value: Int) { map[key] = value }
        override fun getInt(key: String, defaultValue: Int): Int = map[key] as? Int ?: defaultValue
        override fun getIntOrNull(key: String): Int? {
            TODO("Not yet implemented")
        }

        override fun putLong(key: String, value: Long) { map[key] = value }
        override fun getLong(key: String, defaultValue: Long): Long = map[key] as? Long ?: defaultValue
        override fun getLongOrNull(key: String): Long? {
            TODO("Not yet implemented")
        }

        override fun putString(key: String, value: String) { map[key] = value }
        override fun getString(key: String, defaultValue: String): String = map[key] as? String ?: defaultValue
        override fun getStringOrNull(key: String): String? {
            TODO("Not yet implemented")
        }

        override fun putFloat(key: String, value: Float) { map[key] = value }
        override fun getFloat(key: String, defaultValue: Float): Float = map[key] as? Float ?: defaultValue
        override fun getFloatOrNull(key: String): Float? {
            TODO("Not yet implemented")
        }

        override fun putDouble(key: String, value: Double) { map[key] = value }
        override fun getDouble(key: String, defaultValue: Double): Double = map[key] as? Double ?: defaultValue
        override fun getDoubleOrNull(key: String): Double? {
            TODO("Not yet implemented")
        }

        override fun putBoolean(key: String, value: Boolean) { map[key] = value }
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = map[key] as? Boolean ?: defaultValue
        override fun getBooleanOrNull(key: String): Boolean? {
            TODO("Not yet implemented")
        }
    }
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
actual class PlatformImage(val bitmap: Bitmap) {
    actual fun serialize(): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    actual companion object {
        actual fun deserialize(data: ByteArray): PlatformImage {
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            return PlatformImage(bitmap)
        }
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

actual fun createHttpClient(json: Json): HttpClient {
    return HttpClient(OkHttp) {
        install(WebSockets)
        install(ContentNegotiation) {
            json(json)
        }
    }
}
