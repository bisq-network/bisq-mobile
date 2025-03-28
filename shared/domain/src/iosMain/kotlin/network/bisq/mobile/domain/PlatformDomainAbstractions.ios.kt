@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package network.bisq.mobile.domain

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings
import kotlinx.cinterop.*
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import platform.Foundation.*
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy
import kotlin.collections.set

import platform.Foundation.NSException
import platform.Foundation.NSSetUncaughtExceptionHandler
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.experimental.ExperimentalNativeApi
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters

actual fun formatDateTime(dateTime: LocalDateTime): String {
    val formatter = NSDateFormatter().apply {
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterShortStyle
        locale = NSLocale.currentLocale
    }

    val instant = dateTime.toInstant(TimeZone.currentSystemDefault())
    val nsDate = NSDate(instant.toEpochMilliseconds() / 1000.0)

    return formatter.stringFromDate(nsDate)
}

@OptIn(BetaInteropApi::class)
actual fun encodeURIParam(param: String): String {
    return NSString.create(string = param)
        .stringByAddingPercentEncodingWithAllowedCharacters(NSCharacterSet.URLPathAllowedCharacterSet)
        ?: param
}

@OptIn(ExperimentalSettingsImplementation::class)
actual fun getPlatformSettings(): Settings {
    // TODO we might get away just using normal Settings() KMP agnostic implementation,
    // leaving this here to be able to choose the specific one for iOS - defaulting to KeyChain
    return KeychainSettings("Settings")
}

actual fun getDeviceLanguageCode(): String {
    return NSLocale.currentLocale.languageCode ?: "en"
}

private var globalOnCrash: (() -> Unit)? = null
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@Throws(Exception::class)
actual fun setupUncaughtExceptionHandler(onCrash: () -> Unit) {
    // TODO this catches the exceptions but let them go through crashing the app, whether in android it will stop the propagation
    globalOnCrash = onCrash
    NSSetUncaughtExceptionHandler(staticCFunction { exception: NSException? ->
        if (exception != null) {
            println("Uncaught exception: ${exception.name}, reason: ${exception.reason}")
            println("Stack trace: ${exception.callStackSymbols.joinToString("\n")}")

            // TODO report to some sort non-survaillant crashlytics?

            // Let the UI react
            globalOnCrash?.invoke()

            // needed on iOS
            dispatch_async(dispatch_get_main_queue()) {
                println("Performing cleanup after uncaught exception")
            }
        }
//        setUnhandledExceptionHook { throwable ->
//            println("Uncaught Kotlin exception: ${throwable.message}")
//            throwable.printStackTrace()
//
//            // Perform cleanup on the main thread
//            dispatch_async(dispatch_get_main_queue()) {
//                println("Performing cleanup after uncaught Kotlin exception")
//                globalOnCrash?.invoke()
//            }
//        }
    })
}

class IOSUrlLauncher : UrlLauncher {
    override fun openUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null) {
            // fake secondary parameters are important so that iOS compiler knows which override to use
            UIApplication.sharedApplication.openURL(nsUrl, options = mapOf<Any?, String>(), completionHandler = null)
        }
    }
}

class IOSPlatformInfo : PlatformInfo {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatformInfo(): PlatformInfo = IOSPlatformInfo()

actual fun loadProperties(fileName: String): Map<String, String> {
    val bundle = NSBundle.mainBundle
    /*val path = bundle.pathForResource(fileName.removeSuffix(".properties"), "properties")
        ?: throw IllegalArgumentException("Resource not found: $fileName")*/
    val path = bundle.pathForResource(fileName.removeSuffix(".properties"), "properties")
    // FIXME resources not found yet
    if (path == null) {
        return emptyMap()
    }

    val properties = NSDictionary.dictionaryWithContentsOfFile(path) as NSDictionary?
        ?: throw IllegalStateException("Failed to load properties from $path")

    return properties.entriesAsMap()
}

fun NSDictionary.entriesAsMap(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    val keys = this.allKeys as List<*> // `allKeys` provides a list of keys
    for (key in keys) {
        val keyString = key.toString()
        val valueString = this.objectForKey(key).toString()
        map[keyString] = valueString
    }
    return map
}

@Serializable(with = PlatformImageSerializer::class)
actual class PlatformImage(val image: UIImage) {
    actual fun serialize(): ByteArray {
        val nsData: NSData = UIImagePNGRepresentation(image)!!
        return nsData.toByteArray()
    }

    companion actual object {
        actual fun deserialize(data: ByteArray): PlatformImage {
            val nsData = data.toNSData()
            val image = UIImage(data = nsData)!!
            return PlatformImage(image)
        }
    }
}

// Helper extensions for NSData conversion:
@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val byteArray = ByteArray(this.length.toInt())
    byteArray.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return byteArray
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun ByteArray.toNSData(): NSData {
    return NSData.create(bytes = this.refTo(0).getPointer(MemScope()), length = this.size.toULong())
}

actual val decimalFormatter: DecimalFormatter = object : DecimalFormatter {
    override fun format(value: Double, precision: Int): String {
        val pattern = "%.${precision}f"
        return NSString.stringWithFormat(pattern, value)
    }
}
