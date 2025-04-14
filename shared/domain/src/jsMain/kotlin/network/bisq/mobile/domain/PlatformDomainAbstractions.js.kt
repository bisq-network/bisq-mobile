@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package network.bisq.mobile.domain

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Sink
import okio.Source
import kotlin.js.Date

actual fun getSystemFileSystem(): FileSystem {
    // Create a minimal implementation for browser environments
    return BrowserFileSystem()
}

// Minimal implementation of FileSystem for browser environments
private class BrowserFileSystem() : FileSystem() {
    override fun listOrNull(path: Path): List<Path>? {
        throw UnsupportedOperationException("listOrNull not supported in browser environment")
    }

    override fun canonicalize(path: Path): Path {
        throw UnsupportedOperationException("canonicalize not supported in browser environment")
    }

    override fun appendingSink(file: Path, mustExist: Boolean): Sink {
        throw UnsupportedOperationException("appendingSink not supported in browser environment")
    }

    override fun atomicMove(source: Path, target: Path): Unit {
        throw UnsupportedOperationException("atomicMove not supported in browser environment")
    }

    override fun createDirectory(dir: Path, mustCreate: Boolean): Unit {
        throw UnsupportedOperationException("createDirectory not supported in browser environment")
    }

    override fun createSymlink(source: Path, target: Path): Unit {
        throw UnsupportedOperationException("createSymlink not supported in browser environment")
    }

    override fun delete(path: Path, mustExist: Boolean): Unit {
        throw UnsupportedOperationException("delete not supported in browser environment")
    }

    override fun list(dir: Path): List<Path> {
        throw UnsupportedOperationException("list not supported in browser environment")
    }

    override fun metadataOrNull(path: Path): okio.FileMetadata? {
        return null
    }

    override fun openReadOnly(file: Path): okio.FileHandle {
        throw UnsupportedOperationException("openReadOnly not supported in browser environment")
    }

    override fun openReadWrite(file: Path, mustCreate: Boolean, mustExist: Boolean): okio.FileHandle {
        throw UnsupportedOperationException("openReadWrite not supported in browser environment")
    }

    override fun sink(file: Path, mustCreate: Boolean): Sink {
        throw UnsupportedOperationException("sink not supported in browser environment")
    }

    override fun source(file: Path): Source {
        throw UnsupportedOperationException("source not supported in browser environment")
    }

    override fun toString(): String = "BrowserFileSystem (limited functionality)"
}

actual fun formatDateTime(dateTime: LocalDateTime): String {
    val kotlinTimeZone = TimeZone.currentSystemDefault()
    val instant = dateTime.toInstant(kotlinTimeZone)
    val jsDate = Date(instant.toEpochMilliseconds())

    return jsDate.toLocaleString(arrayOf(), object : Date.LocaleOptions {
        override var year: String? = "numeric"
        override var month: String? = "2-digit"
        override var day: String? = "2-digit"
        override var hour: String? = "2-digit"
        override var minute: String? = "2-digit"
        override var second: String? = "2-digit"
        override var weekday: String? = "short"
        override var era: String? = "short"
        override var formatMatcher: String? = "best fit"
        override var localeMatcher: String? = "best fit"
        // Use the same timezone that was used to create the instant
        override var timeZone: String? = kotlinTimeZone.id
        override var timeZoneName: String? = kotlinTimeZone.id
        override var hour12: Boolean? = true
    })
}

actual fun encodeURIParam(param: String): String {
    return js("encodeURIComponent")(param) as String
}

actual fun getPlatformSettings(): Settings {
    return StorageSettings(localStorage)
}

actual fun getDeviceLanguageCode(): String {
    return window.navigator.language.split("-")[0]
}

actual fun setupUncaughtExceptionHandler(onCrash: () -> Unit) {
    window.onerror = { message, _, _, _, error ->
        console.error("Uncaught exception: $message")
        if (error != null) {
            console.error(error)
        }
        onCrash()
        false // Let default handler run
    }

    window.onunhandledrejection = { event ->
        console.error("Unhandled promise rejection: ${event.reason}")
        onCrash()
        false // Let default handler run
    }
}

class WebUrlLauncher : UrlLauncher {
    override fun openUrl(url: String) {
        js("window.open(url, '_blank')")
    }
}

class WebPlatformInfo : PlatformInfo {
    override val name: String = "Web ${window.navigator.userAgent}"
}

actual fun getPlatformInfo(): PlatformInfo = WebPlatformInfo()

actual fun loadProperties(fileName: String): Map<String, String> {
    // In a web environment, we might load properties from a JSON file
    // For now, return an empty map or mock data
    return mapOf(
        "app.name" to "Bisq Web",
        "app.version" to "1.0.0"
    )
}

@Serializable(with = PlatformImageSerializer::class)
actual class PlatformImage(val dataUrl: String) {
    actual fun serialize(): ByteArray {
        // Convert data URL to ByteArray
        // Remove the data URL prefix (e.g., "data:image/png;base64,")
        val base64 = dataUrl.substringAfter("base64,")
        // Use a JS function to convert base64 to ByteArray
        return js("atob")(base64).unsafeCast<String>().encodeToByteArray()
    }

    actual companion object {
        actual fun deserialize(data: ByteArray): PlatformImage {
            // Convert ByteArray to data URL
            val base64 = js("btoa")(data.decodeToString()).unsafeCast<String>()
            return PlatformImage("data:image/png;base64,$base64")
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