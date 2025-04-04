@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package network.bisq.mobile.domain

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import kotlin.js.Date

actual fun getSystemFileSystem(): FileSystem {
    TODO("Not yet implemented")
}

actual fun getPlatformSettings(): Settings {
    return StorageSettings()
}

actual fun formatDateTime(dateTime: LocalDateTime): String {
    val instant = dateTime.toInstant(TimeZone.currentSystemDefault())
    val jsDate = Date(instant.toEpochMilliseconds())
    return jsDate.toLocaleString()
}

actual fun encodeURIParam(param: String): String {
    return js("encodeURIComponent")(param) as String
}

actual fun setupUncaughtExceptionHandler(onCrash: () -> Unit) {
    // In JS, we can use window.onerror
    js("window.onerror = function(message, source, lineno, colno, error) { onCrash(); return false; }")
}

actual fun getDeviceLanguageCode(): String {
    return js("navigator.language || navigator.userLanguage || 'en'") as String
}

class JsPlatformInfo : PlatformInfo {
    override val name: String = js("navigator.userAgent") as String
}

actual fun getPlatformInfo(): PlatformInfo = JsPlatformInfo()

actual fun loadProperties(fileName: String): Map<String, String> {
    // TODO empty map as a stub implementation, load properties from resource
    console.log("Loading properties from $fileName (stub implementation)")
    return emptyMap()
}

@Serializable(with = PlatformImageSerializer::class)
actual class PlatformImage(private val base64Data: String) {
    actual fun serialize(): ByteArray {
        // Convert base64 to ByteArray
        return js("atob")(base64Data).unsafeCast<String>().encodeToByteArray()
    }

    actual companion object {
        actual fun deserialize(data: ByteArray): PlatformImage {
            val base64 = js("btoa")(data.decodeToString()).unsafeCast<String>()
            return PlatformImage(base64)
        }
    }
}

actual val decimalFormatter: DecimalFormatter = object : DecimalFormatter {
    override fun format(value: Double, precision: Int): String {
        return value.toFixed(precision)
    }
}

private fun Double.toFixed(digits: Int): String {
    return this.asDynamic().toFixed(digits) as String
}

actual fun createHttpClient(json: Json): HttpClient {
    return HttpClient(Js) {
        install(WebSockets)
        install(ContentNegotiation) {
            json(json)
        }
    }
}